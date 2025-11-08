# GitHub Copilot Instructions for EBook Reader Android

## Project Overview
This is an Android eBook reader application that supports EPUB and PDF formats. The app allows users to manage their digital book library, track reading progress, and enjoy a comfortable reading experience.

## Technology Stack
- **Language**: Kotlin
- **Minimum SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)
- **Build System**: Gradle (Kotlin DSL)
- **Architecture**: MVVM (Model-View-ViewModel)
- **UI**: ViewBinding, Material Design 3
- **Database**: Room (SQLite)
- **Async**: Coroutines and LiveData
- **Libraries**:
  - EPUB parsing: nl.siegmann.epublib:epublib-core:3.1
  - PDF viewing: com.github.barteksc:android-pdf-viewer
  - Navigation: AndroidX Navigation Component
  - Dependency Injection: None (manual dependency management)

## Code Style and Conventions

### Kotlin Style
- Use Kotlin idioms and features (data classes, extension functions, coroutines)
- Prefer `val` over `var` when possible
- Use meaningful variable and function names in camelCase
- Keep functions small and focused on a single responsibility
- Use trailing commas in multi-line parameter lists

### Android Conventions
- Use ViewBinding for view access (no findViewById)
- Follow Material Design 3 guidelines for UI/UX
- Use string resources for all user-facing text
- Keep business logic out of Activities/Fragments

### Package Structure
```
com.rifters.ebookreader/
├── ui/              # Activities, Fragments, Adapters
├── viewmodel/       # ViewModels
├── database/        # Room database, DAOs
├── repository/      # Data repositories
├── model/           # Data models
└── util/            # Utility classes
```

## Architecture Patterns

### MVVM Pattern
- **Model**: Data classes, Room entities, repositories
- **View**: Activities, Fragments (UI layer)
- **ViewModel**: Business logic, LiveData/StateFlow

### Data Flow
1. View observes ViewModel's LiveData
2. ViewModel interacts with Repository
3. Repository manages data from Room database
4. Changes propagate back through LiveData observers

## Development Guidelines

### Adding New Features
1. Create data model (if needed) with Room entity
2. Update DAO with necessary queries
3. Create/update Repository layer
4. Implement ViewModel with LiveData
5. Create UI with ViewBinding
6. Wire up observers in Activity/Fragment

### Database Changes
- All entities must use Room annotations
- Use `@Entity`, `@PrimaryKey`, `@Dao`, `@Database`
- Increment database version when schema changes
- Provide migration strategy for schema updates

### File Handling
- Request storage permissions appropriately
- Handle scoped storage for Android 10+
- Support both EPUB and PDF formats
- Validate file integrity before processing

### UI Development
- Use Material 3 theme defined in themes.xml
- Follow existing color palette in colors.xml
- Add all strings to strings.xml (no hardcoded text)
- Use ConstraintLayout or LinearLayout as appropriate
- Implement proper error handling and user feedback

## Building and Testing

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
```

### Testing Guidelines
- Write unit tests for ViewModels and business logic
- Use JUnit for unit tests
- Use Espresso for UI tests
- Test database operations with Room's testing utilities
- Mock external dependencies when appropriate

## Common Tasks

### Adding Dependencies
- Add to `app/build.gradle.kts` in the `dependencies` block
- Prefer AndroidX libraries
- Check for latest stable versions
- Consider app size impact

### Working with Room Database
- Entity: Define data structure with `@Entity`
- DAO: Create interface with `@Dao` and query methods
- Database: Add entity to `@Database` annotation
- Use coroutines for database operations (suspend functions)

### Implementing New Book Format
1. Add parsing library dependency
2. Create parser/reader implementation
3. Update Book entity if needed
4. Add MIME type support in ViewerActivity
5. Update file picker to accept new format

## Security and Permissions
- Request permissions at runtime (not just in manifest)
- Handle permission denial gracefully
- Use scoped storage for Android 10+
- Validate user input and file content
- Don't expose sensitive file paths in logs

## Performance Considerations
- Use coroutines for I/O operations
- Implement pagination for large book lists
- Cache book covers efficiently
- Lazy load book content
- Use RecyclerView with DiffUtil for lists

## Accessibility
- Provide content descriptions for UI elements
- Support TalkBack screen reader
- Ensure touch targets are at least 48dp
- Maintain sufficient color contrast
- Support system font size settings

## Known Patterns in This Codebase
- ViewBinding is used throughout (enabled in build.gradle.kts)
- Kotlin Parcelize for data passing between components
- Room database for persistent storage
- LiveData for reactive UI updates
- Material Design 3 theming

## What to Avoid
- Don't use findViewById (use ViewBinding)
- Don't perform I/O on main thread
- Don't hardcode strings in code
- Don't ignore permission checks
- Don't create memory leaks with context references in long-lived objects
- Don't use deprecated APIs when alternatives exist

## Additional Resources
- Package name: `com.rifters.ebookreader`
- Namespace: `com.rifters.ebookreader`
- App name: "EBook Reader"
