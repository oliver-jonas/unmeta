# unmeta

An Android Gradle plugin to remove all Kotlin @Metadata annotations from the build output.

Kotlin @Metadata annotations are not fully processed by ProGuard / R8 and contain unobfuscated symbol information, both in binary and plain text forms. This information can be used to more easily reverse engineer your code.

This plugin allows removing all Kotlin @Metadata annotations from generated class files. This is safe to do as long as:

* you do not intend to use the resulting binaries as a Kotlin library (@Metadata annotations are used to determine Kotlin function definitions),
* you are not using Kotlin Reflection (certain reflection functionality depends on the presence of the @Metadata annotations).

To use the plugin add the following to your app `build.gradle`:

```
buildscript {
    repositories {
        .
        .
        maven { url 'https://jitpack.io' }
    }

    dependencies {
        .
        .
        classpath 'com.github.oliver-jonas.unmeta:unmeta:1.0.0'
    }
}
.
.
apply plugin: 'com.github.oliverjonas.unmeta'
```

To enable the plugin only for release builds add this section:

```
gradle.taskGraph.whenReady { taskGraph ->
    if (!taskGraph.getAllTasks().any { it.name.toLowerCase().contains('release') }) {
        unmeta.enabled = false      
        println sprintf('Unmeta disabled')
    } else {
        unmeta.enabled = true       
        println sprintf('Unmeta enabled')
    }
}
```

In the Gradle build log you will see one line for each class where the plugin removed the @Metadata annotation:

```
Removed @kotlin.Metadata from .../MainActivity.class
```
