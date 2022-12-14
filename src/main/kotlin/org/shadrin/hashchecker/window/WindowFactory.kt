package org.shadrin.hashchecker.window

import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataImportListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import org.shadrin.hashchecker.execution.ChecksumUpdaterProjectImportListener
import org.shadrin.hashchecker.listener.ChecksumUpdateListener
import org.shadrin.hashchecker.listener.TreeFilterStateListener


class WindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val checksumToolWindow = ChecksumToolWindow(project)
        val contentFactory = ContentFactory.getInstance()
        val content: Content = contentFactory.createContent(checksumToolWindow.getContent(), "", false)
        toolWindow.contentManager.addContent(content)
        project.messageBus.connect().subscribe(ChecksumUpdateListener.TOPIC, checksumToolWindow)
        project.messageBus.connect().subscribe(TreeFilterStateListener.TOPIC, checksumToolWindow)
        project.messageBus.connect().subscribe(ProjectDataImportListener.TOPIC, ChecksumUpdaterProjectImportListener(project))

    }
}