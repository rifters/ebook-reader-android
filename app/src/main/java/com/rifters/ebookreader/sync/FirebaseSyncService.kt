package com.rifters.ebookreader.sync

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.rifters.ebookreader.model.BookSyncData
import com.rifters.ebookreader.model.BookmarkSyncData
import kotlinx.coroutines.tasks.await

/**
 * Firebase implementation of CloudSyncService using Firestore for data storage.
 */
class FirebaseSyncService : CloudSyncService {
    
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    
    private companion object {
        const val COLLECTION_BOOKS = "books"
        const val COLLECTION_BOOKMARKS = "bookmarks"
    }
    
    override suspend fun initialize(): Boolean {
        return try {
            firestore.firestoreSettings
            true
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun isAuthenticated(): Boolean {
        return auth.currentUser != null
    }
    
    override suspend fun signInAnonymously(): Result<String> {
        return try {
            val result = auth.signInAnonymously().await()
            val uid = result.user?.uid
            if (uid != null) {
                Result.success(uid)
            } else {
                Result.failure(Exception("Failed to get user ID"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
    
    override suspend fun syncBookProgress(bookData: BookSyncData): Result<Unit> {
        return try {
            val docRef = firestore
                .collection(COLLECTION_BOOKS)
                .document("${bookData.userId}_${bookData.bookId}")
            
            docRef.set(bookData.toMap()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun syncBookmark(bookmarkData: BookmarkSyncData): Result<Unit> {
        return try {
            val docRef = firestore
                .collection(COLLECTION_BOOKMARKS)
                .document("${bookmarkData.userId}_${bookmarkData.bookmarkId}")
            
            docRef.set(bookmarkData.toMap()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getBookProgress(bookId: String, userId: String): Result<BookSyncData?> {
        return try {
            val docRef = firestore
                .collection(COLLECTION_BOOKS)
                .document("${userId}_${bookId}")
            
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                val data = snapshot.toObject(BookSyncData::class.java)
                Result.success(data)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getAllBookProgress(userId: String): Result<List<BookSyncData>> {
        return try {
            val querySnapshot = firestore
                .collection(COLLECTION_BOOKS)
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            val books = querySnapshot.documents.mapNotNull { 
                it.toObject(BookSyncData::class.java) 
            }
            Result.success(books)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getBookmarks(bookId: String, userId: String): Result<List<BookmarkSyncData>> {
        return try {
            val querySnapshot = firestore
                .collection(COLLECTION_BOOKMARKS)
                .whereEqualTo("userId", userId)
                .whereEqualTo("bookId", bookId)
                .get()
                .await()
            
            val bookmarks = querySnapshot.documents.mapNotNull { 
                it.toObject(BookmarkSyncData::class.java) 
            }
            Result.success(bookmarks)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteBookProgress(bookId: String, userId: String): Result<Unit> {
        return try {
            val docRef = firestore
                .collection(COLLECTION_BOOKS)
                .document("${userId}_${bookId}")
            
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteBookmark(bookmarkId: String, userId: String): Result<Unit> {
        return try {
            val docRef = firestore
                .collection(COLLECTION_BOOKMARKS)
                .document("${userId}_${bookmarkId}")
            
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
