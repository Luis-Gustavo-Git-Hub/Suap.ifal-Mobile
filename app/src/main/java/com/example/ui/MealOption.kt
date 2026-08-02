package com.example.ui

data class MealOption(
    val id: String,
    val name: String,
    val date: String,
    var checked: Boolean,
    val disabled: Boolean
)
