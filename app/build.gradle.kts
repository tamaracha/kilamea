import org.gradle.internal.os.OperatingSystem

plugins {
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    alias(libs.plugins.jvm)

    // Apply the application plugin to add support for building a CLI application in Java.
    application
    alias(libs.plugins.badass.runtime)
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    implementation(libs.slf4j.api)
    implementation(libs.slf4j.nop)
    implementation(libs.javax.activation)
    implementation(libs.javax.mail)
    implementation(libs.commons.lang3)
    implementation(libs.gson)
    implementation(libs.jackson)
    implementation(libs.liquibase)
    implementation(libs.sqlite.jdbc)
    implementation(libs.jetty)
    implementation(libs.jetty.util)
    implementation(libs.bundles.gmail)
    implementation(libs.appdirs)
    // Use local swt files because eclipse is not able to build working maven packages
    implementation(fileTree("../lib").include("*.jar"))

        // Use the Kotlin JUnit 5 integration.
    testImplementation(libs.kotlin.test.junit5)

    // Use the JUnit 5 integration.
    testImplementation(libs.junit.jupiter.engine)

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.slf4j.simple)
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(22)
    }
}

application {
    // Define the main class for the application.
    mainClass = "com.github.kilamea.Launcher"
    applicationName = "kilamea"
    if (OperatingSystem.current().isMacOsX) {
        applicationDefaultJvmArgs = listOf("-XstartOnFirstThread")
    }
}

tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
}

runtime {
    options = listOf("--strip-debug", "--compress", "zip-6", "--no-header-files", "--no-man-pages")
    modules = listOf("java.naming", "java.desktop", "java.logging", "java.sql")

    launcher {
        noConsole = true
    }

    jpackage {
        val appName = "Kilamea"
        imageName = appName
        installerName = appName
        installerOptions = mutableListOf("--resource-dir", "src/main/resources")
        if(OperatingSystem.current().isWindows) {
            installerName = "$appName-setup"
            installerOptions.addAll(listOf("--win-per-user-install", "--win-dir-chooser", "--win-menu", "--win-shortcut"))
            installerType = "exe"
        }
        else if (OperatingSystem.current().isLinux) {
            installerOptions.addAll(listOf("--linux-package-name", "kilamea", "--linux-shortcut"))
        }
        else if (OperatingSystem.current().isMacOsX) {
            installerOptions.addAll(listOf("--mac-package-name", "kilamea"))
            installerType = "dmg"
        }
    }
}
