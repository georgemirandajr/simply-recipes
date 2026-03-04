package com.recipebookmarks.data

data class ScaledIngredient(
    val name: String,
    val originalQuantity: Double,
    val scaledQuantity: Double,
    val unit: String,
    val order: Int
)
