package com.recipebookmarks.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.recipebookmarks.domain.RecipeRepository
import com.recipebookmarks.domain.ScalingCalculator

/**
 * Factory for creating RecipeDetailViewModel with repository, scalingCalculator, and recipeId dependencies.
 */
class RecipeDetailViewModelFactory(
    private val repository: RecipeRepository,
    private val scalingCalculator: ScalingCalculator,
    private val recipeId: Long
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RecipeDetailViewModel::class.java)) {
            return RecipeDetailViewModel(repository, scalingCalculator, recipeId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
