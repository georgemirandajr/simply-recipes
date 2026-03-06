package com.recipebookmarks.domain

import kotlinx.coroutines.runBlocking
import org.junit.Test

class DebugStructuredDataTest {
    
    @Test
    fun testMicrodataParsing() = runBlocking {
        val html = """<html><head><title>Chicken Tikka Masala</title></head><body><div itemscope itemtype="http://schema.org/Recipe"><h1 itemprop="name">Chicken Tikka Masala</h1><ul><li itemprop="recipeIngredient">1 kg beef</li><li itemprop="recipeIngredient">4 carrots</li></ul><ol><li itemprop="recipeInstructions">Preheat oven</li><li itemprop="recipeInstructions">Mix ingredients</li></ol><span itemprop="recipeYield">4 servings</span></div></body></html>"""
        
        val parser = RecipeParserImpl()
        val result = parser.parseRecipe(html, "https://example.com")
        
        println("Result type: ${result::class.simpleName}")
        when (result) {
            is ParseResult.Success -> {
                println("Recipe name: ${result.recipe.name}")
                println("Is fallback: ${result.recipe.isFallback}")
                println("Ingredients count: ${result.recipe.ingredients.size}")
                println("Instructions count: ${result.recipe.instructions.size}")
            }
            is ParseResult.Failure -> {
                println("Parse error: ${result.error}")
            }
        }
    }
    
    @Test
    fun testJsonLdParsing() = runBlocking {
        val html = """<html><head><title>Test Recipe</title><script type="application/ld+json">{"@context":"https://schema.org","@type":"Recipe","name":"Test Recipe","recipeIngredient":["1 cup flour","2 eggs"],"recipeInstructions":["Mix ingredients","Bake"],"recipeYield":"4 servings"}</script></head><body><h1>Test Recipe</h1></body></html>"""
        
        val parser = RecipeParserImpl()
        val result = parser.parseRecipe(html, "https://example.com")
        
        println("Result type: ${result::class.simpleName}")
        when (result) {
            is ParseResult.Success -> {
                println("Recipe name: ${result.recipe.name}")
                println("Is fallback: ${result.recipe.isFallback}")
                println("Ingredients count: ${result.recipe.ingredients.size}")
                println("Instructions count: ${result.recipe.instructions.size}")
            }
            is ParseResult.Failure -> {
                println("Parse error: ${result.error}")
            }
        }
    }
}
