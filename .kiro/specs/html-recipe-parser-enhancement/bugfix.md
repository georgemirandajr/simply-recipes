# Bugfix Requirements Document

## Introduction

The Recipe Bookmarks app currently fails to import recipes from many popular recipe websites that use HTML-based recipe plugins like WP Recipe Maker (WPRM). These sites store recipe data in structured HTML with specific CSS classes rather than in JSON-LD, Microdata, or RDFa formats. The parser creates fallback recipes with empty ingredients and instructions even though complete recipe data is available in the HTML, resulting in a poor user experience and requiring manual editing of imported recipes.

This bug affects recipes from Bowl of Delicious, Food Network, Delish, and other popular recipe sites that use HTML-based recipe plugins.

## Bug Analysis

### Current Behavior (Defect)

1.1 WHEN a recipe website uses HTML-based recipe formats (e.g., WPRM plugin with `div.wprm-recipe-template-*` containers) THEN the system fails to extract recipe data and creates a fallback recipe with empty ingredients and instructions

1.2 WHEN recipe data is present in HTML elements with specific class structures (e.g., `div.wprm-recipe-ingredients`, `div.wprm-recipe-instructions`) THEN the system ignores this data and returns ParseResult.Failure(ParseError.NO_RECIPE_DATA)

1.3 WHEN all structured data formats (JSON-LD, Microdata, RDFa) fail to parse THEN the system immediately creates a fallback recipe without attempting HTML-based parsing

1.4 WHEN a recipe from Bowl of Delicious, Food Network, or Delish is imported THEN the system creates a fallback recipe requiring manual editing instead of extracting the available recipe data

### Expected Behavior (Correct)

2.1 WHEN a recipe website uses HTML-based recipe formats (e.g., WPRM plugin with `div.wprm-recipe-template-*` containers) THEN the system SHALL detect and extract recipe data from the HTML structure

2.2 WHEN recipe data is present in HTML elements with specific class structures (e.g., `div.wprm-recipe-ingredients`, `div.wprm-recipe-instructions`) THEN the system SHALL parse these elements and extract ingredients and instructions

2.3 WHEN all structured data formats (JSON-LD, Microdata, RDFa) fail to parse THEN the system SHALL attempt HTML-based parsing before creating a fallback recipe

2.4 WHEN a recipe from Bowl of Delicious, Food Network, or Delish is imported THEN the system SHALL extract complete recipe data including name, ingredients, and instructions without requiring manual editing

### Unchanged Behavior (Regression Prevention)

3.1 WHEN a recipe website provides JSON-LD structured data THEN the system SHALL CONTINUE TO parse it successfully as the primary parsing method

3.2 WHEN a recipe website provides Microdata structured data THEN the system SHALL CONTINUE TO parse it successfully as a secondary parsing method

3.3 WHEN a recipe website provides RDFa structured data THEN the system SHALL CONTINUE TO parse it successfully as a tertiary parsing method

3.4 WHEN a recipe website has no recipe data in any format (structured or HTML-based) THEN the system SHALL CONTINUE TO create a fallback recipe with the page title as the recipe name

3.5 WHEN parsing succeeds with any method THEN the system SHALL CONTINUE TO return ParseResult.Success with a Recipe object containing the extracted data

3.6 WHEN parsing encounters an exception THEN the system SHALL CONTINUE TO handle it gracefully and attempt fallback creation

3.7 WHEN optional recipe fields (yield, servingSize, nutritionInfo) are not present THEN the system SHALL CONTINUE TO create recipes with null values for these fields
