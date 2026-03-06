package com.recipebookmarks.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recipebookmarks.data.Recipe
import com.recipebookmarks.domain.RecipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RecipeEditorViewModel(
    private val repository: RecipeRepository,
    private val recipeId: Long
) : ViewModel() {
    
    private val _recipe = MutableStateFlow<Recipe?>(null)
    val recipe: StateFlow<Recipe?> = _recipe.asStateFlow()
    
    init {
        viewModelScope.launch {
            repository.getRecipeById(recipeId).collect { recipe ->
                _recipe.value = recipe
            }
        }
    }
    
    fun saveRecipe(updatedRecipe: Recipe) {
        viewModelScope.launch {
            repository.updateRecipe(updatedRecipe)
        }
    }
}
