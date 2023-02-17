import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.api.tasks.testing.logging.TestLogEvent.*

plugins {
    kotlin("jvm") version "1.8.10"
    application
}

group = "com.michaelsgroi"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
    minHeapSize = "2048m"
    maxHeapSize = "2048m"
    testLogging {
        showStandardStreams = true
    }
}

kotlin {
    jvmToolchain(8)
}

application {
    mainClass.set("MainKt")
}