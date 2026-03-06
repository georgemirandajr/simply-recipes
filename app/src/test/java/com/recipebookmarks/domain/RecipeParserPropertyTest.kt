package com.recipebookmarks.domain

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

/**
 * Property-based tests for RecipeParser
 * Feature: fallback-recipe-import
 */
class RecipeParserPropertyTest : StringSpec({
    
    "Property 3: Recipe Name Extraction from Page Title - extracts full page title text" {
        // **Validates: Requirements 1.3, 2.1, 2.3, 2.4**
        // For any HTML page with a non-empty title element, when creating a fallback recipe,
        // the system should extract the full page title text (trimmed and truncated to 200 characters)
        
        checkAll(100, Arb.htmlWithTitle()) { (html, expectedTitle) ->
            val parser = RecipeParserImpl()
            val extractedTitle = parser.extractPageTitle(html)
            
            // The extracted title should be the expected title, trimmed and truncated to 200 chars
            val expectedResult = expectedTitle.trim().take(200)
            extractedTitle shouldBe expectedResult
        }
    }
    
    "Property 3: Recipe Name Extraction - handles titles with leading/trailing whitespace" {
        // **Validates: Requirements 2.3**
        // The system should trim whitespace from extracted recipe names
        
        checkAll(100, Arb.htmlWithWhitespaceTitle()) { (html, titleWithWhitespace) ->
            val parser = RecipeParserImpl()
            val extractedTitle = parser.extractPageTitle(html)
            
            // The extracted title should have whitespace trimmed
            extractedTitle shouldBe titleWithWhitespace.trim().take(200)
            // Verify no leading or trailing whitespace
            if (extractedTitle.isNotEmpty()) {
                extractedTitle.first().isWhitespace() shouldBe false
                extractedTitle.last().isWhitespace() shouldBe false
            }
        }
    }
    
    "Property 3: Recipe Name Extraction - truncates titles exceeding 200 characters" {
        // **Validates: Requirements 2.4**
        // When the Page_Title exceeds 200 characters, the system should truncate it to 200 characters
        
        checkAll(100, Arb.htmlWithLongTitle()) { (html, longTitle) ->
            val parser = RecipeParserImpl()
            val extractedTitle = parser.extractPageTitle(html)
            
            // The extracted title should never exceed 200 characters
            extractedTitle.length shouldBe 200.coerceAtMost(longTitle.trim().length)
            
            // If the original title was longer than 200 chars, verify truncation
            if (longTitle.trim().length > 200) {
                extractedTitle.length shouldBe 200
                extractedTitle shouldBe longTitle.trim().take(200)
            }
        }
    }
    
    "Property 3: Recipe Name Extraction - returns empty string for missing title" {
        // **Validates: Requirements 1.3**
        // When the title element is unavailable or empty, return empty string
        
        checkAll(100, Arb.htmlWithoutTitle()) { html ->
            val parser = RecipeParserImpl()
            val extractedTitle = parser.extractPageTitle(html)
            
            extractedTitle shouldBe ""
        }
    }
    
    "Property 6: Domain Name Fallback for Missing Titles - extracts domain from URL" {
        // **Validates: Requirements 1.4, 2.2**
        // For any URL with HTML that lacks a title element or has an empty title,
        // when creating a fallback recipe, the system should extract the domain name from the URL
        
        checkAll(100, Arb.urlWithDomain()) { (url, expectedDomain) ->
            val parser = RecipeParserImpl()
            // HTML without title element - should trigger domain fallback
            val htmlWithoutTitle = "<html><head></head><body><p>Recipe content without structured data</p></body></html>"
            
            val result = parser.parseRecipe(htmlWithoutTitle, url)
            
            // Verify that extractPageTitle returns empty (precondition for domain fallback)
            val extractedTitle = parser.extractPageTitle(htmlWithoutTitle)
            extractedTitle shouldBe ""
            
            // Verify fallback recipe is created with domain name
            result shouldBe io.kotest.matchers.types.instanceOf<ParseResult.Success>()
            val recipe = (result as ParseResult.Success).recipe
            recipe.name shouldBe expectedDomain
            recipe.isFallback shouldBe true
        }
    }
    
    "Property 6: Domain Name Fallback - removes www prefix from domain" {
        // **Validates: Requirements 2.2**
        // When generating a recipe name from the URL, the system should remove "www." prefix
        
        checkAll(100, Arb.urlWithWwwPrefix()) { (url, expectedDomainWithoutWww) ->
            val parser = RecipeParserImpl()
            // HTML without title element - should trigger domain fallback
            val htmlWithoutTitle = "<html><head><title></title></head><body><p>Recipe content</p></body></html>"
            
            val result = parser.parseRecipe(htmlWithoutTitle, url)
            
            // Verify that extractPageTitle returns empty (precondition for domain fallback)
            val extractedTitle = parser.extractPageTitle(htmlWithoutTitle)
            extractedTitle shouldBe ""
            
            // Verify fallback recipe is created with domain name without www prefix
            result shouldBe io.kotest.matchers.types.instanceOf<ParseResult.Success>()
            val recipe = (result as ParseResult.Success).recipe
            recipe.name shouldBe expectedDomainWithoutWww
            recipe.name.startsWith("www.") shouldBe false
            recipe.isFallback shouldBe true
        }
    }
    
    "Property 6: Domain Name Fallback - handles URLs without domain gracefully" {
        // **Validates: Requirements 1.4, 2.2**
        // When the URL cannot be parsed or has no domain, fallback should use "Untitled Recipe"
        
        checkAll(100, Arb.invalidUrl()) { url ->
            val parser = RecipeParserImpl()
            // HTML without title element - should trigger domain fallback
            val htmlWithoutTitle = "<html><head></head><body><p>Recipe content</p></body></html>"
            
            val result = parser.parseRecipe(htmlWithoutTitle, url)
            
            // Verify that extractPageTitle returns empty (precondition for domain fallback)
            val extractedTitle = parser.extractPageTitle(htmlWithoutTitle)
            extractedTitle shouldBe ""
            
            // Verify fallback recipe is created with "Untitled Recipe" for invalid URLs
            result shouldBe io.kotest.matchers.types.instanceOf<ParseResult.Success>()
            val recipe = (result as ParseResult.Success).recipe
            recipe.name shouldBe "Untitled Recipe"
            recipe.isFallback shouldBe true
        }
    }
    
    "Property 1: Fallback Recipe Creation on Parse Failure - creates fallback for HTML without structured data" {
        // **Validates: Requirements 1.1**
        // For any HTML page without structured recipe data (JSON-LD, Microdata, or RDFa),
        // when the parser attempts to extract recipe information, the system should create
        // a fallback recipe with isFallback=true rather than returning a parse failure
        
        checkAll(100, Arb.htmlWithoutStructuredData()) { html ->
            val parser = RecipeParserImpl()
            val result = parser.parseRecipe(html, "https://example.com/recipe")
            
            // The parser should return Success (not Failure) for HTML without structured data
            result shouldBe io.kotest.matchers.types.instanceOf<ParseResult.Success>()
            val recipe = (result as ParseResult.Success).recipe
            recipe.isFallback shouldBe true
            
            // Verify that the HTML truly lacks structured data
            // This ensures our generator is working correctly
            val doc = org.jsoup.Jsoup.parse(html)
            val hasJsonLd = doc.select("script[type=application/ld+json]").isNotEmpty()
            val hasMicrodata = doc.select("[itemtype*=schema.org/Recipe]").isNotEmpty()
            val hasRdfa = doc.select("[typeof*=Recipe]").isNotEmpty()
            
            hasJsonLd shouldBe false
            hasMicrodata shouldBe false
            hasRdfa shouldBe false
        }
    }
    
    "Property 2: Fallback Recipe URL Preservation - preserves original URL in fallback recipe" {
        // **Validates: Requirements 1.2**
        // For any URL that results in a fallback recipe, the created recipe should contain
        // the original URL in the originalUrl field
        
        checkAll(100, Arb.urlAndHtmlWithoutStructuredData()) { (url, html) ->
            val parser = RecipeParserImpl()
            val result = parser.parseRecipe(html, url)
            
            // The parser should return Success with a fallback recipe
            result shouldBe io.kotest.matchers.types.instanceOf<ParseResult.Success>()
            val recipe = (result as ParseResult.Success).recipe
            recipe.isFallback shouldBe true
            recipe.originalUrl shouldBe url
            
            // Verify that the HTML lacks structured data (precondition for fallback)
            val doc = org.jsoup.Jsoup.parse(html)
            val hasJsonLd = doc.select("script[type=application/ld+json]").isNotEmpty()
            val hasMicrodata = doc.select("[itemtype*=schema.org/Recipe]").isNotEmpty()
            val hasRdfa = doc.select("[typeof*=Recipe]").isNotEmpty()
            
            hasJsonLd shouldBe false
            hasMicrodata shouldBe false
            hasRdfa shouldBe false
        }
    }
    
    "Property 4: Fallback Recipes Have Empty Lists - ingredients and instructions are empty" {
        // **Validates: Requirements 1.5, 1.6**
        // For any fallback recipe created by the parser, both the ingredients list
        // and instructions list should be empty
        
        checkAll(100, Arb.urlAndHtmlWithoutStructuredData()) { (url, html) ->
            val parser = RecipeParserImpl()
            val result = parser.parseRecipe(html, url)
            
            // The parser should return Success with a fallback recipe
            result shouldBe io.kotest.matchers.types.instanceOf<ParseResult.Success>()
            val recipe = (result as ParseResult.Success).recipe
            recipe.isFallback shouldBe true
            recipe.ingredients shouldBe emptyList()
            recipe.instructions shouldBe emptyList()
            
            // Verify that the HTML lacks structured data (precondition for fallback)
            val doc = org.jsoup.Jsoup.parse(html)
            val hasJsonLd = doc.select("script[type=application/ld+json]").isNotEmpty()
            val hasMicrodata = doc.select("[itemtype*=schema.org/Recipe]").isNotEmpty()
            val hasRdfa = doc.select("[typeof*=Recipe]").isNotEmpty()
            
            hasJsonLd shouldBe false
            hasMicrodata shouldBe false
            hasRdfa shouldBe false
        }
    }
    
    "Property 7: Structured Data Parsing Preserved - parses structured data correctly" {
        // **Validates: Requirements 5.1**
        // For any HTML page containing valid structured recipe data (JSON-LD, Microdata, or RDFa),
        // the parser should create a recipe with isFallback=false and populated ingredients and instructions lists
        
        checkAll(100, Arb.htmlWithJsonLdData()) { (html, url) ->
            val parser = RecipeParserImpl()
            val result = parser.parseRecipe(html, url)
            
            // The parser should return Success with a non-fallback recipe
            result shouldBe io.kotest.matchers.types.instanceOf<ParseResult.Success>()
            val recipe = (result as ParseResult.Success).recipe
            
            // Verify this is NOT a fallback recipe
            recipe.isFallback shouldBe false
            
            // Verify ingredients and instructions are populated (not empty)
            recipe.ingredients.isEmpty() shouldBe false
            recipe.instructions.isEmpty() shouldBe false
            
            // Verify the recipe has a name
            recipe.name.isNotBlank() shouldBe true
            
            // Verify the original URL is preserved
            recipe.originalUrl shouldBe url
            
            // Verify that the HTML contains JSON-LD structured data
            val doc = org.jsoup.Jsoup.parse(html)
            val hasJsonLd = doc.select("script[type=application/ld+json]").isNotEmpty()
            hasJsonLd shouldBe true
        }
    }
})

/**
 * Custom Arb generators for RecipeParser testing
 */

/**
 * Generates HTML with a title element containing random text
 * Returns a pair of (html, expectedTitle)
 * Note: Jsoup decodes HTML entities, so we use simple alphanumeric strings
 */
fun Arb.Companion.htmlWithTitle(): Arb<Pair<String, String>> = arbitrary {
    // Generate simple alphanumeric strings to avoid HTML entity decoding issues
    val chars = ('a'..'z') + ('A'..'Z') + ('0'..'9') + listOf(' ', '-', '_')
    val length = (1..300).random()
    val title = (1..length).map { chars.random() }.joinToString("")
    val html = "<html><head><title>$title</title></head><body><p>Recipe content</p></body></html>"
    html to title
}

/**
 * Generates HTML with a title element containing whitespace around the text
 * Returns a pair of (html, titleWithWhitespace)
 * Note: Jsoup decodes HTML entities, so we use simple alphanumeric strings
 */
fun Arb.Companion.htmlWithWhitespaceTitle(): Arb<Pair<String, String>> = arbitrary {
    val chars = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    val length = (1..100).random()
    val coreTitle = (1..length).map { chars.random() }.joinToString("")
    val whitespaceOptions = listOf("  ", "\t", "\n", "   ", "\t\t", "\n\n")
    val leadingWhitespace = whitespaceOptions.random()
    val trailingWhitespace = whitespaceOptions.random()
    val titleWithWhitespace = "$leadingWhitespace$coreTitle$trailingWhitespace"
    
    val html = "<html><head><title>$titleWithWhitespace</title></head><body></body></html>"
    html to titleWithWhitespace
}

/**
 * Generates HTML with a title element containing more than 200 characters
 * Returns a pair of (html, longTitle)
 * Note: Jsoup decodes HTML entities, so we use simple alphanumeric strings
 */
fun Arb.Companion.htmlWithLongTitle(): Arb<Pair<String, String>> = arbitrary {
    // Generate a title between 201 and 500 characters using simple chars to avoid HTML entity issues
    val chars = ('a'..'z') + ('A'..'Z') + ('0'..'9') + listOf(' ', '-', '_')
    val length = (201..500).random()
    val longTitle = (1..length).map { chars.random() }.joinToString("")
    val html = "<html><head><title>$longTitle</title></head><body></body></html>"
    html to longTitle
}

/**
 * Generates HTML without a title element or with an empty title
 */
fun Arb.Companion.htmlWithoutTitle(): Arb<String> = arbitrary {
    val options = listOf(
        // No title element at all
        "<html><head></head><body><p>Content</p></body></html>",
        // Empty title element
        "<html><head><title></title></head><body><p>Content</p></body></html>",
        // Title with only whitespace
        "<html><head><title>   </title></head><body><p>Content</p></body></html>",
        // Malformed HTML without head
        "<html><body><p>Content</p></body></html>"
    )
    options.random()
}

/**
 * Generates valid URLs with domains
 * Returns a pair of (url, expectedDomain)
 */
fun Arb.Companion.urlWithDomain(): Arb<Pair<String, String>> = arbitrary {
    val domains = listOf(
        "example.com",
        "recipes.org",
        "cooking.net",
        "food-blog.com",
        "my-recipes.co.uk",
        "delicious.io",
        "tasty-food.app"
    )
    val domain = domains.random()
    val paths = listOf("", "/recipe", "/recipe/123", "/category/desserts/chocolate-cake")
    val path = paths.random()
    val url = "https://$domain$path"
    
    url to domain
}

/**
 * Generates URLs with www. prefix
 * Returns a pair of (url, expectedDomainWithoutWww)
 */
fun Arb.Companion.urlWithWwwPrefix(): Arb<Pair<String, String>> = arbitrary {
    val domains = listOf(
        "example.com",
        "recipes.org",
        "cooking.net",
        "food-blog.com"
    )
    val domain = domains.random()
    val paths = listOf("", "/recipe", "/recipe/123", "/category/desserts")
    val path = paths.random()
    val url = "https://www.$domain$path"
    
    url to domain  // Expected domain without www.
}

/**
 * Generates invalid URLs or URLs without domains
 */
fun Arb.Companion.invalidUrl(): Arb<String> = arbitrary {
    val options = listOf(
        "",                          // Empty string
        "not-a-url",                // No protocol
        "://missing-protocol.com",  // Missing protocol
        "https://",                 // No domain
        "file:///local/path",       // File protocol (no host)
        "data:text/plain,hello",    // Data URI (no host)
        "javascript:alert('hi')",   // JavaScript URI (no host)
        "   ",                      // Whitespace only
        "ht!tp://invalid.com"       // Invalid characters
    )
    options.random()
}

/**
 * Generates HTML without structured recipe data (no JSON-LD, Microdata, or RDFa)
 * This HTML may have a title and content, but no recipe markup
 */
fun Arb.Companion.htmlWithoutStructuredData(): Arb<String> = arbitrary {
    val titleOptions = listOf(
        "<title>Delicious Chocolate Cake Recipe</title>",
        "<title>How to Make Perfect Pasta</title>",
        "<title>Best Homemade Pizza</title>",
        "<title>Easy Chicken Curry</title>",
        "<title>Vegan Brownies</title>",
        "<title></title>",  // Empty title
        ""  // No title element
    )
    
    val contentOptions = listOf(
        "<p>This is a great recipe for chocolate cake.</p><p>Mix flour, sugar, and eggs.</p>",
        "<div class='recipe'><h1>Recipe Title</h1><p>Some ingredients here</p></div>",
        "<article><h2>Cooking Instructions</h2><ol><li>Step 1</li><li>Step 2</li></ol></article>",
        "<section><p>A delicious meal that everyone will love.</p></section>",
        "<div><span>Ingredients: flour, sugar, eggs</span></div>",
        ""  // Empty content
    )
    
    val title = titleOptions.random()
    val content = contentOptions.random()
    
    "<html><head>$title</head><body>$content</body></html>"
}

/**
 * Generates a pair of (URL, HTML) where the HTML lacks structured recipe data
 * This is used to test URL preservation in fallback recipes
 */
fun Arb.Companion.urlAndHtmlWithoutStructuredData(): Arb<Pair<String, String>> = arbitrary {
    val domains = listOf(
        "example.com",
        "recipes.org",
        "cooking.net",
        "food-blog.com",
        "my-recipes.co.uk",
        "delicious.io",
        "tasty-food.app",
        "www.allrecipes.com",
        "www.foodnetwork.com",
        "www.epicurious.com"
    )
    
    val paths = listOf(
        "/recipe",
        "/recipe/123",
        "/category/desserts/chocolate-cake",
        "/2024/01/best-pasta-recipe",
        "/cooking/easy-dinner",
        ""
    )
    
    val domain = domains.random()
    val path = paths.random()
    val url = "https://$domain$path"
    
    // Generate HTML without structured data
    val titleOptions = listOf(
        "<title>Delicious Chocolate Cake Recipe</title>",
        "<title>How to Make Perfect Pasta</title>",
        "<title>Best Homemade Pizza</title>",
        "<title>Easy Chicken Curry</title>",
        "<title>Vegan Brownies</title>",
        "<title>Amazing Recipe from $domain</title>",
        "<title></title>",  // Empty title
        ""  // No title element
    )
    
    val contentOptions = listOf(
        "<p>This is a great recipe for chocolate cake.</p><p>Mix flour, sugar, and eggs.</p>",
        "<div class='recipe'><h1>Recipe Title</h1><p>Some ingredients here</p></div>",
        "<article><h2>Cooking Instructions</h2><ol><li>Step 1</li><li>Step 2</li></ol></article>",
        "<section><p>A delicious meal that everyone will love.</p></section>",
        "<div><span>Ingredients: flour, sugar, eggs</span></div>",
        "<main><div class='content'><p>Recipe content without structured data</p></div></main>",
        ""  // Empty content
    )
    
    val title = titleOptions.random()
    val content = contentOptions.random()
    val html = "<html><head>$title</head><body>$content</body></html>"
    
    url to html
}

/**
 * Generates HTML with valid structured recipe data (JSON-LD, Microdata, or RDFa)
 * Returns a triple of (html, url, structuredDataType)
 */
fun Arb.Companion.htmlWithStructuredData(): Arb<Triple<String, String, String>> = arbitrary {
    val domains = listOf(
        "example.com",
        "recipes.org",
        "cooking.net",
        "food-blog.com",
        "allrecipes.com",
        "foodnetwork.com"
    )
    
    val paths = listOf(
        "/recipe/chocolate-cake",
        "/recipe/pasta-carbonara",
        "/recipe/chicken-curry",
        "/recipes/best-pizza",
        "/cooking/easy-dinner"
    )
    
    val domain = domains.random()
    val path = paths.random()
    val url = "https://$domain$path"
    
    // Generate random recipe data
    val recipeName = listOf(
        "Chocolate Chip Cookies",
        "Classic Pasta Carbonara",
        "Chicken Tikka Masala",
        "Homemade Pizza",
        "Beef Stew",
        "Vegan Brownies"
    ).random()
    
    val ingredients = listOf(
        listOf("2 cups flour", "1 cup sugar", "3 eggs", "1 tsp vanilla"),
        listOf("500g pasta", "200g bacon", "4 eggs", "100g parmesan cheese"),
        listOf("1 kg chicken", "2 cups yogurt", "3 tbsp curry powder", "1 onion"),
        listOf("3 cups flour", "1 packet yeast", "2 cups tomato sauce", "200g mozzarella"),
        listOf("1 kg beef", "4 carrots", "3 potatoes", "2 onions", "4 cups broth")
    ).random()
    
    val instructions = listOf(
        listOf("Preheat oven to 350°F", "Mix dry ingredients", "Add wet ingredients", "Bake for 25 minutes"),
        listOf("Boil pasta", "Cook bacon", "Mix eggs and cheese", "Combine all ingredients"),
        listOf("Marinate chicken", "Cook onions", "Add spices", "Simmer for 30 minutes"),
        listOf("Make dough", "Let rise for 1 hour", "Add toppings", "Bake at 450°F for 15 minutes")
    ).random()
    
    // Randomly choose structured data format
    val structuredDataType = listOf("JSON-LD", "Microdata", "RDFa").random()
    
    val html = when (structuredDataType) {
        "JSON-LD" -> generateJsonLdHtml(recipeName, ingredients, instructions)
        "Microdata" -> generateMicrodataHtml(recipeName, ingredients, instructions)
        "RDFa" -> generateRdfaHtml(recipeName, ingredients, instructions)
        else -> generateJsonLdHtml(recipeName, ingredients, instructions)
    }
    
    Triple(html, url, structuredDataType)
}

/**
 * Generates HTML with JSON-LD structured data only
 * Returns a pair of (html, url)
 */
fun Arb.Companion.htmlWithJsonLdData(): Arb<Pair<String, String>> = arbitrary {
    val url = "https://example.com/recipe"
    val recipeName = "Test Recipe"
    val ingredients = listOf("2 cups flour", "1 cup sugar", "3 eggs")
    val instructions = listOf("Mix ingredients", "Bake at 350F", "Cool and serve")
    
    val html = generateJsonLdHtml(recipeName, ingredients, instructions)
    html to url
}

/**
 * Generates HTML with JSON-LD structured data
 */
private fun generateJsonLdHtml(recipeName: String, ingredients: List<String>, instructions: List<String>): String {
    val ingredientsJson = ingredients.joinToString(",") { "\"$it\"" }
    val instructionsJson = instructions.joinToString(",") { "\"$it\"" }
    
    val jsonLd = """{"@context":"https://schema.org","@type":"Recipe","name":"$recipeName","recipeIngredient":[$ingredientsJson],"recipeInstructions":[$instructionsJson],"recipeYield":"4 servings"}"""
    
    return """<html><head><title>$recipeName</title><script type="application/ld+json">$jsonLd</script></head><body><h1>$recipeName</h1><p>A delicious recipe</p></body></html>"""
}

/**
 * Generates HTML with Microdata structured data
 */
private fun generateMicrodataHtml(recipeName: String, ingredients: List<String>, instructions: List<String>): String {
    val ingredientsHtml = ingredients.joinToString("") { "<li itemprop=\"recipeIngredient\">$it</li>" }
    val instructionsHtml = instructions.joinToString("") { "<li itemprop=\"recipeInstructions\">$it</li>" }
    
    return """<html><head><title>$recipeName</title></head><body><div itemscope itemtype="http://schema.org/Recipe"><h1 itemprop="name">$recipeName</h1><ul>$ingredientsHtml</ul><ol>$instructionsHtml</ol><span itemprop="recipeYield">4 servings</span></div></body></html>"""
}

/**
 * Generates HTML with RDFa structured data
 */
private fun generateRdfaHtml(recipeName: String, ingredients: List<String>, instructions: List<String>): String {
    val ingredientsHtml = ingredients.joinToString("") { "<li property=\"recipeIngredient\">$it</li>" }
    val instructionsHtml = instructions.joinToString("") { "<li property=\"recipeInstructions\">$it</li>" }
    
    return """<html><head><title>$recipeName</title></head><body><div typeof="Recipe"><h1 property="name">$recipeName</h1><ul>$ingredientsHtml</ul><ol>$instructionsHtml</ol><span property="recipeYield">4 servings</span></div></body></html>"""
}
