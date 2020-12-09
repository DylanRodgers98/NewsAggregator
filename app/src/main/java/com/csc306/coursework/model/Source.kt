package com.csc306.coursework.model

import org.apache.commons.lang3.StringUtils

class Source(
    val id: String,
    val name: String
) {
    // no-arg constructor for bean mapping
    constructor() : this(StringUtils.EMPTY, StringUtils.EMPTY)
}