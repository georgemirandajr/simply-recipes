package com.recipebookmarks.domain

import com.recipebookmarks.data.Ingredient
import com.recipebookmarks.data.ScaledIngredient

class ScalingCalculatorImpl : ScalingCalculator {
    override fun scaleIngredients(
        ingredients: List<Ingredient>,
        scalingFactor: ScalingFactor
    ): List<ScaledIngredient> {
        return ingredients.map { ingredient ->
            ScaledIngredient(
                name = ingredient.name,
                originalQuantity = ingredient.quantity,
                scaledQuantity = ingredient.quantity * scalingFactor.multiplier,
                unit = ingredient.unit,
                order = ingredient.order
            )
        }
    }
}
