package com.example.smartswine.util

import com.itextpdf.layout.font.FontProvider
import java.io.File

object FontTest {
    @JvmStatic
    fun main(args: Array<String>) {
        val fontProvider = FontProvider()
        val systemFontsDir = File("/system/fonts")
        println("systemFontsDir exists: \")
    }
}
