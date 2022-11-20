package org.shadrin.hashchecker.extensions

import com.intellij.externalSystem.DependencyModifierService
import com.intellij.openapi.externalSystem.dependency.analyzer.DAArtifact
import com.intellij.openapi.externalSystem.dependency.analyzer.DAModule
import com.intellij.openapi.externalSystem.dependency.analyzer.DependencyAnalyzerDependency
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import org.jetbrains.plugins.gradle.dependency.analyzer.getParentModule
import org.jetbrains.plugins.gradle.dependency.analyzer.getUnifiedCoordinates
import org.shadrin.hashchecker.model.VisibilityFilter
import org.shadrin.hashchecker.window.tree.ArtifactNodeType
import java.io.File
import java.security.MessageDigest

const val DIGEST_ALGORITHM = "SHA-256"

val VISIBILITY_FILTER_KEY = Key<VisibilityFilter>("showVerifiedChecksum")

fun ByteArray.toHex() = joinToString(separator = "") { byte -> "%02x".format(byte) }

fun File.calculateChecksum(): String {
    this.inputStream().use { fis ->
        val digest = MessageDigest.getInstance(DIGEST_ALGORITHM)
        return digest.digest(fis.readAllBytes()).toHex()
    }
}

fun DependencyAnalyzerDependency.getArtifactNodeType() = when(this.data) {
    is DAModule -> ArtifactNodeType.MODULE
    is DAArtifact -> ArtifactNodeType.ARTIFACT
    else -> ArtifactNodeType.UNKNOWN
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

fun String.toTrimmerUrl() = this.trimEnd('/')

fun Project.getVisibilityFilter() = this.getUserData(VISIBILITY_FILTER_KEY) ?: VisibilityFilter.default

fun Project.setVisibilityFilter(filter: VisibilityFilter) = this.putUserData(VISIBILITY_FILTER_KEY, filter)