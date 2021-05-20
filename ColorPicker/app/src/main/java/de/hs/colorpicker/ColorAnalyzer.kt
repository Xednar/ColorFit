package de.hs.colorpicker

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer

class ColorAnalyzer(private val listener: ColorListener) : ImageAnalysis.Analyzer {

    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()    // Rewind the buffer to zero
        val data = ByteArray(remaining())
        get(data)   // Copy the buffer into a byte array
        return data // Return the byte array
    }

    private fun getRGBfromYUV(image: ImageProxy): Triple<Double, Double, Double> {
        val planes = image.planes

        val height = image.height
        val width = image.width

        // Y
        val yArr = planes[0].buffer
        val yArrByteArray = yArr.toByteArray()
        val yPixelStride = planes[0].pixelStride
        val yRowStride = planes[0].rowStride

        // U
        val uArr = planes[1].buffer
        val uArrByteArray =uArr.toByteArray()
        val uPixelStride = planes[1].pixelStride
        val uRowStride = planes[1].rowStride

        // V
        val vArr = planes[2].buffer
        val vArrByteArray = vArr.toByteArray()
        val vPixelStride = planes[2].pixelStride
        val vRowStride = planes[2].rowStride

        val y = yArrByteArray[(height * yRowStride + width * yPixelStride) / 2].toInt() and 255
        val u = (uArrByteArray[(height * uRowStride + width * uPixelStride) / 4].toInt() and 255) - 128
        val v = (vArrByteArray[(height * vRowStride + width * vPixelStride) / 4].toInt() and 255) - 128

        val r = y + (1.370705 * v)
        val g = y - (0.698001 * v) - (0.337633 * u)
        val b = y + (1.732446 * u)

        return Triple(Math.abs(r), Math.abs(g), Math.abs(b))
    }


    // analyze the color
    override fun analyze(image: ImageProxy) {
        val colors = getRGBfromYUV(image)
        val hexColor = String.format("%d-%d-%d", colors.first.toInt(), colors.second.toInt(), colors.third.toInt())
        listener(hexColor)
        image.close()
    }
}
