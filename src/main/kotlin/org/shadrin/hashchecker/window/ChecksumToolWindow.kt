package org.shadrin.hashchecker.window

import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.externalSystem.dependency.analyzer.DependencyAnalyzerDependency
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.Project
import com.intellij.ui.treeStructure.Tree
import org.jetbrains.plugins.gradle.dependency.analyzer.GradleDependencyAnalyzerContributor
import org.shadrin.hashchecker.extensions.getPsi
import org.shadrin.hashchecker.extensions.toChecksumComparisonStatus
import org.shadrin.hashchecker.listener.ChecksumUpdateListener
import org.shadrin.hashchecker.model.ChecksumComparison
import org.shadrin.hashchecker.model.ChecksumComparisonStatus
import org.shadrin.hashchecker.window.tree.ArtifactChecksumTreeNode
import org.shadrin.hashchecker.window.tree.MyTreeNodeType
import org.shadrin.hashchecker.window.tree.toMutableTreeNode
import java.awt.GridLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeNode

class ChecksumToolWindow(private val project: Project) : ChecksumUpdateListener {

    private val treeModel: DefaultTreeModel = DefaultTreeModel(DefaultMutableTreeNode("No result"))

    fun getContent(): JComponent {
        val mainPanel = JPanel()
        val layout = GridLayout().also {
            it.rows = 2
        }
        val artifactsTree = Tree()

        artifactsTree.model = treeModel
        artifactsTree.isHorizontalAutoScrollingEnabled = true
        artifactsTree.scrollsOnExpand = true
        artifactsTree.showsRootHandles = false
        artifactsTree.autoscrolls = true

        mainPanel.layout = layout
        mainPanel.add(artifactsTree)
        val descriptionPanel = JPanel()
        mainPanel.add(descriptionPanel)
        return mainPanel
    }

    private fun updateTree(rootNode: TreeNode) {
        treeModel.setRoot(rootNode)
    }

    override fun notify(checksumComparisonResult: List<ChecksumComparison>) {
        updateTree(DefaultMutableTreeNode("Loading..."))
        runBackgroundableTask("Update checksum tree", project, false) {
            val tree = buildResultsTree(checksumComparisonResult)
            invokeLater {
                updateTree(tree)
            }
        }
    }

    private fun buildResultsTree(checksumComparisonResult: List<ChecksumComparison>): TreeNode {
        val checksumVerification = checksumComparisonResult.associateBy { it.artifactId }
        val rootNode = ArtifactChecksumTreeNode(
            "root",
            ChecksumComparison(artifactId = "stub", status = ChecksumComparisonStatus.UNKNOWN),
            MyTreeNodeType.UNKNOWN
        )
        val gradleDependencyAnalyzerContributor = GradleDependencyAnalyzerContributor(project)
        gradleDependencyAnalyzerContributor.getProjects().forEach { it ->
            val dependencies = gradleDependencyAnalyzerContributor.getDependencies(it)
            dependencies.forEach { dependency ->
                val stack = ArrayDeque<DependencyAnalyzerDependency>()
                var nextDependency: DependencyAnalyzerDependency? = dependency
                do {
                    stack.add(nextDependency!!)
                    nextDependency = nextDependency.parent
                } while(nextDependency != null)

                var parentNode = rootNode
                while(stack.isNotEmpty()) {
                    val next = stack.removeLast()
                    val id = next.data.toString()
                    var existing: ArtifactChecksumTreeNode? = parentNode.children.find { it.artifactId == id }
                    if (existing == null) {
                        val newNode = ArtifactChecksumTreeNode(
                            artifactId = id,
                            checksumVerification = checksumVerification[id] ?: ChecksumComparison(
                                artifactId = id,
                                status = ChecksumComparisonStatus.Skipped("No information for artifact")
                            ),
                            type = next.toChecksumComparisonStatus(),
                            psi = next.getPsi(project),
                            children = mutableListOf(),
                            parent = parentNode
                        )
                        parentNode.children.add(newNode)
                        existing = newNode
                    }
                    parentNode = existing
                }
            }
        }

        return rootNode.toMutableTreeNode()
    }
}