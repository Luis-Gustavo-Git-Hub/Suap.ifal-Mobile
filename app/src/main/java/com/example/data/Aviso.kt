package com.example.data

data class Aviso(
    val sender: String,
    val subject: String,
    val date: String,
    val isRead: Boolean,
    val detailUrl: String = ""
)

data class Attachment(
    val name: String,
    val url: String
)

data class MessageDetail(
    val content: String,
    val attachments: List<Attachment> = emptyList()
)
