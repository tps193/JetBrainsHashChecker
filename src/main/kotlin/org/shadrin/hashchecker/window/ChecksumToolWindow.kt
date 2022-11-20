package org.shadrin.hashchecker.window

import com.intellij.codeInsight.navigation.NavigationUtil
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.externalSystem.dependency.analyzer.DependencyAnalyzerDependency
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.castSafelyTo
import org.jetbrains.plugins.gradle.dependency.analyzer.GradleDependencyAnalyzerContributor
import org.shadrin.hashchecker.extensions.DIGEST_ALGORITHM
import org.shadrin.hashchecker.extensions.getArtifactNodeType
import org.shadrin.hashchecker.extensions.getPsi
import org.shadrin.hashchecker.listener.ChecksumUpdateListener
import org.shadrin.hashchecker.model.ChecksumComparison
import org.shadrin.hashchecker.model.ChecksumComparisonStatus
import org.shadrin.hashchecker.window.tree.*
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeNode


class ChecksumToolWindow(private val project: Project) : ChecksumUpdateListener {

    private val treeModel: DefaultTreeModel = DefaultTreeModel(DefaultMutableTreeNode("No result"))
    private val artifactsTree = Tree()
    private val goToButton = JButton("Go to")

    fun getContent(): JComponent {
        val mainPanel = JPanel().apply {
            layout = GridBagLayout()
        }

        artifactsTree.cellRenderer = ArtifactTreeCellRenderer()
        artifactsTree.model = treeModel

        val scrollPane = JBScrollPane(artifactsTree)

        val constraint = GridBagConstraints().apply {
            gridx = 0
            gridy = 0
            weighty = 1.0
            weightx = 1.0
            fill = GridBagConstraints.BOTH
        }
        mainPanel.add(scrollPane, constraint)

        val descriptionPanel = JPanel()
        descriptionPanel.layout = GridBagLayout()
        val descriptionText = JBTextArea().apply {
            isEditable = false
            lineWrap = true
        }
        val descriptionPanelConstraints = GridBagConstraints().apply {
            fill = GridBagConstraints.BOTH
            weightx = 1.0
            gridx = 0
        }
        descriptionPanel.add(descriptionText, descriptionPanelConstraints)

        descriptionPanelConstraints.gridy = 1
        descriptionPanelConstraints.anchor = GridBagConstraints.CENTER

        descriptionPanel.add(goToButton, descriptionPanelConstraints)
        goToButton.isVisible = false

        constraint.apply {
            gridy++
            weighty = 0.1
        }
        mainPanel.add(descriptionPanel, constraint)

        artifactsTree.addTreeSelectionListener {
            val selection = artifactsTree.lastSelectedPathComponent
            if (selection is DefaultMutableTreeNode) {
                selection.userObject.castSafelyTo<ArtifactChecksumInfo>()?.let {
                    val verification = it.checksumVerification
                    val status = verification.status
                    descriptionText.text = when(status) {
                        is ChecksumComparisonStatus.Error -> """
                            ${status.msg}
                            Local($DIGEST_ALGORITHM): ${verification.localChecksum}
                            Server($DIGEST_ALGORITHM): ${verification.serverChecksum}
                        """.trimIndent()
                        is ChecksumComparisonStatus.Skipped -> status.msg
                        is ChecksumComparisonStatus.OK -> """
                            Verified
                            ($DIGEST_ALGORITHM): ${verification.localChecksum}
                        """.trimIndent()
                        is ChecksumComparisonStatus.NOT_IMPORTED -> "Not imported to Module dependencies"
                        else -> ""
                    }
                    goToButton.isVisible = it.psi != null
                }
            }
        }

        goToButton.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                val selection = artifactsTree.lastSelectedPathComponent
                val data = selection.castSafelyTo<DefaultMutableTreeNode>()
                    ?.userObject
                    ?.castSafelyTo<ArtifactChecksumInfo>()
                data?.psi?.let {
                    if (it.isValid) {
                        NavigationUtil.activateFileWithPsiElement(it)
                    }
                }
            }
        })

        return mainPanel
    }

    private fun updateTree(rootNode: TreeNode) {
        treeModel.setRoot(rootNode)
    }

    override fun notify(checksumComparisonResult: List<ChecksumComparison>) {
        updateTree(DefaultMutableTreeNode("Loading..."))
        goToButton.isVisible = false
        artifactsTree.isRootVisible = true
        runBackgroundableTask("Update checksum tree", project, false) {
            val tree = buildResultsTree(checksumComparisonResult)
            invokeLater {
                updateTree(tree)
                artifactsTree.isRootVisible = false
            }
        }
    }

    private fun buildResultsTree(checksumComparisonResult: List<ChecksumComparison>): TreeNode {
        val checksumVerification = checksumComparisonResult.associateBy { it.artifactId }
        val rootNode = ArtifactChecksumTreeNode(
            ArtifactChecksumInfo(
                "root",
                ChecksumComparison(artifactId = "stub", status = ChecksumComparisonStatus.UNKNOWN),
                ArtifactNodeType.UNKNOWN)
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
                    var existing: ArtifactChecksumTreeNode? = parentNode.children.find { it.artifactChecksumInfo.artifactId == id }
                    if (existing == null) {
                        val type = next.getArtifactNodeType()
                        val newNode = ArtifactChecksumTreeNode(
                            artifactChecksumInfo = ArtifactChecksumInfo(
                                artifactId = id,
                                checksumVerification = checksumVerification[id] ?: ChecksumComparison(
                                    artifactId = id,
                                    status = ChecksumComparisonStatus.NOT_IMPORTED
                                ),
                                type = type,
                                psi = next.getPsi(project) ?: parentNode.artifactChecksumInfo.psi
                            ),
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