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

val HEX = charArrayOf(
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    'a', 'b', 'c', 'd', 'e', 'f'
)

val VISIBILITY_FILTER_KEY = Key<VisibilityFilter>("showVerifiedChecksum")

// Q: How could the current approach hurt performance?
/* A: String.format is quite expensive method here as it does a lot of work inside, creates formatter for each
    iteration of my cycle.
    Better to use something like { byte -> Integer.toHexString(byte.toInt()) } (which is based on bitwise shifting)
    but if we want to get rid of all excessive classes like StringBuilder etc. we can implement it manually like below:
 */
fun ByteArray.toHex(): String {
    val result = CharArray(2 * size)
    var j = 0
    for (i in indices) {
        result[j++] = HEX[0xF0 and this[i].toInt() ushr 4]
        result[j++] = HEX[0x0F and this[i].toInt()]
    }
    return String(result)
}

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