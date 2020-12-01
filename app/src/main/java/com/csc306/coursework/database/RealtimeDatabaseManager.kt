package com.csc306.coursework.database

import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

private val mDatabase: FirebaseDatabase = FirebaseDatabase.getInstance()

class RealtimeDatabaseManager {

    fun setUserFollowingCategories(userUid: String, categoriesFollowing: List<String>) {
        mDatabase.getReference("users")
            .child(userUid)
            .child("following")
            .child("categories")
            .setValue(categoriesFollowing)
    }

    fun getUserFollowingCategories(userUid: String, valueEventListener: ValueEventListener) {
        mDatabase.getReference("users")
            .child(userUid)
            .child("following")
            .child("categories")
            .addListenerForSingleValueEvent(valueEventListener)
    }

}