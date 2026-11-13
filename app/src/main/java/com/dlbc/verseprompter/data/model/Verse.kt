package com.dlbc.verseprompter.data.model

/**
 * A fully-resolved scripture reference, already localised for display.
 *
 * The prompter screen never formats a reference by hand - it calls [display],
 * so the header always matches the version currently loaded.
 */
data class VerseRef(
    val bookNumber: Int,
    val chapter: Int,
    val verse: Int
) : Comparable<VerseRef> {

    override fun compareTo(other: VerseRef): Int =
        compareValuesBy(this, other, { it.bookNumber }, { it.chapter }, { it.verse })

    fun display(languageTag: String = "en"): String {
        val book = BibleBooks.byNumber(bookNumber)?.displayName(languageTag) ?: "?"
        return "$book $chapter:$verse"
    }

    fun next(): VerseRef = copy(verse = verse + 1)

    companion object {
        fun of(bookNumber: Int, chapter: Int, verse: Int) = VerseRef(bookNumber, chapter, verse)
    }
}

/** A single verse as the UI consumes it. */
data class Verse(
    val ref: VerseRef,
    val text: String,
    val versionCode: String,
    val languageTag: String
) {
    val header: String get() = ref.display(languageTag)
}

/**
 * A spoken reference may name a range ("Matthew 5:3 to 10"). The queue holds the
 * whole range but the prompter shows one verse at a time - see the verse-by-verse
 * advance logic in Step 3.
 */
data class VerseRange(
    val bookNumber: Int,
    val chapter: Int,
    val startVerse: Int,
    val endVerse: Int
) {
    val isSingle: Boolean get() = startVerse == endVerse

    fun refs(): List<VerseRef> =
        (startVerse..endVerse).map { VerseRef(bookNumber, chapter, it) }

    fun display(languageTag: String = "en"): String {
        val book = BibleBooks.byNumber(bookNumber)?.displayName(languageTag) ?: "?"
        return if (isSingle) "$book $chapter:$startVerse"
        else "$book $chapter:$startVerse-$endVerse"
    }
}
