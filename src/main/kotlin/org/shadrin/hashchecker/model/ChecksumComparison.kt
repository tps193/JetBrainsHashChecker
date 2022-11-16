package org.shadrin.hashchecker.model

class ChecksumComparison(
    val artifactId: String,
    val localChecksum: String? = null,
    var serverChecksum: String? = null,
    var status: ChecksumComparisonStatus = ChecksumComparisonStatus.UNKNOWN
)