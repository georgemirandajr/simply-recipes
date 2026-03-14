# Design Document: Pinterest Board Import

## Overview

This feature adds Pinterest board import to the existing single-URL recipe import flow. When a user shares a Pinterest board from the Pinterest Android app, the app receives a share intent containing text like `"Check out this board I made on Pinterest! https://pin.it/7AdGtCCtZ"`. The app detects this as a Pinterest board share, resolves the short URL to a canonical board URL, scrapes the board page for external recipe links embedded in `__PWS_DATA__` JSON, and imports each link through the existing `ImportService` pipeline. Fallback recipes (pins with no parseable external recipe) are discarded rather than saved.

The design introduces three new classes (`PinterestBoardDetector`, `PinterestUrlResolver`, `PinterestBoardScraper`) and modifies four existing components (`ShareReceiverActivity`, `ImportWorker`, `ImportService`, `ImportNotificationHelper`).

## Architecture

The Pinterest board import follows a sequential pipeline executed inside a WorkManager `CoroutineWorker`:

```
ShareReceiverActivity
  └─ detects Pinterest share via PinterestBoardDetector
  └─ shows confirmation AlertDialog
  └─ enqueues ImportWorker with board URL input key
       └─ PinterestUrlResolver  (pin.it → pinterest.com/user/board/)
       └─ PinterestBoardScraper (fetch HTML → extract __PWS_DATA__ → collect "link" values)
       └─ ImportService.handleSharedUrls(pinLinks, skipFallbacks = true)
            └─ for each link: NetworkClientImpl → RecipeParserImpl → RecipeRepositoryImpl
       └─ ImportNotificationHelper (progress + completion notifications)
```

The existing single-URL flow is unchanged. The Pinterest pipeline branches at `ImportWorker.doWork()` based on the presence of a new input data key `KEY_BOARD_URL`.

## Components and Interfaces

### PinterestBoardDetector

A pure stateless object (no network I/O) that inspects a shared text string.

```kotlin
object PinterestBoardDetector {
    /** Returns the Pinterest URL extracted from [text], or null if none found. */
    fun detect(text: String): String?
}
```

Detection rules (applied in order):
1. Find any URL in the text via `https?://[^\s]+` regex.
2. If the URL host is `pin.it` → return it (short URL, needs resolution).
3. If the URL host is `pinterest.com` or `www.pinterest.com` and the path has exactly two non-empty segments → return it (direct board URL).
4. Otherwise → return `null`.

### PinterestUrlResolver

Resolves a `pin.it` short URL to a canonical `pinterest.com/{username}/{board-name}/` URL by following HTTP redirects manually (OkHttp is configured with `followRedirects(false)` for this client).

```kotlin
class PinterestUrlResolver(private val networkClient: NetworkClient) {
    suspend fun resolve(url: String): ResolveResult
}

sealed class ResolveResult {
    data class Success(val boardUrl: String) : ResolveResult()
    data class Failure(val reason: ResolveFailureReason) : ResolveResult()
}

enum class ResolveFailureReason {
    TOO_MANY_REDIRECTS,
    NOT_A_BOARD_URL,
    NETWORK_ERROR
}
```

If the input URL already matches the board pattern, it is returned immediately as `Success` with no network calls. Otherwise, the resolver follows `Location` headers up to 5 hops. If the final URL matches `https://www.pinterest.com/{username}/{board-name}/`, it returns `Success`; otherwise `Failure(NOT_A_BOARD_URL)`.

Note: `NetworkClientImpl` follows redirects automatically for normal fetches. `PinterestUrlResolver` needs a separate `OkHttpClient` instance with `followRedirects(false)` so it can inspect each intermediate `Location` header and enforce the 5-hop limit. This client is constructed internally in `PinterestUrlResolver` rather than reusing `NetworkClientImpl`.

### PinterestBoardScraper

Fetches a board page and extracts external recipe URLs from the embedded `__PWS_DATA__` JSON.

```kotlin
class PinterestBoardScraper(private val networkClient: NetworkClient) {
    suspend fun scrape(boardUrl: String): ScrapeResult
}

sealed class ScrapeResult {
    data class Success(val pinLinks: List<String>) : ScrapeResult()
    data class Failure(val reason: ScrapeFailureReason) : ScrapeResult()
}

enum class ScrapeFailureReason {
    FETCH_FAILED,
    NO_PWS_DATA_SCRIPT,
    JSON_PARSE_ERROR,
    NO_LINKS_FOUND
}
```

Scraping steps:
1. Fetch board HTML via `networkClient.fetchHtml(boardUrl)`. Non-2xx or network error → `Failure(FETCH_FAILED)`.
2. Use Jsoup to find a `<script>` tag whose text contains `__PWS_DATA__` or `__PWS_INITIAL_PROPS__`. Not found → `Failure(NO_PWS_DATA_SCRIPT)`.
3. Extract the JSON value: strip the variable assignment prefix (e.g., `window.__PWS_DATA__ = `) and any trailing semicolon, then parse with Gson. Parse error → `Failure(JSON_PARSE_ERROR)`.
4. Recursively walk the `JsonElement` tree collecting all string values for keys named `"link"` where the value starts with `http://` or `https://` and the host does not contain `pinterest.com`.
5. Deduplicate the collected URLs (preserve first-seen order).
6. If the deduplicated list is empty → `Failure(NO_LINKS_FOUND)`.
7. Otherwise → `Success(pinLinks)`.

### Changes to ShareReceiverActivity

In `onCreate`, after extracting the shared text, call `PinterestBoardDetector.detect(text)` before the existing URL extraction:

```
val pinterestUrl = PinterestBoardDetector.detect(sharedText)
if (pinterestUrl != null) {
    showPinterestConfirmationDialog(pinterestUrl)
    return  // don't fall through to existing single-URL flow
}
// existing flow continues...
```

`showPinterestConfirmationDialog` builds an `AlertDialog` with:
- Message: "This will import all recipes from the Pinterest board. This may take several minutes depending on board size. Some recipes may not import correctly."
- Positive button "Import" → calls `startPinterestBoardImport(pinterestUrl)`
- Negative button "Cancel" → `finish()`

`startPinterestBoardImport` enqueues an `ImportWorker` with `KEY_BOARD_URL = pinterestUrl` and immediately calls `finish()`.

### Changes to ImportWorker

Add a new input key `KEY_BOARD_URL`. In `doWork()`, check for this key first:

```kotlin
val boardUrl = inputData.getString(KEY_BOARD_URL)
if (boardUrl != null) {
    return doPinterestBoardImport(boardUrl)
}
// existing single-URL flow unchanged
```

`doPinterestBoardImport` executes the full pipeline:
1. Post an "in progress" notification via `ImportNotificationHelper`.
2. Resolve URL via `PinterestUrlResolver`. On failure → post error notification, return `Result.failure()`.
3. Scrape board via `PinterestBoardScraper`. On failure → post error notification, return `Result.failure()`.
4. Call `importService.handleSharedUrls(pinLinks, skipFallbacks = true)`, updating the progress notification after each URL.
5. Post completion notification with `successCount`, `skippedCount`, `failureCount`.
6. Return `Result.success(outputData)`.

### Changes to ImportService

Add an optional `skipFallbacks: Boolean = false` parameter to `handleSharedUrls`. When `true`, recipes with `isFallback = true` are not persisted and instead increment a new `skippedCount` field on `ImportSummary`.

```kotlin
suspend fun handleSharedUrls(urls: List<String>, skipFallbacks: Boolean = false): ImportSummary
```

`ImportSummary` gains a `skippedCount: Int = 0` field. The existing `fallbackCount` field is repurposed: when `skipFallbacks = false` (existing behavior), fallbacks are still saved and counted in `successCount` + `fallbackCount` as before. When `skipFallbacks = true`, fallbacks are not saved and counted only in `skippedCount`.

### Changes to ImportNotificationHelper

Add Pinterest-specific message functions:

```kotlin
fun getPinterestProgressMessage(processed: Int, total: Int): String
fun getPinterestCompletionMessage(successCount: Int, skippedCount: Int, failureCount: Int): String
fun getPinterestErrorMessage(reason: String): String
```

Add a new overload of `getImportSummaryMessage` that accepts `skippedCount` as a distinct parameter (the existing overload is unchanged for backward compatibility).

## Data Models

### ImportSummary (modified)

```kotlin
data class ImportSummary(
    val successCount: Int,
    val failureCount: Int,
    val failures: List<ImportFailure>,
    val fallbackCount: Int = 0,   // existing: fallbacks saved (skipFallbacks = false)
    val skippedCount: Int = 0     // new: fallbacks discarded (skipFallbacks = true)
) : java.io.Serializable
```

### ImportWorker input/output keys (additions)

```kotlin
const val KEY_BOARD_URL = "board_url"          // input: Pinterest board URL
const val KEY_SKIPPED_COUNT = "skipped_count"  // output: fallbacks discarded
```

### ResolveResult, ScrapeResult

Defined inline with their respective classes above. Both are sealed classes with `Success` and `Failure` variants, keeping error reasons typed via enums.

No new database entities or Room migrations are required. The existing `Recipe` entity with `isFallback: Boolean` is sufficient.

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system — essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Pinterest URL detection is exhaustive and precise

*For any* string, `PinterestBoardDetector.detect` returns a non-null value if and only if the string contains a URL whose host is `pin.it`, or whose host is `pinterest.com`/`www.pinterest.com` with exactly two non-empty path segments. For all other strings it returns `null`.

**Validates: Requirements 1.1, 1.2, 1.3, 1.4**

### Property 2: URL extraction from mixed text

*For any* Pinterest board URL (pin.it or full board URL) embedded in arbitrary surrounding text, `PinterestBoardDetector.detect` returns the Pinterest URL regardless of the surrounding content.

**Validates: Requirements 1.5**

### Property 3: Direct board URLs pass through the resolver unchanged

*For any* URL matching `https://www.pinterest.com/{username}/{board-name}/` (two non-empty path segments), `PinterestUrlResolver.resolve` returns `Success` with the same URL and makes zero network requests.

**Validates: Requirements 3.6**

### Property 4: Redirect chain resolution respects the 5-hop limit

*For any* redirect chain of length N: if N ≤ 5 and the final URL is a valid board URL, the resolver returns `Success(boardUrl)`; if N > 5, the resolver returns `Failure(TOO_MANY_REDIRECTS)`.

**Validates: Requirements 3.2, 3.4**

### Property 5: Pin link extraction filters and deduplicates correctly

*For any* HTML document containing a `__PWS_DATA__` or `__PWS_INITIAL_PROPS__` script tag with embedded JSON, `PinterestBoardScraper.scrape` returns only URLs that (a) start with `http://` or `https://`, (b) do not contain `pinterest.com` in the host, and (c) appear at most once in the result list.

**Validates: Requirements 5.1, 5.2, 5.3, 5.7**

### Property 6: Fallback filtering — only non-fallback recipes are persisted

*For any* list of pin link URLs processed by `ImportService` with `skipFallbacks = true`, the number of `insertRecipe` calls on the repository equals the number of URLs that produced a non-fallback `Recipe`, and `ImportSummary.skippedCount` equals the number of URLs that produced a fallback `Recipe`.

**Validates: Requirements 6.1, 6.2, 6.3, 6.4**

### Property 7: ImportSummary counts are consistent

*For any* import run, `successCount + skippedCount + failureCount` equals the total number of input URLs processed.

**Validates: Requirements 6.4**

### Property 8: Pinterest completion message contains all non-zero counts

*For any* combination of `successCount`, `skippedCount`, and `failureCount` where at least one is greater than zero, `ImportNotificationHelper.getPinterestCompletionMessage` returns a string that contains each non-zero count as a substring.

**Validates: Requirements 9.1, 9.2, 9.3**

## Error Handling

| Stage | Failure condition | Behavior |
|---|---|---|
| URL Resolution | Network error | `Failure(NETWORK_ERROR)` → worker posts "could not resolve board URL" notification, returns `Result.failure()` |
| URL Resolution | >5 redirect hops | `Failure(TOO_MANY_REDIRECTS)` → same as above |
| URL Resolution | Final URL not a board URL | `Failure(NOT_A_BOARD_URL)` → same as above |
| Board Fetch | Non-2xx HTTP status | `Failure(FETCH_FAILED)` → worker posts "could not load board page" notification |
| Board Fetch | Network error | `Failure(FETCH_FAILED)` → same as above |
| Pin Extraction | No `__PWS_DATA__` script | `Failure(NO_PWS_DATA_SCRIPT)` → worker posts "no recipe links found" notification |
| Pin Extraction | Malformed JSON | `Failure(JSON_PARSE_ERROR)` → worker posts "no recipe links found" notification |
| Pin Extraction | Empty link list | `Failure(NO_LINKS_FOUND)` → worker posts "no recipe links found" notification |
| Per-URL import | Parse failure | Counted in `failureCount`; import continues with remaining URLs |
| Per-URL import | Network error | Counted in `failureCount`; import continues with remaining URLs |
| Per-URL import | Fallback recipe | Not persisted; counted in `skippedCount` |

Error notifications from the Pinterest pipeline use distinct message strings from the existing single-URL error messages, so users can distinguish board-level failures from per-recipe failures.

## Testing Strategy

### Unit Tests

Focus on specific examples, edge cases, and error conditions:

- `PinterestBoardDetectorTest`: verify detection of `pin.it` URLs, full board URLs, non-Pinterest URLs, and URLs embedded in Pinterest share text.
- `PinterestUrlResolverTest`: verify pass-through for direct board URLs; verify failure on network error (mock `NetworkClient`).
- `PinterestBoardScraperTest`: verify `NO_PWS_DATA_SCRIPT` failure on HTML with no matching script; verify `JSON_PARSE_ERROR` on malformed JSON; verify `NO_LINKS_FOUND` on JSON with no qualifying links.
- `ImportServiceTest`: verify existing behavior is unchanged when `skipFallbacks = false`; verify fallbacks are not persisted when `skipFallbacks = true`.
- `ImportNotificationHelperTest`: verify zero-result message (Requirement 9.4 example).

### Property-Based Tests

Use [Kotest Property Testing](https://kotest.io/docs/proptest/property-based-testing.html) (already available in the Kotlin ecosystem; add `kotest-property` dependency). Configure each test with `minSuccess = 100`.

Each property test is tagged with a comment referencing the design property it validates.
Tag format: `// Feature: pinterest-board-import, Property {N}: {property_text}`

**Property 1 — Detection exhaustiveness**
Generate arbitrary strings with and without embedded Pinterest URLs. Assert `detect` returns non-null iff the string contains a qualifying URL.

**Property 2 — Extraction from mixed text**
Generate a valid Pinterest URL, prepend/append random strings. Assert `detect` returns the original URL.

**Property 3 — Direct board URL pass-through**
Generate valid board URLs (random usernames and board names). Assert `resolve` returns `Success` with the same URL and that the mock `NetworkClient` receives zero calls.

**Property 4 — Redirect chain length**
Generate redirect chains of random length 1–10 where the terminal URL is a valid board URL. Assert `resolve` returns `Success` for chains ≤ 5 and `Failure(TOO_MANY_REDIRECTS)` for chains > 5.

**Property 5 — Link extraction filter + dedup**
Generate JSON trees with random `"link"` values (mix of pinterest.com links, non-http links, valid external links, duplicates). Assert the result contains only valid external links with no duplicates.

**Property 6 — Fallback filtering**
Generate lists of URLs where each URL is mapped to either a fallback or non-fallback `ParseResult` via a mock parser. Assert `insertRecipe` call count equals non-fallback count and `skippedCount` equals fallback count.

**Property 7 — Count consistency**
Generate arbitrary `ImportSummary` values. Assert `successCount + skippedCount + failureCount == total input URLs`.

**Property 8 — Completion message contains counts**
Generate random non-negative integer triples `(success, skipped, failure)` where at least one is > 0. Assert each non-zero value appears as a substring in the message returned by `getPinterestCompletionMessage`.
