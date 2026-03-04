package com.recipebookmarks.domain

import com.recipebookmarks.data.Recipe

sealed class ParseResult {
    data class Success(val recipe: Recipe) : ParseResult()
    data class Failure(val error: ParseError) : ParseResult()
}

enum class ParseError {
    NO_RECIPE_DATA,
    INVALID_HTML,
    MISSING_REQUIRED_FIELDS
}
