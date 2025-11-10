package com.rifters.ebookreader.util

import org.json.JSONObject

/**
 * Utility class for processing TTS text replacements.
 * Based on LibreraReader's replacement patterns.
 */
object TtsReplacementProcessor {
    
    /**
     * Apply text replacements to the input text.
     * 
     * @param text The input text to process
     * @param replacementsJson JSON string containing replacement rules
     * @param enabled Whether replacements are enabled
     * @return Processed text with replacements applied
     */
    fun applyReplacements(text: String, replacementsJson: String, enabled: Boolean): String {
        if (!enabled || text.isEmpty() || replacementsJson.isEmpty()) {
            return text
        }
        
        var processedText = text
        
        try {
            val replacements = JSONObject(replacementsJson)
            val keys = replacements.keys()
            
            // First pass: System replacements (tokens wrapped in < >)
            while (keys.hasNext()) {
                val key = keys.next()
                val value = replacements.getString(key)
                
                if (key.startsWith("<") && key.endsWith(">")) {
                    processedText = processedText.replace(key, value)
                }
            }
            
            // Second pass: Regular replacements and regex patterns
            val keys2 = replacements.keys()
            while (keys2.hasNext()) {
                val key = keys2.next()
                val value = replacements.getString(key)
                
                // Skip system replacements and disabled entries
                if (key.startsWith("<") && key.endsWith(">")) {
                    continue
                }
                if (key.startsWith("#")) {
                    continue  // Disabled entry
                }
                
                // Check if it's a regex pattern (starts with *)
                if (key.startsWith("*")) {
                    val pattern = key.substring(1)  // Remove the * prefix
                    try {
                        processedText = processedText.replace(Regex(pattern), value)
                    } catch (e: Exception) {
                        // Invalid regex, skip
                        android.util.Log.w("TtsReplacements", "Invalid regex pattern: $pattern", e)
                    }
                } else {
                    // Simple string replacement
                    processedText = processedText.replace(key, value)
                }
            }
            
        } catch (e: Exception) {
            android.util.Log.e("TtsReplacements", "Error applying replacements", e)
            return text  // Return original text on error
        }
        
        return processedText
    }
    
    /**
     * Validate that a JSON string is valid replacement rules.
     */
    fun isValidReplacementsJson(json: String): Boolean {
        return try {
            JSONObject(json)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get a human-readable list of replacements for display.
     */
    fun getReplacementsList(replacementsJson: String): List<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()
        
        try {
            val replacements = JSONObject(replacementsJson)
            val keys = replacements.keys()
            
            while (keys.hasNext()) {
                val key = keys.next()
                val value = replacements.getString(key)
                list.add(Pair(key, value))
            }
        } catch (e: Exception) {
            android.util.Log.e("TtsReplacements", "Error parsing replacements", e)
        }
        
        return list.sortedBy { it.first }
    }
    
    /**
     * Add or update a replacement rule.
     */
    fun addReplacement(replacementsJson: String, key: String, value: String): String {
        return try {
            val replacements = if (replacementsJson.isEmpty()) {
                JSONObject()
            } else {
                JSONObject(replacementsJson)
            }
            
            replacements.put(key, value)
            replacements.toString()
        } catch (e: Exception) {
            android.util.Log.e("TtsReplacements", "Error adding replacement", e)
            replacementsJson
        }
    }
    
    /**
     * Remove a replacement rule.
     */
    fun removeReplacement(replacementsJson: String, key: String): String {
        return try {
            val replacements = JSONObject(replacementsJson)
            replacements.remove(key)
            replacements.toString()
        } catch (e: Exception) {
            android.util.Log.e("TtsReplacements", "Error removing replacement", e)
            replacementsJson
        }
    }
}
