# EBook Reader Android

A modern Android e-book reader application built with Kotlin, supporting PDF, EPUB, MOBI, TXT, and comic book formats (CBZ/CBR).

## Features

- 📚 **Multi-format Support** - PDF, EPUB, MOBI, TXT, CBZ, CBR
- 💾 **Local Database** - Room database for efficient storage
- 📊 **Progress Tracking** - Automatic reading progress and completion tracking
- 🎨 **Material Design 3 UI** - Modern, polished interface with purple theme
- 📖 **Library Management** - Grid and list view options for your collection
- 🖼️ **Dynamic Book Covers** - Auto-generated colorful covers with title and author
- 🔖 **Bookmarks** - Save and manage reading positions with notes
- ☁️ **Cloud Sync** - Sync reading progress across devices with Firebase
- ✨ **Smooth Animations** - Polished transitions and micro-interactions
- 🎯 **Custom Icons** - Consistent design language with vector icons

## Project Structure

### Core Components

#### Activities
- **MainActivity.kt** - Main screen with grid/list toggle, search, sort, and filter
- **ViewerActivity.kt** - Book reading interface with format-specific viewers
- **CollectionsActivity.kt** - Manage book collections and categories
- **NetworkBookActivity.kt** - Download books from URL
- **SettingsActivity.kt** - App preferences and cloud sync settings
- **AboutActivity.kt** - App information and credits
- **EditBookActivity.kt** - Edit book metadata

#### Data Layer
- **Book.kt** - Room entity for book data model with progress tracking
- **Bookmark.kt** - Room entity for bookmarks with notes
- **Collection.kt** - Room entity for organizing books into collections
- **BookDao.kt** - Room DAO for book database operations
- **BookmarkDao.kt** - Room DAO for bookmark operations
- **CollectionDao.kt** - Room DAO for collection management
- **BookDatabase.kt** - Room database configuration with migrations

#### ViewModels
- **BookViewModel.kt** - Manages book data, search, sort, and filter
- **CollectionViewModel.kt** - Manages collections and book-collection relationships
- **SyncViewModel.kt** - Handles cloud sync operations and status

#### Adapters
- **BookAdapter.kt** - RecyclerView adapter with dynamic cover generation
- **BookmarkAdapter.kt** - Displays bookmarks in bottom sheet
- **CollectionAdapter.kt** - Manages collection list display

#### Utilities
- **BookCoverGenerator.kt** - Generates colorful default book covers
- **PreferencesManager.kt** - Manages app preferences and settings
- **FileValidator.kt** - Validates file formats and integrity
- **BitmapCache.kt** - Efficient image caching for covers

### Resources

#### Layouts
- **activity_main.xml** - Main screen with RecyclerView, Toolbar, and FAB
- **activity_viewer.xml** - Book viewer with multiple view types and bottom app bar
- **activity_collections.xml** - Collections management screen
- **activity_settings.xml** - Settings and preferences screen
- **item_book.xml** - List view book card with cover, title, author, and progress
- **item_book_grid.xml** - Grid view book card optimized for cover display
- **item_collection.xml** - Collection item with icon and book count
- **bottom_sheet_bookmarks.xml** - Bookmarks management bottom sheet
- **bottom_sheet_reading_settings.xml** - Reading preferences (font, theme, spacing)

#### Values
- **strings.xml** - All string resources with i18n support
- **colors.xml** - Material Design 3 color palette (purple-based)
- **themes.xml** - Material Design 3 theme with custom styles and animations

#### Drawables
- **Custom Icons** - ic_add, ic_book, ic_bookmark, ic_collection, ic_settings, etc.
- **Animations** - fade_in, slide_in_right, scale_in, and more
- **Ripple Effects** - Touch feedback for interactive elements

#### Menus
- **main_menu.xml** - Search, sort, filter, sync, and view toggle
- **viewer_menu.xml** - Reading options and bookmarks
- **book_menu.xml** - Book actions (edit, delete, share)

## Dependencies

### Core Libraries
- AndroidX Core KTX
- AppCompat
- Material Components
- ConstraintLayout
- Navigation Components

### Architecture Components
- Room Database (with KTX)
- Lifecycle ViewModel & LiveData
- Kotlin Coroutines
- WorkManager (for background sync)

### Cloud Sync
- Firebase BOM (Bill of Materials)
- Firebase Firestore (for data storage)
- Firebase Authentication (anonymous auth)

### File Format Support
- **PDF**: Android built-in PdfRenderer
- **EPUB**: Custom implementation using ZIP extraction
- **MOBI**: Custom implementation with basic PalmDB format reading
- **CBZ**: Apache Commons Compress for ZIP-based comic books
- **CBR**: junrar library for RAR-based comic books
- **Plain Text**: Built-in TextView

### Build Configuration
- Kotlin 1.9.10
- Android Gradle Plugin 8.1.4
- Compile SDK: 34
- Min SDK: 24
- Target SDK: 34

## Building the Project

### Prerequisites
- Android Studio Arctic Fox or later
- JDK 17 or later
- Android SDK with API 34

### Build Instructions

1. Clone the repository:
```bash
git clone https://github.com/rifters/ebook-reader-android.git
cd ebook-reader-android
```

2. (Optional) Set up Firebase for cloud sync:
   - Create a Firebase project and download your `google-services.json`
   - Replace the placeholder file in `app/google-services.json`
   - The app will build and run with the placeholder file, but sync won't work without a real Firebase project

3. Build the project:
```bash
./gradlew build
```

4. Run on a device or emulator:
```bash
./gradlew installDebug
```

## UI/UX Highlights

### Modern Material Design 3
The app features a completely refreshed UI following Material Design 3 guidelines:

- **Color Palette**: Vibrant purple-based theme (#6750A4 primary) with proper elevation and shadows
- **Typography**: Consistent Material 3 text appearances for better readability
- **Cards**: Elevated cards with 12dp corner radius and smooth ripple effects
- **Icons**: Custom vector icons throughout for consistent design language

### Flexible View Modes
Browse your library in two ways:

- **List View**: Detailed view showing cover, title, author, progress bar, and reading status
- **Grid View**: Cover-focused 2-column layout perfect for visual browsing
- Toggle instantly via toolbar menu, preference saved automatically

### Dynamic Book Covers
Books without cover images get beautiful auto-generated covers:

- **10 Color Schemes**: Unique colors assigned via title hash for consistent identity
- **Professional Design**: Dual borders, gradient overlay, and book emoji icon
- **Custom Typography**: Title and author displayed with adaptive sizing

### Smooth Animations
Polished experience with subtle micro-interactions:

- **Activity Transitions**: 300ms slide animations when navigating
- **RecyclerView Items**: Smooth add/remove animations
- **FAB**: Delayed entrance animation (300ms)
- **Empty States**: Fade-in with slide-up effect

### Enhanced Components
- **Empty States**: Large icons with descriptive text instead of plain messages
- **Bottom Sheets**: Reading settings and bookmarks with improved spacing
- **Progress Indicators**: Material 3 circular and linear progress bars
- **Toolbar**: Elevated with proper theming and icon tinting

## Features Roadmap

### Recently Completed ✅
- [x] Material Design 3 UI refresh with purple theme
- [x] Grid and list view toggle for library
- [x] Dynamic book cover generation with title/author
- [x] Smooth animations and transitions throughout app
- [x] Custom vector icons for consistent design
- [x] Enhanced empty states with illustrations
- [x] Cloud sync with Firebase
- [x] Bookmarks with notes support
- [x] Collections for organizing books
- [x] Search, sort, and filter functionality
- [x] File validation and error handling
- [x] Network book download from URL

### Core Features ✅
- [x] Project structure and configuration
- [x] Room database with migrations
- [x] Book library UI with RecyclerView
- [x] Book data model with progress tracking
- [x] Material Design 3 theming
- [x] PDF viewer (Android PdfRenderer)
- [x] EPUB viewer (custom implementation)
- [x] MOBI viewer (basic PalmDB format)
- [x] CBZ (Comic Book ZIP) viewer
- [x] CBR (Comic Book RAR) viewer
- [x] Text file viewer
- [x] Settings and preferences
- [x] About screen

### Future Enhancements 🚀

#### User Experience
- [ ] **Dark Mode** - System-aware dark theme support
- [ ] **Reading Themes** - Additional color schemes (Sepia, Night, Custom)
- [ ] **Font Customization** - More font families and size options
- [ ] **Reading Statistics** - Track reading time, books finished, pages read
- [ ] **Achievements/Badges** - Gamification for reading milestones
- [ ] **Book Recommendations** - Suggest similar books based on reading history
- [ ] **Import/Export** - Backup and restore library data

#### Reading Features
- [x] **Advanced EPUB Rendering** - Better formatting and image support
- [x] **Table of Contents** - Navigate chapters easily
- [x] **Text-to-Speech** - Listen to books with TTS engine
- [x] **Highlighting** - Mark and annotate text passages
- [x] **Dictionary Integration** - Look up words while reading
- [x] **Night Mode Toggle** - Quick toggle in viewer
- [x] **Page Flip Animations** - Realistic page turning effects
- [ ] **Split-Screen Reading** - Compare pages or translations

#### Library Management
- [ ] **Smart Collections** - Auto-organize by author, genre, year
- [ ] **Tags System** - Flexible categorization beyond collections
- [ ] **Reading Goals** - Set and track reading targets
- [ ] **Book Ratings** - Personal rating system
- [ ] **Reading Lists** - Plan your reading queue
- [ ] **Advanced Search** - Full-text search within books
- [ ] **Duplicate Detection** - Find and merge duplicate books

#### Cloud & Sync
- [ ] **Multi-device Sync** - Real-time sync across devices
- [ ] **Sync Book Files** - Optional cloud storage for book files
- [ ] **Collaborative Collections** - Share collections with others
- [ ] **Reading Progress Sharing** - Share updates on social media
- [ ] **Backup to Google Drive** - Alternative cloud backup option

#### Performance & Technical
- [ ] **PDF Rendering Optimization** - Faster page loading and caching
- [ ] **Background Downloads** - Download books in background
- [ ] **Offline Mode Indicator** - Clear sync status display
- [ ] **Battery Optimization** - Reduce battery usage during reading
- [ ] **Memory Management** - Better handling of large files
- [ ] **File Format Extensions** - Support for FB2, AZW3, DjVu

#### Accessibility
- [ ] **Screen Reader Support** - Full TalkBack compatibility
- [ ] **High Contrast Mode** - Better visibility for low vision
- [ ] **Font Scaling** - Respect system font size preferences
- [ ] **Voice Commands** - Navigate with voice
- [ ] **Gesture Customization** - Customize reading gestures

#### Advanced Features
- [ ] **Note Syncing** - Sync highlights and notes to Markdown
- [ ] **Export Annotations** - Export highlights to text/PDF
- [ ] **Reading Challenges** - Participate in community challenges
- [ ] **Book Metadata Editing** - Comprehensive metadata management
- [ ] **Cover Image Search** - Auto-fetch covers from online sources
- [ ] **Plugin System** - Extensible architecture for custom features

## Cloud Sync

The app includes cloud sync functionality to synchronize reading progress, bookmarks, and annotations across multiple devices.

### Features

- **Firebase Integration**: Uses Firebase Firestore for cloud storage and Firebase Auth for user authentication
- **Anonymous Authentication**: Automatically signs in users anonymously for seamless sync
- **Automatic Sync**: Syncs reading progress and bookmarks automatically when changes are made
- **Manual Sync**: Trigger sync manually from the main menu or settings
- **Conflict Resolution**: Uses last-write-wins strategy based on timestamps
- **Offline Support**: Queues changes when offline and syncs when connection is restored
- **Background Sync**: Optional periodic background sync using WorkManager

### Setup Instructions

#### For Development/Testing

1. The app includes a placeholder `google-services.json` file for building
2. To enable actual sync functionality:
   - Create a Firebase project at [Firebase Console](https://console.firebase.google.com/)
   - Add an Android app with package name: `com.rifters.ebookreader`
   - Download your `google-services.json` file
   - Replace the placeholder file in `app/` directory
   - Enable Firestore and Authentication (Anonymous) in Firebase Console

#### For End Users

1. Open the app and go to **Settings**
2. Navigate to **Cloud Sync** section
3. Enable **Cloud Sync** toggle
4. The app will automatically authenticate anonymously
5. Enable **Auto Sync** to sync changes automatically
6. Use **Sync Now** button to manually trigger sync

### Data Synced

- **Reading Progress**: Current page, progress percentage, completion status
- **Bookmarks**: Page number, position, notes, and timestamps
- **Last Opened**: When each book was last read

**Note**: Book files themselves are NOT synced (only metadata). Books must be added to each device individually.

### Privacy

- Uses Firebase Anonymous Authentication (no personal information required)
- Only syncs reading progress and bookmarks, not book content
- Each user gets a unique anonymous ID
- Data is stored in user-specific Firestore collections

## Supported File Formats

### E-Books
- **PDF** - Portable Document Format (using Android PdfRenderer)
- **EPUB** - Electronic Publication (basic HTML extraction)
- **MOBI** - Mobipocket format (basic text extraction)
- **TXT** - Plain text files

### Comic Books
- **CBZ** - Comic Book ZIP archive
- **CBR** - Comic Book RAR archive

## Contributing

Contributions are welcome! Here's how you can help:

### Getting Started
1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes following the project's coding standards
4. Test your changes thoroughly
5. Commit with clear, descriptive messages
6. Push to your fork and submit a Pull Request

### Areas for Contribution
- **Bug Fixes**: Found a bug? Fix it and submit a PR
- **New Features**: Check the roadmap for planned features
- **Documentation**: Improve README, add code comments, or write guides
- **UI/UX**: Design improvements and accessibility enhancements
- **Testing**: Add unit tests, integration tests, or UI tests
- **Performance**: Optimize code for better speed and efficiency
- **Translations**: Help translate the app to other languages

### Code Standards
- Follow Kotlin coding conventions
- Use Material Design 3 components and guidelines
- Maintain MVVM architecture pattern
- Write meaningful commit messages
- Add tests for new features
- Update documentation as needed

## License

[Add your license here]

## Author

rifters

## Acknowledgments

- [Apache Commons Compress](https://commons.apache.org/proper/commons-compress/) for ZIP handling
- [junrar](https://github.com/junrar/junrar) for RAR extraction
- [Firebase](https://firebase.google.com/) for cloud sync infrastructure
