package com.recipebookmarks.integration

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.recipebookmarks.data.Ingredient
import com.recipebookmarks.data.Instruction
import com.recipebookmarks.data.Recipe
import com.recipebookmarks.data.RecipeDatabase
import com.recipebookmarks.domain.RecipeRepositoryImpl
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for recipe editing flows.
 * Tests Requirements: 7.1, 7.2, 7.3, 7.6, 7.7, 7.8, 7.9, 7.10, 7.11, 7.12, 7.13
 */
@RunWith(AndroidJUnit4::class)
class RecipeEditFlowIntegrationTest {

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
    fun testEditFallbackRecipe_AllFieldsEditable() = runTest {
        // Given - Create a fallback recipe
        val fallbackRecipe = Recipe(
            name = "Original Fallback Recipe",
            ingredients = emptyList(),
            instructions = emptyList(),
            originalUrl = "https://example.com/recipe",
            isFallback = true
        )
        val recipeId = repository.insertRecipe(fallbackRecipe)

        // When - Edit all fields
        val updatedRecipe = fallbackRecipe.copy(
            id = recipeId,
            name = "Updated Fallback Recipe",
            ingredients = listOf(
                Ingredient("Flour", 2.0, "cups", 0),
                Ingredient("Sugar", 1.0, "cup", 1)
            ),
            instructions = listOf(
                Instruction("Mix dry ingredients", 0),
                Instruction("Add wet ingredients", 1),
                Instruction("Bake at 350F", 2)
            ),
            yield = "12 servings"
        )
        repository.updateRecipe(updatedRecipe)

        // Then - Verify changes persist
        val recipes = repository.getAllRecipesOnce()
        assertEquals(1, recipes.size)
        
        val savedRecipe = recipes[0]
        assertEquals("Updated Fallback Recipe", savedRecipe.name)
        assertEquals(2, savedRecipe.ingredients.size)
        assertEquals(3, savedRecipe.instructions.size)
        assertEquals("12 servings", savedRecipe.yield)
        assertTrue(savedRecipe.isFallback)
        assertEquals("https://example.com/recipe", savedRecipe.originalUrl)
    }

    @Test
    fun testEditFallbackRecipe_AddIngredients() = runTest {
        // Given - Create a fallback recipe with no ingredients
        val fallbackRecipe = Recipe(
            name = "Recipe Without Ingredients",
            ingredients = emptyList(),
            instructions = emptyList(),
            originalUrl = "https://example.com/recipe",
            isFallback = true
        )
        val recipeId = repository.insertRecipe(fallbackRecipe)

        // When - Add ingredients
        val updatedRecipe = fallbackRecipe.copy(
            id = recipeId,
            ingredients = listOf(
                Ingredient("Flour", 2.0, "cups", 0),
                Ingredient("Sugar", 1.0, "cup", 1),
                Ingredient("Eggs", 2.0, "whole", 2)
            )
        )
        repository.updateRecipe(updatedRecipe)

        // Then - Verify ingredients were added
        val recipes = repository.getAllRecipesOnce()
        val savedRecipe = recipes[0]
        assertEquals(3, savedRecipe.ingredients.size)
        assertEquals("Flour", savedRecipe.ingredients[0].name)
        assertEquals("Sugar", savedRecipe.ingredients[1].name)
        assertEquals("Eggs", savedRecipe.ingredients[2].name)
    }

    @Test
    fun testEditFallbackRecipe_ModifyIngredients() = runTest {
        // Given - Create a fallback recipe with ingredients
        val fallbackRecipe = Recipe(
            name = "Recipe With Ingredients",
            ingredients = listOf(
                Ingredient("Flour", 2.0, "cups", 0),
                Ingredient("Sugar", 1.0, "cup", 1)
            ),
            instructions = emptyList(),
            originalUrl = "https://example.com/recipe",
            isFallback = true
        )
        val recipeId = repository.insertRecipe(fallbackRecipe)

        // When - Modify ingredients
        val updatedRecipe = fallbackRecipe.copy(
            id = recipeId,
            ingredients = listOf(
                Ingredient("Whole Wheat Flour", 3.0, "cups", 0),
                Ingredient("Brown Sugar", 1.5, "cups", 1)
            )
        )
        repository.updateRecipe(updatedRecipe)

        // Then - Verify ingredients were modified
        val recipes = repository.getAllRecipesOnce()
        val savedRecipe = recipes[0]
        assertEquals(2, savedRecipe.ingredients.size)
        assertEquals("Whole Wheat Flour", savedRecipe.ingredients[0].name)
        assertEquals(3.0, savedRecipe.ingredients[0].quantity, 0.01)
        assertEquals("Brown Sugar", savedRecipe.ingredients[1].name)
        assertEquals(1.5, savedRecipe.ingredients[1].quantity, 0.01)
    }

    @Test
    fun testEditFallbackRecipe_RemoveIngredients() = runTest {
        // Given - Create a fallback recipe with ingredients
        val fallbackRecipe = Recipe(
            name = "Recipe With Ingredients",
            ingredients = listOf(
                Ingredient("Flour", 2.0, "cups", 0),
                Ingredient("Sugar", 1.0, "cup", 1),
                Ingredient("Eggs", 2.0, "whole", 2)
            ),
            instructions = emptyList(),
            originalUrl = "https://example.com/recipe",
            isFallback = true
        )
        val recipeId = repository.insertRecipe(fallbackRecipe)

        // When - Remove one ingredient
        val updatedRecipe = fallbackRecipe.copy(
            id = recipeId,
            ingredients = listOf(
                Ingredient("Flour", 2.0, "cups", 0),
                Ingredient("Sugar", 1.0, "cup", 1)
            )
        )
        repository.updateRecipe(updatedRecipe)

        // Then - Verify ingredient was removed
        val recipes = repository.getAllRecipesOnce()
        val savedRecipe = recipes[0]
        assertEquals(2, savedRecipe.ingredients.size)
        assertFalse(savedRecipe.ingredients.any { it.name == "Eggs" })
    }

    @Test
    fun testEditFallbackRecipe_AddInstructions() = runTest {
        // Given - Create a fallback recipe with no instructions
        val fallbackRecipe = Recipe(
            name = "Recipe Without Instructions",
            ingredients = emptyList(),
            instructions = emptyList(),
            originalUrl = "https://example.com/recipe",
            isFallback = true
        )
        val recipeId = repository.insertRecipe(fallbackRecipe)

        // When - Add instructions
        val updatedRecipe = fallbackRecipe.copy(
            id = recipeId,
            instructions = listOf(
                Instruction("Preheat oven to 350F", 0),
                Instruction("Mix ingredients", 1),
                Instruction("Bake for 30 minutes", 2)
            )
        )
        repository.updateRecipe(updatedRecipe)

        // Then - Verify instructions were added
        val recipes = repository.getAllRecipesOnce()
        val savedRecipe = recipes[0]
        assertEquals(3, savedRecipe.instructions.size)
        assertEquals("Preheat oven to 350F", savedRecipe.instructions[0].text)
        assertEquals("Mix ingredients", savedRecipe.instructions[1].text)
        assertEquals("Bake for 30 minutes", savedRecipe.instructions[2].text)
    }

    @Test
    fun testEditFallbackRecipe_ModifyInstructions() = runTest {
        // Given - Create a fallback recipe with instructions
        val fallbackRecipe = Recipe(
            name = "Recipe With Instructions",
            ingredients = emptyList(),
            instructions = listOf(
                Instruction("Mix ingredients", 0),
                Instruction("Bake", 1)
            ),
            originalUrl = "https://example.com/recipe",
            isFallback = true
        )
        val recipeId = repository.insertRecipe(fallbackRecipe)

        // When - Modify instructions
        val updatedRecipe = fallbackRecipe.copy(
            id = recipeId,
            instructions = listOf(
                Instruction("Mix dry and wet ingredients separately", 0),
                Instruction("Bake at 350F for 30 minutes", 1)
            )
        )
        repository.updateRecipe(updatedRecipe)

        // Then - Verify instructions were modified
        val recipes = repository.getAllRecipesOnce()
        val savedRecipe = recipes[0]
        assertEquals(2, savedRecipe.instructions.size)
        assertEquals("Mix dry and wet ingredients separately", savedRecipe.instructions[0].text)
        assertEquals("Bake at 350F for 30 minutes", savedRecipe.instructions[1].text)
    }

    @Test
    fun testEditFallbackRecipe_RemoveInstructions() = runTest {
        // Given - Create a fallback recipe with instructions
        val fallbackRecipe = Recipe(
            name = "Recipe With Instructions",
            ingredients = emptyList(),
            instructions = listOf(
                Instruction("Preheat oven", 0),
                Instruction("Mix ingredients", 1),
                Instruction("Bake", 2)
            ),
            originalUrl = "https://example.com/recipe",
            isFallback = true
        )
        val recipeId = repository.insertRecipe(fallbackRecipe)

        // When - Remove one instruction
        val updatedRecipe = fallbackRecipe.copy(
            id = recipeId,
            instructions = listOf(
                Instruction("Mix ingredients", 0),
                Instruction("Bake", 1)
            )
        )
        repository.updateRecipe(updatedRecipe)

        // Then - Verify instruction was removed
        val recipes = repository.getAllRecipesOnce()
        val savedRecipe = recipes[0]
        assertEquals(2, savedRecipe.instructions.size)
        assertFalse(savedRecipe.instructions.any { it.text == "Preheat oven" })
    }

    @Test
    fun testEditFallbackRecipe_UpdatedAtTimestamp() = runTest {
        // Given - Create a fallback recipe
        val fallbackRecipe = Recipe(
            name = "Original Recipe",
            ingredients = emptyList(),
            instructions = emptyList(),
            originalUrl = "https://example.com/recipe",
            isFallback = true
        )
        val recipeId = repository.insertRecipe(fallbackRecipe)
        
        val originalRecipes = repository.getAllRecipesOnce()
        val originalTimestamp = originalRecipes[0].updatedAt

        // Wait a bit to ensure timestamp difference
        Thread.sleep(100)

        // When - Update the recipe
        val updatedRecipe = fallbackRecipe.copy(
            id = recipeId,
            name = "Updated Recipe"
        )
        repository.updateRecipe(updatedRecipe)

        // Then - Verify updatedAt timestamp was updated
        val recipes = repository.getAllRecipesOnce()
        val savedRecipe = recipes[0]
        assertTrue(savedRecipe.updatedAt > originalTimestamp)
    }

    @Test
    fun testEditNonFallbackRecipe_OnlyNameEditable() = runTest {
        // Given - Create a non-fallback recipe with structured data
        val regularRecipe = Recipe(
            name = "Original Regular Recipe",
            ingredients = listOf(
                Ingredient("Flour", 2.0, "cups", 0),
                Ingredient("Sugar", 1.0, "cup", 1)
            ),
            instructions = listOf(
                Instruction("Mix ingredients", 0),
                Instruction("Bake", 1)
            ),
            originalUrl = "https://example.com/recipe",
            isFallback = false
        )
        val recipeId = repository.insertRecipe(regularRecipe)

        // When - Update only the name (simulating UI restriction)
        val updatedRecipe = regularRecipe.copy(
            id = recipeId,
            name = "Updated Regular Recipe"
            // Note: ingredients and instructions remain unchanged
        )
        repository.updateRecipe(updatedRecipe)

        // Then - Verify only name was updated
        val recipes = repository.getAllRecipesOnce()
        val savedRecipe = recipes[0]
        assertEquals("Updated Regular Recipe", savedRecipe.name)
        assertEquals(2, savedRecipe.ingredients.size)
        assertEquals(2, savedRecipe.instructions.size)
        assertFalse(savedRecipe.isFallback)
    }

    @Test
    fun testCompleteEditFlow_ImportEditSave() = runTest {
        // Given - Create a fallback recipe (simulating import)
        val fallbackRecipe = Recipe(
            name = "example.com",
            ingredients = emptyList(),
            instructions = emptyList(),
            originalUrl = "https://example.com/recipe",
            isFallback = true
        )
        val recipeId = repository.insertRecipe(fallbackRecipe)

        // When - User edits the recipe to add full details
        val fullyEditedRecipe = fallbackRecipe.copy(
            id = recipeId,
            name = "Chocolate Chip Cookies",
            ingredients = listOf(
                Ingredient("Flour", 2.0, "cups", 0),
                Ingredient("Sugar", 1.0, "cup", 1),
                Ingredient("Butter", 0.5, "cup", 2),
                Ingredient("Eggs", 2.0, "whole", 3),
                Ingredient("Chocolate Chips", 1.0, "cup", 4)
            ),
            instructions = listOf(
                Instruction("Preheat oven to 350F", 0),
                Instruction("Cream butter and sugar", 1),
                Instruction("Add eggs and mix", 2),
                Instruction("Add flour and chocolate chips", 3),
                Instruction("Bake for 12 minutes", 4)
            ),
            yield = "24 cookies"
        )
        repository.updateRecipe(fullyEditedRecipe)

        // Then - Verify complete recipe is saved
        val recipes = repository.getAllRecipesOnce()
        assertEquals(1, recipes.size)
        
        val savedRecipe = recipes[0]
        assertEquals("Chocolate Chip Cookies", savedRecipe.name)
        assertEquals(5, savedRecipe.ingredients.size)
        assertEquals(5, savedRecipe.instructions.size)
        assertEquals("24 cookies", savedRecipe.yield)
        assertTrue(savedRecipe.isFallback)
        assertEquals("https://example.com/recipe", savedRecipe.originalUrl)
    }
}
