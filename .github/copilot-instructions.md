# GitHub Copilot Instructions for EBook Reader Android

## Project Overview
This is an Android eBook reader application that supports multiple formats including PDF, EPUB, MOBI, TXT, and comic book formats (CBZ/CBR). The app allows users to manage their digital book library, track reading progress, and enjoy a comfortable reading experience.

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
  - PDF viewing: Built-in Android PdfRenderer (API 21+)
  - EPUB parsing: Custom implementation using built-in ZIP support
  - MOBI support: Custom implementation for basic PalmDB format
  - CBZ support: Apache Commons Compress 1.25.0
  - CBR support: junrar 7.5.5
  - Navigation: AndroidX Navigation Component
  - Preferences: AndroidX Preference Library
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
├── (root)           # Activities, Adapters (MainActivity, ViewerActivity, etc.)
│   ├── MainActivity.kt               # Main book library screen
│   ├── ViewerActivity.kt            # Book reader with format-specific viewers
│   ├── NetworkBookActivity.kt       # Download books from URLs
│   ├── SettingsActivity.kt          # App settings
│   ├── AboutActivity.kt             # About screen
│   ├── BookAdapter.kt               # RecyclerView adapter for book list
│   ├── BookmarkAdapter.kt           # RecyclerView adapter for bookmarks
│   ├── BookmarksBottomSheet.kt      # Bottom sheet for bookmark management
│   └── ReadingSettingsBottomSheet.kt # Bottom sheet for reading preferences
├── viewmodel/       # ViewModels
├── database/        # Room database, DAOs
├── model/           # Data models (Bookmark, ReadingPreferences)
└── util/            # Utility classes
```

## Architecture Patterns

### MVVM Pattern
- **Model**: Data classes, Room entities
- **View**: Activities, Fragments, Bottom Sheets (UI layer)
- **ViewModel**: Business logic, LiveData, direct DAO interaction

### Data Flow
1. View observes ViewModel's LiveData
2. ViewModel interacts directly with DAO (no repository layer)
3. DAO manages data from Room database
4. Changes propagate back through LiveData observers

## Development Guidelines

### Adding New Features
1. Create data model (if needed) with Room entity
2. Update DAO with necessary queries
3. Update ViewModel to interact with DAO using coroutines
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
- Support multiple formats: PDF, EPUB, MOBI, TXT, CBZ, CBR
- Validate file integrity before processing
- Use document picker (ACTION_OPEN_DOCUMENT) for file selection

### UI Development
- Use Material 3 theme defined in themes.xml
- Follow existing color palette in colors.xml
- Add all strings to strings.xml (no hardcoded text)
- Use ConstraintLayout or LinearLayout as appropriate
- Implement proper error handling and user feedback
- Use Bottom Sheets (BottomSheetDialogFragment) for overlays and settings

### Reading Features
- **Bookmarks**: Managed via BookmarksBottomSheet with Room persistence
  - Entity: Bookmark (id, bookId, page, position, note, timestamp)
  - DAO: BookmarkDao for CRUD operations
- **Reading Preferences**: Managed via ReadingSettingsBottomSheet
  - Model: ReadingPreferences (fontFamily, theme, lineSpacing, margins)
  - Themes: Light, Dark, Sepia with customizable colors
  - Stored in SharedPreferences via PreferencesManager utility
- **Network Downloads**: NetworkBookActivity for downloading books from URLs
  - Downloads to app-specific storage directory
  - Automatic addition to library after successful download

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
- ViewModels interact directly with DAOs using viewModelScope

### Implementing New Book Format
1. Add parsing library dependency (if needed - prefer custom implementation)
2. Create parser/reader implementation in ViewerActivity
3. Update Book entity if needed
4. Add MIME type and file extension handling in ViewerActivity
5. Update file picker to accept new format (modify documentIntent in MainActivity)

## Supported File Formats

### E-Book Formats
- **PDF**: Rendered using Android's built-in `PdfRenderer` API (API 21+)
  - Implementation: Direct rendering to ImageView/Bitmap
  - No external dependencies required
- **EPUB**: Custom parser using built-in Java ZIP utilities
  - Implementation: ZIP extraction and HTML content parsing
  - No external dependencies required
- **MOBI**: Custom implementation for basic PalmDB format
  - Implementation: Binary format parsing for text extraction
  - No external dependencies required
- **TXT**: Plain text files displayed in TextView
  - Implementation: Direct file reading with encoding detection

### Comic Book Formats
- **CBZ**: ZIP-based comic book archives
  - Library: Apache Commons Compress 1.25.0
  - Implementation: Extract and display images in sequence
- **CBR**: RAR-based comic book archives
  - Library: junrar 7.5.5
  - Implementation: Extract and display images in sequence

### Format Detection
- File extension-based detection in ViewerActivity
- MIME type verification where applicable
- Graceful fallback for unsupported formats

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
- ViewModels interact directly with DAOs (no repository layer)
- Custom implementations for PDF, EPUB, and MOBI parsing using Android built-in APIs
- Apache Commons Compress for CBZ (ZIP-based comic books)
- junrar library for CBR (RAR-based comic books)
- Bottom sheets for settings and bookmarks UI
- Document picker (ACTION_OPEN_DOCUMENT) for file selection

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
