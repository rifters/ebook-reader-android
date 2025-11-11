package com.rifters.ebookreader.cloud

import android.util.Log
import java.io.File

/**
 * Small utility to resolve file conflicts when downloading/importing files.
 * Strategies: overwrite, rename, skip
 */
object ConflictResolver {

    private const val TAG = "ConflictResolver"

    enum class Strategy {
        OVERWRITE, RENAME, SKIP
    }

    fun resolve(destination: File, strategy: Strategy = Strategy.RENAME): File? {
        return when (strategy) {
            Strategy.OVERWRITE -> {
                // Ensure parent exists
                destination.parentFile?.mkdirs()
                destination
            }
            Strategy.SKIP -> {
                if (destination.exists()) null else {
                    destination.parentFile?.mkdirs()
                    destination
                }
            }
            Strategy.RENAME -> {
                if (!destination.exists()) {
                    destination.parentFile?.mkdirs()
                    destination
                } else {
                    // Find an available name: name (1).ext
                    val parent = destination.parentFile
                    val base = destination.nameWithoutExtension
                    val ext = destination.extension
                    var i = 1
                    var candidate = File(parent, "$base ($i)${if (ext.isNotEmpty()) "." + ext else ""}")
                    while (candidate.exists()) {
                        i++
                        candidate = File(parent, "$base ($i)${if (ext.isNotEmpty()) "." + ext else ""}")
                        if (i > 9999) {
                            Log.w(TAG, "Too many collisions when resolving file name: ${destination.absolutePath}")
                            return null
                        }
                    }
                    candidate.parentFile?.mkdirs()
                    candidate
                }
            }
        }
    }
}
