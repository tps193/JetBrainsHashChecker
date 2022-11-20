package org.shadrin.hashchecker.model

data class VisibilityFilter(
    val showVerified: Boolean,
    val showSkipped: Boolean,
    val showUnimported: Boolean
) {
    companion object {
        val default = VisibilityFilter(true, true, true)
    }
}
