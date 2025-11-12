# Testing Guide for ViewPager2 Pagination

## Overview
This guide provides step-by-step instructions for testing the new ViewPager2-based pagination implementation for PDF and comic book formats.

## Prerequisites
- Android device or emulator running Android 7.0 (API 24) or higher
- Sample files for testing:
  - PDF files (various page counts: small <10 pages, medium 10-50 pages, large >50 pages)
  - Comic book files: CBZ, CBR, CB7, CBT formats
  - EPUB files (to verify existing functionality still works)

## Test Cases

### 1. PDF File Navigation

#### Test 1.1: Basic PDF Loading
**Steps:**
1. Open the app
2. Import a PDF file
3. Open the PDF file

**Expected Results:**
- ✅ PDF opens successfully
- ✅ ViewPager2 displays the first page
- ✅ Page indicator shows "Page 1 of X"
- ✅ Page indicator auto-hides after 2 seconds

#### Test 1.2: Horizontal Page Navigation (Default)
**Steps:**
1. Open a PDF file
2. Swipe left (next page)
3. Swipe right (previous page)
4. Use bottom navigation buttons (Previous/Next)

**Expected Results:**
- ✅ Swipe left advances to next page smoothly
- ✅ Swipe right goes to previous page smoothly
- ✅ Navigation buttons work correctly
- ✅ Page indicator updates with each page change
- ✅ Cannot swipe past first or last page
- ✅ Toast message appears at boundaries

#### Test 1.3: Vertical Page Navigation
**Steps:**
1. Open a PDF file
2. Open Reading Settings (three-dot menu)
3. Change Layout Mode to "Vertical Scroll"
4. Apply settings
5. Swipe up and down to navigate

**Expected Results:**
- ✅ ViewPager2 switches to vertical orientation
- ✅ Swipe up advances to next page
- ✅ Swipe down goes to previous page
- ✅ Smooth page transitions
- ✅ Page indicator updates correctly

#### Test 1.4: Page Loading Performance
**Steps:**
1. Open a large PDF (50+ pages)
2. Navigate to page 1
3. Swipe to page 2 immediately
4. Continue swiping through pages

**Expected Results:**
- ✅ First page loads quickly
- ✅ Nearby pages (±2) load within 1-2 seconds
- ✅ No lag when swiping between loaded pages
- ✅ Pages render at good quality (2x resolution)
- ✅ No crashes or memory errors

#### Test 1.5: UI Toggle
**Steps:**
1. Open a PDF file
2. Tap on the page content
3. Tap again

**Expected Results:**
- ✅ First tap hides toolbar and bottom bar
- ✅ Second tap shows toolbar and bottom bar
- ✅ UI toggle works smoothly

#### Test 1.6: Layout Mode Switching
**Steps:**
1. Open a PDF file in horizontal mode
2. Navigate to page 5
3. Change to vertical mode via Reading Settings
4. Change back to horizontal mode

**Expected Results:**
- ✅ Page position (page 5) is maintained
- ✅ ViewPager2 orientation updates correctly
- ✅ No crashes during mode switch
- ✅ Page indicator remains accurate

### 2. Comic Book Navigation

#### Test 2.1: CBZ Format
**Steps:**
1. Import a CBZ file
2. Open the file
3. Navigate through pages

**Expected Results:**
- ✅ All images extract and load correctly
- ✅ Images display in correct order
- ✅ Pages scale to fit screen
- ✅ Smooth navigation between pages
- ✅ Page indicator shows correct count

#### Test 2.2: CBR Format
**Steps:**
1. Import a CBR file
2. Open the file
3. Navigate through pages

**Expected Results:**
- ✅ RAR archive extracts successfully
- ✅ All images display correctly
- ✅ Navigation works smoothly
- ✅ No extraction errors

#### Test 2.3: CB7 Format
**Steps:**
1. Import a CB7 file (7z archive)
2. Open the file
3. Navigate through pages

**Expected Results:**
- ✅ 7z archive extracts successfully
- ✅ All images display correctly
- ✅ Navigation works smoothly
- ✅ No extraction errors

#### Test 2.4: CBT Format
**Steps:**
1. Import a CBT file (TAR archive)
2. Open the file
3. Navigate through pages

**Expected Results:**
- ✅ TAR archive extracts successfully
- ✅ All images display correctly
- ✅ Navigation works smoothly
- ✅ No extraction errors

#### Test 2.5: Comic Vertical Scrolling
**Steps:**
1. Open a comic book
2. Change to vertical scroll mode
3. Navigate by swiping up/down

**Expected Results:**
- ✅ Vertical navigation works smoothly
- ✅ Pages scale correctly in vertical mode
- ✅ Natural "scroll-reading" experience

### 3. Progress Tracking

#### Test 3.1: Progress Persistence (PDF)
**Steps:**
1. Open a PDF file
2. Navigate to page 10
3. Close the book
4. Re-open the book

**Expected Results:**
- ✅ Book opens to page 10
- ✅ Progress percentage shown in library is accurate
- ✅ Last read timestamp is updated

#### Test 3.2: Progress Persistence (Comic)
**Steps:**
1. Open a comic book
2. Navigate to page 15
3. Close the book
4. Re-open the book

**Expected Results:**
- ✅ Book opens to page 15
- ✅ Progress percentage is accurate
- ✅ Last read timestamp is updated

#### Test 3.3: Bookmarks with ViewPager2
**Steps:**
1. Open a PDF file
2. Navigate to page 8
3. Tap bookmark button
4. Navigate to page 20
5. View bookmarks list
6. Tap on bookmark for page 8

**Expected Results:**
- ✅ Bookmark is created for page 8
- ✅ Bookmark list shows correct page number
- ✅ Tapping bookmark navigates to page 8
- ✅ ViewPager2 displays correct page

### 4. EPUB Files (Verify Existing Functionality)

#### Test 4.1: EPUB Still Works
**Steps:**
1. Open an EPUB file
2. Navigate through chapters
3. Change layout modes

**Expected Results:**
- ✅ EPUB opens successfully using WebView
- ✅ CSS column pagination still works
- ✅ Layout modes work (horizontal, vertical, two-column)
- ✅ No regression in EPUB functionality
- ✅ ViewPager2 is not used for EPUB (uses WebView)

### 5. Edge Cases and Error Handling

#### Test 5.1: Empty PDF
**Steps:**
1. Try to open a corrupted or empty PDF file

**Expected Results:**
- ✅ Error message displayed
- ✅ App doesn't crash
- ✅ User returns to library

#### Test 5.2: Large File Handling
**Steps:**
1. Open a very large PDF (100+ pages or >50MB)

**Expected Results:**
- ✅ File opens successfully (may take time)
- ✅ No out-of-memory errors
- ✅ Priority loading ensures first pages are usable quickly
- ✅ Navigation remains smooth

#### Test 5.3: Rotation
**Steps:**
1. Open a PDF file
2. Rotate device to landscape
3. Navigate pages
4. Rotate back to portrait

**Expected Results:**
- ✅ Page is maintained during rotation
- ✅ ViewPager2 re-layouts correctly
- ✅ No crashes

### 6. Memory Management

#### Test 6.1: Multiple Book Opens
**Steps:**
1. Open a PDF file
2. Return to library
3. Open a comic book
4. Return to library
5. Open another PDF
6. Repeat several times

**Expected Results:**
- ✅ No memory leaks
- ✅ App remains responsive
- ✅ No out-of-memory errors
- ✅ Previous book resources are cleaned up

## Automated Test Scenarios

If you have automated UI tests, consider adding these scenarios:

### Espresso Test Ideas
```kotlin
// Test 1: PDF page navigation
@Test
fun testPdfPageNavigation() {
    // Open PDF
    // Swipe left (next page)
    // Verify page indicator updated
    // Swipe right (previous page)
    // Verify returned to first page
}

// Test 2: Layout mode switching
@Test
fun testLayoutModeSwitching() {
    // Open PDF in horizontal mode
    // Change to vertical mode
    // Verify ViewPager orientation changed
    // Verify current page maintained
}

// Test 3: Page indicator auto-hide
@Test
fun testPageIndicatorAutoHide() {
    // Open PDF
    // Verify page indicator visible
    // Wait 2 seconds
    // Verify page indicator hidden
}
```

## Performance Benchmarks

### Expected Performance Metrics

**PDF Loading:**
- First page visible: < 2 seconds
- Nearby pages (±2) loaded: < 5 seconds
- Navigation response: < 100ms

**Comic Book Loading:**
- Archive extraction: < 10 seconds (depends on file size)
- First page display: < 1 second after extraction
- Navigation response: < 100ms

**Memory Usage:**
- Idle: < 100MB
- With PDF open (10 pages): < 200MB
- With PDF open (50 pages): < 300MB (due to page caching)
- With comic book open: Varies by image sizes

## Reporting Issues

When reporting issues, please include:
1. Device model and Android version
2. File format and approximate size
3. Steps to reproduce
4. Expected vs actual behavior
5. Screenshots or screen recording if applicable
6. LogCat output if app crashes

## Known Limitations

1. **EPUB Files**: Continue to use WebView + CSS columns (not ViewPager2)
2. **Text Files**: Continue to use ScrollView (not ViewPager2)
3. **Large PDFs**: Initial load may be slow, but navigation is smooth once loaded
4. **Memory**: Very large comic books (>100 high-res images) may cause memory pressure

## Success Criteria

The implementation is considered successful if:
- ✅ All PDF formats display correctly with smooth page navigation
- ✅ All comic book formats (CBZ/CBR/CB7/CBT) work correctly
- ✅ Layout mode switching works smoothly
- ✅ Progress tracking and bookmarks work correctly
- ✅ No crashes or memory leaks
- ✅ Performance is acceptable (see benchmarks above)
- ✅ Existing EPUB and TXT functionality not broken

## Additional Notes

- This implementation follows LibreraReader's proven patterns for page navigation
- The ViewPager2 approach provides better UX than the previous single-ImageView approach
- The vertical scrolling mode makes reading manga/comics more natural
- The horizontal mode mimics traditional book page flipping

## Questions or Issues?

If you encounter any issues during testing or have questions about the implementation, please:
1. Check VIEWPAGER2_INTEGRATION.md for technical details
2. Review the code comments in ViewerActivity.kt
3. Report issues with detailed information as outlined above
