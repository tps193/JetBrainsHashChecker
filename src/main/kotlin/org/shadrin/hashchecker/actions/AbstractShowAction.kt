package org.shadrin.hashchecker.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import org.shadrin.hashchecker.extensions.getVisibilityFilter
import org.shadrin.hashchecker.extensions.setVisibilityFilter
import org.shadrin.hashchecker.listener.TreeFilterStateListener
import org.shadrin.hashchecker.model.VisibilityFilter

abstract class AbstractShowAction : ToggleAction() {
    override fun isSelected(e: AnActionEvent): Boolean {
        val filter = e.project?.getVisibilityFilter() ?: VisibilityFilter.default
        return isSelectedImpl(filter)
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        e.project?.apply {
            val newFilter = getVisibilityFilter().run {
                modifyFilter(this, state)
            }
            setVisibilityFilter(newFilter)
            messageBus.syncPublisher(TreeFilterStateListener.TOPIC).refreshTree(newFilter)
        }
    }

    protected abstract fun modifyFilter(filter: VisibilityFilter, state: Boolean): VisibilityFilter

    protected abstract fun isSelectedImpl(filter: VisibilityFilter): Boolean
}