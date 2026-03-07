package com.recipebookmarks.integration

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.recipebookmarks.data.Ingredient
import com.recipebookmarks.data.Instruction
import com.recipebookmarks.data.Recipe
import com.recipebookmarks.data.RecipeDatabase
import com.recipebookmarks.domain.ImportService
import com.recipebookmarks.domain.NetworkClientImpl
import com.recipebookmarks.domain.RecipeParserImpl
import com.recipebookmarks.domain.RecipeRepositoryImpl
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end integration tests for complete user flows.
 * Tests complete workflows from import to view to edit/delete.
 */
@RunWith(AndroidJUnit4::class)
class EndToEndFlowIntegrationTest {

    private lateinit var context: Context
    private lateinit var database: RecipeDatabase
    private lateinit var importService: ImportService
    private lateinit var repository: RecipeRepositoryImpl

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = RecipeDatabase.getDatabase(context, inMemory = true)
        
        val networkClient = NetworkClientImpl()
        val recipeParser = RecipeParserImpl()
        repository = RecipeRepositoryImpl(database.recipeDao())
        
        importService = ImportService(networkClient, recipeParser, repository)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testEndToEndFlow_ImportViewEditSave() = runTest {
        // Step 1: Import a recipe from URL
        val url = "https://example.com/recipe"
        
        // Create a fallback recipe directly (simulating import)
        val importedRecipe = Recipe(
            name = "example.com",
            ingredients = emptyList(),
            instructions = emptyList(),
            originalUrl = url,
            isFallback = true
        )
        val recipeId = repository.insertRecipe(importedRecipe)

        // Step 2: View the recipe (verify it exists)
        var recipes = repository.getAllRecipesOnce()
        assertEquals(1, recipes.size)
        val viewedRecipe = recipes[0]
        assertEquals(recipeId, viewedRecipe.id)
        assertEquals("example.com", viewedRecipe.name)
        assertTrue(viewedRecipe.isFallback)
        assertEquals(url, viewedRecipe.originalUrl)

        // Step 3: Edit the recipe (add ingredients and instructions)
        val editedRecipe = viewedRecipe.copy(
            name = "Chocolate Chip Cookies",
            ingredients = listOf(
                Ingredient("Flour", 2.0, "cups", 0),
                Ingredient("Sugar", 1.0, "cup", 1),
                Ingredient("Chocolate Chips", 1.0, "cup", 2)
            ),
            instructions = listOf(
                Instruction("Mix ingredients", 0),
                Instruction("Bake at 350F for 12 minutes", 1)
            ),
            yield = "24 cookies"
        )
        repository.updateRecipe(editedRecipe)

        // Step 4: View the edited recipe (verify changes persisted)
        recipes = repository.getAllRecipesOnce()
        assertEquals(1, recipes.size)
        val savedRecipe = recipes[0]
        assertEquals("Chocolate Chip Cookies", savedRecipe.name)
        assertEquals(3, savedRecipe.ingredients.size)
        assertEquals(2, savedRecipe.instructions.size)
        assertEquals("24 cookies", savedRecipe.yield)
        assertTrue(savedRecipe.isFallback)
        assertEquals(url, savedRecipe.originalUrl)
    }

    @Test
    fun testEndToEndFlow_ImportViewDelete() = runTest {
        // Step 1: Import a recipe from URL
        val url = "https://example.com/recipe"
        
        // Create a fallback recipe directly (simulating import)
        val importedRecipe = Recipe(
            name = "example.com",
            ingredients = emptyList(),
            instructions = emptyList(),
            originalUrl = url,
            isFallback = true
        )
        val recipeId = repository.insertRecipe(importedRecipe)

        // Step 2: View the recipe (verify it exists)
        var recipes = repository.getAllRecipesOnce()
        assertEquals(1, recipes.size)
        val viewedRecipe = recipes[0]
        assertEquals(recipeId, viewedRecipe.id)
        assertEquals("example.com", viewedRecipe.name)

        // Step 3: Delete the recipe from detail view
        repository.deleteRecipe(recipeId)

        // Step 4: Verify recipe is removed
        recipes = repository.getAllRecipesOnce()
        assertEquals(0, recipes.size)
    }

    @Test
    fun testEndToEndFlow_ImportDeleteFromList() = runTest {
        // Step 1: Import multiple recipes
        val url1 = "https://example.com/recipe1"
        val url2 = "https://example.com/recipe2"
        val url3 = "https://example.com/recipe3"
        
        val recipe1 = Recipe(
            name = "Recipe 1",
            ingredients = emptyList(),
            instructions = emptyList(),
            originalUrl = url1,
            isFallback = true
        )
        val recipe2 = Recipe(
            name = "Recipe 2",
            ingredients = listOf(Ingredient("Flour", 2.0, "cups", 0)),
            instructions = listOf(Instruction("Mix", 0)),
            originalUrl = url2,
            isFallback = false
        )
        val recipe3 = Recipe(
            name = "Recipe 3",
            ingredients = emptyList(),
            instructions = emptyList(),
            originalUrl = url3,
            isFallback = true
        )

        val recipeId1 = repository.insertRecipe(recipe1)
        val recipeId2 = repository.insertRecipe(recipe2)
        val recipeId3 = repository.insertRecipe(recipe3)

        // Step 2: View recipe list (verify all recipes exist)
        var recipes = repository.getAllRecipesOnce()
        assertEquals(3, recipes.size)

        // Step 3: Delete recipe 2 from list
        repository.deleteRecipe(recipeId2)

        // Step 4: Verify recipe 2 is removed from list
        recipes = repository.getAllRecipesOnce()
        assertEquals(2, recipes.size)
        assertTrue(recipes.any { it.id == recipeId1 })
        assertFalse(recipes.any { it.id == recipeId2 })
        assertTrue(recipes.any { it.id == recipeId3 })
    }

    @Test
    fun testEndToEndFlow_ImportMultipleEditOneDeleteAnother() = runTest {
        // Step 1: Import multiple recipes
        val recipe1 = Recipe(
            name = "Fallback Recipe 1",
            ingredients = emptyList(),
            instructions = emptyList(),
            originalUrl = "https://example.com/recipe1",
            isFallback = true
        )
        val recipe2 = Recipe(
            name = "Fallback Recipe 2",
            ingredients = emptyList(),
            instructions = emptyList(),
            originalUrl = "https://example.com/recipe2",
            isFallback = true
        )
        val recipe3 = Recipe(
            name = "Regular Recipe 3",
            ingredients = listOf(Ingredient("Flour", 2.0, "cups", 0)),
            instructions = listOf(Instruction("Mix", 0)),
            originalUrl = "https://example.com/recipe3",
            isFallback = false
        )

        val recipeId1 = repository.insertRecipe(recipe1)
        val recipeId2 = repository.insertRecipe(recipe2)
        val recipeId3 = repository.insertRecipe(recipe3)

        // Step 2: View recipe list
        var recipes = repository.getAllRecipesOnce()
        assertEquals(3, recipes.size)

        // Step 3: Edit recipe 1 (add ingredients and instructions)
        val editedRecipe1 = recipe1.copy(
            id = recipeId1,
            name = "Edited Fallback Recipe 1",
            ingredients = listOf(
                Ingredient("Sugar", 1.0, "cup", 0),
                Ingredient("Butter", 0.5, "cup", 1)
            ),
            instructions = listOf(
                Instruction("Cream butter and sugar", 0),
                Instruction("Bake", 1)
            )
        )
        repository.updateRecipe(editedRecipe1)

        // Step 4: Delete recipe 2
        repository.deleteRecipe(recipeId2)

        // Step 5: Verify final state
        recipes = repository.getAllRecipesOnce()
        assertEquals(2, recipes.size)
        
        // Verify recipe 1 was edited
        val savedRecipe1 = recipes.find { it.id == recipeId1 }
        assertNotNull(savedRecipe1)
        assertEquals("Edited Fallback Recipe 1", savedRecipe1!!.name)
        assertEquals(2, savedRecipe1.ingredients.size)
        assertEquals(2, savedRecipe1.instructions.size)
        
        // Verify recipe 2 was deleted
        assertFalse(recipes.any { it.id == recipeId2 })
        
        // Verify recipe 3 remains unchanged
        val savedRecipe3 = recipes.find { it.id == recipeId3 }
        assertNotNull(savedRecipe3)
        assertEquals("Regular Recipe 3", savedRecipe3!!.name)
    }

    @Test
    fun testEndToEndFlow_ImportEditMultipleTimesSave() = runTest {
        // Step 1: Import a fallback recipe
        val recipe = Recipe(
            name = "example.com",
            ingredients = emptyList(),
            instructions = emptyList(),
            originalUrl = "https://example.com/recipe",
            isFallback = true
        )
        val recipeId = repository.insertRecipe(recipe)

        // Step 2: First edit - add name and ingredients
        val firstEdit = recipe.copy(
            id = recipeId,
            name = "Chocolate Chip Cookies",
            ingredients = listOf(
                Ingredient("Flour", 2.0, "cups", 0),
                Ingredient("Sugar", 1.0, "cup", 1)
            )
        )
        repository.updateRecipe(firstEdit)

        // Step 3: Second edit - add more ingredients and instructions
        val secondEdit = firstEdit.copy(
            ingredients = listOf(
                Ingredient("Flour", 2.0, "cups", 0),
                Ingredient("Sugar", 1.0, "cup", 1),
                Ingredient("Butter", 0.5, "cup", 2),
                Ingredient("Eggs", 2.0, "whole", 3)
            ),
            instructions = listOf(
                Instruction("Mix dry ingredients", 0),
                Instruction("Add wet ingredients", 1)
            )
        )
        repository.updateRecipe(secondEdit)

        // Step 4: Third edit - add yield and more instructions
        val thirdEdit = secondEdit.copy(
            instructions = listOf(
                Instruction("Mix dry ingredients", 0),
                Instruction("Add wet ingredients", 1),
                Instruction("Bake at 350F for 12 minutes", 2)
            ),
            yield = "24 cookies"
        )
        repository.updateRecipe(thirdEdit)

        // Step 5: Verify final state
        val recipes = repository.getAllRecipesOnce()
        assertEquals(1, recipes.size)
        
        val finalRecipe = recipes[0]
        assertEquals("Chocolate Chip Cookies", finalRecipe.name)
        assertEquals(4, finalRecipe.ingredients.size)
        assertEquals(3, finalRecipe.instructions.size)
        assertEquals("24 cookies", finalRecipe.yield)
        assertTrue(finalRecipe.isFallback)
    }

    @Test
    fun testEndToEndFlow_ImportViewEditCancelDelete() = runTest {
        // Step 1: Import a recipe
        val recipe = Recipe(
            name = "example.com",
            ingredients = emptyList(),
            instructions = emptyList(),
            originalUrl = "https://example.com/recipe",
            isFallback = true
        )
        val recipeId = repository.insertRecipe(recipe)

        // Step 2: View the recipe
        var recipes = repository.getAllRecipesOnce()
        assertEquals(1, recipes.size)

        // Step 3: Start editing but cancel (don't save)
        // Simulated by not calling updateRecipe

        // Step 4: Verify recipe remains unchanged
        recipes = repository.getAllRecipesOnce()
        assertEquals(1, recipes.size)
        assertEquals("example.com", recipes[0].name)
        assertTrue(recipes[0].ingredients.isEmpty())
        assertTrue(recipes[0].instructions.isEmpty())

        // Step 5: Delete the recipe
        repository.deleteRecipe(recipeId)

        // Step 6: Verify recipe is deleted
        recipes = repository.getAllRecipesOnce()
        assertEquals(0, recipes.size)
    }

    @Test
    fun testEndToEndFlow_CompleteUserJourney() = runTest {
        // Simulate a complete user journey:
        // 1. User shares multiple URLs
        // 2. Some have structured data, some don't (fallback)
        // 3. User views the list
        // 4. User edits a fallback recipe to add details
        // 5. User deletes a recipe they don't want
        // 6. User views the final list

        // Step 1: Import multiple recipes
        val regularRecipe = Recipe(
            name = "Regular Recipe with Structured Data",
            ingredients = listOf(
                Ingredient("Flour", 2.0, "cups", 0),
                Ingredient("Sugar", 1.0, "cup", 1)
            ),
            instructions = listOf(
                Instruction("Mix ingredients", 0),
                Instruction("Bake", 1)
            ),
            originalUrl = "https://example.com/regular",
            isFallback = false
        )
        
        val fallbackRecipe1 = Recipe(
            name = "example.com",
            ingredients = emptyList(),
            instructions = emptyList(),
            originalUrl = "https://example.com/fallback1",
            isFallback = true
        )
        
        val fallbackRecipe2 = Recipe(
            name = "anothersite.com",
            ingredients = emptyList(),
            instructions = emptyList(),
            originalUrl = "https://anothersite.com/fallback2",
            isFallback = true
        )

        val regularId = repository.insertRecipe(regularRecipe)
        val fallback1Id = repository.insertRecipe(fallbackRecipe1)
        val fallback2Id = repository.insertRecipe(fallbackRecipe2)

        // Step 2: View the list (verify all imported)
        var recipes = repository.getAllRecipesOnce()
        assertEquals(3, recipes.size)
        assertEquals(1, recipes.count { !it.isFallback })
        assertEquals(2, recipes.count { it.isFallback })

        // Step 3: Edit fallback recipe 1 to add full details
        val editedFallback1 = fallbackRecipe1.copy(
            id = fallback1Id,
            name = "Homemade Pizza",
            ingredients = listOf(
                Ingredient("Pizza Dough", 1.0, "ball", 0),
                Ingredient("Tomato Sauce", 0.5, "cup", 1),
                Ingredient("Mozzarella", 2.0, "cups", 2)
            ),
            instructions = listOf(
                Instruction("Roll out dough", 0),
                Instruction("Add sauce and toppings", 1),
                Instruction("Bake at 450F for 15 minutes", 2)
            ),
            yield = "1 large pizza"
        )
        repository.updateRecipe(editedFallback1)

        // Step 4: Delete fallback recipe 2 (user doesn't want it)
        repository.deleteRecipe(fallback2Id)

        // Step 5: View final list
        recipes = repository.getAllRecipesOnce()
        assertEquals(2, recipes.size)
        
        // Verify regular recipe is unchanged
        val savedRegular = recipes.find { it.id == regularId }
        assertNotNull(savedRegular)
        assertEquals("Regular Recipe with Structured Data", savedRegular!!.name)
        assertFalse(savedRegular.isFallback)
        
        // Verify fallback recipe 1 was edited
        val savedFallback1 = recipes.find { it.id == fallback1Id }
        assertNotNull(savedFallback1)
        assertEquals("Homemade Pizza", savedFallback1!!.name)
        assertEquals(3, savedFallback1.ingredients.size)
        assertEquals(3, savedFallback1.instructions.size)
        assertTrue(savedFallback1.isFallback)
        
        // Verify fallback recipe 2 was deleted
        assertFalse(recipes.any { it.id == fallback2Id })
    }
}
