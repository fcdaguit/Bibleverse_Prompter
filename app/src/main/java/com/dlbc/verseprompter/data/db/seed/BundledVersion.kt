package com.dlbc.verseprompter.data.db.seed

import android.content.Context
import org.json.JSONObject

/**
 * One entry of `assets/bibles/manifest.json` - the list of translations shipped
 * inside the APK. The manifest is the single source of truth for what gets
 * seeded, so adding a fourth bundled Bible is a two-step change: drop the JSON
 * in assets and add a line to the manifest.
 */
data class BundledVersion(
    val code: String,
    val name: String,
    val abbreviation: String,
    val languageTag: String,
    val languageLabel: String,
    val assetPath: String,
    val sortOrder: Int,
    /** Rough verse count, used only to render a progress bar. 0 = unknown. */
    val expectedVerses: Int
) {
    companion object {
        const val MANIFEST_PATH = "bibles/manifest.json"

        /**
         * Reads the manifest. Throws if it is missing or malformed - that is a
         * packaging error and should fail loudly during development rather than
         * silently ship an app with no Bible in it.
         */
        fun readManifest(context: Context): List<BundledVersion> {
            val raw = context.assets.open(MANIFEST_PATH).bufferedReader().use { it.readText() }
            val root = JSONObject(raw)
            val array = root.getJSONArray("bundled")
            return (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                BundledVersion(
                    code = o.getString("code"),
                    name = o.getString("name"),
                    abbreviation = o.optString("abbreviation", o.getString("code")),
                    languageTag = o.optString("language", "en"),
                    languageLabel = o.optString("languageLabel", "English"),
                    assetPath = "bibles/" + o.getString("file"),
                    sortOrder = o.optInt("sortOrder", i),
                    expectedVerses = o.optInt("expectedVerses", 0)
                )
            }
        }
    }
}
