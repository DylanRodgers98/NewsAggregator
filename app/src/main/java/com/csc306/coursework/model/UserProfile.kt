package com.csc306.coursework.model

import org.apache.commons.lang3.StringUtils

class UserProfile(
    val displayName: String,
    val location: String?,
    val profilePicURI: String?
) {
    // no-arg constructor for bean mapping
    constructor() : this(StringUtils.EMPTY, null, null)
}