package com.attrigo.asapp.gradle

import java.io.File
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assumptions.assumingThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir

// The hooks every scenario installs, unless it asks for others
private val HOOK_NAMES = listOf("commit-msg", "pre-commit")

// Newlines are spelled \n so every platform gets identical files, and identical scripts compile once
private const val HOOK_SCRIPT = "#!/usr/bin/env bash\nexit 0\n"
private const val FIXTURE_SETTINGS_SCRIPT = "rootProject.name = \"fixture\"\n"
private const val FIXTURE_BUILD_SCRIPT = "plugins {\n    id(\"asapp.root-conventions\")\n}\n"
private const val GLOBAL_GIT_CONFIG_SCRIPT = "[user]\n" +
    "\tname = ASAPP build-logic test\n" +
    "\temail = build-logic-test@asapp.invalid\n" +
    "[init]\n" +
    "\tdefaultBranch = main\n"

private const val MULTI_PROJECT_SUBPROJECT_NAME = "sub"

// Differs from the script above, so it compiles separately — a cost only the multi-project scenario pays
private const val MULTI_PROJECT_FIXTURE_SETTINGS_SCRIPT = "rootProject.name = \"fixture\"\ninclude(\"$MULTI_PROJECT_SUBPROJECT_NAME\")\n"

/**
 * Tests [InstallGitHooks] against disposable git repositories, one per scenario.
 *
 * Each scenario runs a real Gradle build that applies the real convention plugin, so the task's
 * registration is checked along with its behavior. Everything here needs a live git repository,
 * which is why these are full builds rather than isolated unit tests.
 *
 * Coverage:
 * - Every file in the hooks directory is installed, executable, where git looks for it
 * - A worktree installs into the repository's shared hooks directory, not its own private one
 * - `core.hooksPath`, which sends git looking elsewhere, is cleared when the repository set it
 * - A value set outside the repository is left alone and only warned about
 * - A clear that fails, against a locked config file say, warns and blames that failure
 * - A repository that never set the value succeeds, treating "nothing to unset" as fine
 * - Unrelated files already in the hooks directory survive
 * - The task runs every time, and a second run reuses Gradle's configuration cache
 * - The task exists only on the build's root project, never on a subproject
 */
class InstallGitHooksFunctionalTest {

    // Shared by every scenario, and by any second test class — a different environment map restarts Gradle
    companion object {

        @JvmStatic
        @TempDir
        lateinit var globalGitConfigDirectory: File

        private val globalGitConfigFile: File
            get() = globalGitConfigDirectory.resolve("gitconfig")
    }

    @TempDir
    lateinit var workspace: File

    private val repositoryDirectory: File
        get() = workspace.resolve("repository")

    private val linkedWorktreeDirectory: File
        get() = workspace.resolve("linked-worktree")

    private val installedHooksDirectory: File
        get() = repositoryDirectory.resolve(".git/hooks")

    /** Overwrites the shared config before each scenario, with the name and email git needs to commit. */
    @BeforeEach
    fun seedGlobalGitConfig() {
        globalGitConfigFile.writeText(GLOBAL_GIT_CONFIG_SCRIPT)
    }

    // File placement
    @Test
    fun installsEveryHook_PlainRepository() {
        // Given
        createFixtureRepository(repositoryDirectory)

        // When
        runInstallGitHooks(repositoryDirectory)

        // Then
        assertSoftly { softly ->
            HOOK_NAMES.forEach { hookName ->
                softly.assertThat(installedHooksDirectory.resolve(hookName))
                      .describedAs("installed %s", hookName)
                      .hasContent(HOOK_SCRIPT)
            }
        }

        // Windows calls every file executable, so only the check is skipped there — the scenario still runs
        assumingThat(!OS.WINDOWS.isCurrentOs) {
            assertSoftly { softly ->
                HOOK_NAMES.forEach { hookName ->
                    softly.assertThat(installedHooksDirectory.resolve(hookName).canExecute())
                          .describedAs("%s is executable", hookName)
                          .isTrue()
                }
            }
        }
    }

    @Test
    fun preservesExistingFiles_UnrelatedFilePresent() {
        // Given
        createFixtureRepository(repositoryDirectory)
        val markerContent = "left behind by something else\n"
        val unrelatedFile = installedHooksDirectory.resolve("unrelated-marker")
        unrelatedFile.writeText(markerContent)

        // When
        runInstallGitHooks(repositoryDirectory)

        // Then
        assertThat(unrelatedFile)
            .describedAs("unrelated file in the hooks directory")
            .hasContent(markerContent)
    }

    @Test
    fun installsAddedHook_NewFileInSource() {
        // Given
        createFixtureRepository(repositoryDirectory, HOOK_NAMES + "prepare-commit-msg")

        // When
        runInstallGitHooks(repositoryDirectory)

        // Then
        assertThat(installedHooksDirectory.resolve("prepare-commit-msg"))
            .describedAs("the hook added to the source directory, with no change to the task class")
            .exists()
    }

    // Destination resolution
    @Test
    fun resolvesCommonHooksDir_LinkedWorktree() {
        // Given
        createFixtureRepository(repositoryDirectory)
        commitEverything(repositoryDirectory)
        runGitOrFail(repositoryDirectory, "worktree", "add", linkedWorktreeDirectory.absolutePath, "-b", "linked")

        // When
        runInstallGitHooks(linkedWorktreeDirectory)

        // Then
        assertSoftly { softly ->
            HOOK_NAMES.forEach { hookName ->
                softly.assertThat(installedHooksDirectory.resolve(hookName))
                      .describedAs("%s in the shared hooks directory", hookName)
                      .exists()
            }
            softly.assertThat(repositoryDirectory.resolve(".git/worktrees/linked-worktree/hooks"))
                  .describedAs("the linked worktree's own hooks directory, which git never reads")
                  .doesNotExist()
        }
    }

    // core.hooksPath handling
    @Test
    fun succeeds_NoHooksPathSet() {
        // Given
        createFixtureRepository(repositoryDirectory)

        // When
        val actual = runInstallGitHooks(repositoryDirectory)

        // Then
        assertSoftly { softly ->
            softly.assertThat(actual.installGitHooksOutcome)
                  .describedAs("installGitHooks outcome")
                  .isEqualTo(TaskOutcome.SUCCESS)
            softly.assertThat(actual.output)
                  .describedAs("build output")
                  .doesNotContain("WARNING", "core.hooksPath", "not executable")
        }
    }

    @Test
    fun clearsHooksPath_SetLocally() {
        // Given
        createFixtureRepository(repositoryDirectory)
        runGitOrFail(repositoryDirectory, "config", "core.hooksPath", "git/hooks")

        // When
        val actual = runInstallGitHooks(repositoryDirectory)

        // Then
        assertThat(runGit(repositoryDirectory, "config", "--get", "core.hooksPath").output)
            .describedAs("core.hooksPath after the run")
            .isEmpty()
        assertThat(actual.output)
            .describedAs("build output")
            .containsSubsequence("Clearing this repository's core.hooksPath 'git/hooks'", "Cleared core.hooksPath")
    }

    @Test
    fun warnsAboutSurvivingHooksPath_SetGlobally() {
        // Given
        createFixtureRepository(repositoryDirectory)
        runGitOrFail(workspace, "config", "--global", "core.hooksPath", "elsewhere/hooks")

        // When
        val actual = runInstallGitHooks(repositoryDirectory)

        // Then
        assertThat(actual.output)
            .describedAs("build output")
            .contains("core.hooksPath is still set to 'elsewhere/hooks'")
        assertThat(runGit(workspace, "config", "--global", "--get", "core.hooksPath").output)
            .describedAs("the global core.hooksPath, which is not this task's to remove")
            .isEqualTo("elsewhere/hooks")
    }

    @Test
    fun warnsAboutUnsetFailure_ConfigLocked() {
        // Given
        createFixtureRepository(repositoryDirectory)
        runGitOrFail(repositoryDirectory, "config", "core.hooksPath", "git/hooks")
        repositoryDirectory.resolve(".git/config.lock").writeText("")

        // When
        val actual = runInstallGitHooks(repositoryDirectory)

        // Then
        assertSoftly { softly ->
            softly.assertThat(actual.installGitHooksOutcome)
                  .describedAs("installGitHooks outcome")
                  .isEqualTo(TaskOutcome.SUCCESS)
            softly.assertThat(actual.output)
                  .describedAs("build output")
                  .contains("may be read-only or locked", "because clearing it failed above")
        }
        assertThat(runGit(repositoryDirectory, "config", "--get", "core.hooksPath").output)
            .describedAs("core.hooksPath after the run, which the failed clear left in place")
            .isEqualTo("git/hooks")
    }

    // Repeat invocation
    @Test
    fun executes_SecondRun() {
        // Given
        createFixtureRepository(repositoryDirectory)
        val firstResult = runInstallGitHooks(repositoryDirectory)

        // When
        val secondResult = runInstallGitHooks(repositoryDirectory)

        // Then
        assertSoftly { softly ->
            softly.assertThat(firstResult.installGitHooksOutcome)
                  .describedAs("first run outcome")
                  .isEqualTo(TaskOutcome.SUCCESS)
            softly.assertThat(secondResult.installGitHooksOutcome)
                  .describedAs("second run outcome — no declared outputs keeps this SUCCESS rather than UP-TO-DATE; would catch a regression to a stock Copy task")
                  .isEqualTo(TaskOutcome.SUCCESS)
        }
    }

    @Test
    fun reusesConfigurationCacheEntry_SecondRun() {
        // Given
        createFixtureRepository(repositoryDirectory)
        val firstResult = runInstallGitHooks(repositoryDirectory, "--configuration-cache")

        // When
        val secondResult = runInstallGitHooks(repositoryDirectory, "--configuration-cache")

        // Then
        assertSoftly { softly ->
            softly.assertThat(firstResult.output)
                  .describedAs("first run output")
                  .contains("Configuration cache entry stored")
            softly.assertThat(secondResult.output)
                  .describedAs("second run output")
                  .contains("Configuration cache entry reused")
        }
    }

    // Registration surface
    @Test
    fun registersOnRootOnly_MultiProjectBuild() {
        // Given
        createMultiProjectFixtureRepository(repositoryDirectory)

        // When
        val rootResult = runInstallGitHooks(repositoryDirectory)
        val subprojectResult = gradleRunner(repositoryDirectory, ":$MULTI_PROJECT_SUBPROJECT_NAME:installGitHooks").buildAndFail()

        // Then
        assertThat(rootResult.installGitHooksOutcome)
            .describedAs("root installGitHooks outcome")
            .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(subprojectResult.output)
            .describedAs("subproject build output")
            .contains("task 'installGitHooks' not found in project ':$MULTI_PROJECT_SUBPROJECT_NAME'")
    }

    // Fixture repositories
    /**
     * Creates a git repository in [directory] holding a small Gradle project and the named hooks.
     *
     * The project applies the real convention plugin, so the task looks for its hooks exactly
     * where it does in this repository.
     */
    private fun createFixtureRepository(
        directory: File,
        hookNames: List<String> = HOOK_NAMES,
        settingsScript: String = FIXTURE_SETTINGS_SCRIPT
    ) {
        directory.mkdirs()
        runGitOrFail(directory, "init")

        directory.resolve("settings.gradle.kts").writeText(settingsScript)
        directory.resolve("build.gradle.kts").writeText(FIXTURE_BUILD_SCRIPT)

        val hooksSourceDirectory = directory.resolve("git/hooks")
        hooksSourceDirectory.mkdirs()
        hookNames.forEach { hooksSourceDirectory.resolve(it).writeText(HOOK_SCRIPT) }
    }

    /**
     * Same as [createFixtureRepository], but the project also holds one subproject,
     * [MULTI_PROJECT_SUBPROJECT_NAME], which applies nothing.
     *
     * Kept separate rather than adding a subproject to that one: its scripts have to stay
     * identical across every scenario to keep the suite fast, and declaring a subproject
     * changes them.
     */
    private fun createMultiProjectFixtureRepository(directory: File, hookNames: List<String> = HOOK_NAMES) {
        createFixtureRepository(directory, hookNames, MULTI_PROJECT_FIXTURE_SETTINGS_SCRIPT)
        directory.resolve(MULTI_PROJECT_SUBPROJECT_NAME).mkdirs()
    }

    /** Commits everything in [directory], so a worktree can be added from it. */
    private fun commitEverything(directory: File) {
        runGitOrFail(directory, "add", "--all")
        runGitOrFail(directory, "commit", "--message", "Add the fixture project")
    }

    // Build invocation
    private fun runInstallGitHooks(projectDirectory: File, vararg extraArguments: String): BuildResult =
        gradleRunner(projectDirectory, "installGitHooks", *extraArguments).build()

    /** A runner for [projectDirectory] with the isolation every scenario gets, for the caller to build or fail. */
    private fun gradleRunner(projectDirectory: File, vararg arguments: String): GradleRunner =
        GradleRunner.create()
                    .withProjectDir(projectDirectory)
                    .withPluginClasspath()
                    .withEnvironment(gitEnvironment())
                    .withArguments(*arguments, "--stacktrace")

    /** The outcome of the root project's installGitHooks, or null when the build never ran it. */
    private val BuildResult.installGitHooksOutcome: TaskOutcome?
        get() = task(":installGitHooks")?.outcome

    // Environment isolation
    /**
     * The complete environment for the build — Gradle replaces rather than merges, so anything
     * left out here is simply missing. Setting one also rules out debugging these builds.
     */
    private fun gitEnvironment(): Map<String, String> =
        System.getenv().filterKeys { !isGitEnvironmentVariable(it) } + gitConfigIsolation()

    /**
     * Points git at this suite's own config file and ignores the machine-wide one, so a
     * developer's own settings can neither affect these scenarios nor be changed by them.
     */
    private fun gitConfigIsolation(): Map<String, String> = mapOf(
        "GIT_CONFIG_GLOBAL" to globalGitConfigFile.absolutePath,
        "GIT_CONFIG_NOSYSTEM" to "1"
    )

    /** True for any `GIT_*` variable — git sets these itself, and an inherited one would skew a run. */
    private fun isGitEnvironmentVariable(key: String): Boolean = key.startsWith("GIT_")

    // Git invocation
    /** Runs git in [directory] with the same isolation the build gets, and fails if git reports an error. */
    private fun runGitOrFail(directory: File, vararg arguments: String) {
        val actual = runGit(directory, *arguments)
        assertThat(actual.exitValue)
            .describedAs("exit value of git %s in %s, which answered '%s'", arguments.joinToString(" "), directory, actual.output)
            .isZero()
    }

    /** Runs git in [directory] with the same isolation the build gets, and returns whatever it answers. */
    private fun runGit(directory: File, vararg arguments: String): GitResult {
        val processBuilder = ProcessBuilder(listOf("git", *arguments))
            .directory(directory)
            .redirectErrorStream(true)
        val environment = processBuilder.environment()
        environment.keys.removeAll(::isGitEnvironmentVariable)
        environment.putAll(gitConfigIsolation())

        val process = processBuilder.start()
        val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
        return GitResult(process.waitFor(), output)
    }

    private data class GitResult(val exitValue: Int, val output: String)
}