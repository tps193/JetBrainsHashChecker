package org.shadrin.hashchecker.actions

import org.shadrin.hashchecker.model.VisibilityFilter

class ShowVerified : AbstractShowAction() {

    override fun modifyFilter(filter: VisibilityFilter, state: Boolean) = filter.copy(showVerified = state)

    override fun isSelectedImpl(filter: VisibilityFilter): Boolean = filter.showVerified
}