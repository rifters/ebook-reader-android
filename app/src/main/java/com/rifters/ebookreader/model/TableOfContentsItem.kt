package com.rifters.ebookreader.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class TableOfContentsItem(
    val title: String,
    val href: String,
    val page: Int = 0,
    val level: Int = 0
) : Parcelable
