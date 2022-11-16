package org.shadrin.hashchecker.listener

import com.intellij.util.messages.Topic
import org.shadrin.hashchecker.model.ChecksumComparison

interface ChecksumUpdateListener {
    companion object {
        val CHECKSUM_UPDATE_TOPIC = Topic.create("Checksums downloaded", ChecksumUpdateListener::class.java)
    }

    fun notify(checksumComparisonResult: List<ChecksumComparison>)
}