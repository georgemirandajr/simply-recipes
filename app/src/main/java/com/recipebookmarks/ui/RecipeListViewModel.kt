package com.recipebookmarks.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recipebookmarks.data.Category
import com.recipebookmarks.data.Recipe
import com.recipebookmarks.domain.RecipeRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the recipe list screen.
 * Manages recipe list state, search query, and category filtering.
 * 
 * Requirements: 1.1, 9.7, 10.2
 */
class RecipeListViewModel(
    private val repository: RecipeRepository
) : ViewModel() {

    // Search query state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // Category filter state (null means no filter, show all)
    private val _categoryFilter = MutableStateFlow<Category?>(null)
    val categoryFilter: StateFlow<Category?> = _categoryFilter

    /**
     * Recipe list that combines repository data with search and category filters.
     * Automatically updates when repository data changes or filters are modified.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val recipes: StateFlow<List<Recipe>> = combine(
        _searchQuery,
        _categoryFilter
    ) { query, category ->
        Pair(query, category)
    }.flatMapLatest { (query, category) ->
        when {
            // Both search and category filter applied
            query.isNotBlank() && category != null -> {
                repository.getRecipesByCategory(category)
                    .combine(repository.searchRecipes(query)) { categoryRecipes, searchRecipes ->
                        // Intersection of both filters
                        categoryRecipes.filter { recipe ->
                            searchRecipes.any { it.id == recipe.id }
                        }
                    }
            }
            // Only search filter applied
            query.isNotBlank() -> repository.searchRecipes(query)
            // Only category filter applied
            category != null -> repository.getRecipesByCategory(category)
            // No filters, show all recipes
            else -> repository.getAllRecipes()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /**
     * Updates the search query.
     * Requirement 10.2: Filter recipe list based on search query
     */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Updates the category filter.
     * Pass null to clear the filter and show all recipes.
     * Requirement 9.7: Filter recipes by selected category
     */
    fun setCategoryFilter(category: Category?) {
        _categoryFilter.value = category
    }

    /**
     * Clears the search query.
     * Requirement 10.6: Restore full recipe list when search is cleared
     */
    fun clearSearch() {
        _searchQuery.value = ""
    }

    /**
     * Clears the category filter.
     * Requirement 9.8: Display all recipes when no category filter is selected
     */
    fun clearCategoryFilter() {
        _categoryFilter.value = null
    }

    /**
     * Deletes a recipe from the repository.
     * The recipe list will update automatically via Flow.
     * Requirement 8.10: Delete recipe from list view
     */
    fun deleteRecipe(recipeId: Long) {
        viewModelScope.launch {
            repository.deleteRecipe(recipeId)
        }
    }
}
