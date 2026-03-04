package com.recipebookmarks.domain

import com.recipebookmarks.data.Category
import com.recipebookmarks.data.Ingredient
import com.recipebookmarks.data.Instruction
import com.recipebookmarks.data.Recipe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ImportServiceTest {

    private lateinit var importService: ImportService
    private lateinit var networkClient: NetworkClient
    private lateinit var recipeParser: RecipeParser
    private lateinit var recipeRepository: RecipeRepository

    @Before
    fun setup() {
        networkClient = mockk()
        recipeParser = mockk()
        recipeRepository = mockk()
        
        importService = ImportService(networkClient, recipeParser, recipeRepository)
    }

    @Test
    fun `handleSharedUrls processes single URL successfully`() = runTest {
        // Given
        val url = "https://example.com/recipe"
        val html = "<html>Recipe content</html>"
        val recipe = Recipe(
            name = "Test Recipe",
            ingredients = listOf(Ingredient("Flour", 2.0, "cups", 0)),
            instructions = listOf(Instruction("Mix ingredients", 0)),
            yield = "4 servings",
            servingSize = "1 serving",
            nutritionInfo = null,
            originalUrl = null,
            category = Category.DINNER
        )
        val recipeId = 1L

        coEvery { networkClient.fetchHtml(url) } returns NetworkResult.Success(html)
        coEvery { recipeParser.parseRecipe(html, url) } returns ParseResult.Success(recipe)
        coEvery { recipeRepository.insertRecipe(any()) } returns recipeId

        // When
        val summary = importService.handleSharedUrls(listOf(url))

        // Then
        assertEquals(1, summary.successCount)
        assertEquals(0, summary.failureCount)
        assertEquals(0, summary.failures.size)
        
        coVerify { networkClient.fetchHtml(url) }
        coVerify { recipeParser.parseRecipe(html, url) }
        coVerify { recipeRepository.insertRecipe(match { it.originalUrl == url }) }
    }

    @Test
    fun `handleSharedUrls processes multiple URLs successfully`() = runTest {
        // Given
        val url1 = "https://example.com/recipe1"
        val url2 = "https://example.com/recipe2"
        val html1 = "<html>Recipe 1</html>"
        val html2 = "<html>Recipe 2</html>"
        val recipe1 = Recipe(
            name = "Recipe 1",
            ingredients = listOf(Ingredient("Flour", 2.0, "cups", 0)),
            instructions = listOf(Instruction("Mix", 0)),
            yield = null,
            servingSize = null,
            nutritionInfo = null,
            originalUrl = null,
            category = null
        )
        val recipe2 = Recipe(
            name = "Recipe 2",
            ingredients = listOf(Ingredient("Sugar", 1.0, "cup", 0)),
            instructions = listOf(Instruction("Stir", 0)),
            yield = null,
            servingSize = null,
            nutritionInfo = null,
            originalUrl = null,
            category = null
        )

        coEvery { networkClient.fetchHtml(url1) } returns NetworkResult.Success(html1)
        coEvery { networkClient.fetchHtml(url2) } returns NetworkResult.Success(html2)
        coEvery { recipeParser.parseRecipe(html1, url1) } returns ParseResult.Success(recipe1)
        coEvery { recipeParser.parseRecipe(html2, url2) } returns ParseResult.Success(recipe2)
        coEvery { recipeRepository.insertRecipe(any()) } returns 1L andThen 2L

        // When
        val summary = importService.handleSharedUrls(listOf(url1, url2))

        // Then
        assertEquals(2, summary.successCount)
        assertEquals(0, summary.failureCount)
        assertEquals(0, summary.failures.size)
    }

    @Test
    fun `handleSharedUrls handles network failure`() = runTest {
        // Given
        val url = "https://example.com/recipe"
        coEvery { networkClient.fetchHtml(url) } returns NetworkResult.Failure(NetworkError.URL_INACCESSIBLE)

        // When
        val summary = importService.handleSharedUrls(listOf(url))

        // Then
        assertEquals(0, summary.successCount)
        assertEquals(1, summary.failureCount)
        assertEquals(1, summary.failures.size)
        assertEquals(url, summary.failures[0].url)
        assertEquals(ImportError.URL_INACCESSIBLE, summary.failures[0].error)
    }

    @Test
    fun `handleSharedUrls handles parse failure`() = runTest {
        // Given
        val url = "https://example.com/recipe"
        val html = "<html>Invalid recipe</html>"
        
        coEvery { networkClient.fetchHtml(url) } returns NetworkResult.Success(html)
        coEvery { recipeParser.parseRecipe(html, url) } returns ParseResult.Failure(ParseError.NO_RECIPE_DATA)

        // When
        val summary = importService.handleSharedUrls(listOf(url))

        // Then
        assertEquals(0, summary.successCount)
        assertEquals(1, summary.failureCount)
        assertEquals(1, summary.failures.size)
        assertEquals(url, summary.failures[0].url)
        assertEquals(ImportError.PARSE_FAILED, summary.failures[0].error)
    }

    @Test
    fun `handleSharedUrls continues processing after failure`() = runTest {
        // Given
        val url1 = "https://example.com/recipe1"
        val url2 = "https://example.com/recipe2"
        val url3 = "https://example.com/recipe3"
        val html2 = "<html>Recipe 2</html>"
        val html3 = "<html>Recipe 3</html>"
        val recipe2 = Recipe(
            name = "Recipe 2",
            ingredients = listOf(Ingredient("Sugar", 1.0, "cup", 0)),
            instructions = listOf(Instruction("Stir", 0)),
            yield = null,
            servingSize = null,
            nutritionInfo = null,
            originalUrl = null,
            category = null
        )
        val recipe3 = Recipe(
            name = "Recipe 3",
            ingredients = listOf(Ingredient("Salt", 0.5, "tsp", 0)),
            instructions = listOf(Instruction("Add", 0)),
            yield = null,
            servingSize = null,
            nutritionInfo = null,
            originalUrl = null,
            category = null
        )

        coEvery { networkClient.fetchHtml(url1) } returns NetworkResult.Failure(NetworkError.URL_INACCESSIBLE)
        coEvery { networkClient.fetchHtml(url2) } returns NetworkResult.Success(html2)
        coEvery { networkClient.fetchHtml(url3) } returns NetworkResult.Success(html3)
        coEvery { recipeParser.parseRecipe(html2, url2) } returns ParseResult.Success(recipe2)
        coEvery { recipeParser.parseRecipe(html3, url3) } returns ParseResult.Success(recipe3)
        coEvery { recipeRepository.insertRecipe(any()) } returns 1L andThen 2L

        // When
        val summary = importService.handleSharedUrls(listOf(url1, url2, url3))

        // Then
        assertEquals(2, summary.successCount)
        assertEquals(1, summary.failureCount)
        assertEquals(1, summary.failures.size)
        assertEquals(url1, summary.failures[0].url)
        
        // Verify that url2 and url3 were still processed
        coVerify { networkClient.fetchHtml(url2) }
        coVerify { networkClient.fetchHtml(url3) }
    }

    @Test
    fun `handleSharedUrls stores original URL in recipe`() = runTest {
        // Given
        val url = "https://example.com/recipe"
        val html = "<html>Recipe content</html>"
        val recipe = Recipe(
            name = "Test Recipe",
            ingredients = listOf(Ingredient("Flour", 2.0, "cups", 0)),
            instructions = listOf(Instruction("Mix", 0)),
            yield = null,
            servingSize = null,
            nutritionInfo = null,
            originalUrl = null, // Parser doesn't set this
            category = null
        )

        coEvery { networkClient.fetchHtml(url) } returns NetworkResult.Success(html)
        coEvery { recipeParser.parseRecipe(html, url) } returns ParseResult.Success(recipe)
        coEvery { recipeRepository.insertRecipe(any()) } returns 1L

        // When
        importService.handleSharedUrls(listOf(url))

        // Then
        coVerify { 
            recipeRepository.insertRecipe(match { 
                it.originalUrl == url && it.name == "Test Recipe"
            }) 
        }
    }
}
