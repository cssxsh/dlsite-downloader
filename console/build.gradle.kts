import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    shadow
    kotlin("jvm") version Versions.kotlin
    kotlin("plugin.serialization") version Versions.kotlin
    kotlin("kapt") version Versions.kotlin
}

application {
    // Define the main class for the application
    mainClassName = "xyz.cssxsh.dlsite.DLsiteTool"
}

kotlin {
    sourceSets {
        all {
            languageSettings.useExperimentalAnnotation("kotlin.time.ExperimentalTime")
        }
    }
}

dependencies {
    // kotlin
    implementation(kotlin("stdlib", Versions.kotlin))
    implementation(kotlin("serialization", Versions.kotlin))
    implementation(kotlinx("coroutines-core", Versions.coroutines))
    implementation(kotlinx("serialization-runtime", Versions.serialization))
    // ktor
    implementation(ktor("client-core", Versions.ktor))
    implementation(ktor("client-serialization", Versions.ktor))
    implementation(ktor("client-encoding", Versions.ktor))
    // okhttp3
    implementation(ktor("client-okhttp", Versions.ktor)) { exclude(group = "com.squareup.okhttp3") }
    implementation(okhttp3("okhttp", Versions.okhttp3))
    implementation(okhttp3("okhttp-dnsoverhttps", Versions.okhttp3))
    // jsoup
    implementation(jsoup(Versions.jsoup))
    // slf4j
    implementation(slf4j("simple", Versions.slf4j))
    // junit
    testImplementation(group = "org.junit.jupiter", name = "junit-jupiter", version = Versions.junit)
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }
    withType<Test> {
        useJUnitPlatform()
        workingDir = File(rootProject.projectDir, "test").apply { mkdir() }
    }
}