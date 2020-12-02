package com.csc306.coursework.adapter

import android.content.Context
import android.os.AsyncTask
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.view.menu.MenuView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.csc306.coursework.R
import com.csc306.coursework.database.RealtimeDatabaseManager
import com.csc306.coursework.model.Article
import com.csc306.coursework.model.ArticleTitleAnalyser
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

class ArticleListAdapter(private val articles: MutableList<Article>, private val mAuth: FirebaseAuth, private val mDatabase: RealtimeDatabaseManager, private val context: Context) :
    RecyclerView.Adapter<ArticleListAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view: View = inflater.inflate(R.layout.article_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val article: Article = articles[position]
        holder.sourceTextView.text = article.source
        holder.publishedTextView.text = calculateTimeSincePublished(article.publishDate)
        Picasso.get().load(article.imageURL).into(holder.imageView)
        holder.titleTextView.text = article.title
        holder.descriptionTextView.text = article.description
    }

    private fun calculateTimeSincePublished(publishDate: LocalDateTime): String {
        val diff: Long = Duration.between(publishDate, LocalDateTime.now()).toMillis()
        val daysAgo: Long = TimeUnit.MILLISECONDS.toDays(diff)
        if (daysAgo > 0) {
            return daysAgo.toString() + context.getString(R.string.days_ago)
        }
        val hoursAgo: Long = TimeUnit.MILLISECONDS.toHours(diff)
        if (hoursAgo > 0) {
            return hoursAgo.toString() + context.getString(R.string.hours_ago)
        }
        val minutesAgo: Long = TimeUnit.MILLISECONDS.toMinutes(diff)
        if (minutesAgo > 0) {
            return minutesAgo.toString() + context.getString(R.string.minutes_ago)
        }
        val secondsAgo: Long = TimeUnit.MILLISECONDS.toSeconds(diff)
        if (secondsAgo > 0) {
            return secondsAgo.toString() + context.getString(R.string.seconds_ago)
        }
        return context.getString(R.string.now)
    }

    private fun likeArticle(position: Int, view: View) {
        if (position < articles.size) {
            val article: Article = articles[position]
            if (article.titleKeywords == null) {
                ArticleTitleAnalyser(context).execute(article).get()
            }
            val userUid: String = mAuth.currentUser!!.uid
            mDatabase.likeArticle(userUid, article)
            Snackbar.make(view, "You liked ${article.title}. We will try to show you more like it!", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun dislikeArticle(position: Int, view: View) {
        if (position < articles.size) {
            val article: Article = articles[position]
            if (article.titleKeywords == null) {
                ArticleTitleAnalyser(context).execute(article).get()
            }
            val userUid: String = mAuth.currentUser!!.uid
            mDatabase.dislikeArticle(userUid, article)
            articles.removeAt(position)
            Snackbar.make(view, "You disliked ${article.title}. We will try to show you less like it.", Snackbar.LENGTH_LONG).show()
        }
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

    class SwipeCallback(private val adapter: ArticleListAdapter) :
        ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean = false

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position: Int = viewHolder.adapterPosition
            val view: View = viewHolder.itemView
            when (direction) {
                ItemTouchHelper.LEFT -> adapter.dislikeArticle(position, view)
                ItemTouchHelper.RIGHT -> adapter.likeArticle(position, view)
            }
        }

    }

}