package com.example.photoapp10.core.util

import java.text.SimpleDateFormat
import java.util.*

object TimeFormat {
    private val df = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
    fun short(ts: Long): String = df.format(Date(ts))
}


