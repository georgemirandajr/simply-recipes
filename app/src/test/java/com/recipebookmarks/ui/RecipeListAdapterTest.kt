package com.recipebookmarks.ui

import com.recipebookmarks.data.Category
import com.recipebookmarks.data.Recipe
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*

/**
 * Unit tests for RecipeListAdapter
 * Feature: fallback-recipe-import
 * 
 * These tests validate the RecipeListAdapter's constructor and callback handling.
 * Note: Full UI testing including view binding and click handling requires instrumented tests.
 */
class RecipeListAdapterTest : StringSpec({
    
    "adapter accepts onRecipeClick and onDeleteClick callbacks" {
        // Given callbacks are provided
        val onRecipeClick: (Recipe) -> Unit = mockk(relaxed = true)
        val onDeleteClick: (Recipe) -> Unit = mockk(relaxed = true)
        
        // When adapter is created with callbacks
        val adapter = RecipeListAdapter(onRecipeClick, onDeleteClick)
        
        // Then adapter should be created successfully
        adapter shouldNotBe null
    }

    "adapter can be instantiated with lambda callbacks" {
        // Given lambda callbacks
        var clickedRecipe: Recipe? = null
        var deletedRecipe: Recipe? = null
        
        // When adapter is created with lambda callbacks
        val adapter = RecipeListAdapter(
            onRecipeClick = { recipe -> clickedRecipe = recipe },
            onDeleteClick = { recipe -> deletedRecipe = recipe }
        )
        
        // Then adapter should be created successfully
        adapter shouldNotBe null
        clickedRecipe shouldBe null
        deletedRecipe shouldBe null
    }

    "adapter constructor accepts both required parameters" {
        // Given both callbacks are provided
        val onRecipeClick: (Recipe) -> Unit = {}
        val onDeleteClick: (Recipe) -> Unit = {}
        
        // When adapter is created
        val adapter = RecipeListAdapter(onRecipeClick, onDeleteClick)
        
        // Then adapter should be created successfully
        adapter shouldNotBe null
    }
})
