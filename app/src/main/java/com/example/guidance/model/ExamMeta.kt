package com.example.guidance.model

import java.io.File

data class ExamMeta(
    val examInstance: ExamInstance,
    val examName: String
) {
    public val questAmount: Int
    public val courseNames: List<String>
    public val questAmountPerCourse: List<Int>
    public val optionAmount: Int

    init {
        // 시험 메타정보 불러오기
        val metaFile = File(this.examInstance.filesDir, "assets/meta/${this.examName}.txt")
        if (!metaFile.exists()) {
            // 종료
        }

        val metaContent = metaFile.readText().split("\n")
        this.questAmount = metaContent[0].replace("문제 수 : ", "").trim().toInt()
        this.courseNames = metaContent[1].replace("과목 : ", "").trim().split(";").map { cor ->
            cor.trim()
        }
        this.questAmountPerCourse = metaContent[2].replace("과목 당 문제 수 : ", "").trim().split(";").map { per ->
            per.trim().toInt()
        }
        this.optionAmount = metaContent[3].replace("선지 수 : ", "").trim().toInt()

        if (questAmountPerCourse.sum() != questAmount || courseNames.size != questAmountPerCourse.size) {
            // 종료
        }
    }
}