package org.shadrin.hashchecker.window.tree

import com.intellij.psi.PsiElement
import org.shadrin.hashchecker.model.ChecksumComparison
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.MutableTreeNode

data class ArtifactChecksumTreeNode(
    val artifactChecksumInfo: ArtifactChecksumInfo,
    val children: MutableList<ArtifactChecksumTreeNode> = mutableListOf(),
    var parent: ArtifactChecksumTreeNode? = null
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

fun ArtifactChecksumTreeNode.findNode(artifactId: String): ArtifactChecksumTreeNode? {
    this.children.forEach {
        return if(it.artifactChecksumInfo.artifactId == artifactId) it else it.findNode(artifactId)
    }
    return null
}

fun ArtifactChecksumTreeNode.toMutableTreeNode(): MutableTreeNode {
    val node = DefaultMutableTreeNode(this.artifactChecksumInfo)
    for(child in children) {
        node.add(child.toMutableTreeNode())
    }
    return node
}