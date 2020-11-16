package com.csc306.coursework.adapter

import android.content.Context
import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.csc306.coursework.R
import com.csc306.coursework.database.DatabaseManager
import com.csc306.coursework.model.Category
import com.google.android.material.snackbar.Snackbar

class FollowCategoriesAdapter(
    private val categories: MutableList<Category>,
    private val context: Context
) : RecyclerView.Adapter<FollowCategoriesAdapter.ViewHolder>() {

    private val checkBoxStateArray = SparseBooleanArray()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view: View = inflater.inflate(R.layout.category_row_checkbox, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category: Category = categories[position]
        holder.imgView.setImageResource(category.imageDrawable)
        holder.checkBox.text = category.name
        holder.checkBox.isChecked = isFollowingCategory(category.name, position)
    }

    private fun isFollowingCategory(categoryName: String, position: Int): Boolean {
        val isFollowing = context.getSharedPreferences(CATEGORIES_FOLLOWING, Context.MODE_PRIVATE)
            .getBoolean(categoryName, false)
        checkBoxStateArray.put(position, isFollowing)
        return isFollowing
    }

    override fun getItemCount(): Int = categories.size

    inner class ViewHolder(layout: View) : RecyclerView.ViewHolder(layout) {
        val imgView: ImageView = itemView.findViewById(R.id.image)
        val checkBox: CheckBox = itemView.findViewById(R.id.checkbox)

        init {
            checkBox.setOnClickListener { view ->
                val isAlreadyChecked = checkBoxStateArray.get(adapterPosition, false)
                checkBox.isChecked = !isAlreadyChecked
                checkBoxStateArray.put(adapterPosition, !isAlreadyChecked)

                context.getSharedPreferences(CATEGORIES_FOLLOWING, Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean(checkBox.text.toString(), checkBox.isChecked)
                    .apply()

                val nowOrNoLonger: String = if (checkBox.isChecked) "now" else "no longer"
                val text = "You are $nowOrNoLonger following ${checkBox.text}"
                Snackbar.make(view, text, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        const val CATEGORIES_FOLLOWING = "CATEGORIES_FOLLOWING"
    }

}