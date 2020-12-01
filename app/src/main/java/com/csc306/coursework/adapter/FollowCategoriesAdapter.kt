package com.csc306.coursework.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.csc306.coursework.R
import com.csc306.coursework.model.Category

class FollowCategoriesAdapter(
    val categoryState: Array<Pair<Category, Boolean>?>,
    private val context: Context
) : RecyclerView.Adapter<FollowCategoriesAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view: View = inflater.inflate(R.layout.category_row_checkbox, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category: Pair<Category, Boolean> = categoryState[position]!!
        holder.imgView.setImageResource(category.first.imageDrawableResource)
        holder.checkBox.text = context.getString(category.first.nameStringResource)
        holder.checkBox.isChecked = category.second
    }

    override fun getItemCount(): Int = categoryState.size

    inner class ViewHolder(layout: View) : RecyclerView.ViewHolder(layout) {
        val imgView: ImageView = itemView.findViewById(R.id.image)
        val checkBox: CheckBox = itemView.findViewById(R.id.checkbox)

        init {
            checkBox.setOnClickListener {
                val state: Pair<Category, Boolean> = categoryState[adapterPosition]!!
                val category = state.first
                val isFollowing = state.second

                checkBox.isChecked = !isFollowing
                categoryState[adapterPosition] = Pair(category, checkBox.isChecked)
            }
        }
    }

}