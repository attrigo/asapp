package com.attrigo.asapp.gradle

import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import org.gradle.process.ExecOperations

/**
 * Copies every file in the hooks directory into the repository's hooks directory and clears any
 * core.hooksPath that would shadow it.
 *
 * The task copies the files itself, not with Gradle's Copy or Sync — either would make Gradle treat
 * that directory as its own and delete it.
 *
 * Add `--info` to see what it resolved, `--debug` to see every git call.
 */
@UntrackedTask(because = "Writes into the Git repository's hooks directory, which Gradle must neither fingerprint nor clean")
abstract class InstallGitHooks : DefaultTask() {

    /** The directory holding the hook scripts. */
    @get:InputDirectory
    abstract val hooksSource: DirectoryProperty

    @get:Inject
    abstract val execOperations: ExecOperations

    @get:Inject
    abstract val fileSystemOperations: FileSystemOperations

    @TaskAction
    fun install() {
        val trackedHooksDir = hooksSource.get().asFile
        logger.info("Installing the hooks found in {}", trackedHooksDir)

        val gitHooksDir = resolveGitHooksDir(trackedHooksDir)
        val hookNames = copyHooks(trackedHooksDir, gitHooksDir)
        warnAboutNonExecutableHooks(gitHooksDir, hookNames)
        clearHooksPath(trackedHooksDir)
    }

    /** Resolves the hooks directory git actually reads, running git in [workingDirectory]. */
    private fun resolveGitHooksDir(workingDirectory: File): File {
        logger.info("Resolving git hooks directory - it is not always .git/hooks, a worktree points elsewhere")
        val gitHooksDir = File(runGit(workingDirectory, "rev-parse", "--path-format=absolute", "--git-common-dir"), "hooks")
        logger.info("Git hooks directory: {}", gitHooksDir)
        return gitHooksDir
    }

    /**
     * Copies every hook in [trackedHooksDir] into [gitHooksDir] and returns the top-level hook names.
     *
     * Git runs only hooks named exactly in [gitHooksDir], so nested files are copied without checking they can run.
     */
    private fun copyHooks(trackedHooksDir: File, gitHooksDir: File): List<String> {
        val hookNames = trackedHooksDir.listFiles { file -> file.isFile }.orEmpty().map { it.name }.sorted()
        logger.info("Copying hooks {}", hookNames)
        fileSystemOperations.copy {
            from(trackedHooksDir)
            into(gitHooksDir)
            filePermissions { unix("755") } // makes the hooks executable — git ignores one that is not
        }
        logger.lifecycle("Installed the Git hooks from {} into {}", trackedHooksDir, gitHooksDir)
        return hookNames
    }

    /** Warns about every installed hook git would skip for not being executable. */
    private fun warnAboutNonExecutableHooks(gitHooksDir: File, hookNames: List<String>) {
        logger.info("Checking the installed hooks are executable - git skips a non-executable hook silently")
        for (hookName in hookNames) {
            if (!File(gitHooksDir, hookName).canExecute()) {
                logger.warn("WARNING: the installed {} is not executable - git will skip it silently", hookName)
            }
        }
    }

    /**
     * Clears this repository's core.hooksPath and warns about any value that survives, running git
     * in [workingDirectory].
     *
     * `--unset` clears the repository's own config only, never the user's global one or the
     * machine's system one, so a value it leaves behind is not this task's to delete. The exit code
     * says which happened: 0 cleared a value, 5 found none, anything else failed and may have left
     * the repository's own value in place.
     */
    private fun clearHooksPath(workingDirectory: File) {
        logger.info("Clearing core.hooksPath, which would otherwise make git ignore the hooks directory")
        val localHooksPath = runGit(workingDirectory, "config", "--local", "--get", "core.hooksPath", ignoreExitValue = true)
        if (localHooksPath.isNotEmpty()) {
            logger.lifecycle("Clearing this repository's core.hooksPath '{}', which is shadowing the hooks directory", localHooksPath)
        }

        logger.debug("Running git config --unset core.hooksPath in {}", workingDirectory)
        val unsetResult = execOperations.exec {
            workingDir = workingDirectory
            commandLine("git", "config", "--unset", "core.hooksPath")
            isIgnoreExitValue = true
        }
        logger.info("Unsetting core.hooksPath exited with {}", unsetResult.exitValue)
        val localUnsetFailed = unsetResult.exitValue != 0 && unsetResult.exitValue != 5
        if (unsetResult.exitValue == 0) {
            logger.lifecycle("Cleared core.hooksPath, which was shadowing that directory")
        } else if (localUnsetFailed) {
            logger.warn("WARNING: clearing this repository's core.hooksPath failed with exit value {} - its config file may be read-only or locked by another git process", unsetResult.exitValue)
        }

        logger.info("Checking for a global or system core.hooksPath - the core.hooksPath unset above only reached this repository")
        val remainingHooksPath = runGit(workingDirectory, "config", "--get", "core.hooksPath", ignoreExitValue = true)
        if (remainingHooksPath.isEmpty()) {
            logger.info("No core.hooksPath is set, so git will use the installed hooks")
        } else if (localUnsetFailed) {
            logger.warn("WARNING: core.hooksPath is still set to '{}' because clearing it failed above - the installed hooks will not run", remainingHooksPath)
        } else {
            logger.warn("WARNING: core.hooksPath is still set to '{}' outside this repository - the installed hooks will not run", remainingHooksPath)
        }
    }

    /** Runs git in [workingDirectory] and returns its trimmed standard output. */
    private fun runGit(workingDirectory: File, vararg arguments: String, ignoreExitValue: Boolean = false): String {
        logger.debug("Running git {} in {}", arguments.joinToString(" "), workingDirectory)
        val stdoutBuffer = ByteArrayOutputStream()
        execOperations.exec {
            workingDir = workingDirectory
            commandLine(listOf("git") + arguments)
            standardOutput = stdoutBuffer // capture the output instead of printing it
            isIgnoreExitValue = ignoreExitValue
        }
        val gitOutput = stdoutBuffer.toString(Charsets.UTF_8).trim()
        logger.debug("Git answered '{}'", gitOutput)
        return gitOutput
    }
}
