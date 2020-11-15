package com.csc306.coursework.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.csc306.coursework.R
import com.csc306.coursework.database.DatabaseManager
import com.csc306.coursework.model.Category
import com.google.android.material.snackbar.Snackbar

class CategorySelectionAdapter(
    private val categories: MutableList<Category>,
    private val database: DatabaseManager
) : RecyclerView.Adapter<CategorySelectionAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view: View = inflater.inflate(R.layout.category_row_large, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category: Category = categories[position]
        holder.imgView.setImageResource(category.imageDrawable)
        holder.textView.text = category.name
    }

    override fun getItemCount(): Int = categories.size

    inner class ViewHolder(layout: View) : RecyclerView.ViewHolder(layout) {
        val imgView: ImageView = itemView.findViewById(R.id.image)
        val textView: TextView = itemView.findViewById(R.id.name)

        init {
            itemView.setOnClickListener { view ->
                val msg = textView.text
                val snackbar = Snackbar.make(view, "You clicked $msg", Snackbar.LENGTH_LONG)
                snackbar.show()
            }
        }
    }

}