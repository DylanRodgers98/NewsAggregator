package com.csc306.coursework.model

import com.csc306.coursework.R

enum class FollowingCategory(
    override val nameStringResource: Int,
    override val imageDrawableResource: Int
) : ICategory {

    FOLLOWING(R.string.following, 0);

}