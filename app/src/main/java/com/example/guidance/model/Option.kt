package com.example.guidance.model

import kotlin.math.log2
import kotlin.math.pow

class Option(
    val question: Question,
    val optionMapping: MutableMap<Int, OptionData>,     // key: 1부터 2,3,4, ....
    val answerOptionIdx: Int,                   // 맵핑 전 정답정보
    private var selectedOptIdx: Int = 0,
    var isMultipleSelectable: Boolean = false
) {
    val answerMapping: MutableMap<Int, OptionData> = mutableMapOf()     // key: 1부터 2,3,4, ....

    init {
        val mappingArray = this.getMappingArray()

        // 정답 추출 ex) 9 => 8+1, 11 => 8+2+1
        val sortedOption = this.optionMapping.values.sortedBy { it.originNo }
        var mask = (2.0f).pow(this.optionMapping.size-1).toInt()
        for(i in (1..this.optionMapping.size)) {
            var separateAnswer = this.answerOptionIdx.and(mask)
            if (separateAnswer > 0) {
                separateAnswer = log2(separateAnswer.toFloat()).toInt()
                this.answerMapping[mappingArray[separateAnswer]] = sortedOption[separateAnswer]
            }
            mask = mask.shr(1)
        }

        this.correctUpdate()
    }

    // 옵션 선택, optNum은 1부터 2,3,4, ....
    fun select(optNum: Int): Boolean {
        val separateOpt = (2.0f).pow(optNum-1).toInt()
        if (this.selectedOptIdx.and(separateOpt) > 0) {
            return false
        }
        this.selectedOptIdx += separateOpt
        this.correctUpdate()
        return true
    }

    // 옵션 해제, optNum은 1부터 2,3,4, ....
    fun deselect(optNum: Int): Boolean {
        val separateOpt = (2.0f).pow(optNum-1).toInt()
        if (this.selectedOptIdx.and(separateOpt) > 0) {
            this.selectedOptIdx -= separateOpt
            return true
        }
        this.correctUpdate()
        return false
    }

    // 옵션 체크 여부, optNum은 1부터 2,3,4, ....
    fun isSelected(optNum: Int): Boolean {
        val separateOpt = (2.0f).pow(optNum-1).toInt()
        return this.selectedOptIdx.and(separateOpt) > 0
    }

    // course의 정답/오답 실시관 관리
    private fun correctUpdate() {
        this.question.course.correctQuestions.remove(this.question.questNo)
        this.question.course.incorrectQuestions.remove(this.question.questNo)
        if (this.isCorrect()) {
            this.question.course.correctQuestions[this.question.questNo] = this.question
        } else {
            this.question.course.incorrectQuestions[this.question.questNo] = this.question
        }
    }

    // 옵션의 정답 여부
    fun isCorrect(): Boolean {
        // 선택된 옵션 분리
        var correct = true
        var mask = (2.0f).pow(this.optionMapping.size-1).toInt()
        var countVerify = 0
        for(i in (1..this.optionMapping.size)) {
            var separateSelected = this.selectedOptIdx.and(mask)
            if (separateSelected > 0) {
                // 분리된 옵션번호에 대해 log2
                separateSelected = log2(separateSelected.toFloat()).toInt() + 1

                // 정답 확인
                correct = correct && this.answerMapping.contains(separateSelected)
                countVerify++
            }
            mask = mask.shr(1)
        }
        correct = correct && (countVerify == this.answerMapping.size)
        return correct
    }

    fun getSelected(): Int {
        return this.selectedOptIdx
    }

    // 맵핑 배열 반환 (원본 -> 변형)
    fun getMappingArray(): Array<Int> {
        val result = Array<Int>(this.optionMapping.size) { 0 }
        this.optionMapping.keys.forEach { key ->
            result[this.optionMapping[key]!!.originNo-1] = key
        }
        return result
    }
}