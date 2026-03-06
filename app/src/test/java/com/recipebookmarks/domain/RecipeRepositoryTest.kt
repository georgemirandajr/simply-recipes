package com.recipebookmarks.domain

import com.recipebookmarks.data.*
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*
import kotlinx.coroutines.flow.flowOf

/**
 * Unit tests for RecipeRepository
 * Feature: fallback-recipe-import
 */
class RecipeRepositoryTest : StringSpec({
    
    "updateRecipe should update the updatedAt timestamp" {
        // Setup
        val recipeDao = mockk<RecipeDao>()
        val repository: RecipeRepository = RecipeRepositoryImpl(recipeDao)
        
        val originalTimestamp = System.currentTimeMillis() - 10000 // 10 seconds ago
        val recipe = Recipe(
            id = 1L,
            name = "Test Recipe",
            ingredients = emptyList(),
            instructions = emptyList(),
            originalUrl = "https://example.com",
            isFallback = true,
            createdAt = originalTimestamp,
            updatedAt = originalTimestamp
        )
        
        // Capture the updated recipe
        val updatedRecipeSlot = slot<Recipe>()
        coEvery { recipeDao.update(capture(updatedRecipeSlot)) } just Runs
        
        // Execute
        repository.updateRecipe(recipe)
        
        // Verify
        coVerify { recipeDao.update(any()) }
        val capturedRecipe = updatedRecipeSlot.captured
        
        // The updatedAt timestamp should be newer than the original
        (capturedRecipe.updatedAt > originalTimestamp) shouldBe true
        
        // Other fields should remain unchanged
        capturedRecipe.id shouldBe recipe.id
        capturedRecipe.name shouldBe recipe.name
        capturedRecipe.ingredients shouldBe recipe.ingredients
        capturedRecipe.instructions shouldBe recipe.instructions
        capturedRecipe.createdAt shouldBe recipe.createdAt
    }
    
    "deleteRecipe should remove recipe from database" {
        // Setup
        val recipeDao = mockk<RecipeDao>()
        val repository: RecipeRepository = RecipeRepositoryImpl(recipeDao)
        
        val recipeId = 42L
        
        // Mock the delete operation
        coEvery { recipeDao.delete(recipeId) } just Runs
        
        // Execute
        repository.deleteRecipe(recipeId)
        
        // Verify
        coVerify { recipeDao.delete(recipeId) }
    }
    
    "querying deleted recipe should return null" {
        // Setup
        val recipeDao = mockk<RecipeDao>()
        val repository: RecipeRepository = RecipeRepositoryImpl(recipeDao)
        
        val recipeId = 42L
        val recipe = Recipe(
            id = recipeId,
            name = "Test Recipe",
            ingredients = emptyList(),
            instructions = emptyList(),
            originalUrl = "https://example.com",
            isFallback = true
        )
        
        // Mock insert
        coEvery { recipeDao.insert(any()) } returns recipeId
        
        // Mock getById to return the recipe initially
        coEvery { recipeDao.getById(recipeId) } returns flowOf(recipe)
        
        // Insert the recipe
        repository.insertRecipe(recipe)
        
        // Verify recipe exists
        var retrievedRecipe: Recipe? = null
        repository.getRecipeById(recipeId).collect { r ->
            retrievedRecipe = r
        }
        retrievedRecipe shouldNotBe null
        
        // Mock delete
        coEvery { recipeDao.delete(recipeId) } just Runs
        
        // Delete the recipe
        repository.deleteRecipe(recipeId)
        
        // Mock getById to return null after deletion
        coEvery { recipeDao.getById(recipeId) } returns flowOf(null)
        
        // Query the deleted recipe
        var deletedRecipe: Recipe? = recipe
        repository.getRecipeById(recipeId).collect { r ->
            deletedRecipe = r
        }
        
        // Verify it returns null
        deletedRecipe shouldBe null
    }
    
    "updateRecipe should preserve recipe ID" {
        // Setup
        val recipeDao = mockk<RecipeDao>()
        val repository: RecipeRepository = RecipeRepositoryImpl(recipeDao)
        
        val recipeId = 123L
        val recipe = Recipe(
            id = recipeId,
            name = "Original Name",
            ingredients = emptyList(),
            instructions = emptyList(),
            originalUrl = "https://example.com",
            isFallback = true
        )
        
        // Capture the updated recipe
        val updatedRecipeSlot = slot<Recipe>()
        coEvery { recipeDao.update(capture(updatedRecipeSlot)) } just Runs
        
        // Execute
        repository.updateRecipe(recipe)
        
        // Verify
        val capturedRecipe = updatedRecipeSlot.captured
        capturedRecipe.id shouldBe recipeId
    }
    
    "updateRecipe should preserve all fields except updatedAt" {
        // Setup
        val recipeDao = mockk<RecipeDao>()
        val repository: RecipeRepository = RecipeRepositoryImpl(recipeDao)
        
        val ingredients = listOf(Ingredient("Flour", 2.0, "cups", 0))
        val instructions = listOf(Instruction("Mix ingredients", 0))
        val recipe = Recipe(
            id = 1L,
            name = "Test Recipe",
            ingredients = ingredients,
            instructions = instructions,
            yield = "4 servings",
            servingSize = "1 cup",
            nutritionInfo = NutritionInfo(
                calories = 200,
                protein = "10g",
                carbohydrates = "30g",
                fat = "5g",
                fiber = "2g",
                sugar = "5g"
            ),
            originalUrl = "https://example.com",
            category = Category.DESSERT,
            isFallback = false,
            createdAt = 1000L,
            updatedAt = 2000L
        )
        
        // Capture the updated recipe
        val updatedRecipeSlot = slot<Recipe>()
        coEvery { recipeDao.update(capture(updatedRecipeSlot)) } just Runs
        
        // Execute
        repository.updateRecipe(recipe)
        
        // Verify all fields are preserved except updatedAt
        val capturedRecipe = updatedRecipeSlot.captured
        capturedRecipe.id shouldBe recipe.id
        capturedRecipe.name shouldBe recipe.name
        capturedRecipe.ingredients shouldBe recipe.ingredients
        capturedRecipe.instructions shouldBe recipe.instructions
        capturedRecipe.yield shouldBe recipe.yield
        capturedRecipe.servingSize shouldBe recipe.servingSize
        capturedRecipe.nutritionInfo shouldBe recipe.nutritionInfo
        capturedRecipe.originalUrl shouldBe recipe.originalUrl
        capturedRecipe.category shouldBe recipe.category
        capturedRecipe.isFallback shouldBe recipe.isFallback
        capturedRecipe.createdAt shouldBe recipe.createdAt
        
        // Only updatedAt should be different
        capturedRecipe.updatedAt shouldNotBe recipe.updatedAt
        (capturedRecipe.updatedAt > recipe.updatedAt) shouldBe true
    }
    
    "deleteRecipe should work with any valid recipe ID" {
        // Setup
        val recipeDao = mockk<RecipeDao>()
        val repository: RecipeRepository = RecipeRepositoryImpl(recipeDao)
        
        val testIds = listOf(1L, 42L, 999L, 1000000L)
        
        testIds.forEach { recipeId ->
            // Mock the delete operation
            coEvery { recipeDao.delete(recipeId) } just Runs
            
            // Execute
            repository.deleteRecipe(recipeId)
            
            // Verify
            coVerify { recipeDao.delete(recipeId) }
        }
    }
})
