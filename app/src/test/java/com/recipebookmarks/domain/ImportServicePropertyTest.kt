package com.recipebookmarks.domain

import com.recipebookmarks.data.Recipe
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf

/**
 * Property-based tests for ImportService
 * Feature: fallback-recipe-import
 */
class ImportServicePropertyTest : StringSpec({
    
    "Property 5: Fallback Recipe Persistence - fallback recipes persist correctly through import flow" {
        // **Validates: Requirements 1.7**
        // For any fallback recipe created by the import service, when saved to the repository,
        // querying the database by the returned recipe ID should retrieve a recipe with matching data
        
        checkAll(100, Arb.urlAndFallbackHtml()) { (url, html) ->
            // Setup mocks
            val networkClient = mockk<NetworkClient>()
            val recipeParser = RecipeParserImpl()
            val recipeRepository = mockk<RecipeRepository>()
            val importService = ImportService(networkClient, recipeParser, recipeRepository)
            
            // Capture the recipe that gets inserted
            val recipeSlot = slot<Recipe>()
            val recipeId = 42L
            
            // Mock network fetch to return HTML without structured data
            coEvery { networkClient.fetchHtml(url) } returns NetworkResult.Success(html)
            
            // Mock repository insert to capture the recipe and return an ID
            coEvery { recipeRepository.insertRecipe(capture(recipeSlot)) } returns recipeId
            
            // Mock repository getRecipeById to return the captured recipe
            coEvery { recipeRepository.getRecipeById(recipeId) } answers {
                flowOf(recipeSlot.captured.copy(id = recipeId))
            }
            
            // Execute import
            val summary = importService.handleSharedUrls(listOf(url))
            
            // Verify import was successful
            summary.successCount shouldBe 1
            summary.failureCount shouldBe 0
            summary.fallbackCount shouldBe 1
            
            // Verify the recipe was inserted
            coVerify { recipeRepository.insertRecipe(any()) }
            
            // Verify the inserted recipe is a fallback recipe
            val insertedRecipe = recipeSlot.captured
            insertedRecipe.isFallback shouldBe true
            insertedRecipe.originalUrl shouldBe url
            insertedRecipe.ingredients shouldBe emptyList()
            insertedRecipe.instructions shouldBe emptyList()
            insertedRecipe.name.isNotBlank() shouldBe true
            
            // Simulate querying the database by the returned recipe ID
            val retrievedRecipeFlow = recipeRepository.getRecipeById(recipeId)
            var retrievedRecipe: Recipe? = null
            retrievedRecipeFlow.collect { recipe ->
                retrievedRecipe = recipe
            }
            
            // Verify the retrieved recipe matches the inserted recipe
            retrievedRecipe shouldBe insertedRecipe.copy(id = recipeId)
            retrievedRecipe?.isFallback shouldBe true
            retrievedRecipe?.originalUrl shouldBe url
            retrievedRecipe?.ingredients shouldBe emptyList()
            retrievedRecipe?.instructions shouldBe emptyList()
            retrievedRecipe?.name shouldBe insertedRecipe.name
        }
    }
})

/**
 * Custom Arb generators for ImportService testing
 */

/**
 * Generates a pair of (URL, HTML) where the HTML lacks structured recipe data
 * This ensures the parser will create a fallback recipe
 */
fun Arb.Companion.urlAndFallbackHtml(): Arb<Pair<String, String>> = arbitrary {
    val domains = listOf(
        "example.com",
        "recipes.org",
        "cooking.net",
        "food-blog.com",
        "my-recipes.co.uk",
        "delicious.io",
        "tasty-food.app"
    )
    
    val paths = listOf(
        "/recipe",
        "/recipe/123",
        "/category/desserts/chocolate-cake",
        "/2024/01/best-pasta-recipe",
        "/cooking/easy-dinner",
        ""
    )
    
    val domain = domains.random()
    val path = paths.random()
    val url = "https://$domain$path"
    
    // Generate HTML without structured data but with a title
    val titleOptions = listOf(
        "Delicious Chocolate Cake Recipe",
        "How to Make Perfect Pasta",
        "Best Homemade Pizza",
        "Easy Chicken Curry",
        "Vegan Brownies",
        "Amazing Recipe from $domain"
    )
    
    val contentOptions = listOf(
        "<p>This is a great recipe for chocolate cake.</p><p>Mix flour, sugar, and eggs.</p>",
        "<div class='recipe'><h1>Recipe Title</h1><p>Some ingredients here</p></div>",
        "<article><h2>Cooking Instructions</h2><ol><li>Step 1</li><li>Step 2</li></ol></article>",
        "<section><p>A delicious meal that everyone will love.</p></section>",
        "<div><span>Ingredients: flour, sugar, eggs</span></div>"
    )
    
    val title = titleOptions.random()
    val content = contentOptions.random()
    val html = "<html><head><title>$title</title></head><body>$content</body></html>"
    
    url to html
}
