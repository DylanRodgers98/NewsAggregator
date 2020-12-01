package com.csc306.coursework.database

import com.csc306.coursework.model.Category
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.ValueEventListener

class CategoriesFollowingValueEventListener(private val cb: (categoryFollowStateArray: Array<Pair<Category, Boolean>?>) -> Unit) : ValueEventListener {

    override fun onDataChange(snapshot: DataSnapshot) {
        val stringListType: GenericTypeIndicator<List<String>> = object : GenericTypeIndicator<List<String>>() {}
        val categoriesFollowing: List<String>? = snapshot.getValue(stringListType)
        val categories: Array<Category> = Category.values()
        val categoryFollowStateArray: Array<Pair<Category, Boolean>?> = arrayOfNulls(categories.size)
        for (i in categories.indices) {
            val category: Category = categories[i]
            val isFollowing: Boolean = categoriesFollowing?.contains(category.toString()) == true
            categoryFollowStateArray[i] = Pair(category, isFollowing)
        }
        cb(categoryFollowStateArray)
    }

    override fun onCancelled(error: DatabaseError) {
        throw error.toException()
    }

}