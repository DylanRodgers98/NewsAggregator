package com.csc306.coursework.model

class AnalyzeEntitiesResponse(
    val entities: List<Entity>
)

class Entity(
    val name: String,
    val salience: Double
)