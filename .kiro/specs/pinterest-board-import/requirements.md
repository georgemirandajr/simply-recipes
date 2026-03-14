# Requirements Document

## Introduction

The Pinterest Board Import feature allows Simply Recipes users to import all recipes from a Pinterest board in one action. When a user taps "Share" on a Pinterest board in the Pinterest Android app, Simply Recipes receives the share intent, detects it as a Pinterest board share, resolves the short URL to the full board URL, extracts all pin links from the board's embedded page data, and imports each external recipe URL through the existing import pipeline. Fallback recipes (pins with no parseable external recipe) are discarded rather than saved.

## Glossary

- **ShareReceiverActivity**: The existing Android Activity that handles `ACTION_SEND` and `ACTION_SEND_MULTIPLE` share intents.
- **PinterestBoardDetector**: The component responsible for identifying whether shared text contains a Pinterest board URL.
- **PinterestUrlResolver**: The component responsible for following HTTP redirects from a `pin.it` short URL to the canonical `pinterest.com/username/board-name/` URL.
- **PinterestBoardScraper**: The component responsible for fetching a Pinterest board HTML page and extracting external recipe URLs from the embedded `__PWS_DATA__` JSON.
- **ImportService**: The existing service that accepts a list of URLs and imports each as a recipe, tracking successes, fallbacks, and failures.
- **ImportWorker**: The existing WorkManager `CoroutineWorker` that runs `ImportService` in the background.
- **NetworkClientImpl**: The existing OkHttp-based HTTP client with browser-like headers.
- **ImportNotificationHelper**: The existing helper that generates user-facing notification and summary messages.
- **Fallback_Recipe**: A `Recipe` entity where `isFallback = true`, created when the parser cannot extract structured recipe data from a URL.
- **Pin_Link**: The external URL stored in a Pinterest pin's `link` field, pointing to the original recipe webpage.
- **Board_URL**: A canonical Pinterest board URL matching the pattern `https://www.pinterest.com/{username}/{board-name}/`.
- **Short_URL**: A `pin.it` shortened URL that redirects to a Board_URL via HTTP redirects.

## Requirements

### Requirement 1: Pinterest Board Share Detection

**User Story:** As a Simply Recipes user, I want the app to recognize when I share a Pinterest board, so that it triggers the board import flow instead of treating the share text as a single recipe URL.

#### Acceptance Criteria

1. WHEN `ShareReceiverActivity` receives an `ACTION_SEND` intent with `text/plain` MIME type, THE `PinterestBoardDetector` SHALL inspect the `EXTRA_TEXT` string for a URL matching the pattern `pin.it/` or `pinterest.com/{username}/{board-name}/`.
2. WHEN the shared text contains a `pin.it` short URL, THE `PinterestBoardDetector` SHALL classify the intent as a Pinterest board share.
3. WHEN the shared text contains a URL matching `https://www.pinterest.com/{username}/{board-name}/` (two non-empty path segments), THE `PinterestBoardDetector` SHALL classify the intent as a Pinterest board share.
4. WHEN the shared text does not contain a Pinterest board URL pattern, THE `PinterestBoardDetector` SHALL classify the intent as a non-Pinterest share, and `ShareReceiverActivity` SHALL continue with the existing single-URL import flow.
5. THE `PinterestBoardDetector` SHALL extract the Pinterest URL from the shared text even when the text contains additional non-URL content (e.g., "Check out this board I made on Pinterest! https://pin.it/7AdGtCCtZ").

---

### Requirement 2: User Confirmation Dialog

**User Story:** As a Simply Recipes user, I want to be warned before a potentially long board import begins, so that I can decide whether to proceed.

#### Acceptance Criteria

1. WHEN `ShareReceiverActivity` detects a Pinterest board share, THE `ShareReceiverActivity` SHALL display an `AlertDialog` before starting any network activity.
2. THE `AlertDialog` SHALL include the message that the import may take several minutes depending on board size.
3. THE `AlertDialog` SHALL include the message that some recipes may not import correctly.
4. THE `AlertDialog` SHALL present an "Import" confirmation button and a "Cancel" button.
5. WHEN the user taps "Cancel", THE `ShareReceiverActivity` SHALL dismiss the dialog and finish without initiating any import.
6. WHEN the user taps "Import", THE `ShareReceiverActivity` SHALL dismiss the dialog and proceed to the board resolution step.

---

### Requirement 3: Short URL Resolution

**User Story:** As a Simply Recipes user, I want the app to automatically resolve Pinterest short URLs, so that I don't need to manually find the full board URL.

#### Acceptance Criteria

1. WHEN a `pin.it` short URL is detected, THE `PinterestUrlResolver` SHALL perform an HTTP GET request to the short URL using `NetworkClientImpl`.
2. THE `PinterestUrlResolver` SHALL follow HTTP 3xx redirects until a URL matching the `pinterest.com/{username}/{board-name}/` pattern is reached, up to a maximum of 5 redirect hops.
3. WHEN the resolved URL matches `https://www.pinterest.com/{username}/{board-name}/`, THE `PinterestUrlResolver` SHALL return the resolved Board_URL.
4. IF the redirect chain does not resolve to a `pinterest.com/{username}/{board-name}/` URL within 5 hops, THEN THE `PinterestUrlResolver` SHALL return a resolution failure.
5. IF a network error occurs during redirect resolution, THEN THE `PinterestUrlResolver` SHALL return a resolution failure.
6. WHEN a Board_URL (not a short URL) is provided directly, THE `PinterestUrlResolver` SHALL return it unchanged without making any network requests.

---

### Requirement 4: Board HTML Fetching

**User Story:** As a Simply Recipes user, I want the app to retrieve the Pinterest board's content, so that it can find all the pinned recipe links.

#### Acceptance Criteria

1. WHEN a Board_URL is available, THE `PinterestBoardScraper` SHALL fetch the board's HTML page using `NetworkClientImpl`.
2. IF the HTTP response status code is not 2xx, THEN THE `PinterestBoardScraper` SHALL return a fetch failure with the HTTP status code.
3. IF a network error occurs during the board page fetch, THEN THE `PinterestBoardScraper` SHALL return a fetch failure.
4. THE `PinterestBoardScraper` SHALL pass the browser-like `User-Agent` and `Accept` headers already configured in `NetworkClientImpl` when fetching the board page.

---

### Requirement 5: Pin URL Extraction

**User Story:** As a Simply Recipes user, I want the app to extract all recipe links from the Pinterest board, so that every pinned recipe can be imported.

#### Acceptance Criteria

1. WHEN the board HTML page is fetched successfully, THE `PinterestBoardScraper` SHALL locate a `<script>` tag whose content contains the key `__PWS_DATA__` or `__PWS_INITIAL_PROPS__`.
2. THE `PinterestBoardScraper` SHALL parse the JSON object embedded in that script tag.
3. THE `PinterestBoardScraper` SHALL traverse the parsed JSON to collect all string values associated with the key `"link"` that begin with `http://` or `https://` and do not contain `pinterest.com` in the host.
4. IF no script tag containing `__PWS_DATA__` or `__PWS_INITIAL_PROPS__` is found, THEN THE `PinterestBoardScraper` SHALL return an extraction failure.
5. IF the embedded JSON cannot be parsed, THEN THE `PinterestBoardScraper` SHALL return an extraction failure.
6. IF the extracted Pin_Link list is empty after filtering, THEN THE `PinterestBoardScraper` SHALL return an extraction failure indicating no recipe links were found.
7. THE `PinterestBoardScraper` SHALL deduplicate Pin_Links so that the same external URL is not imported more than once per board import operation.

---

### Requirement 6: Fallback Filtering

**User Story:** As a Simply Recipes user, I want only real recipes to be saved, so that my recipe list isn't cluttered with incomplete bookmarks from Pinterest pins that don't link to actual recipes.

#### Acceptance Criteria

1. WHEN `ImportService` processes a Pin_Link URL and the resulting `Recipe` has `isFallback = true`, THE `ImportService` SHALL not persist that recipe to the database.
2. THE `ImportService` SHALL increment a dedicated `skippedCount` for each Pin_Link that produces a Fallback_Recipe, separate from `failureCount`.
3. WHEN `ImportService` processes a Pin_Link URL and the resulting `Recipe` has `isFallback = false`, THE `ImportService` SHALL persist the recipe to the database and increment `successCount`.
4. THE `ImportSummary` returned by `ImportService` for a Pinterest board import SHALL include `successCount`, `skippedCount`, and `failureCount` as distinct values.

---

### Requirement 7: Background Import Execution

**User Story:** As a Simply Recipes user, I want the board import to run in the background, so that I can continue using my phone while recipes are being imported.

#### Acceptance Criteria

1. WHEN the user confirms the import dialog, THE `ShareReceiverActivity` SHALL enqueue a WorkManager work request to perform the board import in the background.
2. THE `ImportWorker` SHALL execute the full Pinterest board import pipeline (URL resolution, board scraping, pin extraction, and per-URL import) within its `doWork()` coroutine.
3. WHILE the import is running, THE `ImportWorker` SHALL post a progress notification using `ImportNotificationHelper` indicating that a Pinterest board import is in progress.
4. THE `ShareReceiverActivity` SHALL finish immediately after enqueueing the work request, without waiting for import completion.

---

### Requirement 8: Import Progress Notification

**User Story:** As a Simply Recipes user, I want to see progress while the board is being imported, so that I know the app is working.

#### Acceptance Criteria

1. WHILE the Pinterest board import is in progress, THE `ImportWorker` SHALL update the progress notification with the count of recipes processed so far out of the total Pin_Links extracted.
2. WHEN the import completes, THE `ImportWorker` SHALL replace the progress notification with a completion notification.
3. THE completion notification SHALL display the number of recipes successfully imported, the number skipped (fallbacks), and the number that failed.

---

### Requirement 9: Import Completion Summary

**User Story:** As a Simply Recipes user, I want a clear summary when the board import finishes, so that I know how many recipes were saved.

#### Acceptance Criteria

1. WHEN the Pinterest board import completes successfully with at least one imported recipe, THE `ImportNotificationHelper` SHALL produce a summary message stating the number of recipes successfully imported.
2. WHEN the Pinterest board import completes with skipped fallback recipes, THE `ImportNotificationHelper` SHALL include the skipped count in the summary message.
3. WHEN the Pinterest board import completes with failed URLs, THE `ImportNotificationHelper` SHALL include the failure count in the summary message.
4. WHEN the Pinterest board import produces zero successfully imported recipes and zero skipped recipes, THE `ImportNotificationHelper` SHALL produce a summary message indicating that no recipes could be imported from the board.

---

### Requirement 10: Error Handling — Resolution and Scraping Failures

**User Story:** As a Simply Recipes user, I want to be informed when the board cannot be accessed, so that I understand why no recipes were imported.

#### Acceptance Criteria

1. IF `PinterestUrlResolver` returns a resolution failure, THEN THE `ImportWorker` SHALL cancel the import and post an error notification stating that the Pinterest board URL could not be resolved.
2. IF `PinterestBoardScraper` returns a fetch failure, THEN THE `ImportWorker` SHALL cancel the import and post an error notification stating that the Pinterest board page could not be loaded.
3. IF `PinterestBoardScraper` returns an extraction failure, THEN THE `ImportWorker` SHALL cancel the import and post an error notification stating that no recipe links were found on the board.
4. THE error notifications in criteria 1–3 SHALL be distinct from the per-recipe failure messages used in the existing single-URL import flow.
