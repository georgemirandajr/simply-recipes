# Implementation Plan: Fallback Recipe Import

## Overview

This implementation adds a fallback mechanism for recipe imports when structured data parsing fails. The system will create minimal recipe bookmarks using page titles and URLs, allowing users to save recipes from any website. The implementation also adds recipe editing and deletion capabilities.

## Tasks

- [x] 1. Update Recipe entity and database schema
  - Add isFallback boolean field with default value false to Recipe entity
  - Add @ColumnInfo annotation for is_fallback column
  - Ensure ingredients and instructions lists support empty defaults
  - Verify Room handles the schema change without requiring migration
  - _Requirements: 1.1, 1.5, 1.6, 5.3, 5.4_

- [x] 1.1 Write property test for Recipe entity
  - **Property 8: Empty List Support in Data Model**
  - **Validates: Requirements 5.3**

- [x] 2. Implement RecipeParser fallback logic
  - [x] 2.1 Add extractPageTitle method to RecipeParser interface and RecipeParserImpl
    - Use Jsoup to extract title element text
    - Trim whitespace from extracted title
    - Truncate title to 200 characters if longer
    - Return empty string if title is unavailable
    - _Requirements: 1.3, 2.1, 2.3, 2.4_

  - [x] 2.2 Write property test for page title extraction
    - **Property 3: Recipe Name Extraction from Page Title**
    - **Validates: Requirements 1.3, 2.1, 2.3, 2.4**

  - [x] 2.3 Add generateNameFromUrl helper method
    - Extract domain name from URL using Uri.parse
    - Remove "www." prefix if present
    - Return "Untitled Recipe" if domain extraction fails
    - _Requirements: 1.4, 2.2_

  - [x] 2.4 Write property test for domain name fallback
    - **Property 6: Domain Name Fallback for Missing Titles**
    - **Validates: Requirements 1.4, 2.2**

  - [x] 2.5 Add createFallbackRecipe helper method
    - Call extractPageTitle to get page title
    - If title is blank, call generateNameFromUrl
    - Create Recipe with isFallback=true, empty ingredients/instructions
    - Set originalUrl field
    - Return ParseResult.Success with fallback recipe
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6_


  - [x] 2.6 Write property test for fallback recipe creation
    - **Property 1: Fallback Recipe Creation on Parse Failure**
    - **Validates: Requirements 1.1**

  - [x] 2.7 Write property test for URL preservation
    - **Property 2: Fallback Recipe URL Preservation**
    - **Validates: Requirements 1.2**

  - [x] 2.8 Write property test for empty lists in fallback recipes
    - **Property 4: Fallback Recipes Have Empty Lists**
    - **Validates: Requirements 1.5, 1.6**

  - [x] 2.9 Modify parseRecipe method to use fallback mechanism
    - Try JSON-LD, Microdata, and RDFa parsing in sequence
    - If all parsing methods fail, call createFallbackRecipe
    - Wrap entire method in try-catch and call createFallbackRecipe on exception
    - _Requirements: 1.1, 5.1_

  - [x] 2.10 Write property test for structured data parsing preservation
    - **Property 7: Structured Data Parsing Preserved**
    - **Validates: Requirements 5.1**

  - [x] 2.11 Write unit tests for RecipeParser edge cases
    - Test empty HTML
    - Test malformed HTML
    - Test very long titles (>200 characters)
    - Test titles with only whitespace
    - Test URLs without domain
    - Test exception handling

- [x] 3. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 4. Update ImportService to track fallback recipes
  - [x] 4.1 Add fallbackCount field to ImportSummary data class
    - Add fallbackCount: Int = 0 field
    - Update constructor and serialization
    - _Requirements: 4.1_

  - [x] 4.2 Modify handleSharedUrls to track fallback recipes
    - Create mutableList to track fallback URLs
    - Check if imported recipe has isFallback=true
    - Add URL to fallback list if true
    - Set fallbackCount in ImportSummary
    - _Requirements: 4.1_

  - [x] 4.3 Write property test for fallback recipe persistence
    - **Property 5: Fallback Recipe Persistence**
    - **Validates: Requirements 1.7**

  - [x] 4.4 Write unit tests for ImportService
    - Test import summary calculation with fallback recipes
    - Test mixed results (success, fallback, failure)
    - Test all success with no fallbacks
    - Test all fallbacks


- [ ] 5. Update ImportNotificationHelper for fallback messages
  - Modify showImportSummary to handle fallbackCount
  - Add string resource for import_success_with_fallbacks message
  - Add string resource for import_mixed_results message
  - Show appropriate message based on success/fallback/failure counts
  - _Requirements: 4.2, 4.3, 4.4_

- [ ] 5.1 Write unit tests for ImportNotificationHelper
  - Test message selection with no fallbacks
  - Test message selection with only fallbacks
  - Test message selection with mixed results

- [x] 6. Add repository methods for update and delete
  - [x] 6.1 Add updateRecipe method to RecipeRepository interface
    - Define suspend fun updateRecipe(recipe: Recipe)
    - _Requirements: 7.12_

  - [x] 6.2 Implement updateRecipe in RecipeRepositoryImpl
    - Call recipeDao.update with recipe
    - Update updatedAt timestamp to current time
    - _Requirements: 7.12_

  - [x] 6.3 Add deleteRecipe method to RecipeRepository interface
    - Define suspend fun deleteRecipe(id: Long)
    - _Requirements: 8.7_

  - [x] 6.4 Implement deleteRecipe in RecipeRepositoryImpl
    - Call recipeDao.deleteById with recipe id
    - _Requirements: 8.7_

  - [x] 6.5 Add update and deleteById methods to RecipeDao
    - Add @Update annotation for update method
    - Add @Query("DELETE FROM recipes WHERE id = :id") for deleteById
    - _Requirements: 7.12, 8.7_

  - [x] 6.6 Write property test for recipe edit persistence
    - **Property 12: Recipe Edit Persistence**
    - **Validates: Requirements 7.12**

  - [x] 6.7 Write property test for recipe deletion confirmation
    - **Property 13: Recipe Deletion Confirmation**
    - **Validates: Requirements 8.7**

  - [x] 6.8 Write property test for recipe deletion cancellation
    - **Property 14: Recipe Deletion Cancellation**
    - **Validates: Requirements 8.8**

  - [x] 6.9 Write unit tests for repository methods
    - Test updateRecipe updates timestamp
    - Test deleteRecipe removes recipe from database
    - Test querying deleted recipe returns null

- [x] 7. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.


- [x] 8. Create RecipeEditorActivity and ViewModel
  - [x] 8.1 Create activity_recipe_editor.xml layout
    - Add EditText for recipe name
    - Add ScrollView with LinearLayout for ingredients container
    - Add ScrollView with LinearLayout for instructions container
    - Add Button for adding ingredients
    - Add Button for adding instructions
    - Add EditText fields for yield and nutrition info
    - Add Save and Cancel buttons
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

  - [x] 8.2 Create RecipeEditorViewModel
    - Add recipeId parameter to constructor
    - Create StateFlow for recipe
    - Load recipe from repository in init block
    - Add saveRecipe method that calls repository.updateRecipe
    - _Requirements: 7.12_

  - [x] 8.3 Create RecipeEditorViewModelFactory
    - Accept RecipeRepository and recipeId parameters
    - Create RecipeEditorViewModel instance
    - _Requirements: 7.12_

  - [x] 8.4 Implement RecipeEditorActivity
    - Initialize ViewModel with factory
    - Initialize all view references
    - Observe recipe StateFlow and populate fields
    - Call configureEditingRestrictions based on isFallback
    - Set up button click listeners
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.13_

  - [x] 8.5 Implement populateFields method
    - Set recipe name in EditText
    - Dynamically add ingredient fields for each ingredient
    - Dynamically add instruction fields for each instruction
    - Set yield and nutrition info if available
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

  - [x] 8.6 Implement configureEditingRestrictions method
    - If isFallback=true, enable all fields and show add buttons
    - If isFallback=false, enable only name field and hide add buttons
    - Disable ingredient/instruction fields for non-fallback recipes
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.13_

  - [x] 8.7 Implement addIngredientField method
    - Create EditText fields for ingredient name, quantity, unit
    - Add remove button for each ingredient
    - Add views to ingredients container
    - _Requirements: 7.6, 7.7, 7.8_

  - [x] 8.8 Implement addInstructionField method
    - Create EditText field for instruction text
    - Add remove button for each instruction
    - Add views to instructions container
    - _Requirements: 7.9, 7.10, 7.11_


  - [x] 8.9 Implement saveRecipe method
    - Collect recipe name from EditText
    - Collect ingredients from dynamic fields
    - Collect instructions from dynamic fields
    - Collect yield and nutrition info
    - Validate all fields (non-empty name, valid quantities)
    - Create updated Recipe object
    - Call viewModel.saveRecipe
    - Navigate back to previous activity
    - _Requirements: 7.12_

  - [x] 8.10 Write property test for recipe name editing
    - **Property 9: Recipe Name Editing for All Recipes**
    - **Validates: Requirements 7.1**

  - [x] 8.11 Write property test for ingredient list editing
    - **Property 10: Ingredient List Editing Operations**
    - **Validates: Requirements 7.6, 7.7, 7.8**

  - [x] 8.12 Write property test for instruction list editing
    - **Property 11: Instruction List Editing Operations**
    - **Validates: Requirements 7.9, 7.10, 7.11**

  - [x] 8.13 Write unit tests for RecipeEditorActivity
    - Test field population for fallback recipes
    - Test field population for non-fallback recipes
    - Test editing restrictions for fallback recipes
    - Test editing restrictions for non-fallback recipes
    - Test validation errors

- [x] 9. Update RecipeDetailActivity for edit and delete
  - [x] 9.1 Add Edit and Delete buttons to activity_recipe_detail.xml
    - Add Button for editing recipe
    - Add Button for deleting recipe
    - Add TextView for fallback message
    - _Requirements: 3.3, 7.1, 8.2_

  - [x] 9.2 Add deleteRecipe method to RecipeDetailViewModel
    - Call repository.deleteRecipe with recipeId
    - _Requirements: 8.7_

  - [x] 9.3 Implement edit button click listener
    - Create Intent for RecipeEditorActivity
    - Pass recipeId as extra
    - Start activity
    - _Requirements: 7.1_

  - [x] 9.4 Implement displayFallbackMessage method
    - Check if recipe.isFallback is true
    - Show fallback message TextView if true
    - Set message text from string resource
    - Hide TextView if false
    - _Requirements: 3.3_

  - [x] 9.5 Implement showDeleteConfirmation method
    - Create AlertDialog with recipe name in message
    - Add positive button that calls viewModel.deleteRecipe and finish()
    - Add negative button that dismisses dialog
    - _Requirements: 8.3, 8.4, 8.5, 8.6, 8.9_


  - [x] 9.6 Wire edit and delete buttons in onCreate
    - Initialize button references
    - Set click listeners
    - Observe recipe to display fallback message
    - _Requirements: 3.3, 7.1, 8.2_

  - [x] 9.7 Write unit tests for RecipeDetailActivity
    - Test edit button launches RecipeEditorActivity
    - Test delete button shows confirmation dialog
    - Test delete confirmation removes recipe and navigates back
    - Test delete cancellation keeps recipe
    - Test fallback message visibility

- [x] 10. Update RecipeListAdapter for fallback indicators and delete
  - [x] 10.1 Add fallback indicator and delete button to item_recipe.xml
    - Add TextView for fallback indicator
    - Add ImageButton for delete action
    - Style appropriately
    - _Requirements: 6.2, 8.1_

  - [x] 10.2 Add onDeleteClick callback to RecipeListAdapter constructor
    - Add parameter: onDeleteClick: (Recipe) -> Unit
    - Pass to RecipeViewHolder
    - _Requirements: 8.1_

  - [x] 10.3 Update RecipeViewHolder.bind method
    - Check recipe.isFallback
    - Show fallback indicator TextView if true, hide if false
    - Set fallback indicator text from string resource
    - Set delete button click listener to call onDeleteClick
    - _Requirements: 6.2, 8.1_

  - [x] 10.4 Write unit tests for RecipeListAdapter
    - Test fallback indicator visibility for fallback recipes
    - Test fallback indicator hidden for non-fallback recipes
    - Test delete button click triggers callback

- [x] 11. Update RecipeListActivity to handle delete from list
  - [x] 11.1 Implement handleDeleteRecipe method in RecipeListActivity
    - Create AlertDialog with recipe name in message
    - Add positive button that calls viewModel.deleteRecipe
    - Add negative button that dismisses dialog
    - _Requirements: 8.3, 8.4, 8.5, 8.6, 8.10_

  - [x] 11.2 Pass handleDeleteRecipe to RecipeListAdapter
    - Update adapter initialization with onDeleteClick callback
    - _Requirements: 8.1, 8.10_

  - [x] 11.3 Add deleteRecipe method to RecipeListViewModel
    - Call repository.deleteRecipe with recipe id
    - Recipe list will update automatically via Flow
    - _Requirements: 8.10_

  - [x] 11.4 Write unit tests for RecipeListActivity
    - Test delete button shows confirmation dialog
    - Test delete confirmation removes recipe from list
    - Test delete cancellation keeps recipe in list


- [x] 12. Add string resources for new UI elements
  - Add fallback_recipe_indicator string
  - Add fallback_recipe_message string
  - Add import_success_with_fallbacks string
  - Add import_mixed_results string
  - Add delete_recipe_title string
  - Add delete_recipe_message string
  - Add delete string
  - Add cancel string
  - Add edit_recipe_title string
  - Add save string
  - _Requirements: 3.3, 4.2, 4.3, 4.4, 6.2, 8.3, 8.4_

- [x] 13. Update RecipeDetailActivity to make URL clickable
  - [x] 13.1 Modify displayRecipe method to handle originalUrl
    - Check if originalUrl is not null
    - Set URL TextView text
    - Make TextView clickable with Intent.ACTION_VIEW
    - _Requirements: 3.1, 3.2_

  - [x] 13.2 Write unit tests for URL click behavior
    - Test clicking URL launches browser intent
    - Test URL visibility for recipes with originalUrl

- [x] 14. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 15. Integration testing with test URLs
  - [x] 15.1 Test import flow with test URLs
    - Test https://joyfoodsunshine.com/the-most-amazing-chocolate-chip-cookies/
    - Test https://www.seriouseats.com/food-lab-best-chocolate-chip-cookie-step-by-step-slideshow
    - Test https://www.bowlofdelicious.com/easy-chicken-tikka-masala/
    - Test https://www.thefreshcooky.com/healthy-mongolian-beef/
    - Test https://www.epicurious.com/recipes/food/views/ba-syn-tandoori-style-roasted-indian-cauliflower
    - Test https://www.bonappetit.com/recipe/smashed-broccoli-pasta
    - Test https://www.foodnetwork.com/recipes/food-network-kitchen/fluffy-japanese-pancakes-3686850
    - Test https://www.delish.com/cooking/recipe-ideas/a60343196/cinnamon-pretzel-bites-recipe/
    - Verify fallback recipes are created with page titles
    - Verify URLs are clickable
    - _Requirements: 1.1, 1.2, 1.3, 3.1, 3.2_

  - [x] 15.2 Test edit flow for fallback recipes
    - Import a fallback recipe
    - Open recipe editor
    - Verify all fields are editable
    - Add ingredients and instructions
    - Save changes
    - Verify changes persist
    - _Requirements: 7.1, 7.2, 7.3, 7.6, 7.7, 7.8, 7.9, 7.10, 7.11, 7.12_

  - [x] 15.3 Test edit flow for non-fallback recipes
    - Import a recipe with structured data
    - Open recipe editor
    - Verify only name field is editable
    - Modify name
    - Save changes
    - Verify name change persists
    - _Requirements: 7.1, 7.13_


  - [x] 15.4 Test delete flow from recipe detail
    - Open a recipe
    - Click delete button
    - Verify confirmation dialog shows recipe name
    - Confirm deletion
    - Verify navigation back to recipe list
    - Verify recipe is removed from database
    - _Requirements: 8.2, 8.3, 8.4, 8.5, 8.7, 8.9_

  - [x] 15.5 Test delete flow from recipe list
    - View recipe list
    - Click delete button on a recipe card
    - Verify confirmation dialog shows recipe name
    - Confirm deletion
    - Verify recipe is removed from list
    - Verify recipe is removed from database
    - _Requirements: 8.1, 8.3, 8.4, 8.5, 8.7, 8.10_

  - [x] 15.6 Test delete cancellation
    - Click delete button
    - Cancel deletion
    - Verify recipe remains in database
    - Verify recipe remains visible in UI
    - _Requirements: 8.6, 8.8_

  - [x] 15.7 Write integration tests for end-to-end flows
    - Test import → view → edit → save flow
    - Test import → view → delete flow
    - Test import → delete from list flow

- [ ] 16. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties (Properties 1-14)
- Unit tests validate specific examples and edge cases
- Integration tests validate end-to-end user flows
- The implementation uses Kotlin for Android development
- Kotest Property Testing library is used for property-based tests
- Room database handles schema changes automatically with default values
