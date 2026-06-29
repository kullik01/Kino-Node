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

// Detect current OS for platform-specific JavaFX dependencies
val osName = System.getProperty("os.name").lowercase()
val platform = when {
    osName.contains("win") -> "win"
    osName.contains("mac") -> "mac"
    osName.contains("linux") -> "linux"
    else -> "linux"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
    modularity.inferModulePath.set(true)
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

application {
    mainModule.set("io.github.kullik01.kinonode")
    mainClass.set("io.github.kullik01.kinonode.Launcher")
    applicationDefaultJvmArgs = listOf(
        "-Dapp.name=KinoNode"
    )
}

dependencies {
    // JavaFX dependencies (platform-specific)
    implementation("org.openjfx:javafx-controls:${javafxVersion}:${platform}")
    implementation("org.openjfx:javafx-graphics:${javafxVersion}:${platform}")
    implementation("org.openjfx:javafx-media:${javafxVersion}:${platform}")
    implementation("org.openjfx:javafx-base:${javafxVersion}:${platform}")
    implementation("org.openjfx:javafx-fxml:${javafxVersion}:${platform}")
    implementation("org.openjfx:javafx-web:${javafxVersion}:${platform}")
    
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
    val zipOsSuffix = if (osName.contains("win")) "Windows" else "Linux"
    imageZip.set(layout.buildDirectory.file("distributions/KinoNode-${version}-${zipOsSuffix}.zip"))
    options.set(listOf(
        "--strip-debug",
        "--compress", "zip-6",
        "--no-header-files",
        "--no-man-pages"
    ))
    launcher {
        name = "KinoNode"
        noConsole = true
    }
    
    jpackage {
        jpackageHome = System.getenv("JAVA_HOME") ?: ""
        val isWindows = osName.contains("win")
        val typeProp = project.findProperty("installerType") as? String
        installerType = typeProp ?: if (isWindows) "exe" else "app-image"
        
        installerName = "KinoNode"
        appVersion = "1.0.0"
        
        imageOptions = listOf(
            "--icon", if (isWindows) 
                "src/main/resources/io/github/kullik01/kinonode/images/logo.ico"
            else 
                "src/main/resources/io/github/kullik01/kinonode/images/logo.png",
            "--description", "KinoNode - Personal Movie Library Manager",
            "--vendor", "Hannah Kullik",
        )
        
        installerOptions = if (isWindows) {
            listOf(
                "--win-dir-chooser",
                "--win-menu",
                "--win-shortcut",
                "--win-upgrade-uuid", "k1n0n0d3-8f83-4a3d-82d8-5f2122650f92"
            )
        } else {
            listOf(
                "--linux-menu-group", "Utility",
                "--linux-shortcut"
            )
        }
    }
}

// Custom task to create Windows zip with KinoNode.exe (using jpackage app-image)
tasks.register<Zip>("windowsAppImageZip") {
    dependsOn("jpackageImage")
    onlyIf { osName.contains("win") }
    
    from(layout.buildDirectory.dir("jpackage/KinoNode")) {
        into("KinoNode-${version}")
    }
    
    archiveFileName.set("KinoNode-${version}-Windows.zip")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
}

// Custom task to create Linux zip with KinoNode (using jpackage app-image)
tasks.register<Zip>("linuxAppImageZip") {
    dependsOn("jpackageImage")
    onlyIf { osName.contains("linux") || osName.contains("unix") }
    
    from(layout.buildDirectory.dir("jpackage/KinoNode")) {
        into("KinoNode-${version}")
    }
    
    archiveFileName.set("KinoNode-${version}-Linux.zip")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
}

configure<com.diffplug.gradle.spotless.SpotlessExtension> {
    java {
        target("src/main/java/**/*.java", "src/test/java/**/*.java")
        eclipse().configFile(rootProject.file("eclipse-java-google-style.xml"))
    }
}


