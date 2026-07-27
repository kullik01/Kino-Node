/**
 * BSD 3-Clause License
 *
 * Copyright (c) 2026, Hannah Kullik
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
plugins {
    java
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("org.beryx.jlink") version "3.2.0"
    id("com.diffplug.spotless") version "6.25.0"
}

group = "io.github.kullik01"
version = "1.0.0"

repositories {
    mavenCentral()
}

val junitVersion = "5.10.2"
val javafxVersion = "25.0.2"
val sqliteVersion = "3.45.1.0"
val atlantafxVersion = "2.0.1"

val osName = System.getProperty("os.name").lowercase()
val isWindows = osName.contains("win")
val isMac = osName.contains("mac")
val isLinux = osName.contains("linux") || osName.contains("unix")

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
    modularity.inferModulePath.set(true)
}

javafx {
    version = javafxVersion
    modules = listOf("javafx.controls", "javafx.graphics", "javafx.media", "javafx.base", "javafx.fxml", "javafx.web")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

application {
    mainModule.set("io.github.kullik01.kinonode")
    mainClass.set("io.github.kullik01.kinonode.Launcher")
    applicationDefaultJvmArgs = listOf(
        "-Dapp.name=KinoNode",
        "-Dprism.allowhidpi=true",
        "-Dprism.lcdtext=true",
        "-Dglass.win.uiScale.enabled=true"
    )
}

dependencies {
    // SQLite Persistence
    implementation("org.xerial:sqlite-jdbc:${sqliteVersion}")
    
    // AtlantaFX styling
    implementation("io.github.mkpaz:atlantafx-base:${atlantafxVersion}")
    
    // JSON parsing
    implementation("com.google.code.gson:gson:2.10.1")
    
    testImplementation("org.junit.jupiter:junit-jupiter:${junitVersion}")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
    jvmArgs(
        "--add-reads", "io.github.kullik01.kinonode=ALL-UNNAMED",
        "--add-opens", "java.base/java.lang=ALL-UNNAMED",
        "--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED",
        "--add-opens", "io.github.kullik01.kinonode/io.github.kullik01.kinonode.model=ALL-UNNAMED",
        "--add-opens", "io.github.kullik01.kinonode/io.github.kullik01.kinonode.controller=ALL-UNNAMED",
        "--add-opens", "io.github.kullik01.kinonode/io.github.kullik01.kinonode.service=ALL-UNNAMED",
        "--add-opens", "io.github.kullik01.kinonode/io.github.kullik01.kinonode.repository=ALL-UNNAMED"
    )
}

jlink {
    imageDir.set(layout.buildDirectory.dir("KinoNode-${version}"))
    val zipOsSuffix = when {
        isWindows -> "Windows"
        isMac -> "macOS"
        else -> "Linux"
    }
    imageZip.set(layout.buildDirectory.file("distributions/KinoNode-${version}-${zipOsSuffix}.zip"))
    options.set(listOf(
        "--strip-debug",
        "--compress", "zip-6",
        "--no-header-files",
        "--no-man-pages",
        "--bind-services"
    ))
    launcher {
        name = "KinoNode"
        noConsole = true
    }
    
    jpackage {
        jpackageHome = System.getenv("JAVA_HOME") ?: ""
        val typeProp = project.findProperty("installerType") as? String
        installerType = typeProp ?: when {
            isWindows -> "exe"
            isMac -> "dmg"
            else -> "deb"
        }
        
        installerName = "KinoNode"
        appVersion = "1.0.0"
        vendor = "Hannah Kullik"
        
        val iconPath = when {
            isWindows -> "src/main/resources/io/github/kullik01/kinonode/images/logo.ico"
            isMac -> "src/main/resources/io/github/kullik01/kinonode/images/logo.icns"
            else -> "src/main/resources/io/github/kullik01/kinonode/images/logo_256.png"
        }
        val iconArgs = if (file(iconPath).exists()) listOf("--icon", file(iconPath).absolutePath) else emptyList()
        val commonOptions = iconArgs + listOf(
            "--description", "Personal Movie Library Manager",
            "--vendor", "Hannah Kullik"
        )
        
        imageOptions = commonOptions
        
        val osSpecificOptions = when {
            isWindows -> listOf(
                "--win-dir-chooser",
                "--win-menu",
                "--win-shortcut",
                "--win-upgrade-uuid", "c1a0b0d3-8f83-4a3d-82d8-5f2122650f92",
                "--install-dir", "KinoNode"
            )
            isMac -> listOf(
                "--mac-package-identifier", "io.github.kullik01.kinonode",
                "--mac-package-name", "KinoNode",
                "--install-dir", "/Applications/KinoNode"
            )
            else -> {
                val linuxCommon = listOf(
                    "--linux-menu-group", "Utility",
                    "--linux-shortcut",
                    "--linux-package-name", "kinonode",
                    "--install-dir", "/opt/KinoNode"
                )
                when (installerType) {
                    "deb" -> linuxCommon + listOf("--linux-deb-maintainer", "hannah@example.com")
                    "rpm" -> linuxCommon + listOf("--linux-rpm-license-type", "BSD-3-Clause")
                    else -> linuxCommon
                }
            }
        }
        
        installerOptions = commonOptions + osSpecificOptions
    }
}

// Preserve generated installer packages across multiple jpackage invocations
tasks.named("jpackage") {
    doLast {
        val outputDir = layout.buildDirectory.dir("jpackage").get().asFile
        val installersDir = layout.buildDirectory.dir("installers").get().asFile
        installersDir.mkdirs()
        outputDir.listFiles()?.forEach { file ->
            if (file.isFile && (file.name.endsWith(".exe") || file.name.endsWith(".msi") ||
                                file.name.endsWith(".deb") || file.name.endsWith(".rpm") ||
                                file.name.endsWith(".dmg") || file.name.endsWith(".pkg"))) {
                file.copyTo(File(installersDir, file.name), overwrite = true)
            }
        }
    }
}

// Custom task to create Windows zip with KinoNode.exe (using jpackage app-image)
tasks.register<Zip>("windowsAppImageZip") {
    group = "distribution"
    description = "Bundles the portable Windows app image into a self-contained zip distribution."
    dependsOn("jpackageImage")
    onlyIf { isWindows }
    
    from(layout.buildDirectory.dir("jpackage/KinoNode")) {
        into("KinoNode")
    }
    
    archiveFileName.set("KinoNode-${version}-Windows-Portable.zip")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
}

// Custom task to create macOS zip with KinoNode.app (using jpackage app-image)
tasks.register<Zip>("macAppImageZip") {
    group = "distribution"
    description = "Bundles the portable macOS app bundle into a self-contained zip distribution."
    dependsOn("jpackageImage")
    onlyIf { isMac }
    
    from(layout.buildDirectory.dir("jpackage/KinoNode.app")) {
        into("KinoNode/KinoNode.app")
    }
    
    archiveFileName.set("KinoNode-${version}-macOS-Portable.zip")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
}

// Custom task to create Linux zip with KinoNode (using jpackage app-image)
tasks.register<Zip>("linuxAppImageZip") {
    group = "distribution"
    description = "Bundles the portable Linux app image into a self-contained zip distribution."
    dependsOn("jpackageImage")
    onlyIf { isLinux }
    
    from(layout.buildDirectory.dir("jpackage/KinoNode")) {
        into("KinoNode")
    }
    
    archiveFileName.set("KinoNode-${version}-Linux-Portable.zip")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
}

// Custom task to create Linux tar.gz with KinoNode (using jpackage app-image)
tasks.register<Tar>("linuxAppImageTar") {
    group = "distribution"
    description = "Bundles the portable Linux app image into a self-contained tar.gz distribution."
    dependsOn("jpackageImage")
    onlyIf { isLinux }
    
    compression = Compression.GZIP
    from(layout.buildDirectory.dir("jpackage/KinoNode")) {
        into("KinoNode")
    }
    
    archiveFileName.set("KinoNode-${version}-Linux-Portable.tar.gz")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
}

configure<com.diffplug.gradle.spotless.SpotlessExtension> {
    java {
        target("src/main/java/**/*.java", "src/test/java/**/*.java")
        eclipse().configFile(rootProject.file("eclipse-java-google-style.xml"))
    }
}
