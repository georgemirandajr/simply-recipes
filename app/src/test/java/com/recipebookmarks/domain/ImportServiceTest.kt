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

    @Test
    fun `handleSharedUrls tracks fallback recipes`() = runTest {
        // Given
        val url1 = "https://example.com/recipe1"
        val url2 = "https://example.com/recipe2"
        val html1 = "<html>Recipe 1</html>"
        val html2 = "<html>Recipe 2</html>"
        
        // Regular recipe with structured data
        val regularRecipe = Recipe(
            name = "Regular Recipe",
            ingredients = listOf(Ingredient("Flour", 2.0, "cups", 0)),
            instructions = listOf(Instruction("Mix", 0)),
            yield = null,
            servingSize = null,
            nutritionInfo = null,
            originalUrl = null,
            category = null,
            isFallback = false
        )
        
        // Fallback recipe without structured data
        val fallbackRecipe = Recipe(
            name = "Fallback Recipe",
            ingredients = emptyList(),
            instructions = emptyList(),
            yield = null,
            servingSize = null,
            nutritionInfo = null,
            originalUrl = null,
            category = null,
            isFallback = true
        )

        coEvery { networkClient.fetchHtml(url1) } returns NetworkResult.Success(html1)
        coEvery { networkClient.fetchHtml(url2) } returns NetworkResult.Success(html2)
        coEvery { recipeParser.parseRecipe(html1, url1) } returns ParseResult.Success(regularRecipe)
        coEvery { recipeParser.parseRecipe(html2, url2) } returns ParseResult.Success(fallbackRecipe)
        coEvery { recipeRepository.insertRecipe(any()) } returns 1L andThen 2L

        // When
        val summary = importService.handleSharedUrls(listOf(url1, url2))

        // Then
        assertEquals(2, summary.successCount)
        assertEquals(0, summary.failureCount)
        assertEquals(1, summary.fallbackCount)
    }

    @Test
    fun `handleSharedUrls tracks multiple fallback recipes`() = runTest {
        // Given
        val url1 = "https://example.com/recipe1"
        val url2 = "https://example.com/recipe2"
        val url3 = "https://example.com/recipe3"
        val html1 = "<html>Recipe 1</html>"
        val html2 = "<html>Recipe 2</html>"
        val html3 = "<html>Recipe 3</html>"
        
        // All fallback recipes
        val fallbackRecipe1 = Recipe(
            name = "Fallback Recipe 1",
            ingredients = emptyList(),
            instructions = emptyList(),
            yield = null,
            servingSize = null,
            nutritionInfo = null,
            originalUrl = null,
            category = null,
            isFallback = true
        )
        
        val fallbackRecipe2 = Recipe(
            name = "Fallback Recipe 2",
            ingredients = emptyList(),
            instructions = emptyList(),
            yield = null,
            servingSize = null,
            nutritionInfo = null,
            originalUrl = null,
            category = null,
            isFallback = true
        )
        
        val fallbackRecipe3 = Recipe(
            name = "Fallback Recipe 3",
            ingredients = emptyList(),
            instructions = emptyList(),
            yield = null,
            servingSize = null,
            nutritionInfo = null,
            originalUrl = null,
            category = null,
            isFallback = true
        )

        coEvery { networkClient.fetchHtml(url1) } returns NetworkResult.Success(html1)
        coEvery { networkClient.fetchHtml(url2) } returns NetworkResult.Success(html2)
        coEvery { networkClient.fetchHtml(url3) } returns NetworkResult.Success(html3)
        coEvery { recipeParser.parseRecipe(html1, url1) } returns ParseResult.Success(fallbackRecipe1)
        coEvery { recipeParser.parseRecipe(html2, url2) } returns ParseResult.Success(fallbackRecipe2)
        coEvery { recipeParser.parseRecipe(html3, url3) } returns ParseResult.Success(fallbackRecipe3)
        coEvery { recipeRepository.insertRecipe(any()) } returns 1L andThen 2L andThen 3L

        // When
        val summary = importService.handleSharedUrls(listOf(url1, url2, url3))

        // Then
        assertEquals(3, summary.successCount)
        assertEquals(0, summary.failureCount)
        assertEquals(3, summary.fallbackCount)
    }

    @Test
    fun `handleSharedUrls handles mixed results with success, fallback, and failure`() = runTest {
        // Given
        val url1 = "https://example.com/recipe1"  // Regular success
        val url2 = "https://example.com/recipe2"  // Fallback
        val url3 = "https://example.com/recipe3"  // Network failure
        val url4 = "https://example.com/recipe4"  // Fallback
        val url5 = "https://example.com/recipe5"  // Regular success
        
        val html1 = "<html>Recipe 1</html>"
        val html2 = "<html>Recipe 2</html>"
        val html4 = "<html>Recipe 4</html>"
        val html5 = "<html>Recipe 5</html>"
        
        // Regular recipe with structured data
        val regularRecipe1 = Recipe(
            name = "Regular Recipe 1",
            ingredients = listOf(Ingredient("Flour", 2.0, "cups", 0)),
            instructions = listOf(Instruction("Mix", 0)),
            yield = null,
            servingSize = null,
            nutritionInfo = null,
            originalUrl = null,
            category = null,
            isFallback = false
        )
        
        // Fallback recipe
        val fallbackRecipe2 = Recipe(
            name = "Fallback Recipe 2",
            ingredients = emptyList(),
            instructions = emptyList(),
            yield = null,
            servingSize = null,
            nutritionInfo = null,
            originalUrl = null,
            category = null,
            isFallback = true
        )
        
        // Fallback recipe
        val fallbackRecipe4 = Recipe(
            name = "Fallback Recipe 4",
            ingredients = emptyList(),
            instructions = emptyList(),
            yield = null,
            servingSize = null,
            nutritionInfo = null,
            originalUrl = null,
            category = null,
            isFallback = true
        )
        
        // Regular recipe with structured data
        val regularRecipe5 = Recipe(
            name = "Regular Recipe 5",
            ingredients = listOf(Ingredient("Sugar", 1.0, "cup", 0)),
            instructions = listOf(Instruction("Stir", 0)),
            yield = null,
            servingSize = null,
            nutritionInfo = null,
            originalUrl = null,
            category = null,
            isFallback = false
        )

        coEvery { networkClient.fetchHtml(url1) } returns NetworkResult.Success(html1)
        coEvery { networkClient.fetchHtml(url2) } returns NetworkResult.Success(html2)
        coEvery { networkClient.fetchHtml(url3) } returns NetworkResult.Failure(NetworkError.URL_INACCESSIBLE)
        coEvery { networkClient.fetchHtml(url4) } returns NetworkResult.Success(html4)
        coEvery { networkClient.fetchHtml(url5) } returns NetworkResult.Success(html5)
        
        coEvery { recipeParser.parseRecipe(html1, url1) } returns ParseResult.Success(regularRecipe1)
        coEvery { recipeParser.parseRecipe(html2, url2) } returns ParseResult.Success(fallbackRecipe2)
        coEvery { recipeParser.parseRecipe(html4, url4) } returns ParseResult.Success(fallbackRecipe4)
        coEvery { recipeParser.parseRecipe(html5, url5) } returns ParseResult.Success(regularRecipe5)
        
        coEvery { recipeRepository.insertRecipe(any()) } returns 1L andThen 2L andThen 3L andThen 4L

        // When
        val summary = importService.handleSharedUrls(listOf(url1, url2, url3, url4, url5))

        // Then
        assertEquals(4, summary.successCount)  // 2 regular + 2 fallback
        assertEquals(1, summary.failureCount)  // 1 network failure
        assertEquals(2, summary.fallbackCount) // 2 fallback recipes
        assertEquals(1, summary.failures.size)
        assertEquals(url3, summary.failures[0].url)
        assertEquals(ImportError.URL_INACCESSIBLE, summary.failures[0].error)
    }

    @Test
    fun `handleSharedUrls with all success and no fallbacks`() = runTest {
        // Given
        val url1 = "https://example.com/recipe1"
        val url2 = "https://example.com/recipe2"
        val html1 = "<html>Recipe 1</html>"
        val html2 = "<html>Recipe 2</html>"
        
        // All regular recipes with structured data
        val regularRecipe1 = Recipe(
            name = "Regular Recipe 1",
            ingredients = listOf(Ingredient("Flour", 2.0, "cups", 0)),
            instructions = listOf(Instruction("Mix", 0)),
            yield = null,
            servingSize = null,
            nutritionInfo = null,
            originalUrl = null,
            category = null,
            isFallback = false
        )
        
        val regularRecipe2 = Recipe(
            name = "Regular Recipe 2",
            ingredients = listOf(Ingredient("Sugar", 1.0, "cup", 0)),
            instructions = listOf(Instruction("Stir", 0)),
            yield = null,
            servingSize = null,
            nutritionInfo = null,
            originalUrl = null,
            category = null,
            isFallback = false
        )

        coEvery { networkClient.fetchHtml(url1) } returns NetworkResult.Success(html1)
        coEvery { networkClient.fetchHtml(url2) } returns NetworkResult.Success(html2)
        coEvery { recipeParser.parseRecipe(html1, url1) } returns ParseResult.Success(regularRecipe1)
        coEvery { recipeParser.parseRecipe(html2, url2) } returns ParseResult.Success(regularRecipe2)
        coEvery { recipeRepository.insertRecipe(any()) } returns 1L andThen 2L

        // When
        val summary = importService.handleSharedUrls(listOf(url1, url2))

        // Then
        assertEquals(2, summary.successCount)
        assertEquals(0, summary.failureCount)
        assertEquals(0, summary.fallbackCount)  // No fallbacks
    }
}
