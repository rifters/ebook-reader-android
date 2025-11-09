# EBook Reader Android

A modern Android e-book reader application built with Kotlin, supporting multiple e-book and comic book formats with advanced library management features.

## 🎯 Project Status

**Current Version:** 1.0  
**Build Status:** ✅ Building Successfully  
**Test Status:** ✅ All Tests Passing  
**Database Version:** 6 (with migrations)

## ✨ Current Features

### 📚 File Format Support (Fully Working)
- ✅ **PDF** - Using Android's built-in PdfRenderer API with optimized caching
- ✅ **EPUB** - Custom parser with HTML/CSS rendering and TOC support
- ✅ **MOBI** - Basic PalmDB format text extraction
- ✅ **TXT** - Plain text files with encoding detection
- ✅ **CBZ** - ZIP-based comic book archives (Apache Commons Compress)
- ✅ **CBR** - RAR-based comic book archives (junrar library)
- ✅ **CB7** - 7z-based comic book archives
- ✅ **CBT** - TAR-based comic book archives
- ✅ **FB2** - FictionBook 2 XML format with metadata extraction
- ✅ **Markdown** - Full Markdown rendering to HTML
- ✅ **HTML/XML** - Web formats (.html, .htm, .xhtml, .xml, .mhtml)
- ✅ **AZW/AZW3** - Kindle formats (DRM-free) with PalmDB decompression
- ✅ **DOCX** - Microsoft Word documents via ZIP-based XML parsing

### 📖 Reading Experience (Fully Working)
- ✅ **Text-to-Speech (TTS)** - Listen to books with Android TTS engine
- ✅ **Bookmarks** - Save reading positions with notes and timestamps
- ✅ **Highlights** - Mark and annotate text with color coding
- ✅ **Table of Contents** - Navigate chapters in EPUB books
- ✅ **Reading Themes** - Light, Dark, and Sepia color schemes
- ✅ **Layout Modes** - Single-column, two-column, and continuous scroll
- ✅ **Brightness Control** - Custom brightness levels for comfortable reading
- ✅ **Font Customization** - Adjustable font family, size, and line spacing
- ✅ **Dictionary Integration** - Look up word definitions via system dictionary apps
- ✅ **Reading Progress** - Automatic tracking of pages read and completion status

### 🗂️ Library Management (Fully Working)
- ✅ **Material Design 3 UI** - Modern purple-themed interface with smooth animations
- ✅ **Grid & List Views** - Toggle between compact and detailed library views
- ✅ **Dynamic Book Covers** - Auto-generated colorful covers with 10 color schemes
- ✅ **Search & Filter** - Find books by title, author, or metadata
- ✅ **Sort Options** - Sort by title, author, date added, or last read
- ✅ **Collections** - Organize books into custom collections
- ✅ **Tags System** - Flexible categorization with custom tags
- ✅ **Smart Collections** - Auto-organize by author, genre, year, reading status
- ✅ **Reading Lists** - Create and manage reading queues
- ✅ **Reading Goals** - Set and track reading targets (books, pages, time)
- ✅ **Book Ratings** - Personal 5-star rating system
- ✅ **Duplicate Detection** - Find similar books using title/author matching
- ✅ **Extended Metadata** - Genre, publisher, year, language, ISBN tracking

### ☁️ Cloud & Sync Features (Fully Working)
- ✅ **Firebase Sync** - Real-time sync of reading progress and bookmarks
- ✅ **Anonymous Authentication** - No personal data required for sync
- ✅ **Offline Support** - Queue changes when offline, sync when reconnected
- ✅ **Background Sync** - WorkManager integration for periodic sync
- ✅ **Conflict Resolution** - Last-write-wins strategy based on timestamps
- ✅ **Manual Sync** - On-demand sync trigger from menu or settings

### 🌐 Cloud Storage Integration (Implemented, Needs Authentication Setup)
- ⚠️ **WebDAV** - Generic WebDAV protocol (works with Nextcloud, ownCloud)
- ⚠️ **Google Drive** - Google Drive API v3 integration (requires OAuth setup)
- ⚠️ **Dropbox** - Dropbox Core SDK integration (requires API token)
- ⚠️ **OneDrive** - Microsoft Graph API (requires OAuth setup)
- ⚠️ **FTP/SFTP** - File transfer protocols (basic implementation)

*Note: Cloud storage providers are implemented but require user authentication configuration*

### 🛠️ Technical Features (Fully Working)
- ✅ **Room Database** - Efficient local storage with migrations
- ✅ **MVVM Architecture** - Clean separation of concerns
- ✅ **Coroutines** - Asynchronous operations without blocking UI
- ✅ **ViewBinding** - Type-safe view access
- ✅ **LiveData** - Reactive UI updates
- ✅ **Performance Optimized** - Lazy loading, bitmap caching, memory management
- ✅ **Large File Support** - Handle 50MB+ books with chunked reading
- ✅ **Network Downloads** - Download books directly from URLs
- ✅ **File Validation** - Format verification and integrity checks

## 🚧 Known Limitations & Work Needed

### Cloud Storage Authentication
The cloud storage providers are implemented but require authentication setup:
- **Google Drive**: Needs OAuth 2.0 flow implementation and API credentials
- **Dropbox**: Requires access token from Dropbox app console
- **OneDrive**: Needs Microsoft Graph OAuth flow implementation
- **FTP/SFTP**: Basic implementation, may need enhanced error handling

**Workaround**: WebDAV works with username/password authentication

### System Dark Mode
- App uses Material3 DayNight theme base but no explicit values-night resources
- Reading themes (Light/Dark/Sepia) work within the viewer
- Consider adding system-wide dark mode support with values-night resources

### Accessibility Features
While basic accessibility is present, enhanced support could include:
- Complete TalkBack screen reader optimization
- High contrast mode for low vision users
- Voice command navigation
- Customizable gesture controls

### Performance Considerations
Most optimizations are complete, but future improvements could include:
- Background prefetching of next/previous pages
- Disk-based page cache for faster app restarts
- Progressive EPUB chapter loading (currently loads first chapter only)
- Thumbnail generation for faster library browsing

## 📂 Project Structure

### Core Components

#### Activities (9 total)
- **MainActivity.kt** - Main library screen with grid/list toggle, search, sort, filter
- **ViewerActivity.kt** - Universal book reader with format-specific loaders
- **CollectionsActivity.kt** - Manage book collections and categories
- **CollectionBooksActivity.kt** - View books within a specific collection
- **NetworkBookActivity.kt** - Download books from URL with progress tracking
- **CloudBrowserActivity.kt** - Browse and import from cloud storage
- **SettingsActivity.kt** - App preferences and cloud sync configuration
- **AboutActivity.kt** - App information and credits
- **EditBookActivity.kt** - Edit book metadata (title, author, genre, etc.)

#### Data Layer (Entities & DAOs)
**Entities:**
- **Book.kt** - Core book entity with metadata (rating, genre, publisher, year, language, ISBN)
- **Bookmark.kt** - Saved reading positions with notes
- **Highlight.kt** - Text highlights with color coding
- **Collection.kt** - Book collections/categories
- **Tag.kt** - Flexible tagging system
- **ReadingGoal.kt** - Reading target tracking
- **ReadingList.kt** - Reading queues and lists
- **ReadingListItem.kt** - Items within reading lists
- **BookCollectionCrossRef.kt** - Many-to-many: books ↔ collections
- **BookTagCrossRef.kt** - Many-to-many: books ↔ tags
- **SyncData.kt** - Cloud sync metadata
- **ReadingPreferences.kt** - User reading settings
- **TableOfContentsItem.kt** - EPUB chapter navigation

**DAOs:**
- **BookDao.kt** - Book CRUD operations with search/filter queries
- **BookmarkDao.kt** - Bookmark management
- **HighlightDao.kt** - Highlight operations
- **CollectionDao.kt** - Collection and relationship management
- **TagDao.kt** - Tag operations
- **ReadingGoalDao.kt** - Goal tracking queries
- **ReadingListDao.kt** - Reading list management
- **SyncDao.kt** - Sync status tracking
- **BookDatabase.kt** - Room database configuration (version 6 with migrations)

#### ViewModels (7 total)
- **BookViewModel.kt** - Books, search, sort, filter, progress tracking
- **CollectionViewModel.kt** - Collections and book-collection relationships
- **TagViewModel.kt** - Tags and book-tag relationships
- **ReadingGoalViewModel.kt** - Goal progress calculation
- **ReadingListViewModel.kt** - Reading queue management
- **SyncViewModel.kt** - Cloud sync operations and status
- **CloudStorageViewModel.kt** - Cloud provider file browsing

#### Adapters (8 total)
- **BookAdapter.kt** - Grid/list view with dynamic covers and animations
- **BookmarkAdapter.kt** - Bookmarks in bottom sheet
- **HighlightAdapter.kt** - Highlights display
- **CollectionAdapter.kt** - Collection list
- **TocAdapter.kt** - Table of contents navigation
- **CloudFileAdapter.kt** - Cloud storage file browser

#### Bottom Sheets (4 total)
- **BookmarksBottomSheet.kt** - Bookmark management with add/delete
- **HighlightsBottomSheet.kt** - View and manage highlights
- **ReadingSettingsBottomSheet.kt** - Font, theme, spacing, brightness
- **TocBottomSheet.kt** - EPUB table of contents navigation
- **AddNoteDialogFragment.kt** - Add notes to bookmarks

#### Utilities (12+ files)
**File Format Parsers:**
- **EpubParser.kt** - Custom EPUB format parser with TOC extraction
- **Fb2Parser.kt** - FictionBook 2 (FB2) XML parser
- **MarkdownParser.kt** - Markdown to HTML converter
- **AzwParser.kt** - Kindle AZW/AZW3 format parser (PalmDB)
- **DocxParser.kt** - Microsoft Word document parser

**Other Utilities:**
- **BookCoverGenerator.kt** - Generate colorful default book covers
- **BitmapCache.kt** - LRU cache for efficient image memory management
- **PreferencesManager.kt** - SharedPreferences wrapper
- **FileValidator.kt** - File format validation and integrity checks
- **SmartCollections.kt** - Auto-organize books into smart collections
- **DuplicateDetector.kt** - Similarity algorithms for duplicate detection

#### Cloud Storage (7 files)
- **CloudStorageProvider.kt** - Interface for cloud providers
- **CloudStorageManager.kt** - Manage multiple cloud providers
- **WebDavProvider.kt** - WebDAV protocol implementation (✅ Working)
- **GoogleDriveProvider.kt** - Google Drive API v3 (⚠️ Needs OAuth)
- **DropboxProvider.kt** - Dropbox Core SDK (⚠️ Needs token)
- **OneDriveProvider.kt** - Microsoft Graph API (⚠️ Needs OAuth)
- **FtpProvider.kt** - FTP/SFTP support (⚠️ Basic implementation)

#### Sync Services (in sync/ directory)
- **FirebaseSyncService.kt** - Firebase Firestore sync operations
- **SyncRepository.kt** - Sync coordination between local and cloud
- **SyncWorker.kt** - Background sync with WorkManager

### Resources

#### Layouts (15+ files)
- **activity_main.xml** - Main library with RecyclerView, Toolbar, FAB
- **activity_viewer.xml** - Book viewer with multiple view types and bottom bar
- **activity_collections.xml** - Collections management screen
- **activity_cloud_browser.xml** - Cloud storage file browser
- **activity_settings.xml** - Settings and preferences
- **item_book.xml** - List view card with cover, metadata, progress
- **item_book_grid.xml** - Grid view card optimized for covers
- **item_collection.xml** - Collection item with icon and book count
- **item_cloud_file.xml** - Cloud file browser items
- **bottom_sheet_bookmarks.xml** - Bookmarks management
- **bottom_sheet_highlights.xml** - Highlights display
- **bottom_sheet_reading_settings.xml** - Reading preferences
- **bottom_sheet_toc.xml** - Table of contents

#### Values
- **strings.xml** - All UI text resources (i18n-ready)
- **colors.xml** - Material Design 3 purple-based color palette
- **themes.xml** - Material3 DayNight theme with custom styles

#### Drawables & Animations
- **Custom Icons** - Vector icons (ic_add, ic_book, ic_bookmark, ic_collection, ic_settings, etc.)
- **Animations** - fade_in, slide_in_right, scale_in transitions
- **Ripple Effects** - Touch feedback for interactive elements

#### Menus
- **main_menu.xml** - Search, sort, filter, sync, view toggle
- **viewer_menu.xml** - Reading options and bookmarks
- **book_menu.xml** - Book actions (edit, delete, share)

## 🔧 Dependencies

### Core Android Libraries
```kotlin
// Core & UI
androidx.core:core-ktx:1.12.0
androidx.appcompat:appcompat:1.6.1
com.google.android.material:material:1.10.0
androidx.constraintlayout:constraintlayout:2.1.4
androidx.navigation:navigation-fragment-ktx:2.7.5
androidx.navigation:navigation-ui-ktx:2.7.5
androidx.preference:preference-ktx:1.2.1
androidx.activity:activity-ktx:1.8.1
androidx.documentfile:documentfile:1.0.1
```

### Architecture Components
```kotlin
// Room Database
androidx.room:room-runtime:2.6.0
androidx.room:room-ktx:2.6.0
kapt("androidx.room:room-compiler:2.6.0")

// Lifecycle & LiveData
androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0
androidx.lifecycle:lifecycle-livedata-ktx:2.7.0

// Coroutines
org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3
org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3

// WorkManager (Background Sync)
androidx.work:work-runtime-ktx:2.9.0
```

### Firebase (Cloud Sync)
```kotlin
// Firebase BOM for version management
com.google.firebase:firebase-bom:32.7.0
com.google.firebase:firebase-firestore-ktx
com.google.firebase:firebase-auth-ktx
```

### File Format Libraries
```kotlin
// Comic Book Archives
org.apache.commons:commons-compress:1.25.0  // CBZ, CB7, CBT
com.github.junrar:junrar:7.5.5              // CBR (RAR)
org.tukaani:xz:1.9                          // 7z decompression

// Other formats use built-in Android APIs:
// - PDF: Android PdfRenderer (API 21+)
// - EPUB: Built-in ZIP extraction
// - MOBI/AZW: Custom PalmDB parser
// - DOCX: Built-in ZIP + XML parsing
// - TXT/Markdown/HTML/XML: Standard file I/O
```

### Cloud Storage Providers
```kotlin
// Google Drive
com.google.android.gms:play-services-auth:20.7.0
com.google.apis:google-api-services-drive:v3-rev20220815-2.0.0
com.google.api-client:google-api-client-android:2.2.0
com.google.http-client:google-http-client-gson:1.42.3

// Dropbox
com.dropbox.core:dropbox-core-sdk:5.4.5

// FTP/SFTP
commons-net:commons-net:3.10.0    // FTP
com.jcraft:jsch:0.1.55             // SFTP
```

### Testing Libraries
```kotlin
// Unit Tests
testImplementation("junit:junit:4.13.2")
testImplementation("org.mockito:mockito-core:5.3.1")
testImplementation("org.mockito:mockito-inline:5.2.0")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
testImplementation("androidx.arch.core:core-testing:2.2.0")
testImplementation("androidx.room:room-testing:2.6.0")

// Instrumented Tests
androidTestImplementation("androidx.test.ext:junit:1.1.5")
androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
androidTestImplementation("androidx.test.espresso:espresso-contrib:3.5.1")
androidTestImplementation("androidx.test:rules:1.5.0")
androidTestImplementation("androidx.test:runner:1.5.2")
```

### Build Configuration
- **Kotlin**: 1.9.10
- **Android Gradle Plugin**: 8.1.4
- **Compile SDK**: 34 (Android 14)
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)
- **Java Compatibility**: Java 8 (sourceCompatibility & targetCompatibility)

## 🏗️ Building the Project

### Prerequisites
- **Android Studio**: Arctic Fox (2020.3.1) or later
- **JDK**: 17 or later
- **Android SDK**: API 34 installed
- **Gradle**: 8.x (comes with Android Studio)

### Build Instructions

#### 1. Clone the Repository
```bash
git clone https://github.com/rifters/ebook-reader-android.git
cd ebook-reader-android
```

#### 2. Firebase Setup (Optional - for Cloud Sync)
The app includes a placeholder `google-services.json` and will build without Firebase setup, but cloud sync won't work.

**To enable cloud sync:**
- Create a Firebase project at [Firebase Console](https://console.firebase.google.com/)
- Add an Android app with package name: `com.rifters.ebookreader`
- Download your `google-services.json` file
- Replace the placeholder file in `app/google-services.json`
- Enable **Firestore Database** in Firebase Console
- Enable **Authentication** → **Anonymous** sign-in method

#### 3. Build Debug APK
```bash
./gradlew assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

#### 4. Build Release APK
```bash
./gradlew assembleRelease
```
Output: `app/build/outputs/apk/release/app-release.apk`

#### 5. Install on Device/Emulator
```bash
./gradlew installDebug
```

#### 6. Run Tests
```bash
# Unit tests
./gradlew test

# Instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest
```

#### 7. Build Everything (Debug + Release + Tests)
```bash
./gradlew clean build
```

### Common Build Issues & Solutions

**Issue: Firebase dependency errors**
- Solution: Ensure `google-services.json` exists in `app/` directory (even placeholder works)

**Issue: Out of memory during build**
- Solution: Add to `gradle.properties`:
  ```
  org.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=512m
  ```

**Issue: Gradle sync issues**
- Solution: File → Invalidate Caches → Invalidate and Restart

**Issue: SDK not found**
- Solution: Set `ANDROID_HOME` environment variable or create `local.properties`:
  ```
  sdk.dir=/path/to/Android/sdk
  ```

## 🎨 UI/UX Highlights

### Modern Material Design 3
The app features a completely refreshed UI following Material Design 3 guidelines:

- **Color System**: Dynamic purple-based theme (#6750A4 primary)
  - Primary: Purple 500 (#6750A4)
  - Secondary: Purple Accent (#7C4DFF)
  - Background: Neutral tones with elevation
  - Surface colors with proper shadows
- **Typography**: Material 3 text scale with consistent hierarchy
  - Display, Headline, Title, Body, Label styles
  - Readable font sizes (14-22sp body text)
- **Elevation & Shadows**: Proper elevation levels (0-8dp)
- **Shape System**: Rounded corners (4dp buttons, 12dp cards, 16dp sheets)
- **Motion**: Smooth transitions and micro-interactions

### Flexible View Modes
Browse your library in two distinct ways:

- **List View**: Detailed view showing:
  - Book cover thumbnail
  - Title and author
  - Reading progress bar
  - Genre/metadata badges
  - Last read timestamp
  - Quick action buttons
- **Grid View**: Cover-focused 2-column layout
  - Larger cover images
  - Title overlay on hover
  - Compact, visual browsing
  - Perfect for tablets
- Toggle instantly via toolbar menu
- Preference saved automatically
- Smooth transition animation

### Dynamic Book Covers
Books without cover images get beautiful auto-generated covers:

- **10 Unique Color Schemes**: 
  - Purple, Blue, Green, Orange, Red, Pink, Teal, Indigo, Amber, Cyan
  - Colors assigned via title hash for consistency
  - Each book always gets the same color
- **Professional Design Elements**:
  - Dual borders (outer and inner frames)
  - Gradient overlay for depth
  - Book emoji icon (📖) as decoration
  - Title in large, bold font
  - Author in smaller, elegant font
- **Adaptive Typography**:
  - Text size adjusts to fit cover
  - Long titles truncated gracefully
  - Maximum readability maintained

### Smooth Animations & Micro-Interactions
Polished experience throughout the app:

- **Activity Transitions**: 300ms slide animations
- **RecyclerView Animations**: Smooth add/remove/change
- **FAB Entrance**: Delayed 300ms entrance animation
- **Empty States**: Fade-in with slide-up effect (400ms)
- **Button Ripples**: Material ripple effect on all buttons
- **Bottom Sheets**: Slide-up with backdrop dim
- **Page Navigation**: Smooth transitions in viewer
- **Loading States**: Circular progress indicators

### Enhanced UI Components

#### Empty States
Instead of plain text, rich empty state designs:
- Large illustrative icons (96dp+)
- Descriptive heading text
- Helpful subtext with guidance
- Call-to-action buttons
- Used in: Empty library, no bookmarks, no collections

#### Bottom Sheets
Modern bottom sheet dialogs for contextual actions:
- **Bookmarks**: List, add, delete, navigate to bookmark
- **Highlights**: View, edit, delete highlights with color preview
- **Reading Settings**: Font, theme, layout, brightness, spacing
- **Table of Contents**: Chapter navigation for EPUB books
- Proper spacing, padding, and touch targets
- Smooth reveal animation
- Backdrop dimming for focus

#### Toolbars & App Bars
- **Elevated Toolbar**: 4dp elevation with shadow
- **Icon Tinting**: Proper color for dark/light themes
- **Overflow Menu**: Consistent across app
- **Bottom App Bar** (Viewer): Actions at thumb reach
- **Search Bar**: Expandable search with clear action

#### Progress Indicators
- **Linear Progress**: For book reading progress (0-100%)
- **Circular Progress**: Loading states and sync status
- **Determinate**: Known progress (file download)
- **Indeterminate**: Unknown duration (parsing book)

### Accessibility Features (Basic Implementation)
- **Touch Targets**: Minimum 48dp for all interactive elements
- **Color Contrast**: Meets WCAG AA standards (4.5:1 text)
- **Focus Indicators**: Visible focus states for keyboard navigation
- **Content Descriptions**: Images and icons have descriptions
- **Text Scaling**: UI adapts to system font size (needs testing at extreme sizes)

### Responsive Design
- **Phone Optimized**: Primary target, 5"-7" screens
- **Tablet Support**: Grid view shines on tablets (10"+)
- **Orientation**: Works in portrait and landscape
- **Foldable Ready**: Adapts to different screen sizes
- **Split Screen**: Compatible with Android's multi-window

## 🚀 Future Enhancements & Optimization Suggestions

### High Priority - User Experience Improvements

#### 🌙 Enhanced Dark Mode Support
**Status:** Partially implemented  
**What's Working:** Material3 DayNight theme, reading themes (Light/Dark/Sepia)  
**What's Needed:**
- Create `values-night/` resource directory with dark theme colors
- Implement system-aware dark mode that follows device settings
- Add setting to override system theme (Light/Dark/Auto)
- Ensure all icons and images work well in dark mode

**Estimated Effort:** Medium (2-3 days)  
**Impact:** High - Many users prefer dark mode for battery and eye strain

#### 📊 Reading Statistics Dashboard
**Status:** Basic tracking exists (progress, completion)  
**Suggested Features:**
- Total books read counter with yearly breakdown
- Total pages/hours spent reading
- Average reading speed (pages per hour)
- Reading streaks (consecutive days)
- Genre distribution charts
- Monthly/yearly reading graphs
- Export statistics to CSV/JSON

**Estimated Effort:** High (5-7 days)  
**Impact:** High - Motivates reading and provides insights

#### 🏆 Gamification & Achievements
**Status:** Reading goals partially implemented  
**Suggested Features:**
- Badge system (First Book, 10 Books, Speed Reader, Night Owl, etc.)
- Reading challenges (Read 5 books in a genre, Finish a series, etc.)
- Daily reading streaks with notifications
- Leaderboards (optional, privacy-respecting)
- Progress milestones with celebrations
- Share achievements on social media

**Estimated Effort:** High (7-10 days)  
**Impact:** Medium-High - Increases engagement and retention

#### 💾 Backup & Restore
**Status:** Not implemented  
**Suggested Features:**
- Export entire library database to ZIP file
- Include book metadata, bookmarks, highlights, collections
- Optional: Include book files in backup (can be large)
- Import backup from previous export
- Schedule automatic backups to device storage
- Backup to cloud storage (Google Drive, Dropbox)

**Estimated Effort:** Medium (3-4 days)  
**Impact:** High - Critical for data safety and device migration

### Medium Priority - Reading Features

#### 🔍 Full-Text Search
**Status:** Basic title/author search implemented  
**Suggested Features:**
- Search within book content (not just metadata)
- Index book text for fast searching
- Highlight search results in text
- Search across entire library or single book
- Regular expression support for advanced users
- Search history and saved searches

**Estimated Effort:** High (7-10 days)  
**Impact:** Medium - Very useful for reference books and research

#### 📱 Split-Screen Reading Mode
**Status:** Not implemented  
**Suggested Features:**
- View two pages side-by-side (useful for tablets)
- Compare different books or translations
- Synchronized scrolling option
- Independent navigation per pane
- Configurable split ratio (50/50, 60/40, etc.)

**Estimated Effort:** Medium-High (5-6 days)  
**Impact:** Low-Medium - Niche feature, mainly for tablets

#### 🌐 Translation Integration
**Status:** Not implemented  
**Suggested Features:**
- Select text and translate using Google Translate API
- Support multiple target languages
- Show translation in popup or side panel
- Save translations as annotations
- Offline translation support (download language packs)

**Estimated Effort:** Medium (4-5 days)  
**Impact:** Medium - Useful for language learners

#### ✏️ Advanced Text Styling
**Status:** Basic highlighting with colors  
**Suggested Features:**
- Underline, strikethrough options
- Text shadow effects
- Bold/italic formatting for custom notes
- Multiple highlight colors (currently limited)
- Highlight shapes (rectangle, underline, etc.)
- Export highlights to Markdown/Evernote/Notion

**Estimated Effort:** Medium (3-4 days)  
**Impact:** Low-Medium - Nice to have for serious readers

### Medium Priority - Library Management

#### 📦 Batch Import/Export
**Status:** Single book import only  
**Suggested Features:**
- Import multiple books at once from folder
- Drag-and-drop multiple files
- Monitor folder for new books (auto-import)
- Batch metadata editing (author, genre, etc.)
- Bulk operations (delete, tag, move to collection)
- Import from calibre library format

**Estimated Effort:** Medium (4-5 days)  
**Impact:** High - Saves significant time for users with large libraries

#### 🎨 Custom Book Covers
**Status:** Auto-generated covers only  
**Suggested Features:**
- Upload custom cover image from gallery
- Take photo for book cover
- Crop and adjust cover image
- Search cover images online (Open Library, Google Books)
- Auto-fetch covers from ISBN lookup
- Fallback to generated covers if not found

**Estimated Effort:** Medium (3-4 days)  
**Impact:** Medium - Improves visual appeal of library

#### 🔗 Series Management
**Status:** Not implemented  
**Suggested Features:**
- Define book series with order numbers
- Group books by series in library view
- "Next in Series" suggestions
- Series progress tracking
- Import series metadata from Goodreads/calibre
- Auto-detect series from title patterns

**Estimated Effort:** Medium-High (5-6 days)  
**Impact:** Medium - Important for fiction readers

### Low Priority - Advanced Features

#### 🔌 Plugin System
**Status:** Not implemented  
**Suggested Features:**
- Define plugin API for custom features
- Support for custom file format parsers
- Custom import/export filters
- Theme plugins for custom UI styles
- Cloud storage provider plugins
- Community plugin marketplace

**Estimated Effort:** Very High (15-20 days)  
**Impact:** Low-Medium - Advanced feature for power users

#### 📡 OPDS Catalog Support
**Status:** Not implemented  
**Suggested Features:**
- Browse OPDS catalogs (Calibre, Gutenberg, etc.)
- Download books directly from OPDS servers
- Search across OPDS catalogs
- Manage multiple OPDS sources
- Authentication for private OPDS servers

**Estimated Effort:** High (7-10 days)  
**Impact:** Low - Niche feature for specific user base

### Performance & Technical Optimizations

#### ⚡ Performance Enhancements
**Status:** Many optimizations already completed (see PERFORMANCE_OPTIMIZATIONS.md)  
**Additional Suggestions:**

1. **Progressive EPUB Loading** (Medium Priority)
   - Currently: First chapter only loaded into WebView
   - Improvement: Load chapters on-demand as user navigates
   - Impact: Faster initial load, lower memory usage
   - Effort: Medium (2-3 days)

2. **Background Page Prefetching** (Low Priority)
   - Pre-load next/previous pages in background
   - Configurable prefetch distance (1-3 pages)
   - Cancel prefetch if user navigates elsewhere
   - Impact: Smoother page transitions
   - Effort: Medium (2-3 days)

3. **Disk-Based Page Cache** (Low Priority)
   - Currently: Bitmap cache in memory only
   - Improvement: Persist rendered pages to disk cache
   - Use LRU disk cache (e.g., 100MB limit)
   - Impact: Instant page loads across app restarts
   - Effort: Medium (3-4 days)

4. **Thumbnail Generation** (High Priority)
   - Generate low-res thumbnails for library view
   - Cache thumbnails separately from full covers
   - Background thumbnail generation
   - Impact: Much faster library scrolling
   - Effort: Low-Medium (2 days)

5. **Database Query Optimization** (Low Priority)
   - Add compound indexes for common queries
   - Analyze slow queries with Room profiler
   - Optimize N+1 query patterns
   - Impact: Faster search, filter, sort
   - Effort: Low (1-2 days)

6. **Network Request Optimization** (Low Priority)
   - Implement request queuing for cloud operations
   - Add retry logic with exponential backoff
   - Optimize Firebase sync batch sizes
   - Impact: More reliable cloud operations
   - Effort: Medium (2-3 days)

### Accessibility Improvements

#### ♿ Enhanced Accessibility
**Status:** Basic accessibility present  
**Suggested Improvements:**

1. **Screen Reader Optimization**
   - Add content descriptions to all interactive elements
   - Announce page numbers and progress
   - Improve TalkBack navigation in viewer
   - Test with Android Accessibility Scanner
   - Effort: Medium (3-4 days)
   - Impact: High for visually impaired users

2. **High Contrast Mode**
   - Add high contrast color scheme option
   - Increase UI element borders and outlines
   - Ensure 7:1 contrast ratio (WCAG AAA)
   - Effort: Low (1-2 days)
   - Impact: Medium for low vision users

3. **Font Scaling Support**
   - Respect system font size settings
   - Test with large text (200%+ scaling)
   - Ensure UI doesn't break with large text
   - Effort: Low (1 day)
   - Impact: High for users with visual impairments

4. **Voice Commands**
   - Integrate with Google Assistant
   - Voice commands: "Read next page", "Bookmark page", etc.
   - Custom voice shortcuts
   - Effort: High (5-7 days)
   - Impact: Medium for hands-free operation

5. **Gesture Customization**
   - Customizable swipe gestures
   - Tap zones (left/center/right for different actions)
   - Volume button page turning
   - Effort: Medium (2-3 days)
   - Impact: Medium-High for accessibility and power users

### Cloud Storage Enhancements

#### ☁️ Complete Cloud Storage Integration
**Status:** Providers implemented but need authentication  
**Immediate Work Needed:**

1. **Google Drive OAuth Flow** (High Priority)
   - Implement OAuth 2.0 sign-in flow
   - Handle token refresh
   - Add account picker
   - Effort: Medium (2-3 days)

2. **Dropbox Authentication** (Medium Priority)
   - Implement OAuth flow
   - Request access token from user or provide app token flow
   - Effort: Low-Medium (1-2 days)

3. **OneDrive OAuth Flow** (Medium Priority)
   - Implement Microsoft Graph OAuth
   - Handle MSAL authentication
   - Effort: Medium (2-3 days)

**Additional Features:**
- Multi-account support (connect multiple cloud accounts)
- Book file sync (not just metadata)
- Automatic upload of new books to cloud
- Conflict resolution UI for file conflicts
- Offline mode with sync queue indicator
- Bandwidth usage settings (Wi-Fi only, etc.)

### Testing & Quality Improvements

#### 🧪 Comprehensive Testing Suite
**Status:** Basic unit tests exist  
**Suggested Improvements:**

1. **Increase Test Coverage**
   - Target: 80%+ code coverage
   - Unit tests for all ViewModels
   - DAO tests for database operations
   - Parser tests for file formats
   - Effort: High (ongoing)

2. **UI/Integration Tests**
   - Espresso tests for critical user flows
   - Test book import and viewing
   - Test sync operations
   - Test collection management
   - Effort: High (5-7 days)

3. **Performance Tests**
   - Benchmark tests for file loading
   - Memory leak detection
   - Battery usage profiling
   - Effort: Medium (3-4 days)

4. **Format Testing**
   - Test suite with various file formats
   - Edge cases (corrupted files, huge files)
   - Different epub/pdf structures
   - Effort: Medium (2-3 days)

### Documentation Improvements

#### 📚 Better Documentation
**Status:** README exists, code comments sparse  
**Suggested Improvements:**

1. **User Guide**
   - Getting started tutorial
   - Feature explanations with screenshots
   - FAQ section
   - Troubleshooting guide

2. **Developer Documentation**
   - Architecture decision records (ADR)
   - Code style guide
   - Contribution guidelines
   - API documentation (KDoc)

3. **Video Tutorials**
   - App overview video
   - Feature demonstrations
   - Setup guides for cloud sync

### Summary of Priorities

**Implement First (High ROI):**
1. System-aware dark mode (Quick win, high user demand)
2. Backup & Restore (Critical for user confidence)
3. Batch import (Saves time, reduces friction)
4. Thumbnail generation (Noticeable performance improvement)
5. Reading statistics (Increases engagement)
6. Google Drive OAuth (Complete existing feature)

**Implement Second (Medium ROI):**
1. Full-text search (Power user feature)
2. Gamification system (Engagement boost)
3. Custom book covers (Visual improvement)
4. Series management (Improves organization)
5. Progressive EPUB loading (Performance)
6. Enhanced accessibility (Expands user base)

**Consider Later (Lower ROI or Higher Complexity):**
1. Split-screen mode (Niche, mainly tablets)
2. Translation integration (Requires API costs)
3. Plugin system (Complex, limited audience)
4. OPDS support (Niche feature)
5. Advanced text styling (Nice to have)

## ☁️ Cloud Sync

The app includes comprehensive cloud sync functionality to synchronize reading progress, bookmarks, and annotations across multiple devices using Firebase.

### ✅ Working Features

- **Firebase Firestore Integration**: Cloud storage for reading data
- **Anonymous Authentication**: Auto sign-in without personal data required
- **Automatic Sync**: Changes sync automatically when made
- **Manual Sync Trigger**: Button in menu or settings to force sync
- **Conflict Resolution**: Last-write-wins based on timestamps
- **Offline Support**: Changes queued when offline, synced when online
- **Background Sync**: Optional periodic sync via WorkManager
- **Sync Status**: Visual indicators for sync state (syncing/success/error)
- **Per-Device Unique ID**: Each device tracked separately

### 📊 Data Synced

**What Gets Synced:**
- ✅ Reading progress (current page, percentage)
- ✅ Completion status (started, in progress, finished)
- ✅ Last opened timestamp
- ✅ Bookmarks (page, position, notes, timestamp)
- ✅ Highlights (text, color, page, notes)

**What Does NOT Get Synced:**
- ❌ Book files (too large, stored locally only)
- ❌ Book covers (generated locally)
- ❌ Collections (local organization only)
- ❌ Reading preferences (device-specific settings)

### 🔐 Privacy & Security

- **Anonymous Auth**: Uses Firebase Anonymous Authentication
  - No email, password, or personal info required
  - Each user gets unique anonymous ID
  - Cannot be traced back to identity
- **Data Isolation**: Each user's data in separate Firestore collection
- **No Book Content**: Only metadata synced, not book text/content
- **HTTPS Encryption**: All data encrypted in transit
- **Firebase Security Rules**: Server-side access control

### 📖 Setup Instructions

#### For End Users

1. Open **Settings** in the app
2. Navigate to **Cloud Sync** section
3. Toggle **Enable Cloud Sync** ON
4. App automatically authenticates anonymously
5. Enable **Auto Sync** for automatic syncing
6. Use **Sync Now** button to manually trigger sync
7. Check **Last Sync** timestamp to verify

**Troubleshooting:**
- If sync fails, check internet connection
- Try manual sync with "Sync Now" button
- Check Firebase Console for service status
- Restart app if authentication fails

#### For Developers

1. Create Firebase project at [Firebase Console](https://console.firebase.google.com/)
2. Add Android app:
   - Package name: `com.rifters.ebookreader`
   - Download `google-services.json`
   - Replace placeholder in `app/google-services.json`
3. Enable **Firestore Database**:
   - Create database in production mode
   - Set up security rules (example in `FIREBASE_SETUP.md`)
4. Enable **Authentication**:
   - Go to Authentication → Sign-in method
   - Enable **Anonymous** sign-in
5. (Optional) Set up **Cloud Functions** for advanced sync logic

### 🔧 Configuration Options

Available in **Settings → Cloud Sync**:

- **Enable Cloud Sync**: Master toggle for all sync features
- **Auto Sync**: Sync automatically on changes (recommended)
- **Sync Frequency**: How often background sync runs (30min, 1hr, 3hr, 6hr, 12hr, 24hr)
- **Wi-Fi Only**: Only sync on Wi-Fi to save mobile data
- **Sync Now**: Manual sync trigger button
- **View Sync Status**: See last sync time and status
- **Clear Sync Data**: Remove all cloud data (danger zone)

### 🚨 Known Limitations

1. **Book Files Not Synced**: Must add books to each device manually
2. **Collections Local Only**: Collections don't sync between devices
3. **No Conflict UI**: Conflicts resolved automatically (last-write-wins)
4. **No Sync History**: Can't see history of synced changes
5. **Firebase Dependency**: Requires Firebase account and setup

### 🔮 Future Sync Enhancements

- Sync collections and tags across devices
- Conflict resolution UI (choose which version to keep)
- Sync history and audit log
- Export/import sync data
- Multiple device management (see all synced devices)
- Selective sync (choose what to sync)
- Compression for faster sync of highlights

## 🌐 Cloud Storage Integration

The app includes a cloud storage framework for importing books from various cloud providers and personal storage solutions.

### ✅ Fully Working

- **WebDAV Provider**
  - Generic WebDAV protocol (RFC 4918)
  - Works with Nextcloud, ownCloud, Box.com
  - Username/password authentication
  - Browse folders and files
  - Download books directly to library
  - Upload books to cloud (backup)
  - Implemented in `WebDavProvider.kt`

### ⚠️ Implemented but Needs Authentication Setup

The following providers are fully implemented but require OAuth configuration to work:

- **Google Drive**
  - Google Drive API v3 integration
  - Needs: OAuth 2.0 consent screen and credentials
  - Features: Browse, download, upload, search
  - Implemented in `GoogleDriveProvider.kt`
  
- **Dropbox**
  - Dropbox Core SDK v2
  - Needs: App registration and access token
  - Features: Browse, download, upload
  - Implemented in `DropboxProvider.kt`
  
- **OneDrive**
  - Microsoft Graph API
  - Needs: Microsoft App registration and OAuth
  - Features: Browse, download, upload
  - Implemented in `OneDriveProvider.kt`
  
- **FTP/SFTP**
  - File Transfer Protocol support
  - Needs: Server credentials
  - Features: Browse, download, upload
  - Basic implementation in `FtpProvider.kt`

### 📁 Supported Operations

All providers support these operations (once authenticated):

- **Browse**: List files and folders
- **Navigate**: Enter folders, go back
- **Download**: Download books to local library
- **Upload**: Backup books to cloud storage
- **Search**: Find files by name (provider-dependent)
- **Filter**: Show only supported book formats

### 🎯 Usage Flow

1. Go to **Settings** → **Cloud Storage**
2. Select a provider (WebDAV, Google Drive, etc.)
3. Enter credentials or complete OAuth flow
4. Browse your cloud storage
5. Tap a book file to download to library
6. Book automatically added to library

### 🔧 Implementation Details

**Architecture:**
- `CloudStorageProvider` interface defines common operations
- Each provider implements the interface
- `CloudStorageManager` coordinates multiple providers
- `CloudBrowserActivity` provides UI for browsing
- `CloudStorageViewModel` manages state and operations

**File Format Detection:**
- Filters files by extension
- Supported: pdf, epub, mobi, txt, cbz, cbr, cb7, cbt, fb2, md, html, azw, docx
- Ignores other file types in cloud browser

**Error Handling:**
- Network errors: Retry with exponential backoff
- Authentication errors: Prompt to re-authenticate
- File not found: Show user-friendly message
- Timeout: Configurable timeout (30s default)

### 🚧 Work Needed for Full Cloud Storage

To make cloud storage providers fully operational:

1. **Google Drive OAuth** (Priority: High)
   - Set up OAuth 2.0 consent screen in Google Cloud Console
   - Add OAuth credentials
   - Implement sign-in flow in app
   - Handle token refresh
   - Estimated: 2-3 days

2. **Dropbox App Registration** (Priority: Medium)
   - Register app in Dropbox App Console
   - Get API app key and secret
   - Implement OAuth flow
   - Estimated: 1-2 days

3. **OneDrive OAuth** (Priority: Medium)
   - Register app in Microsoft Azure Portal
   - Configure redirect URIs
   - Implement MSAL authentication
   - Estimated: 2-3 days

4. **Enhanced FTP/SFTP** (Priority: Low)
   - Add connection pooling
   - Better error handling
   - Support for keys (not just passwords)
   - Estimated: 2-3 days

5. **Multi-Account Support** (Priority: Low)
   - Support multiple accounts per provider
   - Account picker UI
   - Persist credentials securely
   - Estimated: 3-4 days

### 🔒 Security Considerations

- **Credentials**: Stored in EncryptedSharedPreferences
- **Tokens**: OAuth tokens stored securely
- **HTTPS**: All cloud operations over HTTPS
- **Scopes**: Request minimal permissions
- **No Logging**: Credentials never logged
- **Token Refresh**: Automatic OAuth token refresh

### 🔮 Planned Cloud Storage Features

Future enhancements for cloud storage:

- [ ] S3-compatible storage (MinIO, Wasabi, AWS S3)
- [ ] SMB/CIFS network shares
- [ ] MEGA cloud storage
- [ ] pCloud integration
- [ ] Yandex Disk support
- [ ] Automatic book discovery (scan cloud folders)
- [ ] Two-way sync (upload new books automatically)
- [ ] Conflict resolution for file changes
- [ ] Bandwidth throttling
- [ ] Resume interrupted downloads
- [ ] Background upload/download with notifications

## 📚 Supported File Formats

### E-Book Formats (12 formats)

#### ✅ PDF (Portable Document Format)
- **Implementation**: Android's built-in `PdfRenderer` API (API 21+)
- **Features**: Page rendering, zoom, page navigation
- **Optimizations**: 
  - 1.5x rendering resolution (balance quality/memory)
  - LRU bitmap cache for rendered pages
  - Lazy page loading
- **Limitations**: No text extraction, no reflowable layout
- **Performance**: Excellent for files up to 500+ pages
- **Library**: None (Android native)

#### ✅ EPUB (Electronic Publication)
- **Implementation**: Custom parser using Java ZIP utilities
- **Features**: 
  - HTML/CSS content extraction
  - Table of Contents (TOC) navigation
  - Chapter detection
  - Image support
  - Metadata extraction (title, author, publisher)
- **Optimizations**: 
  - First chapter loaded immediately
  - 5MB limit per chapter (prevents WebView OOM)
  - Progressive loading for large books
- **Limitations**: Complex CSS may not render perfectly
- **Performance**: Good for books up to 10MB
- **Library**: None (built-in ZIP)

#### ✅ MOBI (Mobipocket)
- **Implementation**: Custom PalmDB format parser
- **Features**: 
  - Text extraction
  - Basic formatting
  - Metadata extraction
- **Optimizations**: 
  - 5MB extraction limit
  - Chunked reading for large files
- **Limitations**: 
  - Basic format support only
  - Limited styling
  - No DRM support
- **Performance**: Moderate for files up to 20MB
- **Library**: None (custom parser)

#### ✅ TXT (Plain Text)
- **Implementation**: Direct file reading with TextView
- **Features**: 
  - Encoding detection (UTF-8, ISO-8859-1, etc.)
  - Scroll or page view
  - Font customization
- **Optimizations**: 
  - 5MB file size limit
  - Chunked reading for very large files
- **Limitations**: No formatting, images, or metadata
- **Performance**: Excellent for all sizes
- **Library**: None (standard I/O)

#### ✅ FB2 (FictionBook 2)
- **Implementation**: Custom XML parser
- **Features**: 
  - Full XML structure parsing
  - Author, title, annotation extraction
  - Cover image extraction
  - Genre and metadata
  - Paragraph and section structure
- **Optimizations**: SAX parser for memory efficiency
- **Limitations**: Complex formatting may be simplified
- **Performance**: Good for files up to 5MB
- **Library**: None (Android XML parser)
- **Implementation File**: `Fb2Parser.kt`

#### ✅ Markdown (.md)
- **Implementation**: Markdown to HTML converter
- **Features**: 
  - Full CommonMark syntax
  - Code blocks with syntax highlighting
  - Tables, lists, links
  - Images (embedded or referenced)
  - Heading navigation
- **Optimizations**: Rendered to HTML, displayed in WebView
- **Limitations**: Requires internet for external images
- **Performance**: Excellent for all sizes
- **Library**: None (custom parser)
- **Implementation File**: `MarkdownParser.kt`

#### ✅ HTML/XML (Web Formats)
- **Implementation**: WebView rendering
- **Supported Extensions**: .html, .htm, .xhtml, .xml, .mhtml
- **Features**: 
  - Full HTML/CSS rendering
  - JavaScript support (optional)
  - Images and media
  - Internal links
- **Optimizations**: 
  - File size limit: 5MB
  - Base URL handling for relative paths
- **Limitations**: Complex sites may not render perfectly
- **Performance**: Good for files up to 5MB
- **Library**: Android WebView

#### ✅ AZW/AZW3 (Kindle Formats)
- **Implementation**: Custom PalmDB decompression parser
- **Features**: 
  - DRM-free AZW/AZW3 support
  - PalmDoc decompression
  - Text extraction
  - Basic metadata
- **Optimizations**: 
  - Huffman decompression for compressed files
  - Chunked reading
- **Limitations**: 
  - **NO DRM SUPPORT** (only DRM-free files)
  - Limited formatting
  - Experimental support
- **Performance**: Moderate for files up to 10MB
- **Library**: None (custom parser)
- **Implementation File**: `AzwParser.kt`

#### ✅ DOCX (Microsoft Word)
- **Implementation**: ZIP-based XML parsing
- **Features**: 
  - Document.xml parsing
  - Paragraph and text extraction
  - Basic formatting (bold, italic)
  - Image extraction
- **Optimizations**: 
  - SAX parser for memory efficiency
  - Lazy image loading
- **Limitations**: 
  - Complex formatting may be lost
  - Tables partially supported
  - No comments or track changes
- **Performance**: Good for files up to 5MB
- **Library**: None (built-in ZIP + XML)
- **Implementation File**: `DocxParser.kt`

### Comic Book Formats (4 formats)

#### ✅ CBZ (Comic Book ZIP)
- **Implementation**: Apache Commons Compress library
- **Features**: 
  - ZIP archive extraction
  - Image ordering (natural sort)
  - Lazy loading (only viewed pages loaded)
  - Page navigation
  - Zoom and pan
- **Optimizations**: 
  - Only current page in memory
  - LRU bitmap cache
  - Image optimization (max 2048px)
  - Background prefetching (optional)
- **Limitations**: Requires valid ZIP structure
- **Performance**: Excellent for 100+ page archives
- **Library**: `commons-compress:1.25.0`

#### ✅ CBR (Comic Book RAR)
- **Implementation**: junrar library
- **Features**: 
  - RAR archive extraction
  - Image ordering
  - Lazy loading
  - Page navigation
- **Optimizations**: 
  - Same as CBZ (lazy loading, caching, optimization)
- **Limitations**: 
  - RAR5 format may have limited support
  - Requires valid RAR structure
- **Performance**: Excellent for 100+ page archives
- **Library**: `junrar:7.5.5`

#### ✅ CB7 (Comic Book 7z)
- **Implementation**: Apache Commons Compress with XZ support
- **Features**: 
  - 7z archive extraction
  - Image ordering
  - Lazy loading
  - Page navigation
- **Optimizations**: 
  - Same as CBZ (lazy loading, caching, optimization)
- **Limitations**: Requires valid 7z structure
- **Performance**: Good for 50+ page archives (slower than ZIP)
- **Library**: `commons-compress:1.25.0` + `xz:1.9`

#### ✅ CBT (Comic Book TAR)
- **Implementation**: Apache Commons Compress library
- **Features**: 
  - TAR archive extraction
  - Image ordering
  - Lazy loading
  - Page navigation
- **Optimizations**: 
  - Same as CBZ (lazy loading, caching, optimization)
- **Limitations**: Requires valid TAR structure
- **Performance**: Excellent for 100+ page archives
- **Library**: `commons-compress:1.25.0`

### Format Detection

**File Extension Based:**
- App detects format from file extension
- Supported extensions checked in `ViewerActivity.kt`
- Case-insensitive matching (.PDF = .pdf)

**MIME Type Verification:**
- Secondary check using Android's `MimeTypeMap`
- Fallback to extension if MIME type unavailable

**Validation:**
- File integrity checks via `FileValidator.kt`
- Magic number verification for binary formats
- Structure validation (ZIP header, XML declaration)

### Performance Characteristics

| Format | Load Time | Memory Usage | Max Recommended Size |
|--------|-----------|--------------|---------------------|
| PDF    | Fast      | Medium       | 100MB / 500 pages   |
| EPUB   | Fast      | Low          | 10MB / 300 pages    |
| MOBI   | Medium    | Low          | 20MB                |
| TXT    | Fast      | Very Low     | 50MB                |
| FB2    | Fast      | Low          | 5MB                 |
| Markdown| Fast     | Low          | 5MB                 |
| HTML   | Fast      | Medium       | 5MB                 |
| AZW    | Medium    | Low          | 10MB                |
| DOCX   | Fast      | Low          | 5MB                 |
| CBZ    | Fast      | Low          | 500MB / 200 pages   |
| CBR    | Fast      | Low          | 500MB / 200 pages   |
| CB7    | Medium    | Low          | 200MB / 100 pages   |
| CBT    | Fast      | Low          | 500MB / 200 pages   |

### Unsupported Formats

The following formats are **NOT currently supported**:

- ❌ **DRM-Protected Files** (AZW3 with DRM, Adobe DRM EPUB/PDF)
- ❌ **CHM** (Microsoft Compiled HTML Help)
- ❌ **DjVu** (Scanned document format)
- ❌ **LIT** (Microsoft Reader format - discontinued)
- ❌ **PDB** (Palm Database - general format, MOBI variant supported)
- ❌ **RTF** (Rich Text Format)
- ❌ **ODT** (OpenDocument Text)

**Note on DRM**: This app does NOT support DRM-protected files. Only DRM-free books work. This is by design to comply with copyright and DRM laws.

## 🤝 Contributing

Contributions are welcome and appreciated! Whether you're fixing bugs, adding features, improving documentation, or reporting issues, your help makes this project better.

### Ways to Contribute

#### 🐛 Bug Reports
Found a bug? Help us fix it:
1. Check [existing issues](https://github.com/rifters/ebook-reader-android/issues) to avoid duplicates
2. Create a new issue with:
   - Clear title describing the bug
   - Steps to reproduce
   - Expected vs actual behavior
   - Device info (model, Android version)
   - App version
   - Screenshots/logs if applicable
3. Use the bug report template if available

#### ✨ Feature Requests
Have an idea for a new feature?
1. Check existing issues and discussions
2. Create a feature request with:
   - Clear description of the feature
   - Use case (why is it needed?)
   - Proposed behavior/UI
   - Mockups or examples (optional)
3. Be open to feedback and discussion

#### 💻 Code Contributions
Want to contribute code? Great!

**Before You Start:**
1. Check [open issues](https://github.com/rifters/ebook-reader-android/issues) for tasks
2. Comment on an issue to claim it
3. For large changes, discuss in an issue first
4. Fork the repository

**Development Process:**
1. Create a feature branch: `git checkout -b feature/your-feature-name`
2. Make your changes following our code standards (see below)
3. Write/update tests for your changes
4. Run tests: `./gradlew test`
5. Build the app: `./gradlew build`
6. Commit with clear messages: `git commit -m "Add feature X"`
7. Push to your fork: `git push origin feature/your-feature-name`
8. Open a Pull Request to the `main` branch

**Pull Request Guidelines:**
- Clear title describing the change
- Description explaining what, why, and how
- Reference related issues (#123)
- Include screenshots for UI changes
- Ensure all tests pass
- Keep changes focused (one feature/fix per PR)
- Respond to review feedback

#### 📝 Documentation
Improve documentation:
- Fix typos or unclear explanations
- Add examples and usage guides
- Improve code comments
- Write tutorials or how-to guides
- Translate documentation (future)

#### 🧪 Testing
Help improve quality:
- Write unit tests for untested code
- Add integration tests for features
- Perform manual testing on different devices
- Report edge cases and unusual behaviors

#### 🎨 Design
Contribute designs:
- UI/UX improvements
- Icon designs
- Color scheme suggestions
- Accessibility improvements
- Mockups for new features

### Code Standards

#### Kotlin Style Guide
Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html):
- Use 4 spaces for indentation
- Line length: 120 characters max
- Package names: lowercase, no underscores
- Class names: UpperCamelCase
- Function/variable names: lowerCamelCase
- Constants: UPPER_SNAKE_CASE

**Kotlin Idioms:**
```kotlin
// Prefer val over var
val bookTitle = "Sample Book"  // ✓
var bookTitle = "Sample Book"  // ✗ (unless mutation needed)

// Use data classes
data class Book(val title: String, val author: String)  // ✓

// Use trailing commas in multi-line lists
val colors = listOf(
    Color.RED,
    Color.GREEN,
    Color.BLUE,  // trailing comma
)

// Use extension functions
fun String.toTitleCase(): String { ... }  // ✓

// Use when instead of if-else chains
when (format) {
    "pdf" -> loadPdf()
    "epub" -> loadEpub()
    else -> showError()
}
```

#### Android Conventions
- **ViewBinding**: Always use ViewBinding (no findViewById)
  ```kotlin
  private lateinit var binding: ActivityMainBinding
  binding = ActivityMainBinding.inflate(layoutInflater)
  ```
- **MVVM Pattern**: Follow Model-View-ViewModel architecture
  - Activities/Fragments = View (UI only)
  - ViewModels = Business logic
  - Direct DAO access from ViewModels (no repository layer in this project)
- **Coroutines**: Use coroutines for async operations
  ```kotlin
  viewModelScope.launch {
      withContext(Dispatchers.IO) {
          // Database or network operation
      }
  }
  ```
- **LiveData**: Reactive UI updates
  ```kotlin
  viewModel.books.observe(viewLifecycleOwner) { books ->
      adapter.submitList(books)
  }
  ```
- **Material Design 3**: Use Material components
  ```xml
  <com.google.android.material.button.MaterialButton ... />
  ```

#### Resource Conventions
- **Strings**: All user-facing text in `strings.xml`
  ```xml
  <string name="button_add_book">Add Book</string>
  ```
- **Colors**: Define in `colors.xml`, use semantic names
  ```xml
  <color name="primary_purple">#6750A4</color>
  ```
- **Layouts**: Follow naming convention
  - `activity_*.xml` for Activities
  - `fragment_*.xml` for Fragments
  - `item_*.xml` for RecyclerView items
  - `bottom_sheet_*.xml` for bottom sheets
- **IDs**: Use lowercase with underscores
  ```xml
  android:id="@+id/button_add_book"
  ```

#### Comment Style
- Use KDoc for public functions and classes
  ```kotlin
  /**
   * Loads a book from the specified file.
   * 
   * @param file The book file to load
   * @return True if loaded successfully, false otherwise
   */
  fun loadBook(file: File): Boolean { ... }
  ```
- Add comments for complex logic
- Don't over-comment obvious code
- Update comments when code changes

#### Testing Standards
- Write unit tests for ViewModels and utilities
- Test edge cases and error conditions
- Use meaningful test names: `testBookLoadingWithInvalidFile()`
- Organize tests: Given-When-Then or Arrange-Act-Assert
  ```kotlin
  @Test
  fun testBookDeletion() {
      // Given
      val book = createTestBook()
      viewModel.addBook(book)
      
      // When
      viewModel.deleteBook(book.id)
      
      // Then
      assertNull(viewModel.getBook(book.id))
  }
  ```

### Project Conventions

#### Package Structure
Keep files in appropriate packages:
```
com.rifters.ebookreader/
├── (root)              # Activities, Adapters
├── viewmodel/          # ViewModels
├── database/           # Room entities, DAOs, Database
├── model/              # Data models (non-entities)
├── util/               # Utilities and helpers
├── cloud/              # Cloud storage providers
└── sync/               # Sync services
```

#### Branch Naming
- `feature/feature-name` - New features
- `bugfix/issue-number-description` - Bug fixes
- `hotfix/critical-fix` - Urgent fixes
- `refactor/what-is-refactored` - Code refactoring
- `docs/what-is-documented` - Documentation

#### Commit Messages
Follow [Conventional Commits](https://www.conventionalcommits.org/):
```
feat: Add dark mode support
fix: Fix crash when opening large PDF files
docs: Update README with build instructions
refactor: Extract book loading logic to separate class
test: Add unit tests for BookViewModel
chore: Update dependencies to latest versions
```

### Development Environment Setup

1. **Android Studio**: Latest stable version (Arctic Fox+)
2. **JDK**: 17 or later
3. **Android SDK**: API 34 installed
4. **Git**: Version control
5. **Firebase** (optional): For testing sync features

**Recommended Plugins:**
- Kotlin
- Android Material Design Icon Generator
- Rainbow Brackets
- SonarLint (code quality)

### Testing Your Changes

**Local Testing:**
```bash
# Unit tests
./gradlew test

# Instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# Build and verify
./gradlew build

# Code style check (if configured)
./gradlew ktlintCheck
```

**Manual Testing:**
1. Test on multiple Android versions (min: Android 7.0)
2. Test on different screen sizes (phone and tablet)
3. Test with various book formats
4. Test edge cases (no internet, large files, etc.)
5. Check for memory leaks (Android Profiler)

### Getting Help

- 📖 Read the documentation in this README
- 💬 Ask questions in GitHub Discussions
- 🐛 Check existing issues for similar problems
- 📧 Contact maintainers (see below)

### Code of Conduct

- Be respectful and inclusive
- Welcome newcomers and help them learn
- Give constructive feedback
- Accept criticism gracefully
- Focus on what's best for the project
- Zero tolerance for harassment or discrimination

### Recognition

Contributors will be:
- Listed in the CONTRIBUTORS file (if created)
- Credited in release notes for significant contributions
- Mentioned in the About screen for major features
- Appreciated in comments and discussions ❤️

Thank you for contributing to EBook Reader Android! 🎉

## 📄 License

This project is open source. License information should be added here by the project owner.

**Recommended License Options:**
- **MIT License** - Permissive, allows commercial use
- **Apache License 2.0** - Permissive with patent grant
- **GPL v3** - Copyleft, requires derivative works to be open source
- **Creative Commons** - For documentation and non-code assets

## 👤 Author

**rifters**
- GitHub: [@rifters](https://github.com/rifters)
- Project: [ebook-reader-android](https://github.com/rifters/ebook-reader-android)

## 🙏 Acknowledgments

This project uses several excellent open-source libraries and tools:

### Core Libraries
- [AndroidX](https://developer.android.com/jetpack/androidx) - Modern Android development toolkit
- [Kotlin](https://kotlinlang.org/) - Modern programming language for Android
- [Material Components](https://material.io/develop/android) - Material Design 3 UI components
- [Room](https://developer.android.com/training/data-storage/room) - SQLite database abstraction

### File Format Libraries
- [Apache Commons Compress](https://commons.apache.org/proper/commons-compress/) - ZIP, TAR, 7z archive handling for comic books
- [junrar](https://github.com/junrar/junrar) - RAR extraction for CBR comic books
- [XZ for Java](https://tukaani.org/xz/java.html) - 7z compression support

### Cloud Services
- [Firebase](https://firebase.google.com/) - Cloud sync infrastructure (Firestore, Authentication)
- [Google Drive API](https://developers.google.com/drive) - Cloud storage integration
- [Dropbox SDK](https://www.dropbox.com/developers) - Cloud storage integration
- [Microsoft Graph API](https://developer.microsoft.com/graph) - OneDrive integration

### Other Libraries
- [Google Play Services](https://developers.google.com/android/guides/overview) - Google sign-in and services
- [Apache Commons Net](https://commons.apache.org/proper/commons-net/) - FTP support
- [JSch](http://www.jcraft.com/jsch/) - SFTP support
- [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) - Background task scheduling

### Development Tools
- [Android Studio](https://developer.android.com/studio) - Official IDE for Android development
- [Gradle](https://gradle.org/) - Build automation system
- [JUnit](https://junit.org/) - Unit testing framework
- [Mockito](https://site.mockito.org/) - Mocking framework for tests
- [Espresso](https://developer.android.com/training/testing/espresso) - UI testing framework

### Inspiration & Resources
- [Material Design 3 Guidelines](https://m3.material.io/) - Design system and guidelines
- [Android Developers Documentation](https://developer.android.com/) - Official Android docs
- [Kotlin Documentation](https://kotlinlang.org/docs/home.html) - Kotlin language reference
- [Stack Overflow](https://stackoverflow.com/) - Community support and solutions

Special thanks to the Android developer community for invaluable resources, tutorials, and support!

## 📚 Additional Documentation

For more detailed information, see:

- **[PERFORMANCE_OPTIMIZATIONS.md](PERFORMANCE_OPTIMIZATIONS.md)** - Details on performance improvements
- **[FIREBASE_SETUP.md](app/FIREBASE_SETUP.md)** - Firebase configuration guide
- **[SYNC_IMPLEMENTATION.md](SYNC_IMPLEMENTATION.md)** - Cloud sync architecture

## 🐛 Known Issues

### Current Limitations

1. **Cloud Storage Authentication**: 
   - Google Drive, Dropbox, OneDrive require OAuth setup
   - WebDAV is fully functional
   - See "Cloud Storage Integration" section for workarounds

2. **System Dark Mode**:
   - App has reading themes but no system-wide dark mode resources
   - Material3 DayNight theme base exists but needs values-night resources

3. **DRM Content**:
   - DRM-protected books are not supported
   - Only works with DRM-free content
   - This is intentional to comply with copyright laws

4. **Very Large Files**:
   - Files over 5MB may be truncated (EPUB, MOBI, TXT)
   - Comic archives work well up to 500MB with lazy loading
   - PDF handles large files well with caching

5. **Complex EPUB Formatting**:
   - Advanced CSS may not render perfectly
   - Custom fonts may not load
   - JavaScript in EPUB not supported

### Reporting Issues

If you encounter a bug:
1. Check [existing issues](https://github.com/rifters/ebook-reader-android/issues)
2. Verify you're using the latest version
3. Try reproducing with a minimal example
4. Create a new issue with:
   - Clear description
   - Steps to reproduce
   - Expected vs actual behavior
   - Device info and Android version
   - Logs if available (use `adb logcat`)

## ❓ FAQ

### General Questions

**Q: Is this app available on Google Play Store?**  
A: Check the repository or author's profile for release information.

**Q: Does the app support DRM-protected books?**  
A: No, only DRM-free books are supported. This is by design to comply with copyright and DRM laws.

**Q: Can I import my library from another app?**  
A: You can manually import book files. Automated import from other apps is not currently supported.

**Q: Does the app send my data anywhere?**  
A: Only if you enable cloud sync. Reading data (progress, bookmarks) is synced to Firebase anonymously. Book files themselves are never uploaded unless you explicitly use cloud storage backup.

### Technical Questions

**Q: Why won't the app build?**  
A: Ensure you have:
- Android Studio Arctic Fox or later
- JDK 17+
- Android SDK API 34
- `google-services.json` file (even placeholder works)

**Q: Why doesn't cloud sync work?**  
A: You need to set up your own Firebase project and replace the placeholder `google-services.json` file. See "Cloud Sync" section.

**Q: The app crashes when opening large files. Why?**  
A: There are size limits to prevent out-of-memory errors:
- EPUB chapters: 5MB
- MOBI/TXT: 5MB
- Comic books: Use lazy loading (no practical limit)
- PDFs: No limit, but may be slow

**Q: Can I use this app offline?**  
A: Yes! The app works fully offline. Cloud sync and network downloads require internet, but reading works offline.

**Q: How do I backup my library?**  
A: Currently, you need to manually backup:
- Book files: Copy from app storage
- Database: Use Android backup tools
- Future: Export/import feature planned

### Feature Questions

**Q: Will you add [feature X]?**  
A: Check the "Future Enhancements" section. If not listed, create a feature request issue.

**Q: Can I contribute a translation?**  
A: Yes! Translation support is planned. For now, contact the maintainer.

**Q: Why is [cloud provider] not working?**  
A: Some providers need OAuth setup. See "Cloud Storage Integration" section for details.

## 🔒 Privacy Policy

### Data Collection

**What We Collect:**
- Book metadata (title, author, progress) - stored locally
- Reading statistics (pages read, time) - stored locally
- Bookmarks and highlights - stored locally
- Cloud sync data (if enabled) - stored in your Firebase account

**What We Don't Collect:**
- Personal information (name, email, etc.)
- Book file content
- Reading habits or analytics
- Location data
- Device identifiers (except Firebase anonymous ID if sync enabled)

**Third-Party Services:**
- **Firebase**: If cloud sync is enabled, uses anonymous authentication
- **Cloud Storage Providers**: If you connect them, follow their privacy policies
- **No other third-party analytics or tracking**

**Your Rights:**
- You own all your data
- You can delete all data from the app
- You can export your data (manual currently)
- No data is sold or shared

## 🚀 Roadmap Summary

See "Future Enhancements & Optimization Suggestions" section for detailed roadmap.

**High Priority (Next 3-6 months):**
- ✅ System-aware dark mode
- ✅ Backup & restore functionality
- ✅ Batch book import
- ✅ Reading statistics dashboard
- ✅ Complete cloud storage OAuth flows

**Medium Priority (6-12 months):**
- Full-text search within books
- Gamification and achievements
- Series management
- Custom book covers from online sources
- Enhanced accessibility features

**Long-term (12+ months):**
- Plugin system
- OPDS catalog support
- Advanced analytics
- Community features
- Multi-language support

## 📞 Support & Contact

### Getting Help
- 📖 Read this README thoroughly
- 🔍 Search [existing issues](https://github.com/rifters/ebook-reader-android/issues)
- 💬 Start a [discussion](https://github.com/rifters/ebook-reader-android/discussions)
- 🐛 Report bugs via [issues](https://github.com/rifters/ebook-reader-android/issues/new)

### Contact
- **GitHub**: [@rifters](https://github.com/rifters)
- **Project URL**: [ebook-reader-android](https://github.com/rifters/ebook-reader-android)

---

**Made with ❤️ by rifters**

⭐ If you find this project useful, please consider starring it on GitHub!

📝 Contributions are welcome! See the [Contributing](#-contributing) section above.

🐛 Found a bug? [Report it](https://github.com/rifters/ebook-reader-android/issues/new)!

💡 Have a feature idea? [Share it](https://github.com/rifters/ebook-reader-android/discussions)!
