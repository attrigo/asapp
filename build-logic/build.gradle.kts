plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:org.springframework.boot.gradle.plugin:${libs.versions.springBoot.get()}")
    implementation("com.diffplug.spotless:com.diffplug.spotless.gradle.plugin:${libs.versions.spotless.get()}")
    implementation("info.solidsoft.pitest:info.solidsoft.pitest.gradle.plugin:${libs.versions.pitestPlugin.get()}")
    implementation("org.cyclonedx.bom:org.cyclonedx.bom.gradle.plugin:${libs.versions.cyclonedx.get()}")
    implementation("com.gorylenko.gradle-git-properties:com.gorylenko.gradle-git-properties.gradle.plugin:${libs.versions.gitProperties.get()}")
    implementation("org.asciidoctor.jvm.convert:org.asciidoctor.jvm.convert.gradle.plugin:${libs.versions.asciidoctor.get()}")

    // Expose the version catalog to the precompiled convention plugins.
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}
