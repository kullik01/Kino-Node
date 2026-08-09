plugins {
    java
    id("org.openjfx.javafxplugin") version "0.1.0" apply false
    id("org.beryx.jlink") version "3.2.0" apply false
    id("com.diffplug.spotless") version "6.25.0"
}

group = "io.github.kullik01"
version = "1.0.1"

extra["junitVersion"] = "5.10.2"
extra["sqliteVersion"] = "3.45.1.0"
extra["atlantafxVersion"] = "2.0.1"
extra["guiceVersion"] = "6.0.0"
extra["gsonVersion"] = "2.10.1"
extra["javafxVersion"] = "25.0.2"

allprojects {
    group = "io.github.kullik01"
    version = "1.0.1"
    
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
        modularity.inferModulePath.set(true)
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    dependencies {
        testImplementation("org.junit.jupiter:junit-jupiter:${rootProject.extra["junitVersion"] ?: "5.10.2"}")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}

configure<com.diffplug.gradle.spotless.SpotlessExtension> {
    java {
        target("*/src/main/java/**/*.java", "*/src/test/java/**/*.java")
        eclipse().configFile(rootProject.file("eclipse-java-google-style.xml"))
    }
}

tasks.register("jpackage") {
    dependsOn(":kinonode-ui:jpackage")
}

tasks.register("jlinkZip") {
    dependsOn(":kinonode-ui:jlinkZip")
}

tasks.register<Zip>("windowsX64AppImageZip") {
    dependsOn(":kinonode-ui:jpackageImage")
    archiveFileName.set("KinoNode-${version}-windows-x64.zip")
    destinationDirectory.set(file("build/distributions"))
    from(project(":kinonode-ui").layout.buildDirectory.dir("jpackage/KinoNode"))
}

tasks.register("windowsAppImageZip") {
    dependsOn("windowsX64AppImageZip")
}

tasks.register<Zip>("linuxAppImageZip") {
    dependsOn(":kinonode-ui:jpackageImage")
    archiveFileName.set("KinoNode-${version}-linux-x64.zip")
    destinationDirectory.set(file("build/distributions"))
    from(project(":kinonode-ui").layout.buildDirectory.dir("jpackage/kinonode"))
}

tasks.register<Tar>("linuxAppImageTar") {
    dependsOn(":kinonode-ui:jpackageImage")
    archiveFileName.set("KinoNode-${version}-linux-x64.tar.gz")
    destinationDirectory.set(file("build/distributions"))
    compression = Compression.GZIP
    from(project(":kinonode-ui").layout.buildDirectory.dir("jpackage/kinonode"))
}

tasks.register<Zip>("macAppImageZip") {
    dependsOn(":kinonode-ui:jpackageImage")
    archiveFileName.set("KinoNode-${version}-macos-x64.zip")
    destinationDirectory.set(file("build/distributions"))
    from(project(":kinonode-ui").layout.buildDirectory.dir("jpackage/KinoNode.app"))
}

tasks.register<Tar>("macAppImageTar") {
    dependsOn(":kinonode-ui:jpackageImage")
    archiveFileName.set("KinoNode-${version}-macos-x64.tar.gz")
    destinationDirectory.set(file("build/distributions"))
    compression = Compression.GZIP
    from(project(":kinonode-ui").layout.buildDirectory.dir("jpackage/KinoNode.app"))
}

tasks.register<Tar>("windowsX64AppImageTar") {
    dependsOn(":kinonode-ui:jpackageImage")
    archiveFileName.set("KinoNode-${version}-windows-x64.tar.gz")
    destinationDirectory.set(file("build/distributions"))
    compression = Compression.GZIP
    from(project(":kinonode-ui").layout.buildDirectory.dir("jpackage/KinoNode"))
}

tasks.register("windowsAppImageTar") {
    dependsOn("windowsX64AppImageTar")
}

tasks.register("zipDistributions") {
    dependsOn("windowsAppImageZip", "linuxAppImageZip", "macAppImageZip")
}

tasks.register("tarDistributions") {
    dependsOn("windowsAppImageTar", "linuxAppImageTar", "macAppImageTar")
}

tasks.register("allDistributions") {
    dependsOn("zipDistributions", "tarDistributions", "jpackage")
}
