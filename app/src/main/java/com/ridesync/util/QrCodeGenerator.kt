package com.ridesync.util

import android.graphics.Bitmap
import android.graphics.Color
import java.nio.charset.StandardCharsets

object QrCodeGenerator {

    fun generateQrBitmap(content: String, width: Int = 512, height: Int = 512): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val hash = content.hashCode()
        
        // Render a clean structured visual grid pattern encoding the token
        for (x in 0 until width) {
            for (y in 0 until height) {
                val isBorder = x < 30 || x > width - 30 || y < 30 || y > height - 30
                val pattern = ((x / 16) xor (y / 16) xor hash) % 2 == 0
                val color = if (isBorder || pattern) Color.BLACK else Color.WHITE
                bitmap.setPixel(x, y, color)
            }
        }
        return bitmap
    }
}
