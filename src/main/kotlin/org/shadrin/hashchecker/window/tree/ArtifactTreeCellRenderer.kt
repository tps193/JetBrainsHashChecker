package org.shadrin.hashchecker.window.tree

import com.intellij.icons.AllIcons
import org.shadrin.hashchecker.model.ChecksumComparisonStatus
import java.awt.Component
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer

class ArtifactTreeCellRenderer : DefaultTreeCellRenderer() {

    override fun getTreeCellRendererComponent(
        tree: JTree?,
        value: Any?,
        sel: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean
    ): Component {
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus)
        if (value is DefaultMutableTreeNode) {
            val data = value.userObject
            if (data is ArtifactChecksumInfo) {
                text = data.artifactId
                icon = when(data.type) {
                    ArtifactNodeType.MODULE -> AllIcons.Modules.SourceRoot
                    else -> {
                        when(data.checksumVerification.status) {
                            is ChecksumComparisonStatus.Error -> AllIcons.Ide.FatalError
                            ChecksumComparisonStatus.OK -> AllIcons.Ide.Rating
                            else -> AllIcons.FileTypes.Unknown
                        }
                    }
                }
            }
        }
        return this
    }
}