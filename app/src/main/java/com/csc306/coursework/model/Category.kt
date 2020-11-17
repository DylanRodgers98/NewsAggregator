package com.csc306.coursework.model

import com.csc306.coursework.R
import java.util.Locale

enum class Category(val nameStringResource: Int, val imageDrawableResource: Int) {
    GENERAL(R.string.general, R.drawable.general),
    BUSINESS(R.string.business, R.drawable.business),
    HEALTH(R.string.health, R.drawable.health),
    SCIENCE(R.string.science, R.drawable.science),
    TECHNOLOGY(R.string.technology, R.drawable.technology),
    SPORTS(R.string.sports, R.drawable.sports),
    ENTERTAINMENT(R.string.entertainment, R.drawable.entertainment);

    override fun toString(): String {
        return name.toLowerCase(Locale.getDefault())
    }
}