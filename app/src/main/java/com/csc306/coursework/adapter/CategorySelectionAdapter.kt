package com.csc306.coursework.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.csc306.coursework.R
import com.csc306.coursework.activity.MainActivity
import com.csc306.coursework.model.Category
import com.csc306.coursework.model.ICategory
import com.google.android.material.snackbar.Snackbar

class CategorySelectionAdapter(
    private val categories: Array<ICategory>,
    private val context: Context
) : RecyclerView.Adapter<CategorySelectionAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view: View = inflater.inflate(R.layout.category_row_large, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category: ICategory = categories[position]
        holder.imgView.setImageResource(category.imageDrawableResource)
        holder.textView.text = context.getString(category.nameStringResource)
    }

    override fun getItemCount(): Int = categories.size

    inner class ViewHolder(layout: View) : RecyclerView.ViewHolder(layout) {
        val imgView: ImageView = itemView.findViewById(R.id.image)
        val textView: TextView = itemView.findViewById(R.id.name)

        init {
            itemView.setOnClickListener {
                val intent: Intent = Intent(context, MainActivity::class.java)
                    .putExtra(CATEGORY, textView.text)
                context.startActivity(intent)
            }
        }
    }

    companion object {
        const val CATEGORY = "CATEGORY"
    }

}