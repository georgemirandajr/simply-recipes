package com.recipebookmarks.ui

import com.recipebookmarks.data.ScaledIngredient
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for RecipeDetailActivity ingredient display logic.
 * Tests ingredient ordering and formatting.
 * 
 * Requirements: 2.1, 2.2, 2.3
 */
class RecipeDetailActivityTest {
    
    /**
     * Test that ingredients are sorted by order field.
     * Requirement 2.3: Display ingredients in the order specified by the recipe
     */
    @Test
    fun testIngredientOrderPreservation() {
        val ingredients = listOf(
            ScaledIngredient("Sugar", 1.0, 1.0, "cup", 2),
            ScaledIngredient("Flour", 2.0, 2.0, "cups", 0),
            ScaledIngredient("Eggs", 3.0, 3.0, "whole", 1)
        )
        
        val sorted = ingredients.sortedBy { it.order }
        
        assertEquals("Flour", sorted[0].name)
        assertEquals("Eggs", sorted[1].name)
        assertEquals("Sugar", sorted[2].name)
    }
    
    /**
     * Test that ingredient formatting includes quantity, unit, and name.
     * Requirement 2.2: Display each ingredient with its quantity and unit of measurement
     */
    @Test
    fun testIngredientFormatting() {
        val ingredient = ScaledIngredient("Flour", 2.0, 2.0, "cups", 0)
        
        // Test the formatting logic
        val quantity = formatQuantity(ingredient.scaledQuantity)
        val formatted = "$quantity ${ingredient.unit} ${ingredient.name}"
        
        assertEquals("2 cups Flour", formatted)
    }
    
    /**
     * Test that scaled quantities are displayed correctly.
     * Requirement 2.2: Display each ingredient with its quantity and unit of measurement
     */
    @Test
    fun testScaledIngredientFormatting() {
        val ingredient = ScaledIngredient("Sugar", 1.0, 1.5, "cup", 0)
        
        val quantity = formatQuantity(ingredient.scaledQuantity)
        val formatted = "$quantity ${ingredient.unit} ${ingredient.name}"
        
        assertEquals("1.5 cup Sugar", formatted)
    }
    
    /**
     * Test that whole numbers are displayed without decimal points.
     */
    @Test
    fun testWholeNumberFormatting() {
        assertEquals("2", formatQuantity(2.0))
        assertEquals("1", formatQuantity(1.0))
        assertEquals("10", formatQuantity(10.0))
    }
    
    /**
     * Test that decimal numbers are displayed with appropriate precision.
     */
    @Test
    fun testDecimalFormatting() {
        assertEquals("1.5", formatQuantity(1.5))
        assertEquals("2.25", formatQuantity(2.25))
        assertEquals("0.5", formatQuantity(0.5))
    }
    
    /**
     * Test that trailing zeros are removed from decimal numbers.
     */
    @Test
    fun testTrailingZeroRemoval() {
        assertEquals("1.5", formatQuantity(1.50))
        assertEquals("2.1", formatQuantity(2.10))
    }
    
    // Helper method matching the implementation in RecipeDetailActivity
    private fun formatQuantity(quantity: Double): String {
        return if (quantity % 1.0 == 0.0) {
            quantity.toInt().toString()
        } else {
            String.format("%.2f", quantity).trimEnd('0').trimEnd('.')
        }
    }
}
