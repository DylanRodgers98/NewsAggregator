package com.csc306.coursework.adapter

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import com.google.android.gms.common.util.CollectionUtils
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso
import org.apache.commons.lang3.StringUtils
import kotlin.text.StringBuilder

class ArticleListAdapter(
    private var articles: MutableList<Article>,
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
        val likedByString: String? = listLikedBy(article)
        if (likedByString != null) {
            holder.likedByTextView.text = likedByString
            holder.likedByTextView.visibility = View.VISIBLE
        }
        holder.sourceTextView.text = article.source
        holder.publishedTextView.text = article.getTimeSincePublishedString(context)
        if (StringUtils.isNotBlank(article.imageURL)) {
            Picasso.get().load(article.imageURL).into(holder.imageView)
        }
        holder.titleTextView.text = article.title
        holder.descriptionTextView.text = article.description
        holder.articleUrlTextView.text = article.articleURL
    }

    private fun listLikedBy(article: Article): String? {
        if (!CollectionUtils.isEmpty(article.likedBy)) {
            // Liked by
            val sb = StringBuilder(context.getString(R.string.liked_by))
                .append(StringUtils.SPACE)

            if (article.isLiked) {
                // you
                sb.append(context.getString(R.string.you))

                if (article.likedBy!!.size == 1) {
                    // and {display name 0}
                    sb.append(StringUtils.SPACE)
                        .append(context.getString(R.string.and))
                        .append(article.likedBy!![0])
                } else if (article.likedBy!!.size > 1) {
                    // , {display name 0} and
                    sb.append(COMMA_SPACE)
                        .append(article.likedBy!![0])
                        .append(context.getString(R.string.and))
                        .append(StringUtils.SPACE)

                    if (article.likedBy!!.size == 2) {
                        // {display name 1}
                        sb.append(article.likedBy!![1])
                    } else {
                        // {size - 1} others
                        sb.append(article.likedBy!!.size - 1)
                            .append(StringUtils.SPACE)
                            .append(context.getString(R.string.others))
                    }
                }
            } else {
                // {display name 0}
                sb.append(article.likedBy!![0])

                if (article.likedBy!!.size == 2) {
                    // and {display name 1}
                    sb.append(StringUtils.SPACE)
                        .append(context.getString(R.string.and))
                        .append(StringUtils.SPACE)
                        .append(article.likedBy!![1])
                } else if (article.likedBy!!.size > 2) {
                    // , {display name 1}, and {size - 2} others
                    sb.append(COMMA_SPACE)
                        .append(article.likedBy!![1])
                        .append(COMMA_SPACE)
                        .append(context.getString(R.string.and))
                        .append(article.likedBy!!.size - 2)
                        .append(StringUtils.SPACE)
                        .append(context.getString(R.string.others))
                }
            }
            return sb.toString()
        } else if (article.isLiked) {
            // Liked by you
            return StringBuilder(context.getString(R.string.liked_by))
                .append(StringUtils.SPACE)
                .append(context.getString(R.string.you))
                .toString()
        }
        return null
    }

    private fun likeArticle(position: Int, view: View) {
        if (position < articles.size) {
            val userUid: String = auth.currentUser!!.uid
            val article: Article = articles[position]
            article.isLiked = true
            notifyItemChanged(position)
            RealtimeDatabaseManager.likeArticle(userUid, article, context)
            val msg: String = StringBuilder(context.getString(R.string.you_liked))
                .append(StringUtils.SPACE)
                .append(article.title)
                .toString()
            val snackbar = Snackbar.make(view, msg, Snackbar.LENGTH_LONG)
            snackbar.setAction(context.getString(R.string.undo)) {
                article.isLiked = false
                notifyItemChanged(position)
                RealtimeDatabaseManager.undoLike(userUid, article, context)
            }
            snackbar.show()
        }
    }

    private fun dislikeArticle(position: Int, view: View) {
        if (position < articles.size) {
            val userUid: String = auth.currentUser!!.uid
            val article: Article = articles[position]
            article.isLiked = false
            removeArticleAt(position)
            RealtimeDatabaseManager.dislikeArticle(userUid, article, context)
            val msg: String = StringBuilder(context.getString(R.string.you_disliked))
                .append(StringUtils.SPACE)
                .append(article.title)
                .toString()
            val snackbar = Snackbar.make(view, msg, Snackbar.LENGTH_LONG)
            snackbar.setAction(context.getString(R.string.undo)) {
                putArticleAt(position, article)
                RealtimeDatabaseManager.undoDislike(userUid, article, context)
            }
            snackbar.show()
        }
    }

    private fun removeArticleAt(position: Int) {
        articles.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, articles.size)
    }

    private fun putArticleAt(position: Int, article: Article) {
        val newArticles: MutableList<Article> = mutableListOf()
        newArticles.addAll(articles.subList(0, position))
        newArticles.add(article)
        newArticles.addAll(articles.subList(position, articles.size))
        articles.clear()
        articles.addAll(newArticles)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = articles.size

    inner class ViewHolder(layout: View) : RecyclerView.ViewHolder(layout) {
        val likedByTextView: TextView = itemView.findViewById(R.id.liked_by)
        val sourceTextView: TextView = itemView.findViewById(R.id.source)
        val publishedTextView: TextView = itemView.findViewById(R.id.published)
        val imageView: ImageView = itemView.findViewById(R.id.image)
        val titleTextView: TextView = itemView.findViewById(R.id.title)
        val descriptionTextView: TextView = itemView.findViewById(R.id.description)
        val articleUrlTextView: TextView = itemView.findViewById(R.id.article_url)

        init {
            itemView.setOnClickListener {
                val articleUrl: String = articleUrlTextView.text.toString()
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(articleUrl)))
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

    companion object {
        private const val COMMA_SPACE = ", "
    }

}