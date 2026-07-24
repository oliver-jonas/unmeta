package com.github.oliverjonas.unmeta

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property

abstract class UnmetaExtension {

    /** Whether to remove Kotlin metadata annotations. Defaults to true. */
    abstract val enabled: Property<Boolean>

    /**
     * Whether to also remove metadata annotations from library dependencies
     * (e.g. androidx, kotlin-stdlib) rather than only this module's classes.
     * Disabled by default because instrumenting all dependencies makes builds
     * somewhat slower. Must be set in the build script (read at configuration
     * time). Defaults to false.
     */
    abstract val processDependencies: Property<Boolean>

    /**
     * File the plugin writes its removal report to.
     * Defaults to build/outputs/logs/unmeta-report.txt.
     */
    abstract val reportFile: RegularFileProperty

    init {
        enabled.convention(true)
        processDependencies.convention(false)
    }
}
