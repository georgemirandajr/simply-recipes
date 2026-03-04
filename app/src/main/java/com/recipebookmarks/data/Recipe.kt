package com.recipebookmarks.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "recipes")
@TypeConverters(Converters::class)
data class Recipe(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val ingredients: List<Ingredient>,
    val instructions: List<Instruction>,
    val yield: String?,
    val servingSize: String?,
    val nutritionInfo: NutritionInfo?,
    val originalUrl: String?,
    val category: Category?,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
