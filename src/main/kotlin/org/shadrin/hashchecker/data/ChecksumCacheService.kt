package org.shadrin.hashchecker.data

import com.intellij.openapi.components.Service
import org.shadrin.hashchecker.model.json.ArtifactChecksum

@Service
class ChecksumCacheService {
    /*
    TODO: make a memory efficient cache
     */
    private val checksums = mutableMapOf<String, ArtifactChecksum>()

    fun get(artifactId: String): ArtifactChecksum? = checksums[artifactId]

    fun get(artifactIds: Collection<String>): List<ArtifactChecksum> {
        val cachedChecksums = mutableListOf<ArtifactChecksum>()
        artifactIds.forEach { id ->
            checksums[id]?.let {
                cachedChecksums.add(it)
            }
        }
        return cachedChecksums
    }

    fun put(artifactChecksums: Collection<ArtifactChecksum>) {
        checksums.putAll(
            artifactChecksums.map {
                it.identifier to it
            }
        )
    }
}