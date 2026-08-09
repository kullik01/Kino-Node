plugins {
    application
    id("org.openjfx.javafxplugin")
    id("org.beryx.jlink")
}

application {
    mainModule.set("kinonode.ui")
    mainClass.set("io.github.kullik01.kinonode.Launcher")
    applicationDefaultJvmArgs = listOf(
        "-Dapp.name=KinoNode",
        "-Dprism.allowhidpi=true",
        "-Dprism.lcdtext=true",
        "-Dglass.win.uiScale.enabled=true",
        "-Dsun.java2d.dpiaware=true"
    )
}

javafx {
    version = rootProject.extra["javafxVersion"] as String? ?: "25.0.2"
    modules = listOf("javafx.controls", "javafx.graphics", "javafx.media", "javafx.base", "javafx.fxml", "javafx.web")
}

dependencies {
    implementation(project(":kinonode-domain"))
    implementation(project(":kinonode-data"))
    
    implementation("io.github.mkpaz:atlantafx-base:${rootProject.extra["atlantafxVersion"] ?: "2.0.1"}")
    implementation("com.google.inject:guice:${rootProject.extra["guiceVersion"] ?: "6.0.0"}")
    implementation("com.google.code.gson:gson:${rootProject.extra["gsonVersion"] ?: "2.10.1"}")
}

// jlink tasks ...
val isWindows = System.getProperty("os.name").lowercase().contains("win")
val isMac = System.getProperty("os.name").lowercase().contains("mac")
val isLinux = System.getProperty("os.name").lowercase().contains("linux") || System.getProperty("os.name").lowercase().contains("unix")

jlink {
    mergedModuleName.set("kinonode.merged.module")
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
        jpackageHome = System.getenv("JAVA_HOME") ?: System.getProperty("java.home")
        val typeProp = project.findProperty("installerType") as? String
        installerType = typeProp ?: when {
            isWindows -> "exe"
            isMac -> "dmg"
            else -> "deb"
        }
        
        val archProp = (project.findProperty("targetArch") as? String)
            ?: if (System.getProperty("os.arch").lowercase().contains("86") && !System.getProperty("os.arch").lowercase().contains("64")) "x86" else "x64"
        val osSuffix = when {
            isWindows -> "windows"
            isMac -> "macos"
            else -> "linux"
        }
        
        installerName = "KinoNode-${osSuffix}-${archProp}"
        appVersion = "1.0.1"
        vendor = "Hannah Kullik"
        
        val iconPath = when {
            isWindows -> "src/main/resources/io/github/kullik01/kinonode/images/logos/logo.ico"
            isMac -> "src/main/resources/io/github/kullik01/kinonode/images/logos/logo.icns"
            else -> "src/main/resources/io/github/kullik01/kinonode/images/logos/logo.png"
        }
        val iconArgs = if (file(iconPath).exists()) listOf("--icon", file(iconPath).absolutePath) else emptyList()
        val baseOptions = iconArgs + listOf(
            "--description", "Personal Movie Library Manager",
            "--vendor", "Hannah Kullik",
            "--resource-dir", file("src/main/resources/jpackage").absolutePath
        )
        val javaOptionsList = listOf(
            "--java-options", "-Dprism.allowhidpi=true",
            "--java-options", "-Dprism.lcdtext=true",
            "--java-options", "-Dglass.win.uiScale.enabled=true",
            "--java-options", "-Dsun.java2d.dpiaware=true"
        )
        
        imageOptions = baseOptions + javaOptionsList
        
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
        
        installerOptions = baseOptions + osSpecificOptions
    }
}

// Copy the install/uninstall scripts and icon to the image directory
tasks.named("jlink") {
    doLast {
        val imageDirPath = layout.buildDirectory.dir("KinoNode-${version}").get().asFile
        val osName = System.getProperty("os.name").lowercase()
        
        if (!osName.contains("win") && !osName.contains("mac")) {
            // Linux scripts
            copy {
                from("src/main/scripts/linux/install.sh")
                from("src/main/scripts/linux/install_shortcut.sh")
                from("src/main/scripts/linux/uninstall.sh")
                into(imageDirPath)
            }
            ant.withGroovyBuilder {
                "chmod"("file" to File(imageDirPath, "install.sh"), "perm" to "755")
                "chmod"("file" to File(imageDirPath, "install_shortcut.sh"), "perm" to "755")
                "chmod"("file" to File(imageDirPath, "uninstall.sh"), "perm" to "755")
            }
            // Application Icon
            copy {
                from("src/main/resources/io/github/kullik01/kinonode/images/logos/logo_highres.png")
                into(imageDirPath)
                rename { "logo.png" }
            }
        } else if (osName.contains("win")) {
            // Windows scripts & icon
            copy {
                from("src/main/scripts/windows/install_shortcut.bat")
                from("src/main/scripts/windows/uninstall.bat")
                from("src/main/scripts/windows/uninstall.ps1")
                from("src/main/resources/io/github/kullik01/kinonode/images/logos/logo.ico")
                into(imageDirPath)
            }
        } else if (osName.contains("mac")) {
            // macOS scripts & icon
            copy {
                from("src/main/scripts/macos/install_shortcut.command")
                from("src/main/scripts/macos/uninstall.command")
                from("src/main/resources/io/github/kullik01/kinonode/images/logos/logo.icns")
                into(imageDirPath)
            }
            ant.withGroovyBuilder {
                "chmod"("file" to File(imageDirPath, "install_shortcut.command"), "perm" to "755")
                "chmod"("file" to File(imageDirPath, "uninstall.command"), "perm" to "755")
            }
        }
    }
}
