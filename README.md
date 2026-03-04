# Recipe Bookmarks Android App

An Android mobile application for displaying and managing bookmarked recipes.

## Project Structure

```
RecipeBookmarks/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/recipebookmarks/
│   │   │   │   ├── data/          # Data layer (Room database, DAOs, models)
│   │   │   │   ├── domain/        # Business logic (repositories, use cases)
│   │   │   │   ├── ui/            # UI layer (activities, fragments, ViewModels)
│   │   │   │   └── utils/         # Utility classes
│   │   │   ├── res/               # Android resources
│   │   │   └── AndroidManifest.xml
│   │   ├── test/                  # Unit tests
│   │   └── androidTest/           # Instrumented tests
│   └── build.gradle.kts
├── gradle/
├── build.gradle.kts
└── settings.gradle.kts
```

## Dependencies

### Core Android
- AndroidX Core KTX 1.12.0
- AppCompat 1.6.1
- Material Components 1.11.0
- ConstraintLayout 2.1.4

### Architecture Components
- Lifecycle ViewModel KTX 2.7.0
- Lifecycle LiveData KTX 2.7.0
- Lifecycle Runtime KTX 2.7.0

### Database
- Room Runtime 2.6.1
- Room KTX 2.6.1
- Room Compiler 2.6.1 (KSP)

### Coroutines
- Kotlinx Coroutines Core 1.7.3
- Kotlinx Coroutines Android 1.7.3

### Networking
- OkHttp 4.12.0
- Retrofit 2.9.0
- Retrofit Converter Scalars 2.9.0

### HTML Parsing
- Jsoup 1.17.2

### Testing
- JUnit 4.13.2
- Kotest Runner JUnit5 5.8.0
- Kotest Assertions Core 5.8.0
- Kotest Property 5.8.0
- MockK 1.13.9
- Kotlinx Coroutines Test 1.7.3
- AndroidX Arch Core Testing 2.2.0
- AndroidX Test JUnit 1.1.5
- Espresso Core 3.5.1
- Room Testing 2.6.1

## Build Configuration

- **Compile SDK**: 34
- **Min SDK**: 24
- **Target SDK**: 34
- **Java Version**: 17
- **Kotlin Version**: 1.9.20
- **Gradle Version**: 8.2
- **Android Gradle Plugin**: 8.2.0
- **KSP Version**: 1.9.20-1.0.14

## Building the Project

### Prerequisites
- Android Studio Hedgehog or later
- JDK 17 or later
- Android SDK with API level 34

### Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Clean build
./gradlew clean
```

## Features

- Display bookmarked recipes with complete details
- Ingredient scaling (1.0x, 1.5x, 2.0x)
- Recipe categorization and filtering
- Search recipes by name
- Import recipes from shared URLs
- Manual recipe entry
- Offline data persistence with Room database

## Architecture

The app follows clean architecture principles with three main layers:

1. **Data Layer**: Room database, DAOs, and data models
2. **Domain Layer**: Business logic, repositories, and use cases
3. **UI Layer**: Activities, fragments, adapters, and ViewModels

## Testing Strategy

- **Unit Tests**: JUnit and MockK for business logic testing
- **Property-Based Tests**: Kotest property testing for comprehensive coverage
- **Integration Tests**: Room database testing
- **UI Tests**: Espresso for UI flow testing

## License

Copyright 2024
