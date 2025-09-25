package com.example.photoapp10.core.util

import java.text.Normalizer

object TextNorm {
    /** lowercases and strips accents/diacritics */
    fun norm(s: String): String =
        Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .trim()
}

