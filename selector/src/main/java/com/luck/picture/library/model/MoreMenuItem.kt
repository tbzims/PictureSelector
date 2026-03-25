package com.luck.picture.library.model

data class MoreMenuItem(
    val id: Int,
    val iconRes: Int,
    val title: String,
    var isSelected: Boolean = false
)