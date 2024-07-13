package com.example.japanese.lesson.userLesson

class Lesson (
    var id: String = "",
    var userId: String="",
    var name: String="",
    var content: ArrayList<LanguageItem> = arrayListOf(),
)