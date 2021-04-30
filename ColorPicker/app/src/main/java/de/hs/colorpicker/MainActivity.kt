package de.hs.colorpicker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.AuthFailureError
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import kotlinx.android.synthetic.main.activity_main.*
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


typealias ColorListener = (color: String) -> Unit

class MainActivity : AppCompatActivity() {

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

    private var imageCapture: ImageCapture? = null
    private var currentColor = "0-0-0"
    private var firstColor: String? = null
    private var colorResult: String? = null
    private var recommendedColors: List<String>? = null
    private var distance: Double? = null

    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                    this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        // Set up the listener for take photo button
        camera_capture_button.setOnClickListener {
            if (recommendedColors == null) {
            takeFirstColor()
        } }

        cameraExecutor = Executors.newSingleThreadExecutor()

    }

    override fun onRequestPermissionsResult(
            requestCode: Int, permissions: Array<String>, grantResults:
            IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this,
                        "Permissions not granted by the user.",
                        Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun takeFirstColor() {
        if(imageCapture == null) return

        firstColor = currentColor
        val rgbColors = firstColor!!.split("-")

        val queue = Volley.newRequestQueue(this)
       // val url = "http://thecolorapi.com/scheme?rgb=${rgbColors[0]},${rgbColors[1]},${rgbColors[2]}&format=json&mode=analogic&count=1"
        val url = "http://colormind.io/api/"
        val postRequest: StringRequest = object : StringRequest(Method.POST, url,
                Response.Listener {
                    response -> Log.d("Response", response)
                    handleResponse(response) },
                Response.ErrorListener {
                    error -> handleResponse("result={[[${rgbColors[0]},${rgbColors[1]},${rgbColors[2]}]]}")
                    Log.d("ERROR", "error => $error") }
        ) {
            // this is the relevant method
            @Throws(AuthFailureError::class)
            override fun getBody(): ByteArray {
                val httpPostBody = "{\"input\":[[${rgbColors[0]}, ${rgbColors[1]}, ${rgbColors[2]}]],\"model\":\"default\"}"
                return httpPostBody.toByteArray()
            }
        }
        queue.add(postRequest)
    }

    private fun handleResponse(response: String) {
        colorResult = response
        val colors = response.substring(11, response.length-4).replace("[", "")
        recommendedColors = (colors.split("],"))
        Toast.makeText(this, response, Toast.LENGTH_SHORT).show()
    }

    private fun calculateColorDistance(r1:String, g1:String, b1:String, r2:String, g2:String, b2:String): Double {
        return Math.sqrt(Math.pow(r1.toDouble() - r2.toDouble(), 2.0)
                + Math.pow(g1.toDouble() - g2.toDouble(), 2.0)
                + Math.pow(b1.toDouble() - b2.toDouble(), 2.0))
    }

    private fun calculateMinColorDistance(): Double {
        val rgbColors1 = firstColor!!.split("-")
        val rgbColors2 = currentColor.split("-")
        var distance = calculateColorDistance(rgbColors1[0], rgbColors1[1], rgbColors1[2], rgbColors2[0], rgbColors2[1], rgbColors2[2])
        for (recommended: String in recommendedColors!!) {
            val color = recommended.split(",")
            val dist =  calculateColorDistance(color[0], color[1], color[2], rgbColors2[0], rgbColors2[1], rgbColors2[2])
            if (dist < distance) {
                distance = dist
            }
        }
        return distance
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(viewFinder.createSurfaceProvider())
                    }

            imageCapture = ImageCapture.Builder()
                    .build()

            val imageAnalyzer = ImageAnalysis.Builder()
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor, ColorAnalyzer { rgb ->
                            if (recommendedColors == null){
                                Log.d(TAG, "RGB: $rgb")
                            } else {
                                distance = calculateMinColorDistance()
                                Log.d(TAG, "First: ${firstColor}, RGB: $rgb, Dist: $distance")
                            }
                            currentColor = rgb;
                        })
                    }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageCapture, imageAnalyzer)

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))

        var checkDistance = Runnable {
            var text = findViewById<TextView>(R.id.camera_capture_button)
            while (true) {
                if (distance != null && distance!! < 20) {
                    runOnUiThread(Runnable {
                        text.text = ("Matching Color $currentColor")
                    }
                    )
                    break
                }
            }
        }
        var checkDistanceThread = Thread(checkDistance).start()

    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
                baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraXBasic"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.INTERNET)
    }
}