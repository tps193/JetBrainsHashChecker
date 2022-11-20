package org.shadrin.hashchecker.listener

import com.intellij.util.messages.Topic
import org.shadrin.hashchecker.model.VisibilityFilter

interface TreeFilterStateListener {
    companion object {
        val TOPIC = Topic.create("Checksums Tree Filters", TreeFilterStateListener::class.java)
    }

    fun refreshTree(filter: VisibilityFilter)
}