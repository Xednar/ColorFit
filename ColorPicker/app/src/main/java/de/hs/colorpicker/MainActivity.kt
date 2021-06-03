package de.hs.colorpicker

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.math.MathUtils
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


typealias ColorListener = (color: String) -> Unit

class MainActivity : AppCompatActivity() {

    private val colorNameObject = ColorName()

    private var currentState = States.START
    private var torchActive = true
    private var soundActive = true

    private var imageCapture: ImageCapture? = null
    private var currentColor = "0-0-0"
    private var firstColor: String? = null
    private var recommendedColors: MutableList<String>? = null
    private var distance = 9999.0f

    private var color1Name = ""
    private var color2Name = ""
    private var color3Name = ""

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var camera: Camera

    private lateinit var successSound: MediaPlayer
    private lateinit var failureSound: MediaPlayer
    private lateinit var colorName1TextView: TextView
    private lateinit var colorName2TextView: TextView
    private lateinit var colorName3TextView: TextView
    private lateinit var flashButton: ImageButton
    private lateinit var soundButton: ImageButton
    private lateinit var deleteButton1: ImageButton
    private lateinit var deleteButton2: ImageButton
    private lateinit var deleteButton3: ImageButton
    private lateinit var cameraButton: Button

    private var threadsRunning = false

    val updateUI = Runnable {
        runOnUiThread(Runnable {
            colorName1TextView.text = color1Name
            colorName2TextView.text = color2Name
            colorName3TextView.text = color3Name
            cameraButton.isEnabled = color3Name.isBlank()
        })
    }

    val checkDistance = Runnable {
        while (threadsRunning) {
            if (currentState == States.START) {
                successSound.setVolume(0F, 0F)
                failureSound.setVolume(0F, 0F)
            } else if (currentState == States.SCAN) {
                var successVolume =
                    MathUtils.lerp(1.0f, 0.0f, 20.0f / Math.max(distance, 0.0000001f))
                var failureVolume = Math.max(0.0f, 0.8f - successVolume)
                if (!soundActive) {
                    successVolume = 0F
                    failureVolume = 0F
                }

                successSound.setVolume(successVolume, successVolume)
                failureSound.setVolume(failureVolume, failureVolume)
            }
            Thread.sleep(100)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        camera_capture_button.setOnClickListener {
            var canTakeColor = true
            val rgb = currentColor.split("-")
            val colorName = colorNameObject.getColorNameFromRgb(
                rgb.get(0).toInt(),
                rgb.get(1).toInt(),
                rgb.get(2).toInt()
            )
            if (color1Name.isBlank()) {
                color1Name = colorName + " " + currentColor
            } else if (color2Name.isBlank()) {
                color2Name = colorName + " " + currentColor
            } else if (color3Name.isBlank()) {
                color3Name = colorName + " " + currentColor
            } else {
                canTakeColor = false
            }
            if (canTakeColor) {
                Thread(updateUI).start()
                takeColor()
            }
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    fun removeRecommendedColorsForColorId(id: Int) {
        val AMOUNT = 3
        val recommendedColorsCopy = recommendedColors?.toMutableList()
        repeat(AMOUNT) {
            if (recommendedColorsCopy != null) {
                if (recommendedColorsCopy.size >= id*AMOUNT) {
                    recommendedColorsCopy.removeAt((id-1)*AMOUNT)
                }
            }
        }
        recommendedColors = recommendedColorsCopy
    }

    override fun onStart() {
        super.onStart()
        successSound = MediaPlayer.create(applicationContext, R.raw.sound_success)
        successSound.isLooping = true
        successSound.setOnPreparedListener(MediaPlayer.OnPreparedListener { mediaPlayer ->
            mediaPlayer.start(); mediaPlayer.setVolume(
            0.0f,
            0.0f
        )
        })
        failureSound = MediaPlayer.create(applicationContext, R.raw.sound_failure)
        failureSound.isLooping = true
        failureSound.setOnPreparedListener(MediaPlayer.OnPreparedListener { mediaPlayer ->
            mediaPlayer.start(); mediaPlayer.setVolume(
            0.0f,
            0.0f
        )
        })

        colorName1TextView = findViewById<TextView>(R.id.ColorName_1)
        colorName2TextView = findViewById<TextView>(R.id.ColorName_2)
        colorName3TextView = findViewById<TextView>(R.id.ColorName_3)
        flashButton = findViewById<ImageButton>(R.id.imageButton3)
        flashButton.setOnClickListener {
            torchActive = !torchActive
            if (torchActive) {
                flashButton.setImageResource(R.drawable.ic_baseline_flash_on_24)
            } else {
                flashButton.setImageResource(R.drawable.ic_baseline_flash_off_24)
            }
            if (camera.getCameraInfo().hasFlashUnit()) {
                camera.getCameraControl().enableTorch(torchActive)
            }
        }
        soundButton = findViewById<ImageButton>(R.id.imageButton2)
        soundButton.setOnClickListener {
            soundActive = !soundActive
            if (soundActive) {
                soundButton.setImageResource(R.drawable.ic_baseline_volume_up_24)
            } else {
                soundButton.setImageResource(R.drawable.ic_baseline_volume_off_24)
            }
        }
        deleteButton1 = findViewById<ImageButton>(R.id.delete_1)
        deleteButton1.setOnClickListener {
            removeRecommendedColorsForColorId(1)
            color1Name = color2Name
            color2Name = color3Name
            color3Name = ""
            if (color1Name.isBlank()) {
                currentState = States.START
            }
            Thread(updateUI).start()
        }
        deleteButton2 = findViewById<ImageButton>(R.id.delete_2)
        deleteButton2.setOnClickListener {
            removeRecommendedColorsForColorId(2)
            color2Name = color3Name
            color3Name = ""
            Thread(updateUI).start()
        }
        deleteButton3 = findViewById<ImageButton>(R.id.delete_3)
        deleteButton3.setOnClickListener {
            removeRecommendedColorsForColorId(3)
            color3Name = ""
            Thread(updateUI).start()
        }
        cameraButton = findViewById<Button>(R.id.camera_capture_button)
        threadsRunning = true;
        Thread(checkDistance).start()
        Thread(updateUI).start()
    }

    private fun takeColor() {
        if (imageCapture == null) return

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
        val complementaryColor = r.toString() + "," + g.toString() + "," + b.toString()
        Log.d(TAG, "Komplementär: $r, $g, $b")
        addRecommendedColor(complementaryColor)
    }

    private fun calculateAnalogueColors(rgbColors: List<String>) {
        val hsv = FloatArray(3)
        var currentColor =
            Color.rgb(rgbColors[0].toInt(), rgbColors[1].toInt(), rgbColors[2].toInt())
        Color.colorToHSV(currentColor, hsv);

        hsv[0] = hsv[0].plus(30)
        currentColor = Color.HSVToColor(hsv)
        addRecommendedColor(
            "${Color.red(currentColor)},${Color.green(currentColor)},${
                Color.blue(
                    currentColor
                )
            }"
        )
        Log.d(
            TAG,
            "Analog+30: ${Color.red(currentColor)},${Color.green(currentColor)},${
                Color.blue(currentColor)
            }"
        )

        hsv[0] = hsv[0].minus(60)
        currentColor = Color.HSVToColor(hsv)
        addRecommendedColor(
            "${Color.red(currentColor)},${Color.green(currentColor)},${
                Color.blue(
                    currentColor
                )
            }"
        )
        Log.d(
            TAG,
            "Analog-30: ${Color.red(currentColor)},${Color.green(currentColor)},${
                Color.blue(currentColor)
            }"
        )
    }

    private fun addRecommendedColor(color: String) {
        if (recommendedColors == null) {
            recommendedColors = mutableListOf(color)
        } else {
            recommendedColors!!.add(color)
        }
    }

    private fun calculateColorDistance(
        r1: String,
        g1: String,
        b1: String,
        r2: String,
        g2: String,
        b2: String
    ): Float {
        return Math.sqrt(
            Math.pow(r2.toDouble() - r1.toDouble(), 2.0)
                    + Math.pow(g2.toDouble() - g1.toDouble(), 2.0)
                    + Math.pow(b2.toDouble() - b1.toDouble(), 2.0)
        ).toFloat()
    }

    private fun calculateMinColorDistance(): Float {
        // val rgbColors1 = firstColor!!.split("-")
        val rgbColors2 = currentColor.split("-")
        var distance =
            999.0f//calculateColorDistance(rgbColors1[0], rgbColors1[1], rgbColors1[2], rgbColors2[0], rgbColors2[1], rgbColors2[2])
        for (recommended: String in recommendedColors!!) {
            val color = recommended.split(",")
            val dist = calculateColorDistance(
                color[0],
                color[1],
                color[2],
                rgbColors2[0],
                rgbColors2[1],
                rgbColors2[2]
            )
            if (dist < distance) {
                distance = dist
            }
        }
        return distance
    }

    private fun startCamera() {

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.createSurfaceProvider())
                }

            imageCapture = ImageCapture.Builder()
                //.setFlashMode(FLASH_MODE_ON)
                .build()

            val imageAnalyzer = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, ColorAnalyzer { rgb ->
                        if (recommendedColors == null) {
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
                camera = cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture,
                    imageAnalyzer
                )

                if (camera.getCameraInfo().hasFlashUnit()) {
                    camera.getCameraControl().enableTorch(torchActive)
                }

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

            Thread.sleep(100)

        }, ContextCompat.getMainExecutor(this))


    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onPause() {
        super.onPause()
        threadsRunning = false;
        failureSound.stop()
        successSound.stop()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT)
                    .show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraXBasic"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.INTERNET)
    }
}