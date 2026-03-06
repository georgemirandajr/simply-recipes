# Requirements Document

## Introduction

The Recipe Bookmarks Android app currently imports recipes from URLs by parsing structured recipe data (JSON-LD, Microdata, RDFa). When parsing fails due to missing structured data, the import fails completely and shows an error to the user. This feature adds a fallback mechanism that creates a minimal recipe bookmark when structured data parsing fails, allowing users to bookmark any recipe URL regardless of whether it contains structured data.

## Glossary

- **Recipe_Parser**: The component that extracts structured recipe data from web pages
- **Import_Service**: The service that processes recipe URLs and saves recipes to the database
- **Fallback_Recipe**: A minimal recipe bookmark created when structured data parsing fails
- **Structured_Data**: Machine-readable recipe information embedded in web pages (JSON-LD, Microdata, RDFa)
- **Recipe_Bookmark**: A saved recipe entry in the app's database
- **Page_Title**: The HTML title element content from a web page
- **Original_URL**: The source URL from which a recipe was imported
- **Recipe_Editor**: The UI component that allows users to modify recipe details
- **Recipe_Detail_View**: The UI component that displays a single recipe's full information
- **Recipe_List_View**: The UI component that displays all saved recipes

## Requirements

### Requirement 1: Fallback Recipe Creation

**User Story:** As a user, I want to bookmark recipe URLs that don't have structured data, so that I can save recipes from any website.

#### Acceptance Criteria

1. WHEN the Recipe_Parser fails to extract structured data from a URL, THEN the Import_Service SHALL create a Fallback_Recipe
2. THE Fallback_Recipe SHALL contain the Original_URL as a clickable link
3. THE Fallback_Recipe SHALL contain a recipe name extracted from the Page_Title
4. IF the Page_Title is empty or unavailable, THEN the Import_Service SHALL generate a recipe name from the Original_URL domain
5. THE Fallback_Recipe SHALL contain an empty ingredients list
6. THE Fallback_Recipe SHALL contain an empty instructions list
7. THE Import_Service SHALL save the Fallback_Recipe to the database

### Requirement 2: Recipe Name Extraction

**User Story:** As a user, I want fallback recipes to have meaningful names, so that I can identify them in my recipe list.

#### Acceptance Criteria

1. WHEN extracting a recipe name from the Page_Title, THE Import_Service SHALL use the full Page_Title text
2. WHEN generating a recipe name from the Original_URL, THE Import_Service SHALL extract the domain name
3. THE Import_Service SHALL trim whitespace from extracted recipe names
4. WHEN the Page_Title exceeds 200 characters, THE Import_Service SHALL truncate it to 200 characters

### Requirement 3: Original URL Access

**User Story:** As a user, I want to access the original recipe website from a fallback recipe, so that I can view the full recipe content.

#### Acceptance Criteria

1. WHEN viewing a Fallback_Recipe, THE Recipe_Detail_View SHALL display the Original_URL as a clickable link
2. WHEN the user clicks the Original_URL link, THE Recipe_Detail_View SHALL open the URL in the device's default browser
3. THE Recipe_Detail_View SHALL display a message indicating that this is a fallback recipe with limited data

### Requirement 4: User Notification

**User Story:** As a user, I want to know when a fallback recipe was created, so that I understand why the recipe has limited information.

#### Acceptance Criteria

1. WHEN a Fallback_Recipe is created, THE Import_Service SHALL display a notification to the user
2. THE notification SHALL indicate that structured data was not found
3. THE notification SHALL indicate that a minimal bookmark was created
4. THE notification SHALL indicate that the user can access the original recipe via the URL link

### Requirement 5: Backward Compatibility

**User Story:** As a developer, I want the fallback mechanism to work with existing code, so that no breaking changes are introduced.

#### Acceptance Criteria

1. WHEN the Recipe_Parser successfully extracts structured data, THE Import_Service SHALL create a recipe using the existing logic
2. THE Import_Service SHALL maintain the existing ParseResult.Success and ParseResult.Failure types
3. THE Recipe data model SHALL support recipes with empty ingredients and instructions lists
4. THE Room database schema SHALL support storing Fallback_Recipes without migration

### Requirement 6: Recipe List Display

**User Story:** As a user, I want to see fallback recipes in my recipe list, so that I can access all my bookmarked recipes in one place.

#### Acceptance Criteria

1. THE Recipe_List_View SHALL display Fallback_Recipes alongside regular recipes
2. THE Recipe_List_View SHALL visually indicate which recipes are fallback recipes
3. WHEN a Fallback_Recipe has no ingredients, THE Recipe_List_View SHALL display a placeholder message
4. THE Recipe_List_View SHALL allow users to edit Fallback_Recipes to add ingredients and instructions manually

### Requirement 7: Recipe Editing

**User Story:** As a user, I want to edit recipe details, so that I can update recipe names and manually fill in missing information for fallback recipes.

#### Acceptance Criteria

1. THE Recipe_Editor SHALL allow editing the recipe name for all Recipe_Bookmarks
2. WHERE a Recipe_Bookmark is a Fallback_Recipe, THE Recipe_Editor SHALL allow editing the ingredient list
3. WHERE a Recipe_Bookmark is a Fallback_Recipe, THE Recipe_Editor SHALL allow editing the instructions list
4. WHERE a Recipe_Bookmark is a Fallback_Recipe, THE Recipe_Editor SHALL allow editing the yield
5. WHERE a Recipe_Bookmark is a Fallback_Recipe, THE Recipe_Editor SHALL allow editing the nutritional information
6. WHEN editing the ingredient list, THE Recipe_Editor SHALL support adding new ingredients
7. WHEN editing the ingredient list, THE Recipe_Editor SHALL support modifying existing ingredients
8. WHEN editing the ingredient list, THE Recipe_Editor SHALL support removing ingredients
9. WHEN editing the instructions list, THE Recipe_Editor SHALL support adding new instruction steps
10. WHEN editing the instructions list, THE Recipe_Editor SHALL support modifying existing instruction steps
11. WHEN editing the instructions list, THE Recipe_Editor SHALL support removing instruction steps
12. WHEN the user saves edits, THE Recipe_Editor SHALL persist changes to the database
13. WHERE a Recipe_Bookmark is not a Fallback_Recipe, THE Recipe_Editor SHALL restrict editing to the recipe name only

### Requirement 8: Recipe Deletion

**User Story:** As a user, I want to delete recipes from my collection, so that I can remove recipes I no longer need.

#### Acceptance Criteria

1. THE Recipe_List_View SHALL display a delete button on each recipe card
2. THE Recipe_Detail_View SHALL display a delete button
3. WHEN the user clicks a delete button, THE app SHALL display a confirmation dialog
4. THE confirmation dialog SHALL display the recipe name being deleted
5. THE confirmation dialog SHALL provide a confirm action
6. THE confirmation dialog SHALL provide a cancel action
7. WHEN the user confirms deletion, THE app SHALL remove the Recipe_Bookmark from the database
8. WHEN the user cancels deletion, THE app SHALL dismiss the confirmation dialog without deleting the recipe
9. WHEN a recipe is deleted from the Recipe_Detail_View, THE app SHALL navigate the user back to the Recipe_List_View
10. WHEN a recipe is deleted from the Recipe_List_View, THE Recipe_List_View SHALL update to reflect the deletion
