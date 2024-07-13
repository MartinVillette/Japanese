package com.example.japanese.minnaNoNihongo

import com.example.japanese.lesson.LanguageItem

class Profile (
    var id: String = "",
    var chapter:Int=1,
    var content: ArrayList<LanguageItem> = arrayListOf(),
)