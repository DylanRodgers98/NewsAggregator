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
import java.util.*

class FollowCategoriesAdapter(
    private val categories: Array<Category>,
    private val context: Context
) : RecyclerView.Adapter<FollowCategoriesAdapter.ViewHolder>() {

    private val checkBoxState: Array<Pair<Category, Boolean>?> = arrayOfNulls(Category.values().size)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view: View = inflater.inflate(R.layout.category_row_checkbox, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category: Category = categories[position]
        holder.imgView.setImageResource(category.imageDrawableResource)
        holder.checkBox.text = context.getString(category.nameStringResource)
        holder.checkBox.isChecked = isFollowingCategory(category, position)
    }

    private fun isFollowingCategory(category: Category, position: Int): Boolean {
        val isFollowing = context.getSharedPreferences(CATEGORIES_FOLLOWING, Context.MODE_PRIVATE)
            .getBoolean(category.toString(), false)
        checkBoxState[position] = Pair(category, isFollowing)
        return isFollowing
    }

    override fun getItemCount(): Int = categories.size

    inner class ViewHolder(layout: View) : RecyclerView.ViewHolder(layout) {
        val imgView: ImageView = itemView.findViewById(R.id.image)
        val checkBox: CheckBox = itemView.findViewById(R.id.checkbox)

        init {
            checkBox.setOnClickListener { view ->
                val state: Pair<Category, Boolean> = checkBoxState[adapterPosition]!!
                val category = state.first
                val isFollowing = state.second

                checkBox.isChecked = !isFollowing
                checkBoxState[adapterPosition] = Pair(category, checkBox.isChecked)
                context.getSharedPreferences(CATEGORIES_FOLLOWING, Context.MODE_PRIVATE).edit()
                    .putBoolean(category.toString(), checkBox.isChecked)
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