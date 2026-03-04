package com.recipebookmarks.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.recipebookmarks.domain.RecipeRepository

/**
 * Factory for creating RecipeListViewModel with repository dependency.
 */
class RecipeListViewModelFactory(
    private val repository: RecipeRepository
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RecipeListViewModel::class.java)) {
            return RecipeListViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
