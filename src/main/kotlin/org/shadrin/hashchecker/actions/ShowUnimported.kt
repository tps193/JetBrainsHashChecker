package org.shadrin.hashchecker.actions

import org.shadrin.hashchecker.model.VisibilityFilter

class ShowUnimported : AbstractShowAction() {

    override fun modifyFilter(filter: VisibilityFilter, state: Boolean) = filter.copy(showUnimported = state)

    override fun isSelectedImpl(filter: VisibilityFilter): Boolean = filter.showUnimported
}