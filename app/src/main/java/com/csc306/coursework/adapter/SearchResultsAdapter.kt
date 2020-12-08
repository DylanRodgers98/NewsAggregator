package com.csc306.coursework.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.csc306.coursework.model.Article
import com.csc306.coursework.model.Source
import com.csc306.coursework.model.UserProfile

class SearchResultsAdapter(
    private val users: Map<String, UserProfile>,
    private val sources: List<Source>,
    private val articles: List<Article>
) : RecyclerView.Adapter<FollowCategoriesAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FollowCategoriesAdapter.ViewHolder {
        TODO("Not yet implemented")
    }

    override fun onBindViewHolder(holder: FollowCategoriesAdapter.ViewHolder, position: Int) {
        TODO("Not yet implemented")
    }

    override fun getItemCount(): Int {
        TODO("Not yet implemented")
    }

}