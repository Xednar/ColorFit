package de.hs.colorpicker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.core.ImageCapture.FLASH_MODE_ON
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


typealias ColorListener = (color: String) -> Unit

class MainActivity : AppCompatActivity() {

    private var currentState = States.START

    private var imageCapture: ImageCapture? = null
    private var currentColor = "0-0-0"
    private var firstColor: String? = null
    private var recommendedColors: MutableList<String>? = null
    private var distance: Double? = null

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var camera: Camera

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Request camera permissions
        if (allPermissionsGranted()) {
            //cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                    this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        // Set up the listener for take photo button
        camera_capture_button.setOnClickListener {
            if (currentState == States.START) {
                takeFirstColor()
            } else {
                Toast.makeText(this, "TODO: Add $currentColor", Toast.LENGTH_SHORT).show()
            }
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onRequestPermissionsResult(
            requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun takeFirstColor() {
        if(imageCapture == null) return

        firstColor = currentColor
        val rgbColors = firstColor!!.split("-")

        calculateComplementaryColor(rgbColors)
        calculateAnalogueColors(rgbColors)

        currentState = States.SCAN
    }

    // Finding a complementary color is very simple in the RGB model. For any given color, for example, red (#FF0000),
    // you need to find the color, which, after being added to red, creates white (0xFFFFFF). Naturally, all you need to do,
    // is subtract red from white and get cyan (0xFFFFFF - 0xFF0000 = 0x00FFFF).
    private fun calculateComplementaryColor(rgbColors: List<String>) {
        val r = 255 - rgbColors[0].toInt()
        val g = 255 - rgbColors[1].toInt()
        val b = 255 - rgbColors[2].toInt()
        val complementaryColor = r.toString() + "," + g.toString() +  "," + b.toString()
        Log.d(TAG, "Komplementär: $r, $g, $b")
       addRecommendedColor(complementaryColor)
    }

    private fun calculateAnalogueColors(rgbColors: List<String>) {
        val hsv = FloatArray(3)
        var currentColor = Color.rgb(rgbColors[0].toInt(), rgbColors[1].toInt(), rgbColors[2].toInt())
        Color.colorToHSV(currentColor, hsv);

        hsv[0] = hsv[0].plus(30)
        currentColor = Color.HSVToColor(hsv)
        addRecommendedColor("${Color.red(currentColor)},${Color.green(currentColor)},${Color.blue(currentColor)}")
        Log.d(TAG, "Analog+30: ${Color.red(currentColor)},${Color.green(currentColor)},${Color.blue(currentColor)}")

        hsv[0] = hsv[0].minus(60)
        currentColor = Color.HSVToColor(hsv)
        addRecommendedColor("${Color.red(currentColor)},${Color.green(currentColor)},${Color.blue(currentColor)}")
        Log.d(TAG, "Analog-30: ${Color.red(currentColor)},${Color.green(currentColor)},${Color.blue(currentColor)}")
    }

    private fun addRecommendedColor(color: String) {
        if (recommendedColors == null) {
            recommendedColors = mutableListOf(color)
        } else {
            recommendedColors!!.add(color)
        }
    }


    private fun calculateColorDistance(r1:String, g1:String, b1:String, r2:String, g2:String, b2:String): Double {
        return Math.sqrt(Math.pow(r2.toDouble() - r1.toDouble(), 2.0)
                + Math.pow(g2.toDouble() - g1.toDouble(), 2.0)
                + Math.pow(b2.toDouble() - b1.toDouble(), 2.0))
    }

    private fun calculateMinColorDistance(): Double {
       // val rgbColors1 = firstColor!!.split("-")
        val rgbColors2 = currentColor.split("-")
        var distance = 999.0//calculateColorDistance(rgbColors1[0], rgbColors1[1], rgbColors1[2], rgbColors2[0], rgbColors2[1], rgbColors2[2])
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
                .setFlashMode(FLASH_MODE_ON)
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
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, imageAnalyzer)

                if ( camera.getCameraInfo().hasFlashUnit() ) {
                    camera.getCameraControl().enableTorch(true); // or false
                }

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))

        val successSound = MediaPlayer.create(applicationContext, R.raw.sound_success)
        val failureSound = MediaPlayer.create(applicationContext, R.raw.sound_failure)
        val checkDistance = Runnable {
            val text = findViewById<TextView>(R.id.color_name2)


            while (true) {

                if (currentState == States.SCAN) {
                    if (!failureSound.isPlaying) {
                        failureSound.start()
                    }
                    if (distance != null && distance!! < 10) {
                        if (failureSound.isPlaying) {
                            failureSound.stop()
                        }
                        if (!successSound.isPlaying) {
                            successSound.start()
                        }
                        runOnUiThread({
                            text.setText(currentColor)
                        }
                        )
                        break
                    }
                }

            }
        }
        Thread(checkDistance).start()
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