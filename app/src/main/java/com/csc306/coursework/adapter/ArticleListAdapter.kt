package com.csc306.coursework.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.csc306.coursework.R
import com.csc306.coursework.database.RealtimeDatabaseManager
import com.csc306.coursework.model.Article
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso
import org.apache.commons.lang3.StringUtils
import java.lang.StringBuilder
import java.util.concurrent.TimeUnit

class ArticleListAdapter(
    private val articles: MutableList<Article>,
    private val auth: FirebaseAuth,
    private val context: Context
) : RecyclerView.Adapter<ArticleListAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view: View = inflater.inflate(R.layout.article_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val article: Article = articles[position]
        holder.sourceTextView.text = article.source
        holder.publishedTextView.text = article.getTimeSincePublishedString(context)
        if (StringUtils.isNotBlank(article.imageURL)) {
            Picasso.get().load(article.imageURL).into(holder.imageView)
        }
        holder.titleTextView.text = article.title
        holder.descriptionTextView.text = article.description
    }

    private fun likeArticle(position: Int, view: View) {
        if (position < articles.size) {
            val userUid: String = auth.currentUser!!.uid
            val article: Article = articles[position]
            notifyItemChanged(position)
            RealtimeDatabaseManager.likeArticle(userUid, article, context)
            showSnackbar(view, context.getString(R.string.you_liked), article.title)
        }
    }

    private fun dislikeArticle(position: Int, view: View) {
        if (position < articles.size) {
            val userUid: String = auth.currentUser!!.uid
            val article: Article = articles[position]
            removeArticleAt(position)
            RealtimeDatabaseManager.dislikeArticle(userUid, article, context)
            showSnackbar(view, context.getString(R.string.you_disliked), article.title)
        }
    }

    private fun showSnackbar(view: View, messagePrefix: String, articleTitle: String) {
        val msg: String = StringBuilder(messagePrefix)
            .append(StringUtils.SPACE)
            .append(articleTitle)
            .toString()
        Snackbar.make(view, msg, Snackbar.LENGTH_LONG).show()
    }

    private fun removeArticleAt(position: Int) {
        articles.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, articles.size)
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