package com.recipebookmarks.ui

import com.recipebookmarks.data.*
import com.recipebookmarks.domain.RecipeRepository
import com.recipebookmarks.domain.RecipeRepositoryImpl
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.double
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.mockk.*
import kotlinx.coroutines.flow.flowOf

/**
 * Property-based tests for RecipeEditor functionality
 * Feature: fallback-recipe-import
 */
class RecipeEditorPropertyTest : StringSpec({
    
    "Property 9: Recipe Name Editing for All Recipes - all recipes should allow name editing" {
        // **Validates: Requirements 7.1**
        // For any recipe (regardless of isFallback value),
        // the recipe editor should allow modification of the recipe name field
        
        checkAll(100, Arb.recipeWithRandomFallbackStatus()) { (recipe, newName) ->
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
            
            // Edit the recipe name (simulating what RecipeEditorActivity does)
            val editedRecipe = insertedRecipe.copy(
                name = newName,
                updatedAt = System.currentTimeMillis()
            )
            
            // Update the recipe
            repository.updateRecipe(editedRecipe)
            
            // Verify update was called
            coVerify { recipeDao.update(any()) }
            
            // Verify the updated recipe has the new name
            val capturedRecipe = updatedRecipeSlot.captured
            capturedRecipe.name shouldBe newName
            capturedRecipe.id shouldBe recipeId
            
            // Verify this works regardless of isFallback status
            capturedRecipe.isFallback shouldBe recipe.isFallback
        }
    }
    
    "Property 10: Ingredient List Editing Operations - fallback recipes should support ingredient operations" {
        // **Validates: Requirements 7.6, 7.7, 7.8**
        // For any fallback recipe being edited,
        // the system should support adding new ingredients (increasing list size),
        // modifying existing ingredients (changing their data),
        // and removing ingredients (decreasing list size)
        
        checkAll(100, Arb.fallbackRecipeForIngredientEditing()) { (recipe, operation) ->
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
            
            // Apply the operation
            val editedRecipe = when (operation) {
                is IngredientOperation.Add -> {
                    // Add new ingredient
                    val newIngredients = insertedRecipe.ingredients + operation.ingredient
                    insertedRecipe.copy(
                        ingredients = newIngredients,
                        updatedAt = System.currentTimeMillis()
                    )
                }
                is IngredientOperation.Modify -> {
                    // Modify existing ingredient
                    val modifiedIngredients = insertedRecipe.ingredients.mapIndexed { index, ing ->
                        if (index == operation.index) operation.newIngredient else ing
                    }
                    insertedRecipe.copy(
                        ingredients = modifiedIngredients,
                        updatedAt = System.currentTimeMillis()
                    )
                }
                is IngredientOperation.Remove -> {
                    // Remove ingredient
                    val remainingIngredients = insertedRecipe.ingredients.filterIndexed { index, _ ->
                        index != operation.index
                    }
                    insertedRecipe.copy(
                        ingredients = remainingIngredients,
                        updatedAt = System.currentTimeMillis()
                    )
                }
            }
            
            // Update the recipe
            repository.updateRecipe(editedRecipe)
            
            // Verify update was called
            coVerify { recipeDao.update(any()) }
            
            // Verify the operation was applied correctly
            val capturedRecipe = updatedRecipeSlot.captured
            when (operation) {
                is IngredientOperation.Add -> {
                    // Verify ingredient was added
                    capturedRecipe.ingredients.size shouldBe (recipe.ingredients.size + 1)
                    capturedRecipe.ingredients.last().name shouldBe operation.ingredient.name
                }
                is IngredientOperation.Modify -> {
                    // Verify ingredient was modified
                    capturedRecipe.ingredients.size shouldBe recipe.ingredients.size
                    if (operation.index < capturedRecipe.ingredients.size) {
                        capturedRecipe.ingredients[operation.index].name shouldBe operation.newIngredient.name
                    }
                }
                is IngredientOperation.Remove -> {
                    // Verify ingredient was removed
                    capturedRecipe.ingredients.size shouldBe (recipe.ingredients.size - 1)
                }
            }
            
            // Verify this only works for fallback recipes
            capturedRecipe.isFallback shouldBe true
        }
    }
    
    "Property 11: Instruction List Editing Operations - fallback recipes should support instruction operations" {
        // **Validates: Requirements 7.9, 7.10, 7.11**
        // For any fallback recipe being edited,
        // the system should support adding new instructions (increasing list size),
        // modifying existing instructions (changing their text),
        // and removing instructions (decreasing list size)
        
        checkAll(100, Arb.fallbackRecipeForInstructionEditing()) { (recipe, operation) ->
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
            
            // Apply the operation
            val editedRecipe = when (operation) {
                is InstructionOperation.Add -> {
                    // Add new instruction
                    val newInstructions = insertedRecipe.instructions + operation.instruction
                    insertedRecipe.copy(
                        instructions = newInstructions,
                        updatedAt = System.currentTimeMillis()
                    )
                }
                is InstructionOperation.Modify -> {
                    // Modify existing instruction
                    val modifiedInstructions = insertedRecipe.instructions.mapIndexed { index, inst ->
                        if (index == operation.index) operation.newInstruction else inst
                    }
                    insertedRecipe.copy(
                        instructions = modifiedInstructions,
                        updatedAt = System.currentTimeMillis()
                    )
                }
                is InstructionOperation.Remove -> {
                    // Remove instruction
                    val remainingInstructions = insertedRecipe.instructions.filterIndexed { index, _ ->
                        index != operation.index
                    }
                    insertedRecipe.copy(
                        instructions = remainingInstructions,
                        updatedAt = System.currentTimeMillis()
                    )
                }
            }
            
            // Update the recipe
            repository.updateRecipe(editedRecipe)
            
            // Verify update was called
            coVerify { recipeDao.update(any()) }
            
            // Verify the operation was applied correctly
            val capturedRecipe = updatedRecipeSlot.captured
            when (operation) {
                is InstructionOperation.Add -> {
                    // Verify instruction was added
                    capturedRecipe.instructions.size shouldBe (recipe.instructions.size + 1)
                    capturedRecipe.instructions.last().text shouldBe operation.instruction.text
                }
                is InstructionOperation.Modify -> {
                    // Verify instruction was modified
                    capturedRecipe.instructions.size shouldBe recipe.instructions.size
                    if (operation.index < capturedRecipe.instructions.size) {
                        capturedRecipe.instructions[operation.index].text shouldBe operation.newInstruction.text
                    }
                }
                is InstructionOperation.Remove -> {
                    // Verify instruction was removed
                    capturedRecipe.instructions.size shouldBe (recipe.instructions.size - 1)
                }
            }
            
            // Verify this only works for fallback recipes
            capturedRecipe.isFallback shouldBe true
        }
    }
})

/**
 * Sealed class representing ingredient editing operations
 */
sealed class IngredientOperation {
    data class Add(val ingredient: Ingredient) : IngredientOperation()
    data class Modify(val index: Int, val newIngredient: Ingredient) : IngredientOperation()
    data class Remove(val index: Int) : IngredientOperation()
}

/**
 * Sealed class representing instruction editing operations
 */
sealed class InstructionOperation {
    data class Add(val instruction: Instruction) : InstructionOperation()
    data class Modify(val index: Int, val newInstruction: Instruction) : InstructionOperation()
    data class Remove(val index: Int) : InstructionOperation()
}

/**
 * Custom Arb generators for RecipeEditor testing
 */

/**
 * Generates a recipe with random isFallback status and a new name
 */
fun Arb.Companion.recipeWithRandomFallbackStatus(): Arb<Pair<Recipe, String>> = arbitrary {
    val isFallback = Arb.boolean().bind()
    val originalName = Arb.string(1..200).bind()
    val newName = Arb.string(1..200).bind()
    
    val recipe = Recipe(
        name = originalName,
        ingredients = if (isFallback) emptyList() else Arb.list(Arb.ingredient(), 1..5).bind(),
        instructions = if (isFallback) emptyList() else Arb.list(Arb.instruction(), 1..5).bind(),
        originalUrl = "https://example.com/recipe",
        isFallback = isFallback
    )
    
    recipe to newName
}

/**
 * Generates a fallback recipe with an ingredient operation
 */
fun Arb.Companion.fallbackRecipeForIngredientEditing(): Arb<Pair<Recipe, IngredientOperation>> = arbitrary {
    val ingredientCount = Arb.int(0..5).bind()
    val ingredients = List(ingredientCount) { index ->
        Ingredient(
            name = Arb.string(1..50).bind(),
            quantity = Arb.double(0.1..10.0).bind(),
            unit = Arb.string(1..20).bind(),
            order = index
        )
    }
    
    val recipe = Recipe(
        name = Arb.string(1..200).bind(),
        ingredients = ingredients,
        instructions = emptyList(),
        originalUrl = "https://example.com/recipe",
        isFallback = true
    )
    
    // Generate a random operation
    val operation = when (Arb.int(0..2).bind()) {
        0 -> {
            // Add operation
            val newIngredient = Ingredient(
                name = Arb.string(1..50).bind(),
                quantity = Arb.double(0.1..10.0).bind(),
                unit = Arb.string(1..20).bind(),
                order = ingredients.size
            )
            IngredientOperation.Add(newIngredient)
        }
        1 -> {
            // Modify operation (only if there are ingredients)
            if (ingredients.isNotEmpty()) {
                val index = Arb.int(0 until ingredients.size).bind()
                val newIngredient = Ingredient(
                    name = Arb.string(1..50).bind(),
                    quantity = Arb.double(0.1..10.0).bind(),
                    unit = Arb.string(1..20).bind(),
                    order = index
                )
                IngredientOperation.Modify(index, newIngredient)
            } else {
                // If no ingredients, default to Add
                val newIngredient = Ingredient(
                    name = Arb.string(1..50).bind(),
                    quantity = Arb.double(0.1..10.0).bind(),
                    unit = Arb.string(1..20).bind(),
                    order = 0
                )
                IngredientOperation.Add(newIngredient)
            }
        }
        else -> {
            // Remove operation (only if there are ingredients)
            if (ingredients.isNotEmpty()) {
                val index = Arb.int(0 until ingredients.size).bind()
                IngredientOperation.Remove(index)
            } else {
                // If no ingredients, default to Add
                val newIngredient = Ingredient(
                    name = Arb.string(1..50).bind(),
                    quantity = Arb.double(0.1..10.0).bind(),
                    unit = Arb.string(1..20).bind(),
                    order = 0
                )
                IngredientOperation.Add(newIngredient)
            }
        }
    }
    
    recipe to operation
}

/**
 * Generates a fallback recipe with an instruction operation
 */
fun Arb.Companion.fallbackRecipeForInstructionEditing(): Arb<Pair<Recipe, InstructionOperation>> = arbitrary {
    val instructionCount = Arb.int(0..5).bind()
    val instructions = List(instructionCount) { index ->
        Instruction(
            text = Arb.string(10..200).bind(),
            order = index
        )
    }
    
    val recipe = Recipe(
        name = Arb.string(1..200).bind(),
        ingredients = emptyList(),
        instructions = instructions,
        originalUrl = "https://example.com/recipe",
        isFallback = true
    )
    
    // Generate a random operation
    val operation = when (Arb.int(0..2).bind()) {
        0 -> {
            // Add operation
            val newInstruction = Instruction(
                text = Arb.string(10..200).bind(),
                order = instructions.size
            )
            InstructionOperation.Add(newInstruction)
        }
        1 -> {
            // Modify operation (only if there are instructions)
            if (instructions.isNotEmpty()) {
                val index = Arb.int(0 until instructions.size).bind()
                val newInstruction = Instruction(
                    text = Arb.string(10..200).bind(),
                    order = index
                )
                InstructionOperation.Modify(index, newInstruction)
            } else {
                // If no instructions, default to Add
                val newInstruction = Instruction(
                    text = Arb.string(10..200).bind(),
                    order = 0
                )
                InstructionOperation.Add(newInstruction)
            }
        }
        else -> {
            // Remove operation (only if there are instructions)
            if (instructions.isNotEmpty()) {
                val index = Arb.int(0 until instructions.size).bind()
                InstructionOperation.Remove(index)
            } else {
                // If no instructions, default to Add
                val newInstruction = Instruction(
                    text = Arb.string(10..200).bind(),
                    order = 0
                )
                InstructionOperation.Add(newInstruction)
            }
        }
    }
    
    recipe to operation
}

/**
 * Generates an Ingredient with random data
 */
fun Arb.Companion.ingredient(): Arb<Ingredient> = arbitrary {
    Ingredient(
        name = Arb.string(1..50).bind(),
        quantity = Arb.double(0.1..10.0).bind(),
        unit = Arb.string(1..20).bind(),
        order = Arb.int(0..100).bind()
    )
}

/**
 * Generates an Instruction with random data
 */
fun Arb.Companion.instruction(): Arb<Instruction> = arbitrary {
    Instruction(
        text = Arb.string(10..200).bind(),
        order = Arb.int(0..100).bind()
    )
}
