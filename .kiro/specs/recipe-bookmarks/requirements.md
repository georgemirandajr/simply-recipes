# Requirements Document

## Introduction

This document specifies the requirements for an Android mobile application that displays bookmarked recipes. The application allows users to view their saved recipes with complete details including ingredients, instructions, nutritional information, and links to original sources.

## Glossary

- **Recipe_App**: The Android mobile application for displaying bookmarked recipes
- **Recipe**: A bookmarked recipe containing name, ingredients, instructions, and optional metadata
- **Ingredient**: A food item with quantity and unit measurements required for a recipe
- **Instruction**: A single step in the recipe preparation process
- **Nutrition_Info**: Nutritional data including calories, macronutrients, and other dietary information
- **Yield**: The number of servings or quantity produced by a recipe
- **Original_Link**: A URL reference to the source website where the recipe was originally published
- **Scaling_Factor**: A multiplier applied to ingredient quantities (1.0x for single, 1.5x for one-and-a-half, 2.0x for double)
- **Scaled_Quantity**: The calculated ingredient quantity after applying the scaling factor
- **Category**: A classification label for recipes (breakfast, lunch, dinner, dessert, drink, or sauce)
- **Category_Tag**: A category label assigned to a recipe for filtering and organization purposes
- **Category_Filter**: A user-selected category used to display only recipes matching that category
- **Search_Query**: Text input provided by the user to find recipes by name
- **Search_Results**: A filtered list of recipes whose names match the search query
- **Shared_URL**: A web address to a recipe webpage shared with the Recipe_App from another application
- **Bookmark_Folder**: A collection of URLs exported from a web browser's bookmark system
- **Recipe_Parser**: The component that extracts recipe data from webpage content
- **Import_Session**: A single operation where one or more recipe URLs are processed and added to the Recipe_App
- **Manual_Entry_Form**: An interface that allows users to input recipe data manually when automatic extraction fails
- **Ingredient_Entry**: A user interface element for adding individual ingredients with quantity, unit, and name
- **Instruction_Entry**: A user interface element for adding individual preparation steps

## Requirements

### Requirement 1: Display Bookmarked Recipes

**User Story:** As a user, I want to view my bookmarked recipes, so that I can access my saved recipes quickly.

#### Acceptance Criteria

1. THE Recipe_App SHALL display a list of all bookmarked recipes
2. WHEN a user selects a recipe from the list, THE Recipe_App SHALL display the full recipe details
3. THE Recipe_App SHALL display the recipe name for each bookmarked recipe

### Requirement 2: Display Recipe Ingredients

**User Story:** As a user, I want to see the ingredients list for each recipe, so that I know what items I need to prepare the dish.

#### Acceptance Criteria

1. WHEN a recipe is displayed, THE Recipe_App SHALL show the complete ingredients list
2. THE Recipe_App SHALL display each ingredient with its quantity and unit of measurement
3. THE Recipe_App SHALL display ingredients in the order specified by the recipe

### Requirement 3: Display Yield and Serving Information

**User Story:** As a user, I want to see yield and serving size information, so that I can plan portions appropriately.

#### Acceptance Criteria

1. WHEN a recipe is displayed, THE Recipe_App SHALL show the yield information
2. WHEN a recipe is displayed, THE Recipe_App SHALL show the serving size information
3. THE Recipe_App SHALL display yield as the number of servings or quantity produced

### Requirement 4: Display Nutrition Information

**User Story:** As a user, I want to see nutrition information when available, so that I can make informed dietary choices.

#### Acceptance Criteria

1. WHERE nutrition information is provided, THE Recipe_App SHALL display the nutritional data
2. WHERE nutrition information is not provided, THE Recipe_App SHALL indicate that nutritional data is unavailable
3. WHEN nutrition information is displayed, THE Recipe_App SHALL show calories and macronutrients

### Requirement 5: Display Recipe Instructions

**User Story:** As a user, I want to see step-by-step instructions, so that I can follow the recipe to prepare the dish.

#### Acceptance Criteria

1. WHEN a recipe is displayed, THE Recipe_App SHALL show all preparation instructions
2. THE Recipe_App SHALL display instructions in sequential order
3. THE Recipe_App SHALL display each instruction as a distinct step

### Requirement 6: Display Original Recipe Link

**User Story:** As a user, I want to see a link to the original recipe, so that I can visit the source website for additional information.

#### Acceptance Criteria

1. WHEN a recipe is displayed, THE Recipe_App SHALL show the original recipe link
2. WHEN a user selects the original link, THE Recipe_App SHALL open the URL in a web browser
3. THE Recipe_App SHALL display the original link as a clickable element

### Requirement 7: Recipe Data Persistence

**User Story:** As a user, I want my bookmarked recipes to be saved, so that I can access them across app sessions.

#### Acceptance Criteria

1. THE Recipe_App SHALL persist bookmarked recipes between app sessions
2. WHEN the app is closed and reopened, THE Recipe_App SHALL display previously bookmarked recipes
3. THE Recipe_App SHALL maintain recipe data integrity across app restarts

### Requirement 8: Scale Ingredient Quantities

**User Story:** As a user, I want to scale ingredient quantities based on my serving needs, so that I can adjust recipes for different numbers of people.

#### Acceptance Criteria

1. THE Recipe_App SHALL provide scaling options of 1.0x, 1.5x, and 2.0x for ingredient quantities
2. WHEN a user selects a scaling factor, THE Recipe_App SHALL recalculate all ingredient quantities using the selected scaling factor
3. WHEN a user selects a scaling factor, THE Recipe_App SHALL display the scaled quantities for each ingredient
4. WHEN ingredient quantities are scaled, THE Recipe_App SHALL display a notification in the instruction section informing users that instructions may need modification based on the selected quantity
5. THE Recipe_App SHALL display the currently selected scaling factor to the user
6. WHEN a recipe is first displayed, THE Recipe_App SHALL default to 1.0x scaling factor

### Requirement 9: Categorize and Filter Recipes

**User Story:** As a user, I want to categorize and filter my recipes by type, so that I can quickly find recipes appropriate for specific meals or occasions.

#### Acceptance Criteria

1. THE Recipe_App SHALL support the following categories: breakfast, lunch, dinner, dessert, drink, and sauce
2. THE Recipe_App SHALL allow users to assign a category tag to each recipe
3. WHEN a user assigns a category tag to a recipe, THE Recipe_App SHALL persist the category tag with the recipe data
4. THE Recipe_App SHALL allow users to modify the category tag of an existing recipe
5. WHEN a user modifies a category tag, THE Recipe_App SHALL update the recipe with the new category tag
6. THE Recipe_App SHALL provide a filtering interface for selecting a category
7. WHEN a user selects a category filter, THE Recipe_App SHALL display only recipes tagged with the selected category
8. WHEN no category filter is selected, THE Recipe_App SHALL display all bookmarked recipes
9. WHEN a recipe is displayed in the list, THE Recipe_App SHALL show the recipe's category tag
10. WHERE a recipe has no category tag assigned, THE Recipe_App SHALL indicate that the recipe is uncategorized

### Requirement 10: Search Recipes by Name

**User Story:** As a user, I want to search for recipes by typing the recipe name, so that I can quickly find specific recipes without scrolling through the entire list.

#### Acceptance Criteria

1. THE Recipe_App SHALL provide a search input field on the main page
2. WHEN a user enters text into the search input field, THE Recipe_App SHALL filter the recipe list to show only recipes whose names contain the search query
3. WHEN the search query is empty, THE Recipe_App SHALL display all bookmarked recipes
4. THE Recipe_App SHALL perform case-insensitive matching when filtering recipes by name
5. WHEN no recipes match the search query, THE Recipe_App SHALL display a message indicating no results were found
6. WHEN a user clears the search query, THE Recipe_App SHALL restore the full recipe list

### Requirement 11: Import Recipes from Shared URLs

**User Story:** As a user, I want to import recipes by sharing URLs from my web browser or other apps, so that I can quickly add recipes I find online to my bookmarked collection.

#### Acceptance Criteria

1. THE Recipe_App SHALL register as a share target for URL content on the Android system
2. WHEN a user shares a single recipe URL to the Recipe_App, THE Recipe_App SHALL accept the shared URL
3. WHEN a user shares multiple URLs from a bookmark folder to the Recipe_App, THE Recipe_App SHALL accept all shared URLs
4. WHEN a shared URL is received, THE Recipe_App SHALL fetch the webpage content from the URL
5. WHEN webpage content is fetched, THE Recipe_Parser SHALL extract recipe data including name, ingredients, instructions, yield, and nutrition information where available
6. WHEN recipe data is successfully extracted, THE Recipe_App SHALL add the recipe to the bookmarked recipes list
7. WHEN recipe data is successfully extracted, THE Recipe_App SHALL store the original URL as the Original_Link
8. WHEN multiple URLs are shared in a single import session, THE Recipe_App SHALL process each URL sequentially
9. WHEN a recipe is successfully imported, THE Recipe_App SHALL display a confirmation message to the user
10. IF a shared URL cannot be fetched, THEN THE Recipe_App SHALL display an error message indicating the URL is inaccessible
11. IF recipe data cannot be extracted from a webpage, THEN THE Recipe_App SHALL display an error message indicating the page does not contain recognizable recipe data
12. WHEN an import session completes with multiple URLs, THE Recipe_App SHALL display a summary showing the number of successfully imported recipes and the number of failed imports
13. WHEN an import error occurs during a multi-URL import session, THE Recipe_App SHALL continue processing remaining URLs
### Requirement 12: Manual Recipe Data Entry

**User Story:** As a user, I want to manually enter recipe data when automatic import fails, so that I can still save recipes even when the website format is not recognized or the URL is inaccessible.

#### Acceptance Criteria

1. WHEN a shared URL cannot be fetched, THE Recipe_App SHALL offer the user an option to manually enter the recipe data
2. WHEN recipe data cannot be extracted from a webpage, THE Recipe_App SHALL offer the user an option to manually enter the recipe data
3. WHEN a user chooses to manually enter recipe data, THE Recipe_App SHALL display the Manual_Entry_Form
4. THE Manual_Entry_Form SHALL provide input fields for recipe name, yield, and serving size
5. THE Manual_Entry_Form SHALL provide an Ingredient_Entry interface for adding ingredients to the ingredients list
6. WHEN a user adds an ingredient, THE Recipe_App SHALL allow the user to specify the ingredient name, quantity, and unit of measurement
7. THE Manual_Entry_Form SHALL allow users to add multiple ingredients sequentially
8. THE Manual_Entry_Form SHALL allow users to remove ingredients from the ingredients list
9. THE Manual_Entry_Form SHALL provide an Instruction_Entry interface for adding preparation steps
10. WHEN a user adds an instruction, THE Recipe_App SHALL allow the user to enter the instruction text
11. THE Manual_Entry_Form SHALL allow users to add multiple instructions sequentially
12. THE Manual_Entry_Form SHALL allow users to remove instructions from the instructions list
13. THE Manual_Entry_Form SHALL allow users to reorder ingredients in the ingredients list
14. THE Manual_Entry_Form SHALL allow users to reorder instructions in the instructions list
15. WHERE the user has the failed URL available, THE Manual_Entry_Form SHALL allow the user to optionally enter the Original_Link
16. WHEN a user completes manual entry and saves the recipe, THE Recipe_App SHALL validate that the recipe name is not empty
17. WHEN a user completes manual entry and saves the recipe, THE Recipe_App SHALL validate that at least one ingredient has been entered
18. WHEN a user completes manual entry and saves the recipe, THE Recipe_App SHALL validate that at least one instruction has been entered
19. IF manual entry validation fails, THEN THE Recipe_App SHALL display an error message indicating which required fields are missing
20. WHEN manual entry validation succeeds, THE Recipe_App SHALL add the manually entered recipe to the bookmarked recipes list
21. WHEN a manually entered recipe is saved, THE Recipe_App SHALL display a confirmation message to the user
22. THE Manual_Entry_Form SHALL allow users to cancel manual entry without saving the recipe
