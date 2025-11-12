package com.rifters.ebookreader.util

import android.util.Log
import org.json.JSONObject
import java.util.Locale

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
            val systemReplacements = mutableListOf<Pair<String, String>>()
            val regexReplacements = mutableListOf<Pair<Regex, String>>()
            val literalReplacements = mutableListOf<Pair<Regex, String>>()

            val keys = replacements.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                if (key.isEmpty() || key.startsWith("#")) {
                    continue
                }

                val value = replacements.optString(key, "")

                when {
                    key.startsWith("<") && key.endsWith(">") -> {
                        systemReplacements += key to value
                    }
                    key.startsWith("*") -> {
                        val pattern = key.substring(1)
                        try {
                            val regex = Regex(pattern, setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE))
                            regexReplacements += regex to value
                        } catch (e: Exception) {
                            Log.w("TtsReplacements", "Invalid regex pattern: $pattern", e)
                        }
                    }
                    else -> {
                        val regex = Regex(Regex.escape(key), setOf(RegexOption.IGNORE_CASE))
                        literalReplacements += regex to value
                    }
                }
            }

            systemReplacements.forEach { (key, value) ->
                processedText = processedText.replace(key, value)
            }

            regexReplacements.forEach { (regex, value) ->
                processedText = regex.replace(processedText, value)
            }

            literalReplacements.forEach { (regex, value) ->
                processedText = regex.replace(processedText) { matchResult ->
                    applyReplacementCase(matchResult.value, value)
                }
            }
        } catch (e: Exception) {
            Log.e("TtsReplacements", "Error applying replacements", e)
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
            Log.e("TtsReplacements", "Error parsing replacements", e)
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
            Log.e("TtsReplacements", "Error adding replacement", e)
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
            Log.e("TtsReplacements", "Error removing replacement", e)
            replacementsJson
        }
    }

    private fun applyReplacementCase(original: String, replacement: String): String {
        if (replacement.isEmpty()) {
            return replacement
        }

        val hasLetters = original.any { it.isLetter() }
        if (!hasLetters) {
            return replacement
        }

        val locale = Locale.getDefault()

        return when {
            original.all { it.isUpperCase() || !it.isLetter() } -> replacement.uppercase(locale)
            original.all { it.isLowerCase() || !it.isLetter() } -> replacement
            original.first().isUpperCase() && original.drop(1).all { !it.isLetter() || it.isLowerCase() } ->
                replacement.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
            else -> replacement
        }
    }
}
