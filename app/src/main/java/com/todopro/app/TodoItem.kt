package com.todopro.app

import java.util.UUID

// Kategorien
enum class TodoCategory(val emoji: String, val label: String, val color: Int) {
    NONE("", "", 0),
    HOME("🏠", "Zuhause",    0xFF4CAF50.toInt()),
    WORK("💼", "Arbeit",     0xFF2196F3.toInt()),
    SHOPPING("🛒", "Einkaufen", 0xFFFF9800.toInt()),
    SPORT("🏃", "Sport",     0xFFE91E63.toInt()),
    HEALTH("💊", "Gesundheit", 0xFFAB47BC.toInt()),
    LEARN("📚", "Lernen",    0xFF00BCD4.toInt())
}

// Kartenfarben
enum class TodoColor(val colorHex: String, val displayName: String) {
    DEFAULT("", "Standard"),
    RED("#FF5252", "Rot"),
    ORANGE("#FF9800", "Orange"),
    YELLOW("#FFD740", "Gelb"),
    GREEN("#69F0AE", "Gruen"),
    BLUE("#40C4FF", "Blau"),
    PURPLE("#E040FB", "Lila"),
    PINK("#FF80AB", "Pink")
}

// Prioritaets-Level
enum class TodoPriority { NORMAL, HIGH, URGENT }

// Haupt-Datenmodell
data class TodoItem(
    val id: String = UUID.randomUUID().toString(),
    var text: String,
    var reminderTime: Long? = null,
    var isPriority: Boolean = false,
    var priority: String = TodoPriority.NORMAL.name,
    val createdAt: Long = System.currentTimeMillis(),
    var completedAt: Long? = null,
    var isCompleted: Boolean = false,
    var category: String = TodoCategory.NONE.name,
    var cardColor: String = TodoColor.DEFAULT.name,
    var isRecurring: Boolean = false,
    var recurringDays: Int = 0,
    var subtasks: MutableList<String> = mutableListOf(),
    var subtasksDone: MutableList<Boolean> = mutableListOf(),
    var estimatedMinutes: Int = 0,
    var note: String = ""
)

// Auto-Kategorisierungs-Keywords
object AutoCategory {

    private val rules: List<Pair<TodoCategory, List<String>>> = listOf(
        TodoCategory.SHOPPING to listOf(
            "kauf", "kaufen", "bestell", "bestellen", "supermarkt", "einkauf",
            "milch", "brot", "getraenk", "apotheke", "markt", "laden", "shop",
            "amazon", "liefern"
        ),
        TodoCategory.SPORT to listOf(
            "sport", "gym", "training", "lauf", "laufen", "joggen",
            "fitness", "schwimmen", "radfahren", "yoga", "workout", "spazier",
            "wandern", "fahrrad", "fussball", "tennis", "klettern"
        ),
        TodoCategory.HEALTH to listOf(
            "arzt", "doktor", "termin", "krankenhaus", "apotheke", "medizin",
            "tablette", "medikament", "impf", "zahnarzt", "blut", "rezept",
            "gesund", "krank", "therapie", "erholung"
        ),
        TodoCategory.WORK to listOf(
            "arbeit", "buero", "meeting", "konferenz", "praesentation", "report",
            "projekt", "deadline", "chef", "kollege", "email", "mail", "rechnung",
            "angebot", "kunde", "lieferant", "vertrag", "bewerb"
        ),
        TodoCategory.LEARN to listOf(
            "lernen", "lesen", "buch", "kurs", "sprache", "schule", "studium",
            "hausaufgabe", "pruefung", "examen", "vortrag", "recherche", "ueben",
            "podcast", "tutorial", "kapitel"
        ),
        TodoCategory.HOME to listOf(
            "zuhause", "haus", "wohnung", "zimmer", "kueche", "bad", "garten",
            "putzen", "aufraeumen", "kochen", "waschen", "muell", "reparier",
            "handwerker", "moebel", "einrichten", "renovier", "umzug"
        )
    )

    fun detect(text: String): TodoCategory {
        val lower = text.lowercase()
        var bestCategory = TodoCategory.NONE
        var bestScore = 0

        for ((cat, keywords) in rules) {
            val score = keywords.count { lower.contains(it) }
            if (score > bestScore) {
                bestScore = score
                bestCategory = cat
            }
        }
        return bestCategory
    }
}
