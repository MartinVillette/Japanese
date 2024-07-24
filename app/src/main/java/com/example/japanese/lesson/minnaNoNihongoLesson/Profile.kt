package com.example.japanese.lesson.minnaNoNihongoLesson

import com.example.japanese.lesson.userLesson.LanguageItem

class Profile (
    var id: String = "",
    var chapter:String="1",
    var content: ArrayList<LanguageItem> = arrayListOf(),
)