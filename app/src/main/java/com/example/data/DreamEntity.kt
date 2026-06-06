package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dreams")
data class DreamEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val originalText: String,
    val style: String,
    val mood: String,
    val storyNarrative: String,
    val scenesJson: String, // List<DreamScene> serialized to Json
    val interpretation: String,
    val timestamp: Long = System.currentTimeMillis(),
    val musicIndex: Int = 0
)
