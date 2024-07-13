package com.example.japanese.authentication

import com.example.japanese.lesson.Lesson

class User (
    var id: String="",
    var email: String="",
    var firstName: String="",
    var lastName: String="",
    var lessons: ArrayList<Lesson> = arrayListOf(),
    var minnaNoNihongo: Int = 1,
)