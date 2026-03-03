# Design Document: Recipe Bookmarking App

## Overview

The Recipe Bookmarking App is an Android mobile application that enables users to save, organize, and view recipes from various online sources. The app provides a comprehensive recipe viewing experience with ingredient scaling, categorization, search functionality, and both automatic URL-based import and manual data entry capabilities.

The application follows a clean architecture pattern with clear separation between UI, business logic, and data persistence layers. The core functionality centers around recipe data management, with features for importing recipes from shared URLs, displaying recipe details with scaling capabilities, and organizing recipes through categories and search.

Key technical considerations include:
- Android share target integration for seamless URL sharing from browsers and other apps
- Web scraping and parsing for automatic recipe data extraction
- Local data persistence using Room database
- Reactive UI updates using LiveData/Flow patterns
- Ingredient quantity calculation for scaling operations

## Architecture

The application follows a layered architecture with the following components:

### Presentation Layer
- **RecipeListActivity**: Main screen displaying all bookmarked recipes with search and filter capabilities
- **RecipeDetailActivity**: Detailed view of a single recipe with scaling controls
- **ManualEntryActivity**: Form-based interface for manual recipe data entry
- **ImportService**: Background service handling shared URL processing

### Branding and Visual Assets
- **App Icon**: Uses kitchen-icon.png as the launcher icon across all density buckets (mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi)
- **Home Page Header**: Displays kitchen-icon.png as the app branding element on the RecipeListActivity

### Business Logic Layer
- **RecipeRepository**: Manages recipe data operations and coordinates between data sources
- **RecipeParser**: Extracts structured recipe data from HTML content
- **ScalingCalculator**: Computes scaled ingredient quantities based on selected factors
- **SearchFilter**: Implements recipe search and category filtering logic

### Data Layer
- **RecipeDatabase**: Room database for local recipe persistence
- **RecipeDao**: Data access object for recipe CRUD operations
- **NetworkClient**: HTTP client for fetching webpage content from URLs

### Data Flow
1. URL sharing triggers ImportService
2. ImportService fetches webpage content via NetworkClient
3. RecipeParser extracts structured data from HTML
4. RecipeRepository persists data via RecipeDao
5. UI observes repository changes and updates display

## Components and Interfaces

### RecipeRepository
```kotlin
interface RecipeRepository {
    fun getAllRecipes(): Flow<List<Recipe>>
    fun getRecipeById(id: Long): Flow<Recipe?>
    fun searchRecipes(query: String): Flow<List<Recipe>>
    fun getRecipesByCategory(category: Category): Flow<List<Recipe>>
    suspend fun insertRecipe(recipe: Recipe): Long
    suspend fun updateRecipe(recipe: Recipe)
    suspend fun deleteRecipe(id: Long)
    suspend fun importFromUrl(url: String): ImportResult
}
```

### RecipeParser
```kotlin
interface RecipeParser {
    suspend fun parseRecipe(html: String, sourceUrl: String): ParseResult
}

sealed class ParseResult {
    data class Success(val recipe: Recipe) : ParseResult()
    data class Failure(val error: ParseError) : ParseResult()
}

enum class ParseError {
    NO_RECIPE_DATA,
    INVALID_HTML,
    MISSING_REQUIRED_FIELDS
}
```

### ScalingCalculator
```kotlin
interface ScalingCalculator {
    fun scaleIngredients(
        ingredients: List<Ingredient>,
        scalingFactor: ScalingFactor
    ): List<ScaledIngredient>
}

enum class ScalingFactor(val multiplier: Double) {
    SINGLE(1.0),
    ONE_AND_HALF(1.5),
    DOUBLE(2.0)
}
```

### ImportService
```kotlin
class ImportService : IntentService() {
    suspend fun handleSharedUrls(urls: List<String>): ImportSummary
}

data class ImportSummary(
    val successCount: Int,
    val failureCount: Int,
    val failures: List<ImportFailure>
)

data class ImportFailure(
    val url: String,
    val error: ImportError
)

enum class ImportError {
    URL_INACCESSIBLE,
    PARSE_FAILED,
    NETWORK_ERROR
}
```

## Data Models

### Recipe
```kotlin
@Entity(tableName = "recipes")
data class Recipe(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val ingredients: List<Ingredient>,
    val instructions: List<Instruction>,
    val yield: String?,
    val servingSize: String?,
    val nutritionInfo: NutritionInfo?,
    val originalUrl: String?,
    val category: Category?,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
```

### Ingredient
```kotlin
data class Ingredient(
    val name: String,
    val quantity: Double,
    val unit: String,
    val order: Int
)

data class ScaledIngredient(
    val name: String,
    val originalQuantity: Double,
    val scaledQuantity: Double,
    val unit: String,
    val order: Int
)
```

### Instruction
```kotlin
data class Instruction(
    val text: String,
    val order: Int
)
```

### NutritionInfo
```kotlin
data class NutritionInfo(
    val calories: Int?,
    val protein: String?,
    val carbohydrates: String?,
    val fat: String?,
    val fiber: String?,
    val sugar: String?
)
```

### Category
```kotlin
enum class Category {
    BREAKFAST,
    LUNCH,
    DINNER,
    DESSERT,
    DRINK,
    SAUCE,
    UNCATEGORIZED
}
```


## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Recipe persistence round-trip

*For any* recipe with all its data (name, ingredients, instructions, yield, serving size, nutrition info, URL, category), saving it to the database and then retrieving it should produce an equivalent recipe with all fields preserved.

**Validates: Requirements 7.1, 7.2, 7.3**

### Property 2: Recipe list completeness

*For any* collection of recipes, querying all recipes from the repository should return every recipe that was previously saved, with no recipes missing.

**Validates: Requirements 1.1**

### Property 3: Recipe selection navigation

*For any* recipe in the displayed list, selecting that recipe should result in displaying the full details of that specific recipe.

**Validates: Requirements 1.2**

### Property 4: Recipe display includes name

*For any* recipe displayed in the list view, the display should include the recipe's name.

**Validates: Requirements 1.3**

### Property 5: Ingredient list completeness

*For any* recipe with ingredients, displaying the recipe should show all ingredients with no ingredients omitted.

**Validates: Requirements 2.1**

### Property 6: Ingredient display includes quantity and unit

*For any* ingredient in a displayed recipe, the ingredient display should include both the quantity value and the unit of measurement.

**Validates: Requirements 2.2**

### Property 7: Ingredient order preservation

*For any* recipe with ordered ingredients, the display order of ingredients should match the stored order specified in the recipe data.

**Validates: Requirements 2.3**

### Property 8: Yield information display

*For any* recipe with yield information, displaying the recipe should show the yield data.

**Validates: Requirements 3.1**

### Property 9: Serving size information display

*For any* recipe with serving size information, displaying the recipe should show the serving size data.

**Validates: Requirements 3.2**

### Property 10: Yield format display

*For any* recipe with yield information, the yield should be displayed as a number of servings or quantity produced.

**Validates: Requirements 3.3**

### Property 11: Nutrition information display when available

*For any* recipe with nutrition information, displaying the recipe should show the nutritional data including calories and macronutrients.

**Validates: Requirements 4.1, 4.3**

### Property 12: Nutrition unavailable indication

*For any* recipe without nutrition information, displaying the recipe should indicate that nutritional data is unavailable.

**Validates: Requirements 4.2**

### Property 13: Instruction list completeness

*For any* recipe with instructions, displaying the recipe should show all preparation instructions with no instructions omitted.

**Validates: Requirements 5.1**

### Property 14: Instruction order preservation

*For any* recipe with ordered instructions, the display order of instructions should match the stored sequential order specified in the recipe data.

**Validates: Requirements 5.2**

### Property 15: Instruction step distinction

*For any* recipe with multiple instructions, each instruction should be displayed as a distinct, separately identifiable step.

**Validates: Requirements 5.3**

### Property 16: Original link display

*For any* recipe with an original URL, displaying the recipe should show the original recipe link.

**Validates: Requirements 6.1**

### Property 17: Original link opens browser

*For any* recipe with an original URL, selecting the original link should trigger opening that URL in a web browser.

**Validates: Requirements 6.2**

### Property 18: Original link is clickable

*For any* recipe with an original URL, the displayed link should be rendered as a clickable element.

**Validates: Requirements 6.3**

### Property 19: Ingredient scaling calculation

*For any* recipe with ingredients and any scaling factor (1.0x, 1.5x, or 2.0x), applying the scaling factor should recalculate all ingredient quantities by multiplying each original quantity by the scaling factor's multiplier.

**Validates: Requirements 8.2**

### Property 20: Scaled quantities display

*For any* recipe with a non-default scaling factor applied, the displayed ingredient quantities should show the scaled values rather than the original values.

**Validates: Requirements 8.3**

### Property 21: Scaling notification display

*For any* recipe with a scaling factor other than 1.0x applied, the instruction section should display a notification informing users that instructions may need modification.

**Validates: Requirements 8.4**

### Property 22: Current scaling factor display

*For any* recipe being displayed, the currently selected scaling factor should be visible to the user.

**Validates: Requirements 8.5**

### Property 23: Default scaling factor

*For any* recipe when first displayed, the scaling factor should default to 1.0x.

**Validates: Requirements 8.6**

### Property 24: Category assignment and persistence

*For any* recipe and any valid category, assigning the category to the recipe and then retrieving the recipe should show the assigned category.

**Validates: Requirements 9.2, 9.3, 9.4, 9.5**

### Property 25: Category filter accuracy

*For any* selected category and any collection of recipes, applying the category filter should return only recipes tagged with that specific category, excluding all recipes with different or no categories.

**Validates: Requirements 9.7**

### Property 26: No filter shows all recipes

*For any* collection of recipes, when no category filter is selected and the search query is empty, all bookmarked recipes should be displayed.

**Validates: Requirements 9.8, 10.3, 10.6**

### Property 27: Category display in list

*For any* recipe with an assigned category displayed in the list view, the recipe's category tag should be shown.

**Validates: Requirements 9.9**

### Property 28: Uncategorized indication

*For any* recipe without an assigned category, the display should indicate that the recipe is uncategorized.

**Validates: Requirements 9.10**

### Property 29: Search filter accuracy

*For any* search query and any collection of recipes, the filtered results should include only recipes whose names contain the search query as a substring.

**Validates: Requirements 10.2**

### Property 30: Case-insensitive search

*For any* search query, changing the case of letters in the query should not affect which recipes are returned in the search results.

**Validates: Requirements 10.4**

### Property 31: Empty search results message

*For any* search query that matches no recipes in the collection, the display should show a message indicating no results were found.

**Validates: Requirements 10.5**

### Property 32: Single URL acceptance

*For any* valid URL shared to the app, the app should accept and process the shared URL.

**Validates: Requirements 11.2**

### Property 33: Multiple URL acceptance

*For any* list of URLs shared to the app, the app should accept and process all shared URLs.

**Validates: Requirements 11.3**

### Property 34: URL triggers fetch

*For any* URL received by the app, receiving the URL should trigger a fetch operation to retrieve the webpage content.

**Validates: Requirements 11.4**

### Property 35: Recipe parsing extracts required fields

*For any* valid recipe HTML content, the parser should extract at minimum the recipe name, ingredients list, and instructions list.

**Validates: Requirements 11.5**

### Property 36: Successful import adds to list

*For any* successfully parsed recipe, the recipe should appear in the bookmarked recipes list after import.

**Validates: Requirements 11.6**

### Property 37: Import preserves source URL

*For any* recipe imported from a URL, the stored recipe should contain the original URL as the Original_Link field.

**Validates: Requirements 11.7**

### Property 38: Import success confirmation

*For any* successfully imported or manually saved recipe, a confirmation message should be displayed to the user.

**Validates: Requirements 11.9, 12.21**

### Property 39: Inaccessible URL error message

*For any* URL that cannot be fetched due to network or access issues, an error message should be displayed indicating the URL is inaccessible.

**Validates: Requirements 11.10**

### Property 40: Parse failure error message

*For any* webpage that does not contain recognizable recipe data, an error message should be displayed indicating the page does not contain recipe data.

**Validates: Requirements 11.11**

### Property 41: Multi-URL import summary accuracy

*For any* import session with multiple URLs, the displayed summary should show counts that match the actual number of successfully imported recipes and failed imports.

**Validates: Requirements 11.12**

### Property 42: Import continues after errors

*For any* multi-URL import session with one or more failures, all remaining URLs should still be processed and attempted.

**Validates: Requirements 11.13**

### Property 43: Manual entry offered on failure

*For any* URL that fails to fetch or fails to parse, the app should offer the user an option to manually enter the recipe data.

**Validates: Requirements 12.1, 12.2**

### Property 44: Manual entry triggers form display

*For any* user choice to manually enter recipe data, the Manual_Entry_Form should be displayed.

**Validates: Requirements 12.3**

### Property 45: Ingredient addition captures all fields

*For any* ingredient added through the manual entry form, the app should capture the ingredient name, quantity, and unit of measurement.

**Validates: Requirements 12.6**

### Property 46: Multiple items addition

*For any* sequence of ingredients or instructions added to the manual entry form, all items should be successfully added to their respective lists.

**Validates: Requirements 12.7, 12.11**

### Property 47: Item removal from lists

*For any* ingredient or instruction in the manual entry form lists, removing that item should result in it no longer appearing in the list.

**Validates: Requirements 12.8, 12.12**

### Property 48: List reordering preserves all items

*For any* list of ingredients or instructions in the manual entry form, reordering the list should preserve all items with no items lost or duplicated.

**Validates: Requirements 12.13, 12.14**

### Property 49: Optional URL entry

*For any* manual recipe entry, entering an original URL should be optional and the recipe should be saveable with or without a URL.

**Validates: Requirements 12.15**

### Property 50: Empty name validation failure

*For any* manual recipe entry with an empty or whitespace-only name, validation should fail and prevent saving.

**Validates: Requirements 12.16**

### Property 51: Empty ingredients validation failure

*For any* manual recipe entry with zero ingredients, validation should fail and prevent saving.

**Validates: Requirements 12.17**

### Property 52: Empty instructions validation failure

*For any* manual recipe entry with zero instructions, validation should fail and prevent saving.

**Validates: Requirements 12.18**

### Property 53: Validation error messages

*For any* manual recipe entry that fails validation, an error message should be displayed indicating which required fields are missing.

**Validates: Requirements 12.19**

### Property 54: Valid manual entry adds to list

*For any* manual recipe entry that passes validation (has name, at least one ingredient, and at least one instruction), saving should add the recipe to the bookmarked recipes list.

**Validates: Requirements 12.20**

### Property 55: Manual entry cancellation

*For any* manual entry session, canceling the entry should not save the recipe to the bookmarked recipes list.

**Validates: Requirements 12.22**

## Error Handling

The application implements comprehensive error handling across all major operations:

### Network Errors
- **Connection failures**: Display user-friendly error messages when URLs cannot be fetched
- **Timeout handling**: Implement reasonable timeout values (30 seconds) for network requests
- **Retry logic**: Allow users to retry failed imports without re-entering URLs

### Parsing Errors
- **Invalid HTML**: Gracefully handle malformed HTML without crashing
- **Missing recipe data**: Detect when required recipe fields are absent and offer manual entry
- **Partial data**: Accept recipes with optional fields missing (nutrition info, yield, etc.)

### Validation Errors
- **Empty required fields**: Prevent saving recipes without name, ingredients, or instructions
- **Invalid quantities**: Validate that ingredient quantities are positive numbers
- **Duplicate recipes**: Optionally detect and warn about duplicate recipe names

### Database Errors
- **Write failures**: Handle database write errors with user notification and retry options
- **Corruption recovery**: Implement database integrity checks on app startup
- **Migration errors**: Safely handle schema migrations between app versions

### UI Errors
- **Invalid scaling factors**: Constrain scaling factor selection to valid options (1.0x, 1.5x, 2.0x)
- **Empty search results**: Display helpful messages when searches return no results
- **Navigation errors**: Handle cases where recipe IDs become invalid

## Testing Strategy

The testing strategy employs a dual approach combining unit tests for specific scenarios and property-based tests for comprehensive coverage.

### Unit Testing

Unit tests focus on:
- **Specific examples**: Concrete test cases demonstrating correct behavior (e.g., scaling 2 cups by 1.5x yields 3 cups)
- **Edge cases**: Boundary conditions like empty lists, zero quantities, null values
- **Integration points**: Interactions between components (e.g., repository and database)
- **Error conditions**: Specific error scenarios like network failures, parse errors, validation failures

Unit tests should be written using JUnit 5 and MockK for mocking dependencies.

### Property-Based Testing

Property-based tests verify universal properties across randomized inputs using Kotest property testing framework. Each property test should:
- Run a minimum of 100 iterations to ensure comprehensive input coverage
- Reference the corresponding design document property in a comment tag
- Use appropriate generators for domain objects (recipes, ingredients, categories, etc.)

**Tag format**: `// Feature: recipe-bookmarks, Property {number}: {property_text}`

**Example property test**:
```kotlin
// Feature: recipe-bookmarks, Property 1: Recipe persistence round-trip
class RecipePersistencePropertyTest : StringSpec({
    "recipe persistence round-trip" {
        checkAll(100, Arb.recipe()) { recipe ->
            val savedId = repository.insertRecipe(recipe)
            val retrieved = repository.getRecipeById(savedId).first()
            retrieved shouldBe recipe.copy(id = savedId)
        }
    }
})
```

### Property-Based Testing Library

The application will use **Kotest Property Testing** (kotest-property) as the property-based testing library for Kotlin/Android. Kotest provides:
- Built-in generators (Arb) for primitive types
- Composable generators for custom domain objects
- Configurable iteration counts
- Integration with JUnit 5

### Test Coverage Goals

- **Unit test coverage**: Minimum 80% code coverage for business logic and data layers
- **Property test coverage**: All 55 correctness properties implemented as property-based tests
- **UI testing**: Critical user flows tested with Espresso (recipe list, detail view, manual entry)
- **Integration testing**: End-to-end tests for import flow and persistence operations

### Testing Priorities

1. **Critical path**: Recipe import, display, and persistence (Properties 1-7, 32-37)
2. **Data integrity**: Scaling calculations, order preservation, field completeness (Properties 19-23, 7, 14)
3. **User input validation**: Manual entry validation and error handling (Properties 50-55)
4. **Search and filtering**: Category and search filter accuracy (Properties 25-31)
