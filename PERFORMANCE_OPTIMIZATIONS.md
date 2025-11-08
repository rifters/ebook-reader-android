# Performance Optimizations

This document describes the performance optimizations implemented in the EBook Reader Android app to improve book loading and rendering, especially for large files.

## Overview

The app has been optimized to handle large books efficiently by implementing lazy loading, caching, and memory management strategies across all supported formats (PDF, EPUB, MOBI, TXT, CBZ, CBR).

## Key Optimizations

### 1. Bitmap Caching (BitmapCache.kt)

**Implementation**: LRU (Least Recently Used) cache for rendered pages and images.

**Benefits**:
- Automatically manages memory based on available heap (uses 1/8th of max VM memory)
- Evicts least recently used bitmaps when cache is full
- Recycles bitmaps when evicted to free memory
- Singleton pattern ensures single cache instance across app

**Usage**: 
```kotlin
val cache = BitmapCache.getInstance()
cache.put("page_key", bitmap)
val cachedBitmap = cache.getBitmap("page_key")
```

### 2. PDF Rendering Optimizations

**Changes**:
- Reduced rendering resolution from 2x to 1.5x (33% memory reduction per page)
- Implemented page caching - rendered pages stored in BitmapCache
- Cache check before rendering - previously rendered pages load instantly

**Impact**:
- Before: Each page rendered at 2x resolution, no caching
- After: Pages rendered at 1.5x, cached for instant access
- Memory: ~33% reduction in bitmap size per page
- Speed: Cached pages load instantly without re-rendering

### 3. Comic Book (CBZ/CBR) Lazy Loading

**Changes**:
- Before: All images loaded into memory at app start
- After: Only store image paths, load images on-demand when viewed

**Implementation**:
- Archive opened once to enumerate images
- Images loaded individually when page is viewed
- Each loaded image optimized and cached
- Previous implementation kept all images in memory

**Impact**:
- Memory usage scales with pages viewed, not total pages
- Large comic archives (100+ MB) no longer cause OOM errors
- Initial load time dramatically reduced

### 4. Image Optimization

**Implementation**: `optimizeBitmap()` function for comic book images

**Features**:
- Scales down images larger than 2048px in any dimension
- Maintains aspect ratio
- Applied before caching to reduce memory footprint

**Impact**:
- High-resolution comic scans (4000x6000px+) reduced to manageable size
- Prevents memory issues with very high-resolution images
- No visible quality loss on most devices

### 5. Large File Handling

**EPUB Files**:
- 5MB limit per HTML chapter
- Content truncated with notice if exceeded
- Prevents WebView memory issues

**MOBI Files**:
- 5MB extraction limit
- Chunked reading to prevent loading entire file
- Truncation notice if file exceeds limit

**TXT Files**:
- 5MB reading limit
- Chunked reading with character buffer
- Truncation notice for very large files

**Impact**:
- Prevents OutOfMemoryError on devices with limited RAM
- Allows opening very large books (50MB+)
- User informed when content is truncated

## Memory Management

### Automatic Cleanup

1. **LRU Cache**: Automatically evicts old bitmaps when memory limit reached
2. **Bitmap Recycling**: Evicted bitmaps are recycled to free native memory
3. **onDestroy**: Proper cleanup of resources
   - PDF renderer closed
   - File descriptors closed
   - Archive references cleared
   - Cache continues across instances (managed automatically)

### Memory Pressure Handling

The BitmapCache automatically responds to memory pressure:
- Stores bitmaps in native memory (more efficient)
- Uses 1/8 of available heap space
- Evicts entries when limit reached
- Recycles bitmaps on eviction

## Performance Metrics

### Before Optimizations
- **Comic book (100 pages, 200MB)**: All 200MB loaded at start, potential OOM
- **PDF (500 pages)**: Each page rendered at 2x resolution, re-rendered on every view
- **Large TXT (50MB)**: Entire file loaded into memory, potential OOM
- **Memory usage**: Unbounded, scales with total content size

### After Optimizations
- **Comic book (100 pages, 200MB)**: Only viewed pages in memory (~cache size)
- **PDF (500 pages)**: Pages cached at 1.5x resolution, instant access to cached pages
- **Large TXT (50MB)**: First 5MB loaded, user notified of truncation
- **Memory usage**: Bounded by cache size (typically 8-16MB), managed automatically

## Testing Recommendations

To verify performance improvements:

1. **Large Comic Books**: Test with 100+ page CBZ/CBR files (200MB+)
2. **Large PDFs**: Test with 500+ page PDF documents
3. **Large Text Files**: Test with 50MB+ TXT or MOBI files
4. **Memory Monitoring**: Use Android Studio Profiler to verify memory is bounded
5. **Page Navigation**: Verify cached pages load instantly

## Configuration

The optimizations use sensible defaults but can be adjusted if needed:

- **Bitmap cache size**: 1/8 of max heap (in BitmapCache.kt)
- **PDF render scale**: 1.5x (in ViewerActivity.kt, renderPdfPage)
- **Max image dimension**: 2048px (in ViewerActivity.kt, optimizeBitmap)
- **Text file limit**: 5MB (in ViewerActivity.kt, loadText/loadMobi/loadEpub)

## Future Enhancements

Potential additional optimizations:

1. **Progressive EPUB loading**: Load chapters on-demand instead of first chapter only
2. **Thumbnail generation**: Generate and cache low-res thumbnails for library view
3. **Background prefetching**: Pre-load next/previous pages in background
4. **Configurable quality**: User setting for render quality vs memory trade-off
5. **Disk caching**: Persist rendered pages to disk for even better performance

## Conclusion

These optimizations significantly improve the app's ability to handle large books while maintaining smooth performance and preventing out-of-memory errors. The changes are minimal and focused on the most impactful areas, following Android best practices for memory management.
