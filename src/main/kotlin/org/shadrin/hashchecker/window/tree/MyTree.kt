package org.shadrin.hashchecker.window.tree

import com.intellij.psi.PsiElement
import org.shadrin.hashchecker.model.ChecksumComparison
import org.shadrin.hashchecker.model.ChecksumComparisonStatus
import org.shadrin.hashchecker.model.VisibilityFilter
import java.util.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.MutableTreeNode

data class ArtifactChecksumTreeNode(
    val artifactChecksumInfo: ArtifactChecksumInfo,
    val children: MutableList<ArtifactChecksumTreeNode> = mutableListOf(),
    var parent: ArtifactChecksumTreeNode? = null,
    var hasErrorChild: Boolean = false
) {
    override fun toString(): String {
        return artifactChecksumInfo.artifactId
    }
}

data class ArtifactChecksumInfo(
    val artifactId: String,
    val checksumVerification: ChecksumComparison,
    val type: ArtifactNodeType,
    val psi: PsiElement? = null,
)

enum class ArtifactNodeType {
    MODULE,
    ARTIFACT,
    UNKNOWN
}

fun ArtifactChecksumTreeNode.toMutableTreeNode(filter: VisibilityFilter): MutableTreeNode {
    val node = MyDefaultMutableTreeNode(this.artifactChecksumInfo)
    for(child in children) {
        val shouldShow = child.hasErrorChild || child.artifactChecksumInfo.let {
            (it.type == ArtifactNodeType.MODULE) ||
                    when(it.checksumVerification.status) {
                        ChecksumComparisonStatus.OK -> filter.showVerified
                        is ChecksumComparisonStatus.Skipped -> filter.showSkipped
                        ChecksumComparisonStatus.NOT_IMPORTED -> filter.showUnimported
                        else -> true
                    }
        }
        if (shouldShow) {
            node.add(child.toMutableTreeNode(filter))
        }
    }
    return node
}

class MyDefaultMutableTreeNode(userObject: Any) : DefaultMutableTreeNode(userObject) {
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other is MyDefaultMutableTreeNode) {
            return (userObject?.equals(other.userObject)) ?: (other.userObject == null)
        }
        return false;
    }

    override fun hashCode(): Int {
        return Objects.hashCode(userObject)
    }
}