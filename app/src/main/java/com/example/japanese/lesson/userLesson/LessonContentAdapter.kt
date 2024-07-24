package com.example.japanese.lesson.userLesson

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.japanese.R

class LessonContentAdapter (private var lessonContent: List<LanguageItem>) :
    RecyclerView.Adapter<LessonContentAdapter.LessonContentViewHolder>() {

    class LessonContentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val wordTextView: TextView = itemView.findViewById(R.id.wordTextView)
        val expressionTextView: TextView = itemView.findViewById(R.id.expressionTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LessonContentViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_language_item, parent, false) // Use your item layout
        return LessonContentViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return lessonContent.size
    }

    override fun onBindViewHolder(holder: LessonContentViewHolder, position: Int) {
        val languageItem = lessonContent[position]
        holder.wordTextView.text = languageItem.meaning
        holder.expressionTextView.text = languageItem.expression
    }
}