package com.csc306.coursework.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.csc306.coursework.R
import com.csc306.coursework.model.Article
import com.google.android.material.snackbar.Snackbar
import com.squareup.picasso.Picasso
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

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
        holder.publishedTextView.text = calculateTimeSincePublished(holder, article.publishDate)
        Picasso.get().load(article.imageURL).into(holder.imageView)
        holder.titleTextView.text = article.title
        holder.descriptionTextView.text = article.description
    }

    private fun calculateTimeSincePublished(holder: ViewHolder, publishDate: LocalDateTime): String {
        val diff: Long = Duration.between(publishDate, LocalDateTime.now()).toMillis()
        val daysAgo: Long = TimeUnit.MILLISECONDS.toDays(diff)
        if (daysAgo > 0) {
            return daysAgo.toString() + getString(holder, R.string.days_ago)
        }
        val hoursAgo: Long = TimeUnit.MILLISECONDS.toHours(diff)
        if (hoursAgo > 0) {
            return hoursAgo.toString() + getString(holder, R.string.hours_ago)
        }
        val minutesAgo: Long = TimeUnit.MILLISECONDS.toMinutes(diff)
        if (minutesAgo > 0) {
            return minutesAgo.toString() + getString(holder, R.string.minutes_ago)
        }
        val secondsAgo: Long = TimeUnit.MILLISECONDS.toSeconds(diff)
        return secondsAgo.toString() + getString(holder, R.string.seconds_ago)
    }

    private fun getString(holder: ViewHolder, stringResourceId: Int): String {
        return holder.itemView.context.getString(stringResourceId);
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