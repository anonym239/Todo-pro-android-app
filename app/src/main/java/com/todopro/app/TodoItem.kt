package com.todopro.app

import java.util.UUID

// Verfügbare Kategorien
enum class TodoCategory(val emoji: String, val label: String, val color: Int) {
    NONE("", "", 0),
    HOME("🏠", "Zuhause", 0xFF4CAF50.toInt()),
    WORK("💼", "Arbeit", 0xFF2196F3.toInt()),
    SHOPPING("🛒", "Einkaufen", 0xFFFF9800.toInt()),
    SPORT("🏃", "Sport", 0xFFE91E63.toInt()),
    HEALTH("💊", "Gesundheit", 0xFF9C27B0.toInt()),
    LEARN("📚", "Lernen", 0xFF00BCD4.toInt())
}

// Kartenfarben
enum class TodoColor(val colorHex: String, val displayName: String) {
    DEFAULT("", "Standard"),
    RED("#FF5252", "Rot"),
    ORANGE("#FF9800", "Orange"),
    YELLOW("#FFD740", "Gelb"),
    GREEN("#69F0AE", "Grün"),
    BLUE("#40C4FF", "Blau"),
    PURPLE("#E040FB", "Lila"),
    PINK("#FF80AB", "Pink")
}

data class TodoItem(
    val id: String = UUID.randomUUID().toString(),
    var text: String,
    var reminderTime: Long? = null,
    var isPriority: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    var completedAt: Long? = null,
    var isCompleted: Boolean = false,
    var category: String = TodoCategory.NONE.name,
    var cardColor: String = TodoColor.DEFAULT.name,
    var isRecurring: Boolean = false,
    var recurringDays: Int = 0  // Wiederholung in Tagen (0 = keine)
)
