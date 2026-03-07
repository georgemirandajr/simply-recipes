# Integration Tests for Fallback Recipe Import Feature

This directory contains comprehensive integration tests for the fallback recipe import feature. These tests validate end-to-end workflows with real components (no mocks).

## Test Files

### 1. ImportFlowIntegrationTest.kt
Tests the import flow with real URLs to verify fallback recipe creation.

**Requirements Tested:** 1.1, 1.2, 1.3, 3.1, 3.2

**Test Cases:**
- `testImportFromJoyFoodSunshine()` - Tests import from joyfoodsunshine.com
- `testImportFromSeriousEats()` - Tests import from seriouseats.com
- `testImportFromBowlOfDelicious()` - Tests import from bowlofdelicious.com
- `testImportFromTheFreshCooky()` - Tests import from thefreshcooky.com
- `testImportFromEpicurious()` - Tests import from epicurious.com
- `testImportFromBonAppetit()` - Tests import from bonappetit.com
- `testImportFromFoodNetwork()` - Tests import from foodnetwork.com
- `testImportFromDelish()` - Tests import from delish.com
- `testAllTestUrlsInBatch()` - Tests batch import of all 8 URLs

**What is Tested:**
- Fallback recipes are created when structured data is missing
- Recipe names are extracted from page titles
- Original URLs are preserved and clickable
- Recipes are saved to the database

### 2. RecipeEditFlowIntegrationTest.kt
Tests the recipe editing flow for both fallback and non-fallback recipes.

**Requirements Tested:** 7.1, 7.2, 7.3, 7.6, 7.7, 7.8, 7.9, 7.10, 7.11, 7.12, 7.13

**Test Cases:**
- `testEditFallbackRecipe_AllFieldsEditable()` - Verifies all fields can be edited for fallback recipes
- `testEditFallbackRecipe_AddIngredients()` - Tests adding ingredients to fallback recipes
- `testEditFallbackRecipe_ModifyIngredients()` - Tests modifying existing ingredients
- `testEditFallbackRecipe_RemoveIngredients()` - Tests removing ingredients
- `testEditFallbackRecipe_AddInstructions()` - Tests adding instructions
- `testEditFallbackRecipe_ModifyInstructions()` - Tests modifying instructions
- `testEditFallbackRecipe_RemoveInstructions()` - Tests removing instructions
- `testEditFallbackRecipe_UpdatedAtTimestamp()` - Verifies timestamp updates on edit
- `testEditNonFallbackRecipe_OnlyNameEditable()` - Verifies only name can be edited for non-fallback recipes
- `testCompleteEditFlow_ImportEditSave()` - Tests complete flow from import to edit to save

**What is Tested:**
- Fallback recipes allow editing all fields (name, ingredients, instructions, yield)
- Non-fallback recipes only allow name editing
- Changes persist to the database
- Updated timestamp is updated on save

### 3. RecipeDeleteFlowIntegrationTest.kt
Tests the recipe deletion flow from both detail view and list view.

**Requirements Tested:** 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 8.7, 8.8, 8.9, 8.10

**Test Cases:**
- `testDeleteRecipeFromDetail_RemovesFromDatabase()` - Tests deletion from detail view
- `testDeleteRecipeFromDetail_QueryReturnsNull()` - Verifies deleted recipe returns null
- `testDeleteFallbackRecipe_RemovesFromDatabase()` - Tests deleting fallback recipes
- `testDeleteRecipeFromList_RemovesFromDatabase()` - Tests deletion from list view
- `testDeleteRecipeFromList_UpdatesList()` - Verifies list updates after deletion
- `testDeleteCancellation_RecipeRemainsInDatabase()` - Tests canceling deletion
- `testDeleteCancellation_RecipeRemainsVisible()` - Verifies recipe remains visible after cancel
- `testDeleteMultipleRecipes_AllRemoved()` - Tests deleting multiple recipes
- `testDeleteNonExistentRecipe_NoError()` - Tests deleting non-existent recipe doesn't error

**What is Tested:**
- Recipes are removed from database on deletion
- Deleted recipes return null when queried
- Canceling deletion keeps recipe in database
- Multiple recipes can be deleted
- Deleting non-existent recipes doesn't throw errors

### 4. EndToEndFlowIntegrationTest.kt
Tests complete end-to-end user workflows combining import, view, edit, and delete operations.

**Test Cases:**
- `testEndToEndFlow_ImportViewEditSave()` - Tests import → view → edit → save flow
- `testEndToEndFlow_ImportViewDelete()` - Tests import → view → delete flow
- `testEndToEndFlow_ImportDeleteFromList()` - Tests import → delete from list flow
- `testEndToEndFlow_ImportMultipleEditOneDeleteAnother()` - Tests complex multi-recipe workflow
- `testEndToEndFlow_ImportEditMultipleTimesSave()` - Tests multiple edits on same recipe
- `testEndToEndFlow_ImportViewEditCancelDelete()` - Tests edit cancellation then delete
- `testEndToEndFlow_CompleteUserJourney()` - Tests complete user journey with multiple recipes

**What is Tested:**
- Complete workflows from import to final state
- Multiple operations on same recipe
- Multiple recipes with different operations
- Complex user journeys with mixed operations

## Running the Tests

### Prerequisites
- Android device or emulator running API 24+
- Internet connection (for ImportFlowIntegrationTest.kt)

### Run All Integration Tests
```bash
./gradlew connectedDebugAndroidTest
```

### Run Specific Test Class
```bash
./gradlew connectedDebugAndroidTest --tests "com.recipebookmarks.integration.ImportFlowIntegrationTest"
./gradlew connectedDebugAndroidTest --tests "com.recipebookmarks.integration.RecipeEditFlowIntegrationTest"
./gradlew connectedDebugAndroidTest --tests "com.recipebookmarks.integration.RecipeDeleteFlowIntegrationTest"
./gradlew connectedDebugAndroidTest --tests "com.recipebookmarks.integration.EndToEndFlowIntegrationTest"
```

### Run Specific Test Method
```bash
./gradlew connectedDebugAndroidTest --tests "com.recipebookmarks.integration.ImportFlowIntegrationTest.testImportFromJoyFoodSunshine"
```

## Test Setup

All tests use:
- **In-memory database**: Tests use `RecipeDatabase.getDatabase(context, inMemory = true)` for isolation
- **Real components**: No mocks - uses actual NetworkClient, RecipeParser, and Repository
- **Clean state**: Each test starts with a fresh database via `@Before` setup
- **Cleanup**: Database is closed after each test via `@After` tearDown

## Notes

### ImportFlowIntegrationTest.kt
- These tests make real network calls to external websites
- Tests may fail if websites are down or change their structure
- Tests verify that fallback recipes are created when structured data is missing
- Some websites may have structured data, resulting in non-fallback recipes

### Network Requirements
- ImportFlowIntegrationTest requires internet connection
- Other tests (Edit, Delete, EndToEnd) work offline with in-memory database

### Test Isolation
- Each test uses a fresh in-memory database
- Tests are independent and can run in any order
- No test data persists between tests

## Coverage

These integration tests provide comprehensive coverage of:
- ✅ Import flow with real URLs (Task 15.1)
- ✅ Edit flow for fallback recipes (Task 15.2)
- ✅ Edit flow for non-fallback recipes (Task 15.3)
- ✅ Delete flow from recipe detail (Task 15.4)
- ✅ Delete flow from recipe list (Task 15.5)
- ✅ Delete cancellation (Task 15.6)
- ✅ End-to-end workflows (Task 15.7)

All requirements from the fallback-recipe-import spec are validated through these integration tests.
