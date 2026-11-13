package com.dlbc.verseprompter.data.model

/**
 * Canonical 66-book Protestant catalog.
 *
 * [number] is the stable key used everywhere in the database, so a verse row is
 * language-independent: Genesis is always book 1 whether the version is KJV,
 * Ang Dating Biblia or Bugna. Display names per language live here; the spoken
 * aliases and phonetic misrecognitions ("Jobe", "Songs", "Unang Corinto") are
 * handled by the reference parser in Step 3, which builds its lookup table from
 * this catalog.
 *
 * NOTE ON TRANSLATED NAMES: Tagalog and Cebuano book names vary between
 * printings. The spellings below follow the most common Ang Dating Biblia (1905)
 * and Bugna conventions, but please check them against the edition your church
 * uses and correct them here - this is the single place they are defined.
 */
data class BibleBook(
    val number: Int,
    val english: String,
    val tagalog: String,
    val cebuano: String,
    val chapterCount: Int,
    val testament: Testament
) {
    enum class Testament { OLD, NEW }

    /** Display name for a language tag ("en", "fil"/"tl", "ceb"). */
    fun displayName(languageTag: String): String = when (languageTag.lowercase().take(3)) {
        "fil", "tl", "tgl" -> tagalog
        "ceb" -> cebuano
        else -> english
    }
}

object BibleBooks {

    private val OT = BibleBook.Testament.OLD
    private val NT = BibleBook.Testament.NEW

    val ALL: List<BibleBook> = listOf(
        BibleBook(1, "Genesis", "Genesis", "Genesis", 50, OT),
        BibleBook(2, "Exodus", "Exodo", "Exodo", 40, OT),
        BibleBook(3, "Leviticus", "Levitico", "Levitico", 27, OT),
        BibleBook(4, "Numbers", "Mga Bilang", "Numeros", 36, OT),
        BibleBook(5, "Deuteronomy", "Deuteronomio", "Deuteronomio", 34, OT),
        BibleBook(6, "Joshua", "Josue", "Josue", 24, OT),
        BibleBook(7, "Judges", "Mga Hukom", "Mga Maghuhukom", 21, OT),
        BibleBook(8, "Ruth", "Ruth", "Ruth", 4, OT),
        BibleBook(9, "1 Samuel", "1 Samuel", "1 Samuel", 31, OT),
        BibleBook(10, "2 Samuel", "2 Samuel", "2 Samuel", 24, OT),
        BibleBook(11, "1 Kings", "1 Mga Hari", "1 Mga Hari", 22, OT),
        BibleBook(12, "2 Kings", "2 Mga Hari", "2 Mga Hari", 25, OT),
        BibleBook(13, "1 Chronicles", "1 Mga Cronica", "1 Cronicas", 29, OT),
        BibleBook(14, "2 Chronicles", "2 Mga Cronica", "2 Cronicas", 36, OT),
        BibleBook(15, "Ezra", "Ezra", "Esdras", 10, OT),
        BibleBook(16, "Nehemiah", "Nehemias", "Nehemias", 13, OT),
        BibleBook(17, "Esther", "Esther", "Ester", 10, OT),
        BibleBook(18, "Job", "Job", "Job", 42, OT),
        BibleBook(19, "Psalms", "Mga Awit", "Mga Salmo", 150, OT),
        BibleBook(20, "Proverbs", "Mga Kawikaan", "Mga Proverbio", 31, OT),
        BibleBook(21, "Ecclesiastes", "Eclesiastes", "Ecclesiastes", 12, OT),
        BibleBook(22, "Song of Solomon", "Awit ni Solomon", "Ang Awit ni Solomon", 8, OT),
        BibleBook(23, "Isaiah", "Isaias", "Isaias", 66, OT),
        BibleBook(24, "Jeremiah", "Jeremias", "Jeremias", 52, OT),
        BibleBook(25, "Lamentations", "Mga Panaghoy", "Mga Pagbangotan", 5, OT),
        BibleBook(26, "Ezekiel", "Ezekiel", "Ezequiel", 48, OT),
        BibleBook(27, "Daniel", "Daniel", "Daniel", 12, OT),
        BibleBook(28, "Hosea", "Oseas", "Oseas", 14, OT),
        BibleBook(29, "Joel", "Joel", "Joel", 3, OT),
        BibleBook(30, "Amos", "Amos", "Amos", 9, OT),
        BibleBook(31, "Obadiah", "Obadias", "Abdias", 1, OT),
        BibleBook(32, "Jonah", "Jonas", "Jonas", 4, OT),
        BibleBook(33, "Micah", "Mikas", "Miqueas", 7, OT),
        BibleBook(34, "Nahum", "Nahum", "Nahum", 3, OT),
        BibleBook(35, "Habakkuk", "Habakuk", "Habacuc", 3, OT),
        BibleBook(36, "Zephaniah", "Zefanias", "Sofonias", 3, OT),
        BibleBook(37, "Haggai", "Hagai", "Haggeo", 2, OT),
        BibleBook(38, "Zechariah", "Zacarias", "Zacarias", 14, OT),
        BibleBook(39, "Malachi", "Malakias", "Malaquias", 4, OT),
        BibleBook(40, "Matthew", "Mateo", "Mateo", 28, NT),
        BibleBook(41, "Mark", "Marcos", "Marcos", 16, NT),
        BibleBook(42, "Luke", "Lucas", "Lucas", 24, NT),
        BibleBook(43, "John", "Juan", "Juan", 21, NT),
        BibleBook(44, "Acts", "Mga Gawa", "Mga Buhat", 28, NT),
        BibleBook(45, "Romans", "Mga Taga-Roma", "Mga Taga-Roma", 16, NT),
        BibleBook(46, "1 Corinthians", "1 Mga Taga-Corinto", "1 Mga Taga-Corinto", 16, NT),
        BibleBook(47, "2 Corinthians", "2 Mga Taga-Corinto", "2 Mga Taga-Corinto", 13, NT),
        BibleBook(48, "Galatians", "Mga Taga-Galacia", "Mga Taga-Galacia", 6, NT),
        BibleBook(49, "Ephesians", "Mga Taga-Efeso", "Mga Taga-Efeso", 6, NT),
        BibleBook(50, "Philippians", "Mga Taga-Filipos", "Mga Taga-Filipos", 4, NT),
        BibleBook(51, "Colossians", "Mga Taga-Colosas", "Mga Taga-Colosas", 4, NT),
        BibleBook(52, "1 Thessalonians", "1 Mga Taga-Tesalonica", "1 Mga Taga-Tesalonica", 5, NT),
        BibleBook(53, "2 Thessalonians", "2 Mga Taga-Tesalonica", "2 Mga Taga-Tesalonica", 3, NT),
        BibleBook(54, "1 Timothy", "1 Kay Timoteo", "1 Kang Timoteo", 6, NT),
        BibleBook(55, "2 Timothy", "2 Kay Timoteo", "2 Kang Timoteo", 4, NT),
        BibleBook(56, "Titus", "Kay Tito", "Kang Tito", 3, NT),
        BibleBook(57, "Philemon", "Kay Filemon", "Kang Filemon", 1, NT),
        BibleBook(58, "Hebrews", "Mga Hebreo", "Mga Hebreohanon", 13, NT),
        BibleBook(59, "James", "Santiago", "Santiago", 5, NT),
        BibleBook(60, "1 Peter", "1 Pedro", "1 Pedro", 5, NT),
        BibleBook(61, "2 Peter", "2 Pedro", "2 Pedro", 3, NT),
        BibleBook(62, "1 John", "1 Juan", "1 Juan", 5, NT),
        BibleBook(63, "2 John", "2 Juan", "2 Juan", 1, NT),
        BibleBook(64, "3 John", "3 Juan", "3 Juan", 1, NT),
        BibleBook(65, "Jude", "Judas", "Judas", 1, NT),
        BibleBook(66, "Revelation", "Pahayag", "Ang Pinadayag", 22, NT)
    )

    private val booksByNumber: Map<Int, BibleBook> = ALL.associateBy { it.number }

    /** Standard OSIS abbreviations, used as an extra alias source. */
    private fun osisId(number: Int): String = OSIS_IDS[number - 1]

    private val OSIS_IDS = listOf(
        "Gen", "Exod", "Lev", "Num", "Deut", "Josh", "Judg", "Ruth", "1Sam", "2Sam",
        "1Kgs", "2Kgs", "1Chr", "2Chr", "Ezra", "Neh", "Esth", "Job", "Ps", "Prov",
        "Eccl", "Song", "Isa", "Jer", "Lam", "Ezek", "Dan", "Hos", "Joel", "Amos",
        "Obad", "Jonah", "Mic", "Nah", "Hab", "Zeph", "Hag", "Zech", "Mal", "Matt",
        "Mark", "Luke", "John", "Acts", "Rom", "1Cor", "2Cor", "Gal", "Eph", "Phil",
        "Col", "1Thess", "2Thess", "1Tim", "2Tim", "Titus", "Phlm", "Heb", "Jas",
        "1Pet", "2Pet", "1John", "2John", "3John", "Jude", "Rev"
    )

    /**
     * Normalised-name lookup used by the asset seeder when a JSON file identifies
     * books by name instead of by number. The parser in Step 3 extends this with
     * spoken aliases and fuzzy matching; this map only needs to be exact-ish.
     */
    private val byNormalisedName: Map<String, BibleBook> = buildMap {
        ALL.forEach { book ->
            listOf(book.english, book.tagalog, book.cebuano).forEach { name ->
                put(normalise(name), book)
            }
            // Common short forms found in public-domain JSON dumps.
            put(normalise(book.english.replace(" ", "")), book)
            put(normalise(osisId(book.number)), book)
        }
        // A handful of alternate English spellings that show up in free datasets.
        put(normalise("Song of Songs"), booksByNumber.getValue(22))
        put(normalise("Canticles"), booksByNumber.getValue(22))
        put(normalise("Psalm"), booksByNumber.getValue(19))
        put(normalise("Revelation of John"), booksByNumber.getValue(66))
        put(normalise("Acts of the Apostles"), booksByNumber.getValue(44))
    }

    fun byNumber(number: Int): BibleBook? = booksByNumber[number]

    fun byName(raw: String): BibleBook? = byNormalisedName[normalise(raw)]

    /** Resolves either "43" or "John" or "Juan" to a book. */
    fun resolve(raw: String): BibleBook? =
        raw.trim().toIntOrNull()?.let { byNumber(it) } ?: byName(raw)

    fun chapterCount(bookNumber: Int): Int = booksByNumber[bookNumber]?.chapterCount ?: 0

    /** Lower-case, strip spaces, dots and hyphens so "1 Cor." == "1cor". */
    private fun normalise(value: String): String =
        value.lowercase().filter { it.isLetterOrDigit() }

}
