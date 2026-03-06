package com.recipebookmarks.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.recipebookmarks.domain.RecipeRepository

class RecipeEditorViewModelFactory(
    private val repository: RecipeRepository,
    private val recipeId: Long
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RecipeEditorViewModel::class.java)) {
            return RecipeEditorViewModel(repository, recipeId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
