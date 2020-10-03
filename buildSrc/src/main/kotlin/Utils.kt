@file:Suppress("unused")

import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.plugin.use.PluginDependenciesSpec
import org.gradle.plugin.use.PluginDependencySpec

fun DependencyHandler.kotlinx(module: String, version: String = Versions.kotlin) =
    "org.jetbrains.kotlinx:kotlinx-$module:$version"

fun DependencyHandler.ktor(module: String, version: String = Versions.ktor) =
    "io.ktor:ktor-$module:$version"

fun DependencyHandler.jmetro(version: String = Versions.jmetro) =
    "org.jfxtras:jmetro:$version"

fun DependencyHandler.slf4j(module: String, version: String = Versions.slf4j) =
    "org.slf4j:slf4j-$module:$version"

fun DependencyHandler.log4j(module: String, version: String = Versions.log4j) =
    "org.apache.logging.log4j:log4j-$module:$version"

fun DependencyHandler.javafx(version: String = Versions.javafx) =
    "org.openjfx:javafx:$version"

fun DependencyHandler.jsoup(version: String = Versions.jsoup) =
    "org.jsoup:jsoup:$version"

fun DependencyHandler.okhttp3(module: String, version: String = Versions.okhttp3) =
    "com.squareup.okhttp3:$module:$version"

fun DependencyHandler.atomicfu(version: String = Versions.atomicfu) =
    "org.jetbrains.kotlinx:atomicfu:$version"

fun DependencyHandler.lombok(version: String = Versions.lombok) =
    "org.projectlombok:lombok:$version"

val PluginDependenciesSpec.shadow : PluginDependencySpec
    get() = id("com.github.johnrengelman.shadow").version(Versions.shadow)

val PluginDependenciesSpec.javafx: PluginDependencySpec
    get() = id("org.openjfx.javafxplugin").version("0.0.9")