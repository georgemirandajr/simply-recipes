package com.recipebookmarks.domain

import com.recipebookmarks.data.Ingredient
import com.recipebookmarks.data.ScaledIngredient

interface ScalingCalculator {
    fun scaleIngredients(
        ingredients: List<Ingredient>,
        scalingFactor: ScalingFactor
    ): List<ScaledIngredient>
}
