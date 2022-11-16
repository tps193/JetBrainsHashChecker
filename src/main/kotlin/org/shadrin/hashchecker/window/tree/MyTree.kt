package org.shadrin.hashchecker.window.tree

import org.shadrin.hashchecker.model.ChecksumComparison
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.MutableTreeNode

data class ArtifactChecksumTreeNode(
    val artifactId: String,
    val checksumVerification: ChecksumComparison,
    val type: MyTreeNodeType,
    val psi: Any? = null,
    val children: MutableList<ArtifactChecksumTreeNode> = mutableListOf(),
    var parent: ArtifactChecksumTreeNode? = null
) {
    override fun toString(): String {
        return artifactId
    }
}

enum class MyTreeNodeType {
    MODULE,
    ARTIFACT,
    UNKNOWN
}

fun ArtifactChecksumTreeNode.findNode(artifactId: String): ArtifactChecksumTreeNode? {
    this.children.forEach {
        return if(it.artifactId == artifactId) it else it.findNode(artifactId)
    }
    return null
}

fun ArtifactChecksumTreeNode.toMutableTreeNode(): MutableTreeNode {
    val node = DefaultMutableTreeNode(this)
    for(child in children) {
        node.add(child.toMutableTreeNode())
    }
    return node
}