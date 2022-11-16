package org.shadrin.hashchecker.model.json

import kotlinx.serialization.Serializable

@Serializable
data class ArtifactIdList(
    val artifactIds: List<String>
)