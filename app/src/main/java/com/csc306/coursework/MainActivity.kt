package com.csc306.coursework

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.csc306.coursework.adapter.CategorySelectionAdapter
import com.csc306.coursework.adapter.FollowCategoriesAdapter
import com.csc306.coursework.model.Category

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.title = getString(R.string.follow_categories)
        setSupportActionBar(toolbar)

        val recyclerView: RecyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = FollowCategoriesAdapter(getCategories())
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar, menu)
        return super.onCreateOptionsMenu(menu)
    }

//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        val toolbarView: View = findViewById(R.id.toolbar)
//        when (item.itemId) {
//            R.id.back -> {
//                val snackbar = Snackbar.make(toolbarView, "You clicked back", Snackbar.LENGTH_LONG)
//                snackbar.show()
//                return true
//            }
//        }
//        return super.onOptionsItemSelected(item)
//    }

    private fun getCategories(): MutableList<Category> {
        return arrayListOf(
            Category(getString(R.string.business), R.drawable.business),
            Category(getString(R.string.entertainment), R.drawable.entertainment),
            Category(getString(R.string.general), R.drawable.general),
            Category(getString(R.string.health), R.drawable.health),
            Category(getString(R.string.science), R.drawable.science),
            Category(getString(R.string.sports), R.drawable.sports),
            Category(getString(R.string.technology), R.drawable.technology))
    }

}