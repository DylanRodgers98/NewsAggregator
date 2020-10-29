package com.csc306.coursework.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.csc306.coursework.R
import com.csc306.coursework.model.Article
import com.csc306.coursework.model.Category
import com.google.android.material.snackbar.Snackbar
import com.squareup.picasso.Picasso

class ArticleListAdapter(private val articles: MutableList<Article>) :
    RecyclerView.Adapter<ArticleListAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view: View = inflater.inflate(R.layout.article_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val article: Article = articles[position]
        holder.sourceTextView.text = article.source
//        holder.publishedTextView.text = article.source
        Picasso.get().load(article.imageURL).into(holder.imageView);
        holder.titleTextView.text = article.title
        holder.descriptionTextView.text = article.description
    }

    override fun getItemCount(): Int = articles.size

    inner class ViewHolder(layout: View) : RecyclerView.ViewHolder(layout) {
        val sourceTextView: TextView = itemView.findViewById(R.id.source)
        val publishedTextView: TextView = itemView.findViewById(R.id.published)
        val imageView: ImageView = itemView.findViewById(R.id.image)
        val titleTextView: TextView = itemView.findViewById(R.id.title)
        val descriptionTextView: TextView = itemView.findViewById(R.id.description)

        init {
            itemView.setOnClickListener { view ->
                val msg = titleTextView.text
                val snackbar = Snackbar.make(view, "You clicked $msg", Snackbar.LENGTH_LONG)
                snackbar.show()
            }
        }
    }

}