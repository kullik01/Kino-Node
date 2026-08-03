plugins {
    java
    id("org.openjfx.javafxplugin") version "0.1.0" apply false
    id("org.beryx.jlink") version "3.2.0" apply false
    id("com.diffplug.spotless") version "6.25.0"
}

group = "io.github.kullik01"
version = "1.0.0"

extra["junitVersion"] = "5.10.2"
extra["sqliteVersion"] = "3.45.1.0"
extra["atlantafxVersion"] = "2.0.1"
extra["guiceVersion"] = "6.0.0"
extra["gsonVersion"] = "2.10.1"
extra["javafxVersion"] = "25.0.2"

allprojects {
    group = "io.github.kullik01"
    version = "1.0.0"
    
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
            vendor.set(JvmVendorSpec.ADOPTIUM)
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
