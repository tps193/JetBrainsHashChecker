package org.shadrin.hashchecker.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import org.shadrin.hashchecker.extensions.VISIBILITY_FILTER_KEY
import org.shadrin.hashchecker.extensions.getVisibilityFilter
import org.shadrin.hashchecker.extensions.setVisibilityFilter
import org.shadrin.hashchecker.listener.TreeFilterStateListener
import org.shadrin.hashchecker.model.VisibilityFilter

class ShowSkipped : ToggleAction() {

    override fun isSelected(e: AnActionEvent): Boolean {
        val filter = e.project?.getVisibilityFilter() ?: VisibilityFilter.default
        return filter.showSkipped
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        e.project?.apply {
            val newFilter = getVisibilityFilter().copy(showSkipped = state)
            setVisibilityFilter(newFilter)
            messageBus.syncPublisher(TreeFilterStateListener.TOPIC).refreshTree(newFilter)
        }
    }
}