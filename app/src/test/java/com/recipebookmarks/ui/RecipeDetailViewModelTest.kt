package com.recipebookmarks.ui

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.recipebookmarks.data.Category
import com.recipebookmarks.data.Ingredient
import com.recipebookmarks.data.Instruction
import com.recipebookmarks.data.Recipe
import com.recipebookmarks.domain.RecipeRepository
import com.recipebookmarks.domain.ScalingCalculator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for RecipeDetailViewModel.
 * Tests delete functionality and other ViewModel operations.
 * 
 * Requirements: 8.7
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RecipeDetailViewModelTest {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    private val testDispatcher = StandardTestDispatcher()
    
    private lateinit var repository: RecipeRepository
    private lateinit var scalingCalculator: ScalingCalculator
    private lateinit var viewModel: RecipeDetailViewModel
    
    private val testRecipeId = 1L
    private val testRecipe = Recipe(
        id = testRecipeId,
        name = "Test Recipe",
        ingredients = listOf(
            Ingredient("Flour", 2.0, "cups", 0)
        ),
        instructions = listOf(
            Instruction("Mix ingredients", 0)
        ),
        yield = "4 servings",
        servingSize = "1 cup",
        nutritionInfo = null,
        originalUrl = "https://example.com/recipe",
        category = Category.DINNER,
        isFallback = false,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        scalingCalculator = mockk(relaxed = true)
        
        coEvery { repository.getRecipeById(testRecipeId) } returns flowOf(testRecipe)
        
        viewModel = RecipeDetailViewModel(repository, scalingCalculator, testRecipeId)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    /**
     * Test that deleteRecipe calls repository.deleteRecipe with correct ID.
     * Requirement 8.7: Remove the Recipe_Bookmark from database when user confirms deletion
     */
    @Test
    fun testDeleteRecipe() = runTest {
        // When
        viewModel.deleteRecipe()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        coVerify { repository.deleteRecipe(testRecipeId) }
    }
    
    /**
     * Test that deleteRecipe is called with the correct recipe ID.
     * Requirement 8.7: Remove the Recipe_Bookmark from database when user confirms deletion
     */
    @Test
    fun testDeleteRecipeWithSpecificId() = runTest {
        // Given
        val specificRecipeId = 42L
        val specificRecipe = testRecipe.copy(id = specificRecipeId)
        coEvery { repository.getRecipeById(specificRecipeId) } returns flowOf(specificRecipe)
        
        val specificViewModel = RecipeDetailViewModel(repository, scalingCalculator, specificRecipeId)
        
        // When
        specificViewModel.deleteRecipe()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        coVerify { repository.deleteRecipe(specificRecipeId) }
    }
}
