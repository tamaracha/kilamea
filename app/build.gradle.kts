plugins {
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    alias(libs.plugins.jvm)

    // Apply the application plugin to add support for building a CLI application in Java.
    application
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
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
    // Use local swt files because eclipse is not able to build working maven packages
    implementation(fileTree("../lib").include("*.jar"))

        // Use the Kotlin JUnit 5 integration.
    testImplementation(libs.kotlin.test.junit5)

    // Use the JUnit 5 integration.
    testImplementation(libs.junit.jupiter.engine)

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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
}

tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
}
