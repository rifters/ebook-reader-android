# Build Test Checklist for EBook Reader Android

This checklist helps ensure all features work correctly after a new build. Use this to systematically test the application and identify issues quickly.

## Pre-Build Checks
- [ ] All code changes committed
- [ ] No merge conflicts
- [ ] Dependencies up to date
- [ ] Build configuration correct (debug vs release)

## Build Process
- [ ] Clean build successful: `./gradlew clean assembleDebug`
- [ ] No build errors or warnings
- [ ] APK generated successfully
- [ ] App installs on test device without errors

## 1. Library Management

### Basic Operations
- [ ] App launches successfully
- [ ] Library screen displays (grid/list view)
- [ ] Toggle between grid and list view works
- [ ] Empty library shows appropriate message

### Adding Books
- [ ] Import book from device storage works
- [ ] Import from Downloads folder works
- [ ] Network book download works (URL import)
- [ ] Cloud storage import works (Google Drive, Dropbox, etc.)
- [ ] Duplicate detection triggers appropriately

### Book Management
- [ ] Search books by title works
- [ ] Search books by author works
- [ ] Sort by title works
- [ ] Sort by author works
- [ ] Sort by date added works
- [ ] Sort by last read works
- [ ] Sort by progress works
- [ ] Filter by format works (PDF, EPUB, etc.)
- [ ] Filter by status works (reading, completed, to-read)
- [ ] Filter by rating works
- [ ] Filter by tags works
- [ ] Filter by collections works

### Book Details
- [ ] View book details
- [ ] Edit book metadata (title, author, genre, etc.)
- [ ] Change book cover
- [ ] Delete book with confirmation
- [ ] Book rating can be set/changed

## 2. Reading Experience

### Format Support
Test with at least one file of each format:

#### Text-Based Formats (TTS Supported)
- [ ] **EPUB**: Opens and displays correctly
- [ ] **MOBI**: Opens and displays correctly
- [ ] **AZW/AZW3**: Opens and displays correctly
- [ ] **FB2**: Opens and displays correctly
- [ ] **DOCX**: Opens and displays correctly
- [ ] **Markdown (.md)**: Opens and displays correctly
- [ ] **HTML/XML**: Opens and displays correctly
- [ ] **TXT**: Opens and displays correctly

#### Image-Based Formats (TTS Not Supported)
- [ ] **PDF**: Opens and displays correctly
- [ ] **CBZ** (ZIP comics): Opens and displays correctly
- [ ] **CBR** (RAR comics): Opens and displays correctly
- [ ] **CB7** (7z comics): Opens and displays correctly
- [ ] **CBT** (TAR comics): Opens and displays correctly

### Navigation
- [ ] Page forward works (swipe/button)
- [ ] Page backward works (swipe/button)
- [ ] Jump to page/chapter works
- [ ] Table of Contents displayed (EPUB)
- [ ] TOC navigation works
- [ ] Progress bar updates correctly
- [ ] Page count displays accurately

### Reading Settings
- [ ] Customize Reading menu accessible during reading
- [ ] Font family changes apply (sans-serif, serif, monospace)
- [ ] Font size slider works and applies changes
- [ ] Line spacing adjustment works
- [ ] Margin adjustment works
- [ ] Theme changes apply (Light, Dark, Sepia)
- [ ] Night mode toggle works
- [ ] Settings persist after closing book
- [ ] Settings apply immediately without restart

### Text-to-Speech (TTS)
**Note**: TTS only works for text-based formats (EPUB, TXT, HTML, MOBI, AZW, FB2, DOCX, Markdown)

#### TTS Button States
- [ ] TTS button disabled before book loads
- [ ] TTS button enabled after text content loads
- [ ] TTS button disabled for PDF files
- [ ] TTS button disabled for comic book formats (CBZ/CBR/CB7/CBT)
- [ ] TTS button shows play icon when stopped
- [ ] TTS button shows pause icon when playing

#### TTS Functionality
- [ ] TTS play button starts reading (text formats)
- [ ] TTS reads text correctly
- [ ] TTS pause button stops reading
- [ ] TTS resume continues from where it stopped
- [ ] TTS rate adjustment works (0.5x to 2.0x)
- [ ] TTS pitch adjustment works (0.5x to 2.0x)
- [ ] TTS settings save and persist

#### TTS Error Handling
- [ ] Shows "TTS is initializing" if clicked before ready
- [ ] Shows "No text content available" for image formats
- [ ] Shows appropriate error if TTS engine not available
- [ ] No crashes when TTS fails

### Bookmarks & Highlights
- [ ] Add bookmark at current position
- [ ] View bookmarks list
- [ ] Navigate to bookmark
- [ ] Delete bookmark
- [ ] Add highlight to selected text
- [ ] View highlights list
- [ ] Navigate to highlight
- [ ] Delete highlight
- [ ] Add notes to bookmarks
- [ ] Add notes to highlights

### Progress Tracking
- [ ] Reading progress saves automatically
- [ ] Return to last read position on reopen
- [ ] Progress percentage updates correctly
- [ ] Completion status updates (to-read → reading → completed)

## 3. Collections & Organization

### Collections
- [ ] Create new collection
- [ ] View collections list
- [ ] Add book to collection
- [ ] Remove book from collection
- [ ] View books in collection
- [ ] Edit collection name/description
- [ ] Delete collection (books remain in library)
- [ ] Smart collections work (by author, genre, year)

### Tags
- [ ] Create new tag
- [ ] Assign tag to book
- [ ] Remove tag from book
- [ ] Filter books by tag
- [ ] Color-coded tags display correctly
- [ ] Edit tag name/color
- [ ] Delete tag

### Reading Lists
- [ ] Create reading list
- [ ] Add books to reading list
- [ ] Reorder books in list
- [ ] Remove books from list
- [ ] Delete reading list
- [ ] Navigate through reading list sequentially

## 4. Goals & Tracking

### Reading Goals
- [ ] Set goal (books/pages/time)
- [ ] View goal progress
- [ ] Goal progress updates with reading
- [ ] Complete goal notification
- [ ] Edit existing goal
- [ ] Delete goal

### Statistics
- [ ] View reading statistics
- [ ] Statistics update correctly
- [ ] Time tracking works
- [ ] Pages read count accurate

## 5. Cloud Integration

### Firebase Sync
- [ ] Enable sync in settings
- [ ] Sign in to Firebase account
- [ ] Manual sync triggers correctly
- [ ] Auto sync works in background
- [ ] Reading progress syncs across devices
- [ ] Bookmarks sync across devices
- [ ] Highlights sync across devices
- [ ] Conflict resolution works (concurrent edits)
- [ ] Offline changes sync when online

### Cloud Storage
- [ ] Connect to Google Drive
- [ ] Browse Google Drive folders
- [ ] Download book from Google Drive
- [ ] Connect to Dropbox
- [ ] Browse Dropbox folders
- [ ] Download book from Dropbox
- [ ] Connect to OneDrive
- [ ] Browse OneDrive folders
- [ ] Download book from OneDrive
- [ ] Connect to WebDAV server
- [ ] Connect to FTP/SFTP server
- [ ] Network error handling works

## 6. Settings & Preferences

### App Settings
- [ ] Open settings screen
- [ ] Enable/disable sync
- [ ] Enable/disable auto-sync
- [ ] View sync status
- [ ] Cloud storage configuration
- [ ] Theme preference saves
- [ ] Default view mode saves (grid/list)
- [ ] About screen displays version info

## 7. Error Handling

### File Errors
- [ ] Corrupted file shows error message
- [ ] Unsupported format shows error message
- [ ] Missing file shows error message
- [ ] Large file warning displays
- [ ] Permission denied shows appropriate message

### Network Errors
- [ ] Network unavailable handled gracefully
- [ ] Download failure shows error
- [ ] Cloud sync failure shows error
- [ ] Retry mechanism works

### Memory Errors
- [ ] Large file doesn't crash app
- [ ] Out of memory handled gracefully
- [ ] App recovers from memory pressure

## 8. UI/UX

### Visual Elements
- [ ] Dynamic book covers generate correctly
- [ ] Material Design 3 theme applied
- [ ] Icons display correctly
- [ ] Colors match theme (purple scheme)
- [ ] Layout responsive to different screen sizes
- [ ] Portrait orientation works
- [ ] Landscape orientation works

### Animations
- [ ] Page transitions smooth
- [ ] Bottom sheet animations work
- [ ] List animations work
- [ ] No animation stuttering or lag

### Accessibility
- [ ] TalkBack screen reader works
- [ ] Content descriptions present
- [ ] Touch targets adequate size (48dp min)
- [ ] Color contrast sufficient
- [ ] Text scales with system font size

## 9. Performance

### Speed
- [ ] App launches quickly (< 3 seconds)
- [ ] Book opens quickly
- [ ] Page navigation responsive
- [ ] Search results instant
- [ ] No UI freezing or ANR

### Battery
- [ ] No excessive battery drain
- [ ] Background sync doesn't drain battery
- [ ] TTS doesn't excessively drain battery

### Storage
- [ ] App size reasonable
- [ ] Cache size managed
- [ ] Books stored efficiently
- [ ] Old cache cleaned up

## 10. Edge Cases

### Empty States
- [ ] Empty library message shown
- [ ] No bookmarks message shown
- [ ] No collections message shown
- [ ] No reading lists message shown

### Large Data
- [ ] Library with 100+ books performs well
- [ ] Large EPUB (10MB+) opens correctly
- [ ] Long reading session stable

### Interruptions
- [ ] Phone call during reading handled
- [ ] App switch preserves state
- [ ] Screen rotation preserves reading position
- [ ] Low battery warning doesn't crash
- [ ] Incoming notification doesn't interrupt

## 11. Regression Testing

After bug fixes, verify:
- [ ] Original issue resolved
- [ ] No new issues introduced
- [ ] Related features still work
- [ ] Performance not degraded

## Testing Notes Template

For each test session, document:

```
Date: [YYYY-MM-DD]
Build: [Build number/commit hash]
Device: [Device model and Android version]
Tester: [Your name]

Issues Found:
1. [Issue description]
   - Steps to reproduce
   - Expected behavior
   - Actual behavior
   - Severity: Critical/High/Medium/Low

2. [Issue description]
   ...

Passed Tests: [Number]
Failed Tests: [Number]
Blocked Tests: [Number]

Notes:
[Any additional observations]
```

## Issue Severity Guide

- **Critical**: App crashes, data loss, security vulnerability
- **High**: Feature completely broken, major functionality impaired
- **Medium**: Feature partially broken, workaround available
- **Low**: Minor UI issue, cosmetic problem, rare edge case

## Quick Smoke Test (5 minutes)

For rapid verification after minor changes:

1. [ ] App launches
2. [ ] Open an EPUB file
3. [ ] Navigate a few pages
4. [ ] Test TTS play/pause
5. [ ] Change font size
6. [ ] Add a bookmark
7. [ ] Toggle night mode
8. [ ] Return to library
9. [ ] Search for a book
10. [ ] Close app (no crashes)

## Automation Notes

Consider automating:
- Unit tests for ViewModels
- UI tests for critical flows (Espresso)
- Integration tests for database operations
- Performance benchmarks

## Feedback

If you find issues during testing:
1. Check if issue is already reported
2. Document with screenshots/logs
3. Include device and Android version
4. Note steps to reproduce
5. Report through issue tracker

---

**Last Updated**: 2025-11-09
**Version**: 1.0
