package com.recipebookmarks.domain

interface RecipeParser {
    suspend fun parseRecipe(html: String, sourceUrl: String): ParseResult
    fun extractPageTitle(html: String): String
}
