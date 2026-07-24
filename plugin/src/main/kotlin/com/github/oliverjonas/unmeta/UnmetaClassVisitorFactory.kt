package com.github.oliverjonas.unmeta

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import com.android.build.api.instrumentation.InstrumentationParameters
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassVisitor
import java.io.File

interface UnmetaParameters : InstrumentationParameters {
    @get:Input
    val enabled: Property<Boolean>

    @get:Internal
    val reportFile: Property<String>
}

abstract class UnmetaClassVisitorFactory : AsmClassVisitorFactory<UnmetaParameters> {

    override fun createClassVisitor(
        classContext: ClassContext,
        nextClassVisitor: ClassVisitor
    ): ClassVisitor {
        if (!parameters.get().enabled.get()) {
            return nextClassVisitor
        }
        val reportFile = File(parameters.get().reportFile.get())
        return UnmetaClassVisitor(
            classContext.currentClassData.className,
            instrumentationContext.apiVersion.get(),
            nextClassVisitor
        ) { message -> UnmetaReport.append(reportFile, message) }
    }

    override fun isInstrumentable(classData: ClassData): Boolean = true
}

class UnmetaClassVisitor(
    private val className: String,
    api: Int,
    next: ClassVisitor,
    private val log: (String) -> Unit
) : ClassVisitor(api, next) {

    override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor? {
        return when (descriptor) {
            "Lkotlin/Metadata;" -> {
                log("Removed @Metadata annotation from $className")
                null
            }
            "Lkotlin/coroutines/jvm/internal/DebugMetadata;" -> {
                log("Removed @DebugMetadata annotation from $className")
                null
            }
            "Lkotlin/jvm/internal/SourceDebugExtension;" -> {
                log("Removed @SourceDebugExtension annotation from $className")
                null
            }
            else -> super.visitAnnotation(descriptor, visible)
        }
    }
}

/** Serializes report writes across the visitor instances of a build process. */
internal object UnmetaReport {
    private val lock = Any()

    fun append(file: File, message: String) {
        synchronized(lock) {
            file.parentFile?.mkdirs()
            file.appendText(message + System.lineSeparator())
        }
    }
}
