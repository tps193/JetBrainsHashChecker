package org.shadrin.hashchecker.model

// Q: Why is sealed class better than enum here?
/* A: statuses like OK, UNKNOWN, NOT_IMPORTED don't have any additional information, they are just constants,
    while Error and Skipped instances will be unique objects as they keep a custom message which is set outside by
    application (it might be a network error,missing jar or something else). Sealed class allows to follow this approach.
    Enum supposes that all my enum members would have the message (empty for those which don't need it but still have),
    also it will be no way to set custom error message.
 */
sealed class ChecksumComparisonStatus {
    object OK : ChecksumComparisonStatus()
    object UNKNOWN : ChecksumComparisonStatus()
    class Error(val msg: String) : ChecksumComparisonStatus()
    class Skipped(val msg: String) : ChecksumComparisonStatus()
    object NOT_IMPORTED : ChecksumComparisonStatus()
}