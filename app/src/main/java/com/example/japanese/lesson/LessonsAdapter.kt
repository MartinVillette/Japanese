package com.example.japanese.lesson

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.japanese.R

class LessonsAdapter(private var lessons: List<Lesson>) :
    RecyclerView.Adapter<LessonsAdapter.LessonViewHolder>() {

    class LessonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val parentContext: Context = itemView.context
        val lessonLayout: LinearLayout = itemView.findViewById(R.id.lessonItemLayout)
        val lessonNameTextView: TextView = itemView.findViewById(R.id.lessonNameTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LessonViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lesson, parent, false) // Use your item layout
        return LessonViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return lessons.size
    }

    override fun onBindViewHolder(holder: LessonViewHolder, position: Int) {
        val lesson = lessons[position]
        holder.lessonNameTextView.text = lesson.name
        holder.lessonLayout.setOnClickListener {
            // Handle lesson click event
            val intent = Intent(holder.parentContext, LessonActivity::class.java)
            intent.putExtra("lessonId", lesson.id)
            holder.parentContext.startActivity(intent)
        }
        // ... bind other fields (e.g., lessonDescription, image using Glide)
    }
}