package org.shadrin.hashchecker.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressManager
import org.shadrin.hashchecker.execution.ChecksumUpdater
import java.util.logging.Logger

class AnalyzeHashesAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.let {
            ProgressManager.getInstance().run(ChecksumUpdater(it))
        }
    }
}