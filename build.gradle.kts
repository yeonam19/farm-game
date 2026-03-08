buildscript {
    repositories {
        mavenCentral()
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
    }
}

allprojects {
    repositories {
        mavenCentral()
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
        maven("https://jitpack.io")
    }

    group = "com.farmgame"
    version = "1.0.0"
}

plugins {
    kotlin("jvm") version "1.9.22" apply false
}
