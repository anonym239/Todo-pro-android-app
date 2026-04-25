package com.todopro.app

import java.util.UUID

data class TodoItem(
    val id: String = UUID.randomUUID().toString(),
    var text: String,
    var reminderTime: Long? = null, // Unix timestamp in ms
    var isPriority: Boolean = false  // starts with "!" 
)
