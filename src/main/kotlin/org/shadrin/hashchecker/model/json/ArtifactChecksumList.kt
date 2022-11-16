package org.shadrin.hashchecker.model.json

import kotlinx.serialization.Serializable

@Serializable
data class ArtifactChecksumList(
    val result: List<ArtifactChecksum> = listOf()
)
