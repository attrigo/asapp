import com.attrigo.asapp.gradle.InstallGitHooks

// # Tasks

// ## Developer commands

// Installs the project's Git hooks; run once per clone, and again after editing a hook
tasks.register<InstallGitHooks>("installGitHooks") {
    group = "build setup"
    description = "Installs the project's Git hooks into the repository's hooks directory."

    hooksSource = layout.settingsDirectory.dir("git/hooks")
}

// ## Aggregates

// Contributes the build logic's own checks to the CI umbrella
tasks.register("ciBuild") {
    group = "build"
    description = "Runs every check the CI pipeline gates on."

    dependsOn(gradle.includedBuild("build-logic").task(":check"))
}
