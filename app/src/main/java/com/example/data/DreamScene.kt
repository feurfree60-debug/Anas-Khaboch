package com.example.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DreamScene(
    val sceneNumber: Int,
    val scriptText: String,
    val visualPrompt: String,
    var imageBase64: String? = null // Can be populated upon generation
)
