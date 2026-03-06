package com.recipebookmarks.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recipebookmarks.data.Category
import com.recipebookmarks.data.Recipe
import com.recipebookmarks.data.ScaledIngredient
import com.recipebookmarks.domain.RecipeRepository
import com.recipebookmarks.domain.ScalingCalculator
import com.recipebookmarks.domain.ScalingFactor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the recipe detail screen.
 * Manages recipe data loading, scaling factor state, and scaled ingredient calculations.
 * 
 * Requirements: 1.2, 8.6
 */
class RecipeDetailViewModel(
    private val repository: RecipeRepository,
    private val scalingCalculator: ScalingCalculator,
    private val recipeId: Long
) : ViewModel() {

    // Scaling factor state (default to 1.0x)
    // Requirement 8.6: Default to 1.0x scaling factor when recipe is first displayed
    private val _scalingFactor = MutableStateFlow(ScalingFactor.SINGLE)
    val scalingFactor: StateFlow<ScalingFactor> = _scalingFactor

    /**
     * Recipe data loaded from repository.
     * Requirement 1.2: Load recipe by ID from repository
     */
    val recipe: StateFlow<Recipe?> = repository.getRecipeById(recipeId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    /**
     * Scaled ingredients calculated based on current scaling factor.
     * Combines recipe data with scaling factor to produce scaled ingredient list.
     * Requirements 8.2, 8.3: Call ScalingCalculator when scaling factor changes
     */
    val scaledIngredients: StateFlow<List<ScaledIngredient>> = combine(
        recipe,
        _scalingFactor
    ) { currentRecipe, factor ->
        if (currentRecipe != null) {
            scalingCalculator.scaleIngredients(currentRecipe.ingredients, factor)
        } else {
            emptyList()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /**
     * Updates the scaling factor.
     * Valid values are SINGLE (1.0x), ONE_AND_HALF (1.5x), and DOUBLE (2.0x).
     * Requirements 8.5, 8.6: Add scaling factor state management
     */
    fun setScalingFactor(factor: ScalingFactor) {
        _scalingFactor.value = factor
    }
    
    /**
     * Updates the category of the current recipe.
     * Requirements 9.2, 9.4: Allow users to assign and modify category tags
     */
    fun updateCategory(category: Category) {
        viewModelScope.launch {
            recipe.value?.let { currentRecipe ->
                val updatedRecipe = currentRecipe.copy(
                    category = category,
                    updatedAt = System.currentTimeMillis()
                )
                repository.updateRecipe(updatedRecipe)
            }
        }
    }
    
    /**
     * Deletes the current recipe from the database.
     * Requirement 8.7: Remove the Recipe_Bookmark from database when user confirms deletion
     */
    fun deleteRecipe() {
        viewModelScope.launch {
            repository.deleteRecipe(recipeId)
        }
    }
}
