package com.recipebookmarks.integration

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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
 * Integration tests for the import flow with real URLs.
 * Tests Requirements: 1.1, 1.2, 1.3, 3.1, 3.2
 */
@RunWith(AndroidJUnit4::class)
class ImportFlowIntegrationTest {

    private lateinit var context: Context
    private lateinit var database: RecipeDatabase
    private lateinit var importService: ImportService
    private lateinit var repository: RecipeRepositoryImpl

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        
        // Create in-memory database for testing
        database = RecipeDatabase.getDatabase(context, inMemory = true)
        
        // Create real instances (not mocks) for integration testing
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
    fun testImportFromJoyFoodSunshine() = runTest {
        // Given
        val url = "https://joyfoodsunshine.com/the-most-amazing-chocolate-chip-cookies/"
        
        // When
        val summary = importService.handleSharedUrls(listOf(url))
        
        // Then
        assertEquals(1, summary.successCount)
        assertEquals(0, summary.failureCount)
        
        // Verify recipe was created
        val recipes = repository.getAllRecipesOnce()
        assertEquals(1, recipes.size)
        
        val recipe = recipes[0]
        assertNotNull(recipe.name)
        assertTrue(recipe.name.isNotBlank())
        assertEquals(url, recipe.originalUrl)
        
        // Verify it's either a fallback or has structured data
        if (recipe.isFallback) {
            assertEquals(1, summary.fallbackCount)
            assertTrue(recipe.ingredients.isEmpty())
            assertTrue(recipe.instructions.isEmpty())
        } else {
            assertEquals(0, summary.fallbackCount)
            assertFalse(recipe.ingredients.isEmpty() && recipe.instructions.isEmpty())
        }
    }

    @Test
    fun testImportFromSeriousEats() = runTest {
        // Given
        val url = "https://www.seriouseats.com/food-lab-best-chocolate-chip-cookie-step-by-step-slideshow"
        
        // When
        val summary = importService.handleSharedUrls(listOf(url))
        
        // Then
        assertEquals(1, summary.successCount)
        assertEquals(0, summary.failureCount)
        
        // Verify recipe was created
        val recipes = repository.getAllRecipesOnce()
        assertEquals(1, recipes.size)
        
        val recipe = recipes[0]
        assertNotNull(recipe.name)
        assertTrue(recipe.name.isNotBlank())
        assertEquals(url, recipe.originalUrl)
    }

    @Test
    fun testImportFromBowlOfDelicious() = runTest {
        // Given
        val url = "https://www.bowlofdelicious.com/easy-chicken-tikka-masala/"
        
        // When
        val summary = importService.handleSharedUrls(listOf(url))
        
        // Then
        assertEquals(1, summary.successCount)
        assertEquals(0, summary.failureCount)
        
        // Verify recipe was created
        val recipes = repository.getAllRecipesOnce()
        assertEquals(1, recipes.size)
        
        val recipe = recipes[0]
        assertNotNull(recipe.name)
        assertTrue(recipe.name.isNotBlank())
        assertEquals(url, recipe.originalUrl)
    }

    @Test
    fun testImportFromTheFreshCooky() = runTest {
        // Given
        val url = "https://www.thefreshcooky.com/healthy-mongolian-beef/"
        
        // When
        val summary = importService.handleSharedUrls(listOf(url))
        
        // Then
        assertEquals(1, summary.successCount)
        assertEquals(0, summary.failureCount)
        
        // Verify recipe was created
        val recipes = repository.getAllRecipesOnce()
        assertEquals(1, recipes.size)
        
        val recipe = recipes[0]
        assertNotNull(recipe.name)
        assertTrue(recipe.name.isNotBlank())
        assertEquals(url, recipe.originalUrl)
    }

    @Test
    fun testImportFromEpicurious() = runTest {
        // Given
        val url = "https://www.epicurious.com/recipes/food/views/ba-syn-tandoori-style-roasted-indian-cauliflower"
        
        // When
        val summary = importService.handleSharedUrls(listOf(url))
        
        // Then
        assertEquals(1, summary.successCount)
        assertEquals(0, summary.failureCount)
        
        // Verify recipe was created
        val recipes = repository.getAllRecipesOnce()
        assertEquals(1, recipes.size)
        
        val recipe = recipes[0]
        assertNotNull(recipe.name)
        assertTrue(recipe.name.isNotBlank())
        assertEquals(url, recipe.originalUrl)
    }

    @Test
    fun testImportFromBonAppetit() = runTest {
        // Given
        val url = "https://www.bonappetit.com/recipe/smashed-broccoli-pasta"
        
        // When
        val summary = importService.handleSharedUrls(listOf(url))
        
        // Then
        assertEquals(1, summary.successCount)
        assertEquals(0, summary.failureCount)
        
        // Verify recipe was created
        val recipes = repository.getAllRecipesOnce()
        assertEquals(1, recipes.size)
        
        val recipe = recipes[0]
        assertNotNull(recipe.name)
        assertTrue(recipe.name.isNotBlank())
        assertEquals(url, recipe.originalUrl)
    }

    @Test
    fun testImportFromFoodNetwork() = runTest {
        // Given
        val url = "https://www.foodnetwork.com/recipes/food-network-kitchen/fluffy-japanese-pancakes-3686850"
        
        // When
        val summary = importService.handleSharedUrls(listOf(url))
        
        // Then
        assertEquals(1, summary.successCount)
        assertEquals(0, summary.failureCount)
        
        // Verify recipe was created
        val recipes = repository.getAllRecipesOnce()
        assertEquals(1, recipes.size)
        
        val recipe = recipes[0]
        assertNotNull(recipe.name)
        assertTrue(recipe.name.isNotBlank())
        assertEquals(url, recipe.originalUrl)
    }

    @Test
    fun testImportFromDelish() = runTest {
        // Given
        val url = "https://www.delish.com/cooking/recipe-ideas/a60343196/cinnamon-pretzel-bites-recipe/"
        
        // When
        val summary = importService.handleSharedUrls(listOf(url))
        
        // Then
        assertEquals(1, summary.successCount)
        assertEquals(0, summary.failureCount)
        
        // Verify recipe was created
        val recipes = repository.getAllRecipesOnce()
        assertEquals(1, recipes.size)
        
        val recipe = recipes[0]
        assertNotNull(recipe.name)
        assertTrue(recipe.name.isNotBlank())
        assertEquals(url, recipe.originalUrl)
    }

    @Test
    fun testAllTestUrlsInBatch() = runTest {
        // Given
        val urls = listOf(
            "https://joyfoodsunshine.com/the-most-amazing-chocolate-chip-cookies/",
            "https://www.seriouseats.com/food-lab-best-chocolate-chip-cookie-step-by-step-slideshow",
            "https://www.bowlofdelicious.com/easy-chicken-tikka-masala/",
            "https://www.thefreshcooky.com/healthy-mongolian-beef/",
            "https://www.epicurious.com/recipes/food/views/ba-syn-tandoori-style-roasted-indian-cauliflower",
            "https://www.bonappetit.com/recipe/smashed-broccoli-pasta",
            "https://www.foodnetwork.com/recipes/food-network-kitchen/fluffy-japanese-pancakes-3686850",
            "https://www.delish.com/cooking/recipe-ideas/a60343196/cinnamon-pretzel-bites-recipe/"
        )
        
        // When
        val summary = importService.handleSharedUrls(urls)
        
        // Then
        assertEquals(8, summary.successCount)
        assertTrue(summary.failureCount <= 1) // Allow for occasional network issues
        
        // Verify all recipes were created
        val recipes = repository.getAllRecipesOnce()
        assertEquals(summary.successCount, recipes.size)
        
        // Verify all recipes have names and URLs
        recipes.forEach { recipe ->
            assertNotNull(recipe.name)
            assertTrue(recipe.name.isNotBlank())
            assertNotNull(recipe.originalUrl)
            assertTrue(urls.contains(recipe.originalUrl))
        }
        
        // Verify fallback recipes have empty lists
        recipes.filter { it.isFallback }.forEach { recipe ->
            assertTrue(recipe.ingredients.isEmpty())
            assertTrue(recipe.instructions.isEmpty())
        }
    }
}
