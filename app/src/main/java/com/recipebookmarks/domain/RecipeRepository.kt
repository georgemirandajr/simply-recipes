package com.recipebookmarks.domain

import com.recipebookmarks.data.Category
import com.recipebookmarks.data.Recipe
import kotlinx.coroutines.flow.Flow

interface RecipeRepository {
    fun getAllRecipes(): Flow<List<Recipe>>
    fun getRecipeById(id: Long): Flow<Recipe?>
    fun searchRecipes(query: String): Flow<List<Recipe>>
    fun getRecipesByCategory(category: Category): Flow<List<Recipe>>
    suspend fun insertRecipe(recipe: Recipe): Long
    suspend fun updateRecipe(recipe: Recipe)
    suspend fun deleteRecipe(id: Long)
    suspend fun importFromUrl(url: String): ImportResult
}

sealed class ImportResult {
    data class Success(val recipeId: Long) : ImportResult()
    data class Failure(val error: ImportError) : ImportResult()
}

enum class ImportError {
    URL_INACCESSIBLE,
    PARSE_FAILED,
    NETWORK_ERROR
}
