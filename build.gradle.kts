import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.31"
    id("org.jetbrains.compose") version "0.3.2"
}

group = "group.cbpwds"
version = "1.0"

repositories {
    jcenter()
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation("org.xerial:sqlite-jdbc:3.34.0")
    implementation("org.json:json:20210307")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

compose.desktop {
    application {
        mainClass = "GUIKt"
        nativeDistributions {
            targetFormats(TargetFormat.Msi)
            packageName = "simple-internet-banking"
            packageVersion = "1.0.0"
        }
    }
}