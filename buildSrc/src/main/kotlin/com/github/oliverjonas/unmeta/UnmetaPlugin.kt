@file:Suppress("unused")

package com.github.oliverjonas.unmeta

import com.android.build.api.transform.*
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.internal.pipeline.TransformManager
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.objectweb.asm.*
import java.io.File

class UnmetaPlugin : Plugin<Project> {

    private lateinit var extension: UnmetaExtension

    override fun apply(project: Project) {
        extension = project.extensions.create("unmeta", UnmetaExtension::class.java)

        val android = project.android()
        android.registerTransform(UnmetaTransform(extension, android))
    }
}

open class UnmetaExtension {
    var isEnabled: Boolean = true
}

class UnmetaTransform(
    private val extension: UnmetaExtension,
    private val android: BaseExtension
) : Transform() {

    override fun getName(): String {
        return "RemoveKotlinMetadata"
    }

    override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> {
        return TransformManager.CONTENT_CLASS
    }

    override fun getScopes(): MutableSet<in QualifiedContent.Scope> {
        return TransformManager.PROJECT_ONLY
    }

    override fun isIncremental(): Boolean {
        return false
    }

    override fun transform(invocation: TransformInvocation) {

        if (!invocation.isIncremental) {
            invocation.outputProvider.deleteAll()
        }

        val inputs = invocation.inputs.flatMap { it.jarInputs + it.directoryInputs }
        val outputs = inputs.map { input ->
            val format = if (input is JarInput) Format.JAR else Format.DIRECTORY
            invocation.outputProvider.getContentLocation(
                input.name,
                input.contentTypes,
                input.scopes,
                format
            )
        }

        if (!extension.isEnabled) {
            copyInputsToOutputs(inputs.map { it.file }, outputs)
            return
        }

        for ((index, input) in inputs.withIndex()) {

            val file = File(input.file.absolutePath)
            val output = outputs[index]

            file.walk().forEach {
                if (it.isFile) {

                    val sourceClassBytes = it.readBytes()
                    var modifiedClassBytes: ByteArray? = null

                    if (it.name.endsWith(".class")) {
                        modifiedClassBytes = modifyClass(it.path, sourceClassBytes)
                    }

                    if (modifiedClassBytes == null) {
                        modifiedClassBytes = sourceClassBytes
                    }

                    val relativePath =
                        it.absolutePath.replace(file.absolutePath, "")

                    val target = File(output.absolutePath + relativePath)
                    target.parentFile.mkdirs()
                    target.writeBytes(modifiedClassBytes)
                }
            }
        }
    }

    private fun copyInputsToOutputs(inputs: List<File>, outputs: List<File>) {
        inputs.zip(outputs) { input, output ->
            input.copyRecursively(output, overwrite = true)
        }
    }

    private fun modifyClass(path: String, srcClass: ByteArray): ByteArray? {
        val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
        val visitor = UnmetaClassVisitor(path, classWriter)
        val cr = ClassReader(srcClass)
        cr.accept(visitor, 0)
        return if (visitor.modified) classWriter.toByteArray() else null
    }
}

class UnmetaClassVisitor(private val path: String, cv: ClassVisitor) :
    ClassVisitor(Opcodes.ASM4, cv), Opcodes {

    var modified = false

    override fun visitAnnotation(desc: String?, visible: Boolean): AnnotationVisitor? {
        return when (desc) {
            "Lkotlin/Metadata;" -> {
                println("Removed @Metadata annotation from $path")
                modified = true
                null
            }
            "Lkotlin/coroutines/jvm/internal/DebugMetadata;" -> {
                println("Removed @DebugMetadata annotation from $path")
                modified = true
                null
            }
            else -> {
                super.visitAnnotation(desc, visible)
            }
        }
    }
}