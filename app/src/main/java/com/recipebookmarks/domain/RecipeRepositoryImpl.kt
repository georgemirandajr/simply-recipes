package com.recipebookmarks.domain

import com.recipebookmarks.data.Category
import com.recipebookmarks.data.Recipe
import com.recipebookmarks.data.RecipeDao
import kotlinx.coroutines.flow.Flow

class RecipeRepositoryImpl(
    private val recipeDao: RecipeDao
) : RecipeRepository {

    override fun getAllRecipes(): Flow<List<Recipe>> {
        return recipeDao.getAll()
    }

    override fun getRecipeById(id: Long): Flow<Recipe?> {
        return recipeDao.getById(id)
    }

    override fun searchRecipes(query: String): Flow<List<Recipe>> {
        return recipeDao.searchByName(query)
    }

    override fun getRecipesByCategory(category: Category): Flow<List<Recipe>> {
        return recipeDao.getByCategory(category)
    }

    override suspend fun insertRecipe(recipe: Recipe): Long {
        return recipeDao.insert(recipe)
    }

    override suspend fun updateRecipe(recipe: Recipe) {
        recipeDao.update(recipe.copy(updatedAt = System.currentTimeMillis()))
    }

    override suspend fun deleteRecipe(id: Long) {
        recipeDao.delete(id)
    }

    override suspend fun importFromUrl(url: String): ImportResult {
        // Implementation will be added in a later task
        TODO("Import from URL functionality will be implemented in task 14")
    }
}
