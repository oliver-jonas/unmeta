package com.github.oliverjonas.unmeta

import com.android.build.api.instrumentation.FramesComputationMode
import com.android.build.api.instrumentation.InstrumentationScope
import com.android.build.api.variant.AndroidComponentsExtension
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

@Suppress("unused")
class UnmetaPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create("unmeta", UnmetaExtension::class.java)
        extension.reportFile.convention(
            project.layout.buildDirectory.file("outputs/logs/unmeta-report.txt")
        )

        val androidComponents =
            project.extensions.findByType(AndroidComponentsExtension::class.java)
                ?: throw GradleException(
                    "Project ${project.name} is not an Android project. " +
                        "Apply the Android plugin before the unmeta plugin."
                )

        androidComponents.onVariants(androidComponents.selector().all()) { variant ->
            val scope =
                if (extension.processDependencies.get()) InstrumentationScope.ALL
                else InstrumentationScope.PROJECT
            variant.instrumentation.transformClassesWith(
                UnmetaClassVisitorFactory::class.java,
                scope
            ) { params ->
                params.enabled.set(extension.enabled)
                params.reportFile.set(extension.reportFile.map { it.asFile.absolutePath })
            }
            variant.instrumentation.setAsmFramesComputationMode(FramesComputationMode.COPY_FRAMES)
        }

        // Start each instrumentation run with a fresh report. If the transform
        // task is UP-TO-DATE the report from its last actual run is kept.
        val reportFile = extension.reportFile.asFile
        project.tasks.configureEach { task ->
            if (task.name.startsWith("transform") && task.name.endsWith("ClassesWithAsm")) {
                task.doFirst { reportFile.get().delete() }
            }
        }
    }
}
