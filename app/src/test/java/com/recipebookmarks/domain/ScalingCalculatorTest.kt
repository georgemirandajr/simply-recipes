package com.recipebookmarks.domain

import com.recipebookmarks.data.Ingredient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ScalingCalculatorTest {
    private lateinit var calculator: ScalingCalculator

    @BeforeEach
    fun setup() {
        calculator = ScalingCalculatorImpl()
    }

    @Test
    fun `scaleIngredients with SINGLE factor returns original quantities`() {
        val ingredients = listOf(
            Ingredient("Flour", 2.0, "cups", 0),
            Ingredient("Sugar", 1.0, "cup", 1),
            Ingredient("Eggs", 3.0, "whole", 2)
        )

        val scaled = calculator.scaleIngredients(ingredients, ScalingFactor.SINGLE)

        assertEquals(3, scaled.size)
        assertEquals(2.0, scaled[0].originalQuantity, 0.001)
        assertEquals(2.0, scaled[0].scaledQuantity, 0.001)
        assertEquals(1.0, scaled[1].originalQuantity, 0.001)
        assertEquals(1.0, scaled[1].scaledQuantity, 0.001)
        assertEquals(3.0, scaled[2].originalQuantity, 0.001)
        assertEquals(3.0, scaled[2].scaledQuantity, 0.001)
    }

    @Test
    fun `scaleIngredients with ONE_AND_HALF factor multiplies by 1_5`() {
        val ingredients = listOf(
            Ingredient("Flour", 2.0, "cups", 0),
            Ingredient("Sugar", 1.0, "cup", 1)
        )

        val scaled = calculator.scaleIngredients(ingredients, ScalingFactor.ONE_AND_HALF)

        assertEquals(2, scaled.size)
        assertEquals(2.0, scaled[0].originalQuantity, 0.001)
        assertEquals(3.0, scaled[0].scaledQuantity, 0.001)
        assertEquals(1.0, scaled[1].originalQuantity, 0.001)
        assertEquals(1.5, scaled[1].scaledQuantity, 0.001)
    }

    @Test
    fun `scaleIngredients with DOUBLE factor multiplies by 2_0`() {
        val ingredients = listOf(
            Ingredient("Flour", 2.0, "cups", 0),
            Ingredient("Sugar", 1.0, "cup", 1)
        )

        val scaled = calculator.scaleIngredients(ingredients, ScalingFactor.DOUBLE)

        assertEquals(2, scaled.size)
        assertEquals(2.0, scaled[0].originalQuantity, 0.001)
        assertEquals(4.0, scaled[0].scaledQuantity, 0.001)
        assertEquals(1.0, scaled[1].originalQuantity, 0.001)
        assertEquals(2.0, scaled[1].scaledQuantity, 0.001)
    }

    @Test
    fun `scaleIngredients preserves ingredient properties`() {
        val ingredients = listOf(
            Ingredient("Flour", 2.0, "cups", 0)
        )

        val scaled = calculator.scaleIngredients(ingredients, ScalingFactor.ONE_AND_HALF)

        assertEquals("Flour", scaled[0].name)
        assertEquals("cups", scaled[0].unit)
        assertEquals(0, scaled[0].order)
    }

    @Test
    fun `scaleIngredients with empty list returns empty list`() {
        val ingredients = emptyList<Ingredient>()

        val scaled = calculator.scaleIngredients(ingredients, ScalingFactor.DOUBLE)

        assertEquals(0, scaled.size)
    }
}
