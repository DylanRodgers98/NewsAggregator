package com.csc306.coursework.database

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class ThrowingValueEventListener(private val func: (snapshot: DataSnapshot) -> Unit) : ValueEventListener {

    override fun onDataChange(snapshot: DataSnapshot) {
        func(snapshot)
    }

    override fun onCancelled(error: DatabaseError) {
        throw error.toException()
    }

}