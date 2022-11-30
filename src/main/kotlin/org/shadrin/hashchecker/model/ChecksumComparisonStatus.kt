package org.shadrin.hashchecker.model

// TODO: Why is sealed class better than enum here?
sealed class ChecksumComparisonStatus {
    object OK : ChecksumComparisonStatus()
    object UNKNOWN : ChecksumComparisonStatus()
    class Error(val msg: String) : ChecksumComparisonStatus()
    class Skipped(val msg: String) : ChecksumComparisonStatus()
    object NOT_IMPORTED : ChecksumComparisonStatus()
}