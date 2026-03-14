package com.recipebookmarks.domain

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.recipebookmarks.data.Ingredient
import com.recipebookmarks.data.Instruction
import com.recipebookmarks.data.NutritionInfo
import com.recipebookmarks.data.Recipe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class RecipeParserImpl : RecipeParser {
    private val gson = Gson()

    override suspend fun parseRecipe(html: String, sourceUrl: String): ParseResult = withContext(Dispatchers.Default) {
        try {
            val document = Jsoup.parse(html)
            
            // Try JSON-LD first (most common and structured)
            val jsonLdResult = parseJsonLd(document, sourceUrl)
            if (jsonLdResult is ParseResult.Success) {
                return@withContext jsonLdResult
            }
            
            // Try Microdata
            val microdataResult = parseMicrodata(document, sourceUrl)
            if (microdataResult is ParseResult.Success) {
                return@withContext microdataResult
            }
            
            // Try RDFa
            val rdfaResult = parseRdfa(document, sourceUrl)
            if (rdfaResult is ParseResult.Success) {
                return@withContext rdfaResult
            }
            
            // Try Food Network site-specific parsing
            val foodNetworkResult = parseFoodNetwork(document, sourceUrl)
            if (foodNetworkResult is ParseResult.Success) {
                return@withContext foodNetworkResult
            }

            // All parsing methods failed - create fallback recipe
            createFallbackRecipe(html, sourceUrl)
        } catch (e: Exception) {
            // Even on exception, try to create fallback
            createFallbackRecipe(html, sourceUrl)
        }
    }

    override fun extractPageTitle(html: String): String {
        return try {
            val document = Jsoup.parse(html)
            val title = document.select("title").text().trim()
            
            when {
                title.isNotBlank() -> title.take(200)  // Truncate to 200 chars
                else -> ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun parseJsonLd(document: Document, sourceUrl: String): ParseResult {
        try {
            val scriptElements = document.select("script[type=application/ld+json]")
            
            for (script in scriptElements) {
                val jsonText = script.html()
                
                val jsonElement = JsonParser.parseString(jsonText)
                
                // Handle both single object and array of objects
                val recipes = when {
                    jsonElement.isJsonArray -> {
                        jsonElement.asJsonArray.filter { 
                            it.isJsonObject && isRecipeType(it.asJsonObject)
                        }.map { it.asJsonObject }
                    }
                    jsonElement.isJsonObject && isRecipeType(jsonElement.asJsonObject) -> {
                        listOf(jsonElement.asJsonObject)
                    }
                    jsonElement.isJsonObject -> {
                        emptyList()
                    }
                    else -> {
                        emptyList()
                    }
                }
                
                if (recipes.isNotEmpty()) {
                    val recipeJson = recipes.first()
                    return parseJsonLdRecipe(recipeJson, sourceUrl)
                }
            }
            
            return ParseResult.Failure(ParseError.NO_RECIPE_DATA)
        } catch (e: Exception) {
            return ParseResult.Failure(ParseError.INVALID_HTML)
        }
    }

    private fun isRecipeType(json: JsonObject): Boolean {
        val typeElement = json.get("@type") ?: return false
        
        return when {
            typeElement.isJsonPrimitive -> {
                val type = typeElement.asString
                type.equals("Recipe", ignoreCase = true)
            }
            typeElement.isJsonArray -> {
                // @type can be an array like ["Recipe", "NewsArticle"]
                typeElement.asJsonArray.any { 
                    it.isJsonPrimitive && it.asString.equals("Recipe", ignoreCase = true)
                }
            }
            else -> false
        }
    }

    private fun parseJsonLdRecipe(json: JsonObject, sourceUrl: String): ParseResult {
        try {
            // Extract required fields
            val name = json.get("name")?.asString
            if (name.isNullOrBlank()) {
                return ParseResult.Failure(ParseError.MISSING_REQUIRED_FIELDS)
            }
            
            // Extract ingredients
            val ingredients = parseJsonLdIngredients(json)
            if (ingredients.isEmpty()) {
                return ParseResult.Failure(ParseError.MISSING_REQUIRED_FIELDS)
            }
            
            // Extract instructions
            val instructions = parseJsonLdInstructions(json)
            if (instructions.isEmpty()) {
                return ParseResult.Failure(ParseError.MISSING_REQUIRED_FIELDS)
            }
            
            // Extract optional fields
            val yield = json.get("recipeYield")?.let { 
                when {
                    it.isJsonPrimitive -> it.asString
                    it.isJsonArray && it.asJsonArray.size() > 0 -> it.asJsonArray.first().asString
                    else -> null
                }
            }
            
            val servingSize = json.get("servingSize")?.asString
            
            val nutritionInfo = json.get("nutrition")?.let { parseJsonLdNutrition(it.asJsonObject) }
            
            val recipe = Recipe(
                name = name,
                ingredients = ingredients,
                instructions = instructions,
                yield = yield,
                servingSize = servingSize,
                nutritionInfo = nutritionInfo,
                originalUrl = sourceUrl,
                category = null
            )
            
            return ParseResult.Success(recipe)
        } catch (e: Exception) {
            return ParseResult.Failure(ParseError.INVALID_HTML)
        }
    }

    private fun parseJsonLdIngredients(json: JsonObject): List<Ingredient> {
        val ingredientsJson = json.get("recipeIngredient") ?: return emptyList()
        
        if (!ingredientsJson.isJsonArray) return emptyList()
        
        return ingredientsJson.asJsonArray.mapIndexed { index, element ->
            val ingredientText = element.asString
            parseIngredientText(ingredientText, index)
        }
    }

    private fun parseIngredientText(text: String, order: Int): Ingredient {
        // Simple parsing: try to extract quantity, unit, and name
        val parts = text.trim().split(" ", limit = 3)
        
        return when {
            parts.size >= 3 -> {
                val quantity = parts[0].toDoubleOrNull() ?: 1.0
                Ingredient(
                    name = parts.drop(2).joinToString(" "),
                    quantity = quantity,
                    unit = parts[1],
                    order = order
                )
            }
            parts.size == 2 -> {
                val quantity = parts[0].toDoubleOrNull() ?: 1.0
                Ingredient(
                    name = parts[1],
                    quantity = quantity,
                    unit = "",
                    order = order
                )
            }
            else -> {
                Ingredient(
                    name = text,
                    quantity = 1.0,
                    unit = "",
                    order = order
                )
            }
        }
    }

    private fun parseJsonLdInstructions(json: JsonObject): List<Instruction> {
        val instructionsJson = json.get("recipeInstructions") ?: return emptyList()
        
        return when {
            instructionsJson.isJsonArray -> {
                instructionsJson.asJsonArray.mapIndexedNotNull { index, element ->
                    when {
                        element.isJsonPrimitive -> Instruction(element.asString, index)
                        element.isJsonObject -> {
                            val text = element.asJsonObject.get("text")?.asString
                            text?.let { Instruction(it, index) }
                        }
                        else -> null
                    }
                }
            }
            instructionsJson.isJsonPrimitive -> {
                listOf(Instruction(instructionsJson.asString, 0))
            }
            else -> emptyList()
        }
    }

    private fun parseJsonLdNutrition(nutritionJson: JsonObject): NutritionInfo? {
        try {
            return NutritionInfo(
                calories = nutritionJson.get("calories")?.asString?.filter { it.isDigit() }?.toIntOrNull(),
                protein = nutritionJson.get("proteinContent")?.asString,
                carbohydrates = nutritionJson.get("carbohydrateContent")?.asString,
                fat = nutritionJson.get("fatContent")?.asString,
                fiber = nutritionJson.get("fiberContent")?.asString,
                sugar = nutritionJson.get("sugarContent")?.asString
            )
        } catch (e: Exception) {
            return null
        }
    }

    private fun parseMicrodata(document: Document, sourceUrl: String): ParseResult {
        try {
            // Look for elements with itemtype="http://schema.org/Recipe"
            val recipeElements = document.select("[itemtype*=schema.org/Recipe]")
            
            if (recipeElements.isEmpty()) {
                return ParseResult.Failure(ParseError.NO_RECIPE_DATA)
            }
            
            val recipeElement = recipeElements.first()!!
            
            // Extract name
            val name = recipeElement.select("[itemprop=name]").text()
            if (name.isBlank()) {
                return ParseResult.Failure(ParseError.MISSING_REQUIRED_FIELDS)
            }
            
            // Extract ingredients
            val ingredientElements = recipeElement.select("[itemprop=recipeIngredient]")
            val ingredients = ingredientElements.mapIndexed { index, element ->
                parseIngredientText(element.text(), index)
            }
            
            if (ingredients.isEmpty()) {
                return ParseResult.Failure(ParseError.MISSING_REQUIRED_FIELDS)
            }
            
            // Extract instructions
            val instructionElements = recipeElement.select("[itemprop=recipeInstructions]")
            val instructions = if (instructionElements.size == 1 && instructionElements.first()!!.children().isNotEmpty()) {
                // Instructions might be in child elements
                instructionElements.first()!!.children().mapIndexed { index, element ->
                    Instruction(element.text(), index)
                }
            } else {
                instructionElements.mapIndexed { index, element ->
                    Instruction(element.text(), index)
                }
            }
            
            if (instructions.isEmpty()) {
                return ParseResult.Failure(ParseError.MISSING_REQUIRED_FIELDS)
            }
            
            // Extract optional fields
            val yield = recipeElement.select("[itemprop=recipeYield]").text().takeIf { it.isNotBlank() }
            val servingSize = recipeElement.select("[itemprop=servingSize]").text().takeIf { it.isNotBlank() }
            
            // Extract nutrition info
            val nutritionElement = recipeElement.select("[itemprop=nutrition]").firstOrNull()
            val nutritionInfo = nutritionElement?.let { parseMicrodataNutrition(it) }
            
            val recipe = Recipe(
                name = name,
                ingredients = ingredients,
                instructions = instructions,
                yield = yield,
                servingSize = servingSize,
                nutritionInfo = nutritionInfo,
                originalUrl = sourceUrl,
                category = null
            )
            
            return ParseResult.Success(recipe)
        } catch (e: Exception) {
            return ParseResult.Failure(ParseError.INVALID_HTML)
        }
    }

    private fun parseMicrodataNutrition(element: Element): NutritionInfo? {
        try {
            return NutritionInfo(
                calories = element.select("[itemprop=calories]").text().filter { it.isDigit() }.toIntOrNull(),
                protein = element.select("[itemprop=proteinContent]").text().takeIf { it.isNotBlank() },
                carbohydrates = element.select("[itemprop=carbohydrateContent]").text().takeIf { it.isNotBlank() },
                fat = element.select("[itemprop=fatContent]").text().takeIf { it.isNotBlank() },
                fiber = element.select("[itemprop=fiberContent]").text().takeIf { it.isNotBlank() },
                sugar = element.select("[itemprop=sugarContent]").text().takeIf { it.isNotBlank() }
            )
        } catch (e: Exception) {
            return null
        }
    }

    private fun parseRdfa(document: Document, sourceUrl: String): ParseResult {
        try {
            // Look for elements with typeof="Recipe" or property containing recipe
            val recipeElements = document.select("[typeof*=Recipe], [property*=recipe]")
            
            if (recipeElements.isEmpty()) {
                return ParseResult.Failure(ParseError.NO_RECIPE_DATA)
            }
            
            val recipeElement = recipeElements.first()!!
            
            // Extract name
            val name = recipeElement.select("[property=name]").text()
            if (name.isBlank()) {
                return ParseResult.Failure(ParseError.MISSING_REQUIRED_FIELDS)
            }
            
            // Extract ingredients
            val ingredientElements = recipeElement.select("[property=recipeIngredient]")
            val ingredients = ingredientElements.mapIndexed { index, element ->
                parseIngredientText(element.text(), index)
            }
            
            if (ingredients.isEmpty()) {
                return ParseResult.Failure(ParseError.MISSING_REQUIRED_FIELDS)
            }
            
            // Extract instructions
            val instructionElements = recipeElement.select("[property=recipeInstructions]")
            val instructions = instructionElements.mapIndexed { index, element ->
                Instruction(element.text(), index)
            }
            
            if (instructions.isEmpty()) {
                return ParseResult.Failure(ParseError.MISSING_REQUIRED_FIELDS)
            }
            
            // Extract optional fields
            val yield = recipeElement.select("[property=recipeYield]").text().takeIf { it.isNotBlank() }
            val servingSize = recipeElement.select("[property=servingSize]").text().takeIf { it.isNotBlank() }
            
            // Extract nutrition info
            val nutritionElement = recipeElement.select("[property=nutrition]").firstOrNull()
            val nutritionInfo = nutritionElement?.let { parseRdfaNutrition(it) }
            
            val recipe = Recipe(
                name = name,
                ingredients = ingredients,
                instructions = instructions,
                yield = yield,
                servingSize = servingSize,
                nutritionInfo = nutritionInfo,
                originalUrl = sourceUrl,
                category = null
            )
            
            return ParseResult.Success(recipe)
        } catch (e: Exception) {
            return ParseResult.Failure(ParseError.INVALID_HTML)
        }
    }

    private fun parseRdfaNutrition(element: Element): NutritionInfo? {
        try {
            return NutritionInfo(
                calories = element.select("[property=calories]").text().filter { it.isDigit() }.toIntOrNull(),
                protein = element.select("[property=proteinContent]").text().takeIf { it.isNotBlank() },
                carbohydrates = element.select("[property=carbohydrateContent]").text().takeIf { it.isNotBlank() },
                fat = element.select("[property=fatContent]").text().takeIf { it.isNotBlank() },
                fiber = element.select("[property=fiberContent]").text().takeIf { it.isNotBlank() },
                sugar = element.select("[property=sugarContent]").text().takeIf { it.isNotBlank() }
            )
        } catch (e: Exception) {
            return null
        }
    }

    private fun parseFoodNetwork(document: Document, sourceUrl: String): ParseResult {
        try {
            // Title: div#recipeHead > div.assetTitle > h2
            val titleEl = document.select("div#recipeHead div.assetTitle h2").firstOrNull()
                ?: return ParseResult.Failure(ParseError.NO_RECIPE_DATA)
            val name = titleEl.text().trim()
            if (name.isBlank()) return ParseResult.Failure(ParseError.MISSING_REQUIRED_FIELDS)

            // Yield: div.o-RecipeInfo ul.o-RecipeInfo__m-Yield
            val yieldEl = document.select("div.o-RecipeInfo ul.o-RecipeInfo__m-Yield").firstOrNull()
            val yield = yieldEl?.text()?.trim()?.takeIf { it.isNotBlank() }

            // Ingredients: div.recipe-body > div.bodyLeft
            val ingredientsContainer = document.select("div.recipe-body div.bodyLeft").firstOrNull()
                ?: return ParseResult.Failure(ParseError.MISSING_REQUIRED_FIELDS)
            val ingredientEls = ingredientsContainer.select("li, p").filter { it.text().isNotBlank() }
            if (ingredientEls.isEmpty()) return ParseResult.Failure(ParseError.MISSING_REQUIRED_FIELDS)
            val ingredients = ingredientEls.mapIndexed { i, el -> parseIngredientText(el.text().trim(), i) }

            // Instructions: div.bodyRight > section > div.o-Method__m-Body
            val instructionsContainer = document.select("div.bodyRight section div.o-Method__m-Body").firstOrNull()
                ?: return ParseResult.Failure(ParseError.MISSING_REQUIRED_FIELDS)
            val instructionEls = instructionsContainer.select("li, p").filter { it.text().isNotBlank() }
            if (instructionEls.isEmpty()) return ParseResult.Failure(ParseError.MISSING_REQUIRED_FIELDS)
            val instructions = instructionEls.mapIndexed { i, el -> Instruction(el.text().trim(), i) }

            return ParseResult.Success(
                Recipe(
                    name = name,
                    ingredients = ingredients,
                    instructions = instructions,
                    yield = yield,
                    servingSize = null,
                    nutritionInfo = null,
                    originalUrl = sourceUrl,
                    category = null
                )
            )
        } catch (e: Exception) {
            return ParseResult.Failure(ParseError.INVALID_HTML)
        }
    }

    private fun generateNameFromUrl(url: String): String {
        return try {
            // Use java.net.URL for better unit test compatibility
            val javaUrl = java.net.URL(url)
            val host = javaUrl.host
            when {
                host.isNullOrBlank() -> "Untitled Recipe"
                else -> host.removePrefix("www.")
            }
        } catch (e: Exception) {
            "Untitled Recipe"
        }
    }

    private fun createFallbackRecipe(html: String, sourceUrl: String): ParseResult {
        val pageTitle = extractPageTitle(html)
        val recipeName = when {
            pageTitle.isNotBlank() -> pageTitle
            else -> generateNameFromUrl(sourceUrl)
        }
        
        val fallbackRecipe = Recipe(
            name = recipeName,
            ingredients = emptyList(),
            instructions = emptyList(),
            originalUrl = sourceUrl,
            isFallback = true
        )
        
        return ParseResult.Success(fallbackRecipe)
    }
}
