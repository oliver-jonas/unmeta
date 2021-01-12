package com.github.oliverjonas.unmeta

import com.android.build.gradle.BaseExtension
import org.gradle.api.GradleException
import org.gradle.api.Project

fun Project.android(): BaseExtension {
    val android = project.extensions.findByType(BaseExtension::class.java)
    if (android != null) {
        return android
    } else {
        throw GradleException("Project $name is not an Android project")
    }
}