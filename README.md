# unmeta

An Android Gradle plugin to remove all Kotlin `@Metadata`, `@DebugMetadata` and `@SourceDebugExtension` annotations from the build output.

Kotlin `@Metadata`, `@DebugMetadata` and `@SourceDebugExtension` annotations are not fully processed by ProGuard / R8 and contain un-obfuscated symbol information, both in binary and plain text forms. This information can be used to more easily reverse engineer your code.

This plugin allows removing all Kotlin `@Metadata` / `@DebugMetadata` / `@SourceDebugExtension` annotations from generated class files. This is safe to do as long as:

* you do not intend to use the resulting binaries as a Kotlin library (`@Metadata` annotations are used to determine Kotlin function definitions),
* you are not using Kotlin Reflection (certain reflection functionality depends on the presence of the `@Metadata` annotations).

Version 2.0.0 uses the AGP Instrumentation API (`AsmClassVisitorFactory`) and requires Android Gradle Plugin 7.1+ (tested up to 8.9.3). The old Transform API used by 1.x was removed in AGP 8.0.

## Usage

Add JitPack to the plugin repositories in your `settings.gradle`:

```groovy
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { url 'https://jitpack.io' }
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == 'com.github.oliverjonas.unmeta') {
                useModule("com.github.oliver-jonas.unmeta:unmeta:${requested.version}")
            }
        }
    }
}
```

Then apply the plugin in your app `build.gradle` (after the Android plugin):

```groovy
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
    id 'com.github.oliverjonas.unmeta' version '2.0.0'
}
```

Alternatively, the classic `buildscript` style still works:

```groovy
buildscript {
    repositories {
        maven { url 'https://jitpack.io' }
    }
    dependencies {
        classpath 'com.github.oliver-jonas.unmeta:unmeta:2.0.0'
    }
}

apply plugin: 'com.github.oliverjonas.unmeta'
```

## Configuration

```groovy
unmeta {
    // Remove metadata annotations (default: true)
    enabled = true
    // Also remove metadata from library dependencies such as androidx /
    // kotlin-stdlib, not just this module's classes (default: false)
    processDependencies = false
    // Where to write the removal report
    // (default: build/outputs/logs/unmeta-report.txt)
    reportFile = layout.buildDirectory.file('outputs/logs/unmeta-report.txt')
}
```

`processDependencies` is disabled by default for build speed: instrumenting
every dependency prevents the Android Gradle plugin from reusing its pre-dexed
library caches, which makes builds somewhat slower. If you want metadata
annotations removed from dependency classes as well (androidx, kotlin-stdlib,
coroutines, etc.), set it to `true` and expect somewhat slower build times.

To enable the plugin only for release builds:

```groovy
unmeta {
    enabled = gradle.startParameter.taskNames.any { it.toLowerCase().contains('release') }
}
```

## Report

The plugin writes one line for each removed annotation to
`build/outputs/logs/unmeta-report.txt`:

```
Removed @DebugMetadata annotation from com.github.oliverjonas.unmeta.MainActivity$onCreate$1
Removed @Metadata annotation from com.github.oliverjonas.unmeta.MainActivity$onCreate$1
Removed @Metadata annotation from com.github.oliverjonas.unmeta.MainActivity
Removed @Metadata annotation from com.github.oliverjonas.unmeta.SuspendTest
Removed @Metadata annotation from _COROUTINE.ArtificialStackFrames
Removed @Metadata annotation from _COROUTINE.CoroutineDebuggingKt
Removed @SourceDebugExtension annotation from _COROUTINE.CoroutineDebuggingKt
.
.
.
```

The report is rewritten each time class instrumentation actually runs. If the
build is up to date (`transformDebugClassesWithAsm UP-TO-DATE` in the build
log), the classes were already instrumented in a previous build and the report
from that run is kept. With `processDependencies = true`, dependency classes
are instrumented inside cached Gradle artifact transforms, so their report
entries may only appear in the build where the dependency is first processed.

## Building this project

The repository contains the plugin itself in `plugin/` (an included build) and a sample app in `app/` that applies it. Open the root project in Android Studio or run:

```
./gradlew :app:assembleRelease
```
