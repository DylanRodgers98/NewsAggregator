package com.csc306.coursework.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.csc306.coursework.R
import com.csc306.coursework.activity.UserProfileActivity
import com.csc306.coursework.model.Article
import com.csc306.coursework.model.Source
import com.csc306.coursework.model.UserProfile
import com.squareup.picasso.Picasso
import org.apache.commons.lang3.StringUtils

class SearchResultsAdapter(
    private val users: Map<String, UserProfile>,
    private val sources: List<Source>,
    private val articles: List<Article>,
    private val context: Context
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun getItemViewType(position: Int): Int {
        val usersStart = 0
        val usersEnd: Int = getUsersCount() - 1
        if (usersEnd > -1 && position in usersStart..usersEnd) {
            return USER_VIEW_TYPE
        }
        val sourcesStart: Int = usersEnd + 1
        val sourcesEnd: Int = sourcesStart + getSourcesCount() - 1
        if (sourcesEnd > -1 && position in sourcesStart..sourcesEnd) {
            return SOURCE_VIEW_TYPE
        }
        val articlesStart: Int = sourcesEnd + 1
        val articlesEnd: Int = articlesStart + getArticlesCount() - 1
        if (articlesEnd > -1 && position in articlesStart..articlesEnd) {
            return ARTICLE_VIEW_TYPE
        }
        return super.getItemViewType(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        when (viewType) {
            USER_VIEW_TYPE -> {
                val view: View = inflater.inflate(R.layout.user_row, parent, false)
                return UserViewHolder(view)
            }
            SOURCE_VIEW_TYPE -> {
                val view: View = inflater.inflate(R.layout.source_row, parent, false)
                return SourceViewHolder(view)
            }
            ARTICLE_VIEW_TYPE -> {
                val view: View = inflater.inflate(R.layout.article_row, parent, false)
                return ArticleViewHolder(view)
            }
        }
        throw IllegalArgumentException("No valid ViewHolder found for view of type $viewType")
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            USER_VIEW_TYPE -> bindUserViewHolder(holder as UserViewHolder, position)
            SOURCE_VIEW_TYPE -> bindSourceViewHolder(holder as SourceViewHolder, position)
            ARTICLE_VIEW_TYPE -> bindArticleViewHolder(holder as ArticleViewHolder, position)
        }
    }

    private fun bindUserViewHolder(holder: UserViewHolder, position: Int) {
        val userEntry: Map.Entry<String, UserProfile> = users.entries.toTypedArray()[position]
        val userUid: String = userEntry.key
        val userProfile: UserProfile = userEntry.value
        if (userProfile.profilePicURI != null) {
            Picasso.get().load(userProfile.profilePicURI).into(holder.profilePicImageView)
        }
        holder.displayNameTextView.text = userProfile.displayName
        holder.userUidTextView.text = userUid
    }

    private fun bindSourceViewHolder(holder: SourceViewHolder, position: Int) {
        val actualPosition: Int = position - getUsersCount()
        val source: Source = sources[actualPosition]
        holder.idTextView.text = source.id
        holder.nameTextView.text = source.name
    }

    private fun bindArticleViewHolder(holder: ArticleViewHolder, position: Int) {
        val actualPosition: Int = position - getUsersCount() - getSourcesCount()
        val article: Article = articles[actualPosition]
        holder.sourceTextView.text = article.source
        holder.publishedTextView.text = article.getTimeSincePublishedString(context)
        if (StringUtils.isNotBlank(article.imageURL)) {
            Picasso.get().load(article.imageURL).into(holder.imageView)
        }
        holder.titleTextView.text = article.title
        holder.descriptionTextView.text = article.description
    }

    override fun getItemCount(): Int = getUsersCount() + getSourcesCount() + getArticlesCount()

    private fun getUsersCount(): Int = users.size

    private fun getSourcesCount(): Int = sources.size

    private fun getArticlesCount(): Int = articles.size

    inner class UserViewHolder(layout: View) : RecyclerView.ViewHolder(layout) {
        val profilePicImageView: ImageView = itemView.findViewById(R.id.profile_pic)
        val displayNameTextView: TextView = itemView.findViewById(R.id.display_name)
        val userUidTextView: TextView = itemView.findViewById(R.id.user_uid)

        init {
            itemView.setOnClickListener {
                val intent: Intent = Intent(context, UserProfileActivity::class.java)
                    .putExtra(UserProfileActivity.USER_UID, userUidTextView.text)
                context.startActivity(intent)
            }
        }
    }

    inner class SourceViewHolder(layout: View) : RecyclerView.ViewHolder(layout) {
        val idTextView: TextView = itemView.findViewById(R.id.id)
        val nameTextView: TextView = itemView.findViewById(R.id.name)
    }

    inner class ArticleViewHolder(layout: View) : RecyclerView.ViewHolder(layout) {
        val sourceTextView: TextView = itemView.findViewById(R.id.source)
        val publishedTextView: TextView = itemView.findViewById(R.id.published)
        val imageView: ImageView = itemView.findViewById(R.id.image)
        val titleTextView: TextView = itemView.findViewById(R.id.title)
        val descriptionTextView: TextView = itemView.findViewById(R.id.description)
    }

    companion object {
        private const val USER_VIEW_TYPE = 1
        private const val SOURCE_VIEW_TYPE = 2
        private const val ARTICLE_VIEW_TYPE = 3
    }

}