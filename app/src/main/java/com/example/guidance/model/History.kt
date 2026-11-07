package com.example.guidance.model

import java.time.LocalDateTime

data class History(
    val fileName: String,
    var recentWrong: LocalDateTime,     // 밀리초 형식으로 저장
    var accWrongCount: Int,
    var comboWrongCount: Int
)