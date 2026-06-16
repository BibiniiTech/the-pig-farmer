package com.example.smartswine.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object GlobalNotice {
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    fun show(msg: String) {
        _message.value = msg
    }

    fun clear() {
        _message.value = null
    }
}