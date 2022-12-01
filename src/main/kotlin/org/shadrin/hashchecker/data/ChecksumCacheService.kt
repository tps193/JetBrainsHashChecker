package org.shadrin.hashchecker.data

import org.shadrin.hashchecker.model.json.ArtifactChecksum

// TODO: Is it necessary to have it as a service?
/* A: no, as it is application-level service to implement application level cache it might be just a singleton
    And also btw even with service implementation I used it incorrectly:
    1. I forgot to add it to plugin.xml
    2. I invoked it using project.getService instead of application manager.
 */

object ChecksumCacheService {
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