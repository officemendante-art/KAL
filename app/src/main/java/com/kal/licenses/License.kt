package com.kal.licenses

data class License(
    val id: Long,
    val name: String,
    val author: String,
    val description: String,
    val license: String,
    val url: String
)

