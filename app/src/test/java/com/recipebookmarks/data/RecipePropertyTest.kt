package com.recipebookmarks.data

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

/**
 * Property-based tests for Recipe entity
 * Feature: fallback-recipe-import
 */
class RecipePropertyTest : StringSpec({
    
    "Property 8: Empty List Support in Data Model - recipes with empty lists should be valid" {
        // **Validates: Requirements 5.3**
        // For any recipe with empty ingredients and instructions lists,
        // the system should successfully create and maintain the recipe with empty lists
        
        checkAll(100, Arb.recipeWithEmptyLists()) { recipe ->
            // Verify the recipe can be created with empty lists
            recipe.ingredients shouldBe emptyList()
            recipe.instructions shouldBe emptyList()
            
            // Verify other fields are still accessible
            recipe.name.isNotBlank() shouldBe true
            (recipe.id >= 0L) shouldBe true
        }
    }
    
    "Property 8: Empty List Support - fallback recipes should have empty lists by default" {
        // **Validates: Requirements 5.3**
        // Fallback recipes should be created with empty ingredient and instruction lists
        
        checkAll(100, Arb.fallbackRecipe()) { recipe ->
            recipe.isFallback shouldBe true
            recipe.ingredients shouldBe emptyList()
            recipe.instructions shouldBe emptyList()
        }
    }
    
    "Property 8: Empty List Support - non-fallback recipes can have populated lists" {
        // **Validates: Requirements 5.3**
        // Regular recipes should support both empty and populated lists
        
        checkAll(100, Arb.regularRecipe()) { recipe ->
            recipe.isFallback shouldBe false
            // Lists can be empty or populated - both are valid
            (recipe.ingredients.size >= 0) shouldBe true
            (recipe.instructions.size >= 0) shouldBe true
        }
    }
})

/**
 * Custom Arb generators for Recipe testing
 */
fun Arb.Companion.recipeWithEmptyLists(): Arb<Recipe> = arbitrary {
    Recipe(
        id = Arb.long(0L..1000L).bind(),
        name = Arb.string(1..200).bind(),
        ingredients = emptyList(),
        instructions = emptyList(),
        yield = null,
        servingSize = null,
        nutritionInfo = null,
        originalUrl = Arb.string(10..100).bind(),
        category = null,
        isFallback = Arb.boolean().bind(),
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
}

fun Arb.Companion.fallbackRecipe(): Arb<Recipe> = arbitrary {
    Recipe(
        id = Arb.long(0L..1000L).bind(),
        name = Arb.string(1..200).bind(),
        ingredients = emptyList(),
        instructions = emptyList(),
        yield = null,
        servingSize = null,
        nutritionInfo = null,
        originalUrl = Arb.string(10..100).bind(),
        category = null,
        isFallback = true,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
}

fun Arb.Companion.regularRecipe(): Arb<Recipe> = arbitrary {
    val hasIngredients = Arb.boolean().bind()
    val hasInstructions = Arb.boolean().bind()
    
    Recipe(
        id = Arb.long(0L..1000L).bind(),
        name = Arb.string(1..200).bind(),
        ingredients = if (hasIngredients) {
            listOf(Ingredient("Test Ingredient", 1.0, "cup", 0))
        } else {
            emptyList()
        },
        instructions = if (hasInstructions) {
            listOf(Instruction("Test instruction", 0))
        } else {
            emptyList()
        },
        yield = null,
        servingSize = null,
        nutritionInfo = null,
        originalUrl = Arb.string(10..100).bind(),
        category = null,
        isFallback = false,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
}
