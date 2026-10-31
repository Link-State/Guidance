package com.example.guidence.model

import java.util.Calendar
import java.util.Date

data class History(
    val fileName: String,
    var recentWrong: Calendar,
    var accWrongCount: Int,
    var comboWrongCount: Int
)