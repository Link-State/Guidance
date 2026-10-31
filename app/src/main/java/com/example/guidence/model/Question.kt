package com.example.guidence.model

import androidx.compose.ui.graphics.ImageBitmap

class Question(
    val fileName: String,
    val examName: String,
    val examDate: String,
    val examNum: String,
    val statement: String,
    val pictures: String,
    val bitmapPicture: ImageBitmap?,
    val altPicture: String = "",
    val options: Array<Option?>,
    val answer: Int = 0,
    val comment: String = "",
    val partNo: Int = 0,
    var marking: Int = 0
) {
}