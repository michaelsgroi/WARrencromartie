plugins {
    kotlin("jvm") version "1.9.20"
    id("com.github.ben-manes.versions") version "0.49.0"
    id("com.diffplug.spotless") version "6.22.0"
    application
}

group = "com.michaelsgroi"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.1")
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
    jvmToolchain(17)
}

application {
    mainClass.set("MainKt")
}