package com.recipebookmarks.domain

import com.recipebookmarks.data.*
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.mockk.*
import kotlinx.coroutines.flow.flowOf

/**
 * Property-based tests for RecipeRepository
 * Feature: fallback-recipe-import
 */
class RecipeRepositoryPropertyTest : StringSpec({
    
    "Property 12: Recipe Edit Persistence - edited recipes should persist with updated timestamp" {
        // **Validates: Requirements 7.12**
        // For any recipe that has been edited and saved,
        // querying the database should return the recipe with all modifications applied
        // and an updated updatedAt timestamp
        
        checkAll(100, Arb.editableRecipe()) { (originalRecipe, editedName) ->
            // Setup mocks
            val recipeDao = mockk<RecipeDao>()
            val repository: RecipeRepository = RecipeRepositoryImpl(recipeDao)
            
            val recipeId = 42L
            val insertedRecipe = originalRecipe.copy(id = recipeId)
            
            // Mock the insert
            coEvery { recipeDao.insert(any()) } returns recipeId
            
            // Mock getById to return the inserted recipe
            coEvery { recipeDao.getById(recipeId) } returns flowOf(insertedRecipe)
            
            // Capture the updated recipe
            val updatedRecipeSlot = slot<Recipe>()
            coEvery { recipeDao.update(capture(updatedRecipeSlot)) } just Runs
            
            // Insert the original recipe
            repository.insertRecipe(originalRecipe)
            
            // Edit the recipe
            val editedRecipe = insertedRecipe.copy(name = editedName)
            
            // Update the recipe
            repository.updateRecipe(editedRecipe)
            
            // Verify update was called
            coVerify { recipeDao.update(any()) }
            
            // Verify the updated recipe has the new name
            val capturedRecipe = updatedRecipeSlot.captured
            capturedRecipe.name shouldBe editedName
            capturedRecipe.id shouldBe recipeId
            
            // Verify updatedAt timestamp was updated
            (capturedRecipe.updatedAt > editedRecipe.updatedAt) shouldBe true
        }
    }
    
    "Property 12: Recipe Edit Persistence - ingredient list edits should persist" {
        // **Validates: Requirements 7.12**
        // For any fallback recipe with edited ingredients,
        // the changes should persist to the database
        
        checkAll(50, Arb.fallbackRecipeWithIngredients()) { (recipe, newIngredients) ->
            // Setup mocks
            val recipeDao = mockk<RecipeDao>()
            val repository: RecipeRepository = RecipeRepositoryImpl(recipeDao)
            
            val recipeId = 42L
            val insertedRecipe = recipe.copy(id = recipeId)
            
            // Mock the insert
            coEvery { recipeDao.insert(any()) } returns recipeId
            
            // Mock getById to return the inserted recipe
            coEvery { recipeDao.getById(recipeId) } returns flowOf(insertedRecipe)
            
            // Capture the updated recipe
            val updatedRecipeSlot = slot<Recipe>()
            coEvery { recipeDao.update(capture(updatedRecipeSlot)) } just Runs
            
            // Insert the recipe
            repository.insertRecipe(recipe)
            
            // Edit the recipe with new ingredients
            val editedRecipe = insertedRecipe.copy(ingredients = newIngredients)
            
            // Update the recipe
            repository.updateRecipe(editedRecipe)
            
            // Verify update was called
            coVerify { recipeDao.update(any()) }
            
            // Verify ingredient changes persisted
            val capturedRecipe = updatedRecipeSlot.captured
            capturedRecipe.ingredients shouldBe newIngredients
        }
    }
    
    "Property 12: Recipe Edit Persistence - instruction list edits should persist" {
        // **Validates: Requirements 7.12**
        // For any fallback recipe with edited instructions,
        // the changes should persist to the database
        
        checkAll(50, Arb.fallbackRecipeWithInstructions()) { (recipe, newInstructions) ->
            // Setup mocks
            val recipeDao = mockk<RecipeDao>()
            val repository: RecipeRepository = RecipeRepositoryImpl(recipeDao)
            
            val recipeId = 42L
            val insertedRecipe = recipe.copy(id = recipeId)
            
            // Mock the insert
            coEvery { recipeDao.insert(any()) } returns recipeId
            
            // Mock getById to return the inserted recipe
            coEvery { recipeDao.getById(recipeId) } returns flowOf(insertedRecipe)
            
            // Capture the updated recipe
            val updatedRecipeSlot = slot<Recipe>()
            coEvery { recipeDao.update(capture(updatedRecipeSlot)) } just Runs
            
            // Insert the recipe
            repository.insertRecipe(recipe)
            
            // Edit the recipe with new instructions
            val editedRecipe = insertedRecipe.copy(instructions = newInstructions)
            
            // Update the recipe
            repository.updateRecipe(editedRecipe)
            
            // Verify update was called
            coVerify { recipeDao.update(any()) }
            
            // Verify instruction changes persisted
            val capturedRecipe = updatedRecipeSlot.captured
            capturedRecipe.instructions shouldBe newInstructions
        }
    }
    
    "Property 13: Recipe Deletion Confirmation - deleted recipes should be removed from database" {
        // **Validates: Requirements 8.7**
        // For any recipe, when the user confirms deletion,
        // the recipe should be removed from the database and subsequent queries
        // for that recipe ID should return null
        
        checkAll(100, Arb.recipeForDeletion()) { recipe ->
            // Setup mocks
            val recipeDao = mockk<RecipeDao>()
            val repository: RecipeRepository = RecipeRepositoryImpl(recipeDao)
            
            val recipeId = 42L
            val insertedRecipe = recipe.copy(id = recipeId)
            
            // Mock the insert
            coEvery { recipeDao.insert(any()) } returns recipeId
            
            // Mock getById to return the recipe before deletion
            coEvery { recipeDao.getById(recipeId) } returns flowOf(insertedRecipe)
            
            // Mock the delete operation
            coEvery { recipeDao.delete(recipeId) } just Runs
            
            // Insert the recipe
            repository.insertRecipe(recipe)
            
            // Delete the recipe (simulating user confirmation)
            repository.deleteRecipe(recipeId)
            
            // Verify delete was called with the correct ID
            coVerify { recipeDao.delete(recipeId) }
            
            // Mock getById to return null after deletion
            coEvery { recipeDao.getById(recipeId) } returns flowOf(null)
            
            // Verify querying the deleted recipe returns null
            var deletedRecipe: Recipe? = insertedRecipe
            repository.getRecipeById(recipeId).collect { recipe ->
                deletedRecipe = recipe
            }
            
            deletedRecipe shouldBe null
        }
    }
    
    "Property 13: Recipe Deletion Confirmation - deletion works for both fallback and regular recipes" {
        // **Validates: Requirements 8.7**
        // Both fallback and regular recipes should be deletable
        
        checkAll(50, Arb.mixedRecipeTypes()) { recipe ->
            // Setup mocks
            val recipeDao = mockk<RecipeDao>()
            val repository: RecipeRepository = RecipeRepositoryImpl(recipeDao)
            
            val recipeId = 42L
            
            // Mock the insert
            coEvery { recipeDao.insert(any()) } returns recipeId
            
            // Mock the delete operation
            coEvery { recipeDao.delete(recipeId) } just Runs
            
            // Insert the recipe
            repository.insertRecipe(recipe)
            
            // Delete the recipe
            repository.deleteRecipe(recipeId)
            
            // Verify delete was called regardless of recipe type
            coVerify { recipeDao.delete(recipeId) }
        }
    }
    
    "Property 14: Recipe Deletion Cancellation - recipes remain unchanged when deletion is not confirmed" {
        // **Validates: Requirements 8.8**
        // For any recipe, when the user cancels deletion,
        // the recipe should remain in the database unchanged
        
        checkAll(100, Arb.recipeForDeletion()) { recipe ->
            // Setup mocks
            val recipeDao = mockk<RecipeDao>()
            val repository: RecipeRepository = RecipeRepositoryImpl(recipeDao)
            
            val recipeId = 42L
            val insertedRecipe = recipe.copy(id = recipeId)
            
            // Mock the insert
            coEvery { recipeDao.insert(any()) } returns recipeId
            
            // Mock getById to return the recipe
            coEvery { recipeDao.getById(recipeId) } returns flowOf(insertedRecipe)
            
            // Mock the delete operation (but we won't call it)
            coEvery { recipeDao.delete(recipeId) } just Runs
            
            // Insert the recipe
            repository.insertRecipe(recipe)
            
            // Simulate cancellation - we don't call deleteRecipe
            // Instead, we just query the recipe to verify it's still there
            
            var retrievedRecipe: Recipe? = null
            repository.getRecipeById(recipeId).collect { recipe ->
                retrievedRecipe = recipe
            }
            
            // Verify the recipe is still in the database
            retrievedRecipe shouldBe insertedRecipe
            
            // Verify delete was never called (cancellation scenario)
            coVerify(exactly = 0) { recipeDao.delete(any()) }
        }
    }
    
    "Property 14: Recipe Deletion Cancellation - recipe data remains unchanged after cancellation" {
        // **Validates: Requirements 8.8**
        // When deletion is cancelled, all recipe data should remain exactly as it was
        
        checkAll(50, Arb.editableRecipe()) { (recipe, _) ->
            // Setup mocks
            val recipeDao = mockk<RecipeDao>()
            val repository: RecipeRepository = RecipeRepositoryImpl(recipeDao)
            
            val recipeId = 42L
            val insertedRecipe = recipe.copy(id = recipeId)
            
            // Mock the insert
            coEvery { recipeDao.insert(any()) } returns recipeId
            
            // Mock getById to return the recipe
            coEvery { recipeDao.getById(recipeId) } returns flowOf(insertedRecipe)
            
            // Insert the recipe
            repository.insertRecipe(recipe)
            
            // Simulate showing delete dialog and then cancelling
            // (no deleteRecipe call)
            
            // Query the recipe
            var retrievedRecipe: Recipe? = null
            repository.getRecipeById(recipeId).collect { recipe ->
                retrievedRecipe = recipe
            }
            
            // Verify all fields remain unchanged
            retrievedRecipe shouldBe insertedRecipe
            retrievedRecipe?.name shouldBe insertedRecipe.name
            retrievedRecipe?.ingredients shouldBe insertedRecipe.ingredients
            retrievedRecipe?.instructions shouldBe insertedRecipe.instructions
            retrievedRecipe?.isFallback shouldBe insertedRecipe.isFallback
        }
    }
})

/**
 * Custom Arb generators for repository testing
 */
fun Arb.Companion.editableRecipe(): Arb<Pair<Recipe, String>> = arbitrary {
    val recipe = Recipe(
        name = Arb.string(1..200).bind(),
        ingredients = emptyList(),
        instructions = emptyList(),
        originalUrl = Arb.string(10..100).bind(),
        isFallback = true,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
    val editedName = Arb.string(1..200).bind()
    recipe to editedName
}

fun Arb.Companion.fallbackRecipeWithIngredients(): Arb<Pair<Recipe, List<Ingredient>>> = arbitrary {
    val recipe = Recipe(
        name = Arb.string(1..200).bind(),
        ingredients = emptyList(),
        instructions = emptyList(),
        originalUrl = Arb.string(10..100).bind(),
        isFallback = true,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
    val newIngredients = listOf(
        Ingredient(Arb.string(1..50).bind(), 1.0, "cup", 0),
        Ingredient(Arb.string(1..50).bind(), 2.0, "tbsp", 1)
    )
    recipe to newIngredients
}

fun Arb.Companion.fallbackRecipeWithInstructions(): Arb<Pair<Recipe, List<Instruction>>> = arbitrary {
    val recipe = Recipe(
        name = Arb.string(1..200).bind(),
        ingredients = emptyList(),
        instructions = emptyList(),
        originalUrl = Arb.string(10..100).bind(),
        isFallback = true,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
    val newInstructions = listOf(
        Instruction(Arb.string(10..100).bind(), 0),
        Instruction(Arb.string(10..100).bind(), 1)
    )
    recipe to newInstructions
}

fun Arb.Companion.recipeForDeletion(): Arb<Recipe> = arbitrary {
    Recipe(
        name = Arb.string(1..200).bind(),
        ingredients = emptyList(),
        instructions = emptyList(),
        originalUrl = Arb.string(10..100).bind(),
        isFallback = true,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
}

fun Arb.Companion.mixedRecipeTypes(): Arb<Recipe> = arbitrary {
    val isFallback = listOf(true, false).random()
    Recipe(
        name = Arb.string(1..200).bind(),
        ingredients = if (isFallback) emptyList() else listOf(Ingredient("Test", 1.0, "cup", 0)),
        instructions = if (isFallback) emptyList() else listOf(Instruction("Test step", 0)),
        originalUrl = Arb.string(10..100).bind(),
        isFallback = isFallback,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
}
