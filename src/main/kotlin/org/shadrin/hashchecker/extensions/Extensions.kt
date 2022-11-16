package org.shadrin.hashchecker.extensions

import com.intellij.externalSystem.DependencyModifierService
import com.intellij.openapi.externalSystem.dependency.analyzer.DAArtifact
import com.intellij.openapi.externalSystem.dependency.analyzer.DAModule
import com.intellij.openapi.externalSystem.dependency.analyzer.DependencyAnalyzerDependency
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.plugins.gradle.dependency.analyzer.getParentModule
import org.jetbrains.plugins.gradle.dependency.analyzer.getUnifiedCoordinates
import org.shadrin.hashchecker.window.tree.MyTreeNodeType
import java.io.File
import java.security.MessageDigest

fun ByteArray.toHex() = joinToString(separator = "") { byte -> "%02x".format(byte) }

fun File.calculateChecksum(): String {
    this.inputStream().use { fis ->
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(fis.readAllBytes()).toHex()
    }
}

fun DependencyAnalyzerDependency.toChecksumComparisonStatus() = when(this.data) {
    is DAModule -> MyTreeNodeType.MODULE
    is DAArtifact -> MyTreeNodeType.ARTIFACT
    else -> MyTreeNodeType.UNKNOWN
}

fun DependencyAnalyzerDependency.getPsi(project: Project): PsiElement? {
    val dependency = getUnifiedCoordinates(this)?.let { coordinates ->
        getParentModule(project, this)?.let { module ->
            val dependencyModifierService = DependencyModifierService.getInstance(project)
            dependencyModifierService.declaredDependencies(module)
                .find { it.coordinates == coordinates }
        }
    }
    return dependency?.psiElement
}