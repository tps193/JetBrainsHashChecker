package org.shadrin.hashchecker.window

import com.intellij.codeInsight.navigation.NavigationUtil
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.externalSystem.dependency.analyzer.DependencyAnalyzerDependency
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.castSafelyTo
import org.jetbrains.plugins.gradle.dependency.analyzer.GradleDependencyAnalyzerContributor
import org.shadrin.hashchecker.extensions.DIGEST_ALGORITHM
import org.shadrin.hashchecker.extensions.getArtifactNodeType
import org.shadrin.hashchecker.extensions.getPsi
import org.shadrin.hashchecker.extensions.getVisibilityFilter
import org.shadrin.hashchecker.listener.ChecksumUpdateListener
import org.shadrin.hashchecker.listener.TreeFilterStateListener
import org.shadrin.hashchecker.model.ChecksumComparison
import org.shadrin.hashchecker.model.ChecksumComparisonStatus
import org.shadrin.hashchecker.model.VisibilityFilter
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
import javax.swing.tree.TreePath


class ChecksumToolWindow(private val project: Project) : ChecksumUpdateListener, TreeFilterStateListener {

    private val treeModel: DefaultTreeModel = DefaultTreeModel(DefaultMutableTreeNode("No result"))
    private val artifactsTree = Tree()
    private val goToButton = JButton("Go to")

    private var artifactChecksumTreeRoot: ArtifactChecksumTreeNode = createStubChecksumTreeNode()
    fun getContent(): JComponent {
        val simpleToolWindowPanel = SimpleToolWindowPanel(true)

        val mainPanel = JPanel().apply {
            layout = GridBagLayout()
        }
        simpleToolWindowPanel.setContent(mainPanel)

        val actionManager = ActionManager.getInstance()
        val toolbar = actionManager.createActionToolbar("Any", (actionManager
            .getAction("ChecksumVerification.View") as DefaultActionGroup), true)
        toolbar.targetComponent = simpleToolWindowPanel
        simpleToolWindowPanel.toolbar = toolbar.component

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

        return simpleToolWindowPanel
    }

    private fun updateTree(rootNode: TreeNode) {
        treeModel.setRoot(rootNode)
    }

    override fun notify(checksumComparisonResult: List<ChecksumComparison>) {
        updateTree(DefaultMutableTreeNode("Loading..."))
        goToButton.isVisible = false
        artifactsTree.isRootVisible = true
        runBackgroundableTask("Update checksum tree", project, false) {
            buildResultsTree(checksumComparisonResult)
            val newRoot = artifactChecksumTreeRoot.toMutableTreeNode(project.getVisibilityFilter())
            invokeLater {
                updateTree(newRoot)
                artifactsTree.isRootVisible = false
                expandAllNodes(artifactsTree)
            }
        }
    }

    private fun expandAllNodes(tree: Tree, startingIndex: Int = 0, rowCount: Int = tree.rowCount) {
        for (i in startingIndex until rowCount) {
            tree.expandRow(i)
        }
        if (tree.rowCount != rowCount) {
            expandAllNodes(tree, rowCount, tree.rowCount)
        }
    }

    private fun buildResultsTree(checksumComparisonResult: List<ChecksumComparison>) {
        val checksumVerification = checksumComparisonResult.associateBy { it.artifactId }
        val rootNode = createStubChecksumTreeNode()
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
                        if (newNode.artifactChecksumInfo.checksumVerification.status is ChecksumComparisonStatus.Error) {
                            markParentNodesHavingErrorNode(parentNode)
                        }
                        parentNode.children.add(newNode)
                        existing = newNode
                    }
                    parentNode = existing
                }
            }
        }
        artifactChecksumTreeRoot = rootNode
    }

    private fun markParentNodesHavingErrorNode(parentNode: ArtifactChecksumTreeNode) {
        var parent: ArtifactChecksumTreeNode? = parentNode
        while(parent != null && !parent.hasErrorChild) {
            parent.hasErrorChild = true
            parent = parent.parent
        }
    }

    override fun refreshTree(filter: VisibilityFilter) {
        val expansions = artifactsTree.getExpandedDescendants(TreePath(treeModel.root))

        runBackgroundableTask("Refresh checksum tree", project, false) {
            val newRoot = artifactChecksumTreeRoot.toMutableTreeNode(filter)
            invokeLater {
                updateTree(newRoot)
                artifactsTree.isRootVisible = false
                expansions?.asIterator()?.forEach {
                    artifactsTree.expandPath(it)
                }
            }
        }
    }

    private fun createStubChecksumTreeNode() = ArtifactChecksumTreeNode(
        ArtifactChecksumInfo(
        "root",
        ChecksumComparison(artifactId = "stub", status = ChecksumComparisonStatus.UNKNOWN),
        ArtifactNodeType.UNKNOWN)
    )
}