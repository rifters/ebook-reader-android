# GitHub Copilot Instructions for EBook Reader Android

## Project Overview
This is an Android eBook reader application that supports multiple formats including PDF, EPUB, MOBI, AZW/AZW3, FB2, DOCX, Markdown, HTML/XML, TXT, and comic book formats (CBZ/CBR/CB7/CBT). The app features a comprehensive book library management system with cloud sync via Firebase, cloud storage integration (Google Drive, Dropbox, OneDrive, FTP/SFTP, WebDAV), collections, tags, reading goals, reading lists, bookmarks, highlights, and text-to-speech support.

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
  - MOBI/AZW/AZW3 support: Custom implementation for PalmDB format
  - FB2: Custom XML parser for FictionBook format
  - DOCX: Custom ZIP-based XML parser
  - Markdown: Custom Markdown to HTML converter
  - Comic books: Apache Commons Compress 1.25.0 (CBZ/CB7/CBT), junrar 7.5.5 (CBR), XZ 1.9 (7z)
  - Cloud sync: Firebase Firestore and Authentication
  - Cloud storage: Google Drive API, Dropbox SDK, FTP/SFTP (commons-net, jsch)
  - Background tasks: WorkManager for sync operations
  - Navigation: AndroidX Navigation Component
  - Preferences: AndroidX Preference Library
  - Dependency Injection: None (manual dependency management)

## Key Features and User Workflows

### Library Management
- **Grid/List View**: Toggle between grid and list display modes
- **Search**: Search books by title, author, or metadata
- **Sort**: Sort by title, author, date added, last read, progress
- **Filter**: Filter by format, status (reading, completed, to-read), rating, tags, collections
- **Add Books**: Import from device storage, download from URL, or cloud storage
- **Edit Metadata**: Modify title, author, genre, publisher, year, language, ISBN, rating
- **Delete Books**: Remove books from library with confirmation
- **Dynamic Covers**: Auto-generated colorful covers when no cover image exists

### Reading Experience
- **Multi-Format Support**: PDF, EPUB, MOBI, AZW/AZW3, FB2, DOCX, Markdown, HTML, TXT, CBZ/CBR/CB7/CBT
- **Reading Modes**: Single-column, two-column, or continuous scroll layouts
- **Themes**: Light, Dark, Sepia with customizable colors
- **Font Settings**: Adjust font family, size, line spacing, margins
- **Brightness Control**: Custom brightness settings for comfortable reading
- **Bookmarks**: Save reading positions with optional notes
- **Highlights**: Mark text passages with color-coded highlights and notes
- **Table of Contents**: Navigate EPUB chapters via TOC bottom sheet
- **Text-to-Speech**: Listen to books with Android TTS engine
- **Progress Tracking**: Automatic progress calculation and completion tracking

### Organization
- **Collections**: Group books into custom collections (e.g., "Favorites", "Work", "Fiction")
- **Tags**: Flexible tagging system with color-coded tags
- **Smart Collections**: Auto-organize by author, genre, year, reading status
- **Reading Lists**: Create and manage reading queues
- **Duplicate Detection**: Find and identify duplicate books in library

### Goals and Tracking
- **Reading Goals**: Set targets for books, pages, or time
- **Progress Tracking**: Track reading progress towards goals
- **Statistics**: View reading statistics and achievements

### Cloud Integration
- **Firebase Sync**: Sync reading progress, bookmarks, highlights across devices
- **Cloud Storage**: Import books from Google Drive, Dropbox, OneDrive, WebDAV, FTP/SFTP
- **Background Sync**: Automatic background sync using WorkManager
- **Conflict Resolution**: Handle concurrent edits gracefully

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
├── (root)           # Activities, Adapters, Fragments, Dialogs
│   ├── MainActivity.kt                  # Main book library screen with grid/list toggle
│   ├── ViewerActivity.kt                # Book reader with format-specific viewers
│   ├── CollectionsActivity.kt           # Manage book collections
│   ├── CollectionBooksActivity.kt       # View books in a specific collection
│   ├── NetworkBookActivity.kt           # Download books from URLs
│   ├── CloudBrowserActivity.kt          # Browse and download from cloud storage
│   ├── SettingsActivity.kt              # App settings and cloud sync config
│   ├── AboutActivity.kt                 # About screen
│   ├── EditBookActivity.kt              # Edit book metadata
│   ├── Book.kt                          # Room entity for books
│   ├── BookAdapter.kt                   # RecyclerView adapter for book list
│   ├── BookmarkAdapter.kt               # RecyclerView adapter for bookmarks
│   ├── CollectionAdapter.kt             # RecyclerView adapter for collections
│   ├── CloudFileAdapter.kt              # RecyclerView adapter for cloud files
│   ├── HighlightAdapter.kt              # RecyclerView adapter for highlights
│   ├── TocAdapter.kt                    # RecyclerView adapter for table of contents
│   ├── BookmarksBottomSheet.kt          # Bottom sheet for bookmark management
│   ├── HighlightsBottomSheet.kt         # Bottom sheet for highlight management
│   ├── TocBottomSheet.kt                # Bottom sheet for table of contents
│   ├── ReadingSettingsBottomSheet.kt    # Bottom sheet for reading preferences
│   └── AddNoteDialogFragment.kt         # Dialog for adding notes
├── viewmodel/       # ViewModels for business logic
│   ├── BookViewModel.kt                 # Manages books, search, sort, filter
│   ├── CollectionViewModel.kt           # Manages collections
│   ├── SyncViewModel.kt                 # Handles cloud sync operations
│   ├── CloudStorageViewModel.kt         # Manages cloud storage connections
│   ├── TagViewModel.kt                  # Manages tags
│   ├── ReadingGoalViewModel.kt          # Tracks reading goals
│   └── ReadingListViewModel.kt          # Manages reading lists
├── database/        # Room database, DAOs, entities
│   ├── BookDatabase.kt                  # Room database configuration
│   ├── BookDao.kt                       # CRUD operations for books
│   ├── BookmarkDao.kt                   # CRUD operations for bookmarks
│   ├── CollectionDao.kt                 # CRUD operations for collections
│   ├── TagDao.kt                        # CRUD operations for tags
│   ├── ReadingGoalDao.kt                # CRUD operations for reading goals
│   ├── ReadingListDao.kt                # CRUD operations for reading lists
│   ├── HighlightDao.kt                  # CRUD operations for highlights
│   └── SyncDao.kt                       # CRUD operations for sync data
├── model/           # Data models and entities
│   ├── Bookmark.kt                      # Bookmark entity
│   ├── Collection.kt                    # Collection entity
│   ├── Tag.kt                           # Tag entity
│   ├── ReadingGoal.kt                   # Reading goal entity
│   ├── ReadingList.kt                   # Reading list entity
│   ├── ReadingListItem.kt               # Reading list item entity
│   ├── Highlight.kt                     # Highlight entity
│   ├── BookCollectionCrossRef.kt        # Many-to-many: Book-Collection
│   ├── BookTagCrossRef.kt               # Many-to-many: Book-Tag
│   ├── CollectionRelations.kt           # Collection with books relation
│   ├── SyncData.kt                      # Sync data entity
│   ├── SyncStatus.kt                    # Sync status enum
│   ├── ReadingPreferences.kt            # Reading preferences model
│   └── TableOfContentsItem.kt           # TOC item model
├── sync/            # Cloud synchronization services
│   ├── FirebaseSyncService.kt           # Firebase Firestore sync service
│   ├── CloudSyncService.kt              # Cloud sync coordination service
│   ├── SyncRepository.kt                # Sync data repository
│   └── SyncWorker.kt                    # Background sync worker
├── cloud/           # Cloud storage providers
│   ├── CloudStorageManager.kt           # Manages cloud storage connections
│   ├── CloudStorageProvider.kt          # Base interface for providers
│   ├── GoogleDriveProvider.kt           # Google Drive integration
│   ├── DropboxProvider.kt               # Dropbox integration
│   ├── OneDriveProvider.kt              # OneDrive integration
│   ├── WebDavProvider.kt                # WebDAV integration
│   └── FtpProvider.kt                   # FTP/SFTP integration
└── util/            # Utility classes
    ├── BookCoverGenerator.kt            # Generates colorful book covers
    ├── PreferencesManager.kt            # Manages app preferences
    ├── FileValidator.kt                 # Validates file formats
    ├── BitmapCache.kt                   # Image caching for covers
    ├── EpubParser.kt                    # Custom EPUB parser
    ├── AzwParser.kt                     # AZW/AZW3 parser
    ├── Fb2Parser.kt                     # FictionBook 2 parser
    ├── DocxParser.kt                    # DOCX parser
    ├── MarkdownParser.kt                # Markdown to HTML converter
    └── DuplicateDetector.kt             # Duplicate book detection
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
- Current database version: 6 (includes collections, tags, goals, lists, highlights, sync)
- Increment database version when schema changes
- Provide migration strategy for schema updates
- Use cross-reference tables for many-to-many relationships (BookCollectionCrossRef, BookTagCrossRef)

### File Handling
- Request storage permissions appropriately
- Handle scoped storage for Android 10+
- Support multiple formats: PDF, EPUB, MOBI, AZW/AZW3, FB2, DOCX, Markdown, HTML/XML, TXT, CBZ, CBR, CB7, CBT
- Validate file integrity before processing using FileValidator
- Use document picker (ACTION_OPEN_DOCUMENT) for file selection
- Cloud storage integration for importing books from Google Drive, Dropbox, OneDrive, WebDAV, FTP/SFTP

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
- **Highlights**: Managed via HighlightsBottomSheet with color-coded text selection
  - Entity: Highlight (id, bookId, text, color, note, page, position)
  - DAO: HighlightDao for CRUD operations
- **Table of Contents**: Managed via TocBottomSheet for EPUB chapter navigation
  - Model: TableOfContentsItem (title, link, level)
  - Extracted from EPUB NCX/NAV files
- **Reading Preferences**: Managed via ReadingSettingsBottomSheet
  - Model: ReadingPreferences (fontFamily, theme, lineSpacing, margins, brightness)
  - Themes: Light, Dark, Sepia with customizable colors
  - Stored in SharedPreferences via PreferencesManager utility
- **Collections**: Organize books into collections via CollectionsActivity
  - Entity: Collection (id, name, description, icon, createdAt)
  - Many-to-many relationship using BookCollectionCrossRef
- **Tags**: Flexible tagging system for book categorization
  - Entity: Tag (id, name, color)
  - Many-to-many relationship using BookTagCrossRef
- **Reading Goals**: Track reading targets via ReadingGoalViewModel
  - Entity: ReadingGoal (id, type, target, current, startDate, endDate)
  - Types: Books, Pages, Time
- **Reading Lists**: Manage reading queues via ReadingListViewModel
  - Entity: ReadingList (id, name, description)
  - Entity: ReadingListItem (id, readingListId, bookId, order)
- **Cloud Sync**: Firebase Firestore sync via SyncViewModel and FirebaseSyncService
  - Entity: SyncData (id, bookId, lastReadPage, progress, lastSyncTime)
  - Background sync using WorkManager (SyncWorker)
- **Cloud Storage**: Import books from cloud storage via CloudBrowserActivity
  - Providers: Google Drive, Dropbox, OneDrive, WebDAV, FTP/SFTP
  - Downloads to app-specific storage directory
- **Network Downloads**: NetworkBookActivity for downloading books from URLs
  - Downloads to app-specific storage directory
  - Automatic addition to library after successful download
- **Text-to-Speech**: Listen to books with Android TTS engine
  - Integrated in ViewerActivity with playback controls
- **Smart Collections**: Auto-organize books by author, genre, year, status
  - Utility: SmartCollections for intelligent categorization
- **Duplicate Detection**: Find duplicate books using similarity algorithms
  - Utility: DuplicateDetector for identifying duplicates

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
2. Create parser/reader implementation in util/ package (e.g., AzwParser, Fb2Parser)
3. Update ViewerActivity to handle the new format
4. Update Book entity if new metadata fields are needed
5. Add MIME type and file extension handling in ViewerActivity
6. Update file picker to accept new format (modify documentIntent in MainActivity)
7. Add file validation in FileValidator utility

## Supported File Formats

### E-Book Formats
- **PDF**: Rendered using Android's built-in `PdfRenderer` API (API 21+)
  - Implementation: Direct rendering to ImageView/Bitmap
  - No external dependencies required
- **EPUB**: Custom parser using built-in Java ZIP utilities
  - Implementation: ZIP extraction and HTML content parsing
  - Table of Contents support via NCX/NAV files
  - No external dependencies required
- **MOBI**: Custom implementation for basic PalmDB format
  - Implementation: Binary format parsing for text extraction
  - No external dependencies required
- **AZW/AZW3**: Kindle format support (DRM-free only)
  - Implementation: Custom parser with PalmDB decompression
  - Parser: AzwParser in util/ package
- **FB2**: FictionBook 2 XML format
  - Implementation: Custom XML parser with metadata extraction
  - Parser: Fb2Parser in util/ package
- **DOCX**: Microsoft Word documents
  - Implementation: ZIP-based XML parsing
  - Parser: DocxParser in util/ package
- **Markdown**: Markdown files (.md)
  - Implementation: Markdown to HTML converter
  - Parser: MarkdownParser in util/ package
- **HTML/XML**: Web formats (.html, .htm, .xhtml, .xml, .mhtml)
  - Implementation: Direct WebView rendering
- **TXT**: Plain text files displayed in TextView
  - Implementation: Direct file reading with encoding detection

### Comic Book Formats
- **CBZ**: ZIP-based comic book archives
  - Library: Apache Commons Compress 1.25.0
  - Implementation: Extract and display images in sequence
- **CBR**: RAR-based comic book archives
  - Library: junrar 7.5.5
  - Implementation: Extract and display images in sequence
- **CB7**: 7z-based comic book archives
  - Library: Apache Commons Compress 1.25.0 with XZ 1.9
  - Implementation: Extract and display images in sequence
- **CBT**: TAR-based comic book archives
  - Library: Apache Commons Compress 1.25.0
  - Implementation: Extract and display images in sequence

### Format Detection
- File extension-based detection in ViewerActivity
- MIME type verification where applicable
- FileValidator utility for integrity checking
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

## Firebase and Cloud Sync
- **Firebase Setup**: Configure Firebase in google-services.json (see FIREBASE_SETUP.md)
- **Authentication**: Firebase Auth for user authentication
- **Firestore**: Store and sync reading progress, bookmarks, highlights across devices
- **Sync Service**: FirebaseSyncService handles bidirectional sync
- **Sync Worker**: Background sync using WorkManager (SyncWorker)
- **Sync Repository**: SyncRepository manages sync operations and conflict resolution
- **Sync Data**: SyncData entity tracks last sync time and reading progress
- **Sync Status**: Enum for tracking sync state (SYNCING, SUCCESS, ERROR)
- **Implementation Guidelines**:
  - Always handle offline scenarios gracefully
  - Implement conflict resolution for concurrent edits
  - Use coroutines for async Firebase operations
  - Sync on app launch and periodically in background
  - Respect user's sync preferences (enabled/disabled)

## Cloud Storage Integration
- **Providers**: Google Drive, Dropbox, OneDrive, WebDAV, FTP/SFTP
- **Manager**: CloudStorageManager coordinates multiple providers
- **Base Interface**: CloudStorageProvider defines common operations
- **Activities**: CloudBrowserActivity for browsing and downloading books
- **ViewModel**: CloudStorageViewModel manages cloud connections and state
- **Implementation Guidelines**:
  - Each provider implements CloudStorageProvider interface
  - Handle authentication and token refresh
  - Support file listing, downloading, and uploading
  - Cache directory listings for performance
  - Provide progress feedback during operations
  - Handle network errors and retries gracefully
  - Store credentials securely using Android KeyStore or SharedPreferences with encryption
- **API Requirements**:
  - Google Drive: Requires API credentials in google-services.json
  - Dropbox: Requires app key and secret
  - OneDrive: Requires client ID from Azure AD
  - WebDAV/FTP: User provides server URL and credentials

## Known Patterns in This Codebase
- ViewBinding is used throughout (enabled in build.gradle.kts)
- Kotlin Parcelize for data passing between components
- Room database for persistent storage (current version: 6)
- LiveData for reactive UI updates
- Material Design 3 theming with purple color scheme
- ViewModels interact directly with DAOs (no repository layer, except SyncRepository for sync operations)
- Custom implementations for PDF, EPUB, MOBI, AZW/AZW3, FB2, DOCX, and Markdown parsing using Android built-in APIs
- Apache Commons Compress for CBZ, CB7, CBT (ZIP/7z/TAR-based comic books)
- junrar library for CBR (RAR-based comic books)
- XZ library for 7z decompression support
- Firebase Firestore and Authentication for cloud sync
- WorkManager for background sync operations
- Google Drive API, Dropbox SDK for cloud storage
- commons-net and jsch for FTP/SFTP support
- Bottom sheets for settings, bookmarks, highlights, and table of contents UI
- Document picker (ACTION_OPEN_DOCUMENT) for file selection
- Cross-reference tables for many-to-many relationships (collections, tags)
- Dynamic book cover generation with BookCoverGenerator
- BitmapCache for efficient image caching

## What to Avoid
- Don't use findViewById (use ViewBinding)
- Don't perform I/O on main thread (use coroutines)
- Don't hardcode strings in code (use strings.xml)
- Don't ignore permission checks (especially storage permissions)
- Don't create memory leaks with context references in long-lived objects
- Don't use deprecated APIs when alternatives exist
- Don't commit sensitive data (API keys, credentials) to version control
- Don't sync large book files over Firebase (sync metadata only)
- Don't store plain text passwords (use Android KeyStore)
- Don't ignore error handling in cloud operations

## Additional Resources
- Package name: `com.rifters.ebookreader`
- Namespace: `com.rifters.ebookreader`
- App name: "EBook Reader"
