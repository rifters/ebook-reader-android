package com.rifters.ebookreader.cloud

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.charset.Charset

/**
 * Simple file-based cache for cloud directory listings.
 * Stores JSON files under app cache directory.
 */
class FileListingCache(private val context: Context) {

    private val TAG = "FileListingCache"

    private fun cacheFile(provider: String, path: String?): File {
        val safePath = (path ?: "_").replace("/", "_")
        val name = "cloud_listing_${provider}_$safePath.json"
        return File(context.cacheDir, name)
    }

    fun saveListing(provider: String, path: String?, files: List<CloudFile>) {
        try {
            val arr = JSONArray()
            files.forEach { f ->
                val o = JSONObject()
                o.put("id", f.id)
                o.put("name", f.name)
                o.put("path", f.path)
                o.put("isDirectory", f.isDirectory)
                o.put("size", f.size)
                o.put("mimeType", f.mimeType)
                o.put("modifiedTime", f.modifiedTime)
                o.put("provider", f.provider)
                arr.put(o)
            }
            cacheFile(provider, path).writeText(arr.toString(), Charset.forName("UTF-8"))
        } catch (e: Exception) {
            Log.e(TAG, "Error saving listing", e)
        }
    }

    fun loadListing(provider: String, path: String?): List<CloudFile> {
        try {
            val f = cacheFile(provider, path)
            if (!f.exists()) return emptyList()
            val raw = f.readText(Charset.forName("UTF-8"))
            val arr = JSONArray(raw)
            val out = mutableListOf<CloudFile>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                out.add(
                    CloudFile(
                        id = o.optString("id"),
                        name = o.optString("name"),
                        path = o.optString("path"),
                        isDirectory = o.optBoolean("isDirectory"),
                        size = o.optLong("size"),
                        mimeType = o.optString("mimeType"),
                        modifiedTime = o.optLong("modifiedTime"),
                        provider = o.optString("provider")
                    )
                )
            }
            return out
        } catch (e: Exception) {
            Log.e(TAG, "Error loading listing", e)
            return emptyList()
        }
    }
}
