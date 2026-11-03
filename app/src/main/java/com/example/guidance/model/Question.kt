package com.example.guidance.model

import androidx.compose.ui.graphics.ImageBitmap

class Question(
    val fileName: String,
    val examName: String,
    val examDate: String,
    val examNum: String,
    val courseNum: Int,
    val courseName: String,
    var statement: String,
    val pictures: String,
    val bitmapPicture: ImageBitmap?,
    var altPicture: String = "",
    val options: Array<Option?>,
    val answer: Int = 0,
    var comment: String = "",
    val partNo: Int = 0,
    var marking: Int = 0,
    val changeState: MutableMap<String, Boolean> = mutableMapOf()
) {
}