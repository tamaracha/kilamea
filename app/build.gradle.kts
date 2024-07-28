import org.gradle.nativeplatform.platform.OperatingSystem
import org.gradle.nativeplatform.platform.Architecture
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

plugins {
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    alias(libs.plugins.jvm)

    // Apply the application plugin to add support for building a CLI application in Java.
    application
    alias(libs.plugins.badass.runtime)
}

val currentOs: OperatingSystem = DefaultNativePlatform.getCurrentOperatingSystem()
val currentArch: Architecture = DefaultNativePlatform.getCurrentArchitecture()

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
    implementation(libs.jface)
    implementation(libs.commands)
    // Load platform-native SWT dependency
    if (currentOs.isWindows) {
        if(currentArch.name == "x86-64") {
            implementation(libs.swt.windows.x64)
        } else if(currentArch.name == "aarch64") {
            implementation(libs.swt.windows.aarch64)
        }
    }
    else if (currentOs.isMacOsX) {
        if(currentArch.name == "x86-64") {
            implementation(libs.swt.mac.x64)
        } else if(currentArch.name == "aarch64") {
            implementation(libs.swt.mac.aarch64)
        }
    }
    else if (currentOs.isLinux) {
        if(currentArch.name == "x86-64") {
            implementation(libs.swt.linux.x64)
        } else if(currentArch.name == "aarch64") {
            implementation(libs.swt.linux.aarch64)
        }
    }
    // Load SWT language pack
    implementation(fileTree("../lib").include("*.jar"))

        // Use the Kotlin JUnit 5 integration.
    testImplementation(libs.kotlin.test.junit5)

    // Use the JUnit 5 integration.
    testImplementation(libs.junit.jupiter.engine)

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.slf4j.simple)
}

// Prevent automagic platform detection hack for SWT which doesn't work with gradle
configurations {
    implementation {
        exclude(group = "org.eclipse.platform", module = "org.eclipse.swt.\${osgi.platform}")
    }
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
    if (currentOs.isMacOsX) {
        applicationDefaultJvmArgs = listOf("-XstartOnFirstThread")
    }
}

tasks.named<Jar>("jar") {
    dependsOn(configurations.runtimeClasspath)

    manifest {
        attributes(mapOf(
            "Main-Class" to application.mainClass,
            "Class-Path" to configurations.runtimeClasspath.get().joinToString(" ") { it.name }
        ))
    }
}

tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
}

runtime {
    options = listOf("--strip-debug", "--compress", "zip-6", "--no-header-files", "--no-man-pages")
    modules = listOf("java.naming", "java.logging", "java.sql")

    launcher {
        noConsole = true
    }

    jpackage {
        val appName = "Kilamea"
        imageName = appName
        installerName = appName
        installerOptions = mutableListOf("--resource-dir", "src/main/resources")
        if(currentOs.isWindows) {
            installerName = "$appName-setup"
            installerOptions.addAll(listOf("--win-per-user-install", "--win-dir-chooser", "--win-menu", "--win-shortcut"))
            installerType = "exe"
        }
        else if (currentOs.isLinux) {
            installerOptions.addAll(listOf("--linux-package-name", "kilamea", "--linux-shortcut"))
        }
        else if (currentOs.isMacOsX) {
            installerOptions.addAll(listOf("--mac-package-name", "kilamea"))
            installerType = "dmg"
        }
    }
}
