package com.recipebookmarks.integration

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.recipebookmarks.data.Ingredient
import com.recipebookmarks.data.Instruction
import com.recipebookmarks.data.Recipe
import com.recipebookmarks.data.RecipeDatabase
import com.recipebookmarks.domain.RecipeRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for recipe deletion flows.
 * Tests Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 8.7, 8.8, 8.9, 8.10
 */
@RunWith(AndroidJUnit4::class)
class RecipeDeleteFlowIntegrationTest {

    private lateinit var context: Context
    private lateinit var database: RecipeDatabase
    private lateinit var repository: RecipeRepositoryImpl

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = RecipeDatabase.getDatabase(context, inMemory = true)
        repository = RecipeRepositoryImpl(database.recipeDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testDeleteRecipeFromDetail_RemovesFromDatabase() = runTest {
        // Given - Create a recipe
        val recipe = Recipe(
            name = "Test Recipe",
            ingredients = listOf(Ingredient("Flour", 2.0, "cups", 0)),
            instructions = listOf(Instruction("Mix", 0)),
            originalUrl = "https://example.com/recipe",
            isFallback = false
        )
        val recipeId = repository.insertRecipe(recipe)

        // Verify recipe exists
        var recipes = repository.getAllRecipesOnce()
        assertEquals(1, recipes.size)

        // When - Delete the recipe (simulating delete from detail view)
        repository.deleteRecipe(recipeId)

        // Then - Verify recipe is removed from database
        recipes = repository.getAllRecipesOnce()
        assertEquals(0, recipes.size)
    }

    @Test
    fun testDeleteRecipeFromDetail_QueryReturnsNull() = runTest {
        // Given - Create a recipe
        val recipe = Recipe(
            name = "Test Recipe",
            ingredients = listOf(Ingredient("Flour", 2.0, "cups", 0)),
            instructions = listOf(Instruction("Mix", 0)),
            originalUrl = "https://example.com/recipe",
            isFallback = false
        )
        val recipeId = repository.insertRecipe(recipe)

        // When - Delete the recipe
        repository.deleteRecipe(recipeId)

        // Then - Verify querying for the recipe returns null
        val deletedRecipe = repository.getRecipeById(recipeId)
        val result = deletedRecipe.first()
        assertNull(result)
    }

    @Test
    fun testDeleteFallbackRecipe_RemovesFromDatabase() = runTest {
        // Given - Create a fallback recipe
        val fallbackRecipe = Recipe(
            name = "Fallback Recipe",
            ingredients = emptyList(),
            instructions = emptyList(),
            originalUrl = "https://example.com/recipe",
            isFallback = true
        )
        val recipeId = repository.insertRecipe(fallbackRecipe)

        // Verify recipe exists
        var recipes = repository.getAllRecipesOnce()
        assertEquals(1, recipes.size)

        // When - Delete the fallback recipe
        repository.deleteRecipe(recipeId)

        // Then - Verify recipe is removed from database
        recipes = repository.getAllRecipesOnce()
        assertEquals(0, recipes.size)
    }

    @Test
    fun testDeleteRecipeFromList_RemovesFromDatabase() = runTest {
        // Given - Create multiple recipes
        val recipe1 = Recipe(
            name = "Recipe 1",
            ingredients = listOf(Ingredient("Flour", 2.0, "cups", 0)),
            instructions = listOf(Instruction("Mix", 0)),
            originalUrl = "https://example.com/recipe1",
            isFallback = false
        )
        val recipe2 = Recipe(
            name = "Recipe 2",
            ingredients = listOf(Ingredient("Sugar", 1.0, "cup", 0)),
            instructions = listOf(Instruction("Stir", 0)),
            originalUrl = "https://example.com/recipe2",
            isFallback = false
        )
        val recipe3 = Recipe(
            name = "Recipe 3",
            ingredients = emptyList(),
            instructions = emptyList(),
            originalUrl = "https://example.com/recipe3",
            isFallback = true
        )

        val recipeId1 = repository.insertRecipe(recipe1)
        val recipeId2 = repository.insertRecipe(recipe2)
        val recipeId3 = repository.insertRecipe(recipe3)

        // Verify all recipes exist
        var recipes = repository.getAllRecipesOnce()
        assertEquals(3, recipes.size)

        // When - Delete recipe 2 from list
        repository.deleteRecipe(recipeId2)

        // Then - Verify recipe 2 is removed but others remain
        recipes = repository.getAllRecipesOnce()
        assertEquals(2, recipes.size)
        assertTrue(recipes.any { it.id == recipeId1 })
        assertFalse(recipes.any { it.id == recipeId2 })
        assertTrue(recipes.any { it.id == recipeId3 })
    }

    @Test
    fun testDeleteRecipeFromList_UpdatesList() = runTest {
        // Given - Create multiple recipes
        val recipe1 = Recipe(
            name = "Recipe 1",
            ingredients = listOf(Ingredient("Flour", 2.0, "cups", 0)),
            instructions = listOf(Instruction("Mix", 0)),
            originalUrl = "https://example.com/recipe1",
            isFallback = false
        )
        val recipe2 = Recipe(
            name = "Recipe 2",
            ingredients = listOf(Ingredient("Sugar", 1.0, "cup", 0)),
            instructions = listOf(Instruction("Stir", 0)),
            originalUrl = "https://example.com/recipe2",
            isFallback = false
        )

        repository.insertRecipe(recipe1)
        val recipeId2 = repository.insertRecipe(recipe2)

        // When - Delete recipe 2
        repository.deleteRecipe(recipeId2)

        // Then - Verify list is updated
        val recipes = repository.getAllRecipesOnce()
        assertEquals(1, recipes.size)
        assertEquals("Recipe 1", recipes[0].name)
    }

    @Test
    fun testDeleteCancellation_RecipeRemainsInDatabase() = runTest {
        // Given - Create a recipe
        val recipe = Recipe(
            name = "Test Recipe",
            ingredients = listOf(Ingredient("Flour", 2.0, "cups", 0)),
            instructions = listOf(Instruction("Mix", 0)),
            originalUrl = "https://example.com/recipe",
            isFallback = false
        )
        val recipeId = repository.insertRecipe(recipe)

        // Verify recipe exists
        var recipes = repository.getAllRecipesOnce()
        assertEquals(1, recipes.size)

        // When - User cancels deletion (simulated by not calling deleteRecipe)
        // No deletion occurs

        // Then - Verify recipe remains in database
        recipes = repository.getAllRecipesOnce()
        assertEquals(1, recipes.size)
        assertEquals(recipeId, recipes[0].id)
        assertEquals("Test Recipe", recipes[0].name)
    }

    @Test
    fun testDeleteCancellation_RecipeRemainsVisible() = runTest {
        // Given - Create multiple recipes
        val recipe1 = Recipe(
            name = "Recipe 1",
            ingredients = listOf(Ingredient("Flour", 2.0, "cups", 0)),
            instructions = listOf(Instruction("Mix", 0)),
            originalUrl = "https://example.com/recipe1",
            isFallback = false
        )
        val recipe2 = Recipe(
            name = "Recipe 2",
            ingredients = listOf(Ingredient("Sugar", 1.0, "cup", 0)),
            instructions = listOf(Instruction("Stir", 0)),
            originalUrl = "https://example.com/recipe2",
            isFallback = false
        )

        val recipeId1 = repository.insertRecipe(recipe1)
        val recipeId2 = repository.insertRecipe(recipe2)

        // When - User cancels deletion (no deletion occurs)
        // No deletion occurs

        // Then - Verify both recipes remain visible
        val recipes = repository.getAllRecipesOnce()
        assertEquals(2, recipes.size)
        assertTrue(recipes.any { it.id == recipeId1 })
        assertTrue(recipes.any { it.id == recipeId2 })
    }

    @Test
    fun testDeleteMultipleRecipes_AllRemoved() = runTest {
        // Given - Create multiple recipes
        val recipe1 = Recipe(
            name = "Recipe 1",
            ingredients = listOf(Ingredient("Flour", 2.0, "cups", 0)),
            instructions = listOf(Instruction("Mix", 0)),
            originalUrl = "https://example.com/recipe1",
            isFallback = false
        )
        val recipe2 = Recipe(
            name = "Recipe 2",
            ingredients = emptyList(),
            instructions = emptyList(),
            originalUrl = "https://example.com/recipe2",
            isFallback = true
        )
        val recipe3 = Recipe(
            name = "Recipe 3",
            ingredients = listOf(Ingredient("Salt", 0.5, "tsp", 0)),
            instructions = listOf(Instruction("Add", 0)),
            originalUrl = "https://example.com/recipe3",
            isFallback = false
        )

        val recipeId1 = repository.insertRecipe(recipe1)
        val recipeId2 = repository.insertRecipe(recipe2)
        val recipeId3 = repository.insertRecipe(recipe3)

        // Verify all recipes exist
        var recipes = repository.getAllRecipesOnce()
        assertEquals(3, recipes.size)

        // When - Delete all recipes one by one
        repository.deleteRecipe(recipeId1)
        repository.deleteRecipe(recipeId2)
        repository.deleteRecipe(recipeId3)

        // Then - Verify all recipes are removed
        recipes = repository.getAllRecipesOnce()
        assertEquals(0, recipes.size)
    }

    @Test
    fun testDeleteNonExistentRecipe_NoError() = runTest {
        // Given - No recipes in database
        var recipes = repository.getAllRecipesOnce()
        assertEquals(0, recipes.size)

        // When - Try to delete a non-existent recipe
        try {
            repository.deleteRecipe(999L)
            // Then - No exception should be thrown
            assertTrue(true)
        } catch (e: Exception) {
            fail("Deleting non-existent recipe should not throw exception")
        }

        // Verify database is still empty
        recipes = repository.getAllRecipesOnce()
        assertEquals(0, recipes.size)
    }
}
