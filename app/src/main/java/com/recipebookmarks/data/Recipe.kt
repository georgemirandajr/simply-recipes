package com.recipebookmarks.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "recipes")
@TypeConverters(Converters::class)
data class Recipe(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val ingredients: List<Ingredient> = emptyList(),
    val instructions: List<Instruction> = emptyList(),
    val yield: String? = null,
    val servingSize: String? = null,
    val nutritionInfo: NutritionInfo? = null,
    val originalUrl: String? = null,
    val category: Category? = null,
    @ColumnInfo(name = "is_fallback")
    val isFallback: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
