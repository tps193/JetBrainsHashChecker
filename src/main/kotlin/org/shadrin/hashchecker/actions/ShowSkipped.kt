package org.shadrin.hashchecker.actions

import org.shadrin.hashchecker.model.VisibilityFilter

class ShowSkipped : AbstractShowAction() {
    override fun modifyFilter(filter: VisibilityFilter, state: Boolean) = filter.copy(showSkipped = state)

    override fun isSelectedImpl(filter: VisibilityFilter): Boolean = filter.showSkipped
}