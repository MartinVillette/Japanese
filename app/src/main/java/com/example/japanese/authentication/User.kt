package com.example.japanese.authentication

import com.example.japanese.lesson.userLesson.Lesson
import com.example.japanese.lesson.minnaNoNihongoLesson.Profile

class User (
    var id: String="",
    var email: String="",
    var firstName: String="",
    var lastName: String="",
    var lessons: ArrayList<Lesson> = arrayListOf(),
    var profiles: ArrayList<Profile> = arrayListOf(),
    var minnaNoNihongo: Int = 1,
)