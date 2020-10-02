import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    javafx
    shadow
    kotlin("jvm") version Versions.kotlin
    kotlin("plugin.serialization") version Versions.kotlin
    kotlin("kapt") version Versions.kotlin
}

application {
    // Define the main class for the application
    mainClassName = "xyz.cssxsh.dlsite.Loader"
}

javafx {
    version = Versions.javafx
    modules("javafx.controls", "javafx.fxml")
}

dependencies {
    // kotlin
    implementation(kotlin("stdlib", Versions.kotlin))
    implementation(kotlinx("coroutines-core", Versions.coroutines))
    implementation(project(":console"))
    // jmetro
    implementation(jmetro(Versions.jmetro))
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