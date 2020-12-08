package com.csc306.coursework.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.csc306.coursework.R
import com.csc306.coursework.adapter.CategorySelectionAdapter
import com.csc306.coursework.model.Category
import com.csc306.coursework.model.FollowingCategory
import com.csc306.coursework.model.ICategory

class ChooseCategoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recycler_and_toolbar)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.title = getString(R.string.categories_title)

        val recyclerView: RecyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val categories: Array<ICategory> = arrayOf(FollowingCategory.FOLLOWING, *Category.values())
        recyclerView.adapter = CategorySelectionAdapter(categories, this)
    }

}