# EBook Reader Android

A modern Android e-book reader application built with Kotlin, supporting PDF, EPUB, and TXT formats.

## Features

- 📚 Support for multiple formats (PDF, EPUB, TXT)
- 💾 Local database storage using Room
- 📊 Reading progress tracking
- 🎨 Material Design 3 UI
- 📖 Book library management
- 🔖 Bookmark support (in progress)
- 🌙 Night mode ready (in progress)

## Project Structure

### Core Components

#### Activities
- **MainActivity.kt** - Main screen displaying the book library with RecyclerView
- **ViewerActivity.kt** - Book reading interface with format-specific viewers

#### Data Layer
- **Book.kt** - Room entity for book data model
- **BookDao.kt** - Room DAO for database operations
- **BookDatabase.kt** - Room database configuration

#### ViewModel
- **BookViewModel.kt** - MVVM architecture ViewModel for managing book data

#### Adapters
- **BookAdapter.kt** - RecyclerView adapter with DiffUtil for efficient list updates

### Resources

#### Layouts
- **activity_main.xml** - Main screen with RecyclerView, Toolbar, and FAB
- **activity_viewer.xml** - Book viewer with multiple view types (PDF, WebView, TextView)
- **item_book.xml** - Book item card with cover, title, author, and progress

#### Values
- **strings.xml** - All string resources
- **colors.xml** - Color palette
- **themes.xml** - Material Design 3 theme configuration

#### Menus
- **main_menu.xml** - Main activity menu with settings and about options

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

### File Format Support
- **PDF**: android-pdf-viewer by barteksc
- **EPUB**: epublib-core
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

2. Build the project:
```bash
./gradlew build
```

3. Run on a device or emulator:
```bash
./gradlew installDebug
```

## Features Roadmap

### Implemented ✅
- [x] Project structure and configuration
- [x] Room database setup
- [x] Book library UI with RecyclerView
- [x] Book data model with progress tracking
- [x] Material Design 3 theming
- [x] Basic PDF viewer integration
- [x] Basic EPUB viewer integration
- [x] Text file viewer
- [x] ProGuard configuration

### In Progress 🚧
- [ ] File picker integration
- [ ] Bookmark functionality
- [ ] Settings screen
- [ ] About screen
- [ ] Cover image loading and caching

### Planned 📋
- [ ] Advanced EPUB rendering
- [ ] Table of Contents navigation
- [ ] Text-to-Speech support
- [ ] Search functionality
- [ ] Night mode toggle
- [ ] Font size adjustment
- [ ] Reading statistics
- [ ] Cloud sync support

## License

[Add your license here]

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Author

rifters

## Acknowledgments

- [android-pdf-viewer](https://github.com/barteksc/AndroidPdfViewer) by barteksc
- [epublib](https://github.com/psiegman/epublib) by Paul Siegmann
