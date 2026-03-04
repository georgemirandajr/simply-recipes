package com.recipebookmarks.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromIngredientList(value: List<Ingredient>?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toIngredientList(value: String?): List<Ingredient>? {
        return value?.let {
            val type = object : TypeToken<List<Ingredient>>() {}.type
            gson.fromJson(it, type)
        }
    }

    @TypeConverter
    fun fromInstructionList(value: List<Instruction>?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toInstructionList(value: String?): List<Instruction>? {
        return value?.let {
            val type = object : TypeToken<List<Instruction>>() {}.type
            gson.fromJson(it, type)
        }
    }

    @TypeConverter
    fun fromNutritionInfo(value: NutritionInfo?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toNutritionInfo(value: String?): NutritionInfo? {
        return value?.let { gson.fromJson(it, NutritionInfo::class.java) }
    }

    @TypeConverter
    fun fromCategory(value: Category?): String? {
        return value?.name
    }

    @TypeConverter
    fun toCategory(value: String?): Category? {
        return value?.let { Category.valueOf(it) }
    }
}
