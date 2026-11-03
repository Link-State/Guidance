package com.example.guidance.model

import java.util.Calendar

data class History(
    val fileName: String,
    var recentWrong: Calendar,
    var accWrongCount: Int,
    var comboWrongCount: Int
)