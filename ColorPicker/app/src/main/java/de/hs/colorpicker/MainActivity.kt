package de.hs.colorpicker

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.constraintlayout.widget.ConstraintLayout
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
    @Volatile private var recommendedColors: MutableList<String>? = null

    private var previous_small_smiley_state_1 = 5
    private var previous_small_smiley_state_2 = 5
    private var previous_small_smiley_state_3 = 5

  //  private var distance = 9999.0f
    private var distanceTo1 = 9999.0f
    private var distanceTo2 = 9999.0f
    private var distanceTo3 = 9999.0f
    //private var previous_big_smiley_state = 3

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
    private lateinit var smileyBigImageView: ImageView
    private lateinit var smileySmallImageView_1: ImageView
    private lateinit var smileySmallImageView_2: ImageView
    private lateinit var smileySmallImageView_3: ImageView
    private lateinit var flashButton: ImageButton
    private lateinit var soundButton: ImageButton
    private lateinit var deleteButton1: ImageButton
    private lateinit var deleteButton2: ImageButton
    private lateinit var deleteButton3: ImageButton
    private lateinit var infoButton1: ImageButton
    private lateinit var infoButton2: ImageButton
    private lateinit var infoButton3: ImageButton
    private lateinit var infoLayerCloseButton: ImageButton
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
                smileyBigImageView.setImageResource(R.drawable.ic_face_5)
                smileySmallImageView_1.setImageResource(R.drawable.face_small_5)
                smileySmallImageView_2.setImageResource(R.drawable.face_small_5)
                smileySmallImageView_3.setImageResource(R.drawable.face_small_5)
            } else if (currentState == States.SCAN) {
                //Log.i(TAG, "DISTANCE: $distance")

                // update small smileys

                // smiley 1
                if (distanceTo1 > 85  && previous_small_smiley_state_1 > 1) {
                    previous_small_smiley_state_1 = 1
                    smileySmallImageView_1.setImageResource(R.drawable.face_small_1)
                }

                if ( (distanceTo1 > 75  && previous_small_smiley_state_1 < 2) || // 60 - 80
                    (distanceTo1 > 65 && previous_small_smiley_state_1 > 2)) {
                    previous_small_smiley_state_1 = 2
                    smileySmallImageView_1.setImageResource(R.drawable.face_small_2)
                }

                if ( (distanceTo1 > 55  && previous_small_smiley_state_1 < 3) || // 40-60
                    (distanceTo1 > 45 && previous_small_smiley_state_1 > 3)) {
                    previous_small_smiley_state_1 = 3
                    smileySmallImageView_1.setImageResource(R.drawable.face_small_3)
                }

                if ( (distanceTo1 > 35  && previous_small_smiley_state_1 < 4) || // 20-40
                    (distanceTo1 > 25 && previous_small_smiley_state_1 > 4)) {
                    previous_small_smiley_state_1 = 4
                    smileySmallImageView_1.setImageResource(R.drawable.face_small_4)
                }

                if (distanceTo1 <= 15 && previous_small_smiley_state_1 < 5) {
                    previous_small_smiley_state_1 = 5
                    smileySmallImageView_1.setImageResource(R.drawable.face_small_5)
                }

                // smiley 2
                if (distanceTo2 > 85  && previous_small_smiley_state_2 > 1) {
                    previous_small_smiley_state_2 = 1
                    smileySmallImageView_2.setImageResource(R.drawable.face_small_1)
                }

                if ( (distanceTo2 > 75  && previous_small_smiley_state_2 < 2) || // 60 - 80
                    (distanceTo2 > 65 && previous_small_smiley_state_2 > 2)) {
                    previous_small_smiley_state_2 = 2
                    smileySmallImageView_2.setImageResource(R.drawable.face_small_2)
                }

                if ( (distanceTo2 > 55  && previous_small_smiley_state_2 < 3) || // 40-60
                    (distanceTo2 > 45 && previous_small_smiley_state_2 > 3)) {
                    previous_small_smiley_state_2 = 3
                    smileySmallImageView_2.setImageResource(R.drawable.face_small_3)
                }

                if ( (distanceTo2 > 35  && previous_small_smiley_state_2 < 4) || // 20-40
                    (distanceTo2 > 25 && previous_small_smiley_state_2 > 4)) {
                    previous_small_smiley_state_2 = 4
                    smileySmallImageView_2.setImageResource(R.drawable.face_small_4)
                }

                if (distanceTo2 <= 15 && previous_small_smiley_state_2 < 5) {
                    previous_small_smiley_state_2 = 5
                    smileySmallImageView_2.setImageResource(R.drawable.face_small_5)
                }

                // smiley 3
                if (distanceTo3 > 85  && previous_small_smiley_state_3 > 1) {
                    previous_small_smiley_state_3 = 1
                    smileySmallImageView_3.setImageResource(R.drawable.face_small_1)
                }

                if ( (distanceTo3 > 75  && previous_small_smiley_state_3 < 2) || // 60 - 80
                    (distanceTo3 > 65 && previous_small_smiley_state_3 > 2)) {
                    previous_small_smiley_state_3 = 2
                    smileySmallImageView_3.setImageResource(R.drawable.face_small_2)
                }

                if ( (distanceTo3 > 55  && previous_small_smiley_state_3 < 3) || // 40-60
                    (distanceTo3 > 45 && previous_small_smiley_state_3 > 3)) {
                    previous_small_smiley_state_3 = 3
                    smileySmallImageView_3.setImageResource(R.drawable.face_small_3)
                }

                if ( (distanceTo3 > 35  && previous_small_smiley_state_3 < 4) || // 20-40
                    (distanceTo3 > 25 && previous_small_smiley_state_3 > 4)) {
                    previous_small_smiley_state_3 = 4
                    smileySmallImageView_3.setImageResource(R.drawable.face_small_4)
                }

                if (distanceTo3 <= 15 && previous_small_smiley_state_3 < 5) {
                    previous_small_smiley_state_3 = 5
                    smileySmallImageView_3.setImageResource(R.drawable.face_small_5)
                }

                // big smiley and sound
                var successVolume = 1.0F
                val minImageState = Math.min(previous_small_smiley_state_1,
                    Math.min(previous_small_smiley_state_2, previous_small_smiley_state_3))

                if (minImageState == 1) {
                    successVolume = 0.0F
                    smileyBigImageView.setImageResource(R.drawable.ic_face_1)
                }

                else if (minImageState == 2) {
                    successVolume = 0.25F
                    smileyBigImageView.setImageResource(R.drawable.ic_face_2)
                }

                else if (minImageState == 3) {
                    successVolume = 0.5F
                    smileyBigImageView.setImageResource(R.drawable.ic_face_3)
                }

                else if (minImageState == 4) {
                    successVolume = 0.75F
                    smileyBigImageView.setImageResource(R.drawable.ic_face_4)
                }

                else if (minImageState == 5) {
                    smileyBigImageView.setImageResource(R.drawable.ic_face_5)
                }
                var failureVolume = 1.0f - successVolume

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
                color1Name = colorName + "#" + currentColor
            } else if (color2Name.isBlank()) {
                color2Name = colorName + "#" + currentColor
            } else if (color3Name.isBlank()) {
                color3Name = colorName + "#" + currentColor
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

    @Synchronized
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
            previous_small_smiley_state_1 = previous_small_smiley_state_2
            previous_small_smiley_state_2 = previous_small_smiley_state_3
            previous_small_smiley_state_3 = 5
            distanceTo1  = distanceTo2
            distanceTo2  = distanceTo3
            distanceTo3  = 9999.0F
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
            previous_small_smiley_state_2 = previous_small_smiley_state_3
            previous_small_smiley_state_3 = 5
            distanceTo2  = distanceTo3
            distanceTo3  = 9999.0F
            Thread(updateUI).start()
        }
        deleteButton3 = findViewById<ImageButton>(R.id.delete_3)
        deleteButton3.setOnClickListener {
            removeRecommendedColorsForColorId(3)
            color3Name = ""
            previous_small_smiley_state_3 = 5
            distanceTo3 = 9999.0F
            Thread(updateUI).start()
        }
        infoLayerCloseButton = findViewById<ImageButton>(R.id.infoLayerExitButton)
        infoLayerCloseButton.setOnClickListener {
            findViewById<ConstraintLayout>(R.id.info_layer).visibility = INVISIBLE
        }
        infoButton1 = findViewById<ImageButton>(R.id.Info_1)
        infoButton1.setOnClickListener {
            if (color1Name.isNotBlank()) {
                val colorName = color1Name.split("#")[0].split(" ").last()
                findViewById<TextView>(R.id.Layer_Farbname).text = colorName
                findViewById<TextView>(R.id.Layer_Info).text = getResources().getString(getResources().getIdentifier(colorName, "string", getPackageName()))
                findViewById<ConstraintLayout>(R.id.info_layer).visibility = VISIBLE
            }
        }
        infoButton2 = findViewById<ImageButton>(R.id.Info_2)
        infoButton2.setOnClickListener {
            if (color2Name.isNotBlank()) {
                val colorName = color2Name.split("#")[0].split(" ").last()
                findViewById<TextView>(R.id.Layer_Farbname).text = colorName
                findViewById<TextView>(R.id.Layer_Info).text = getResources().getString(getResources().getIdentifier(colorName, "string", getPackageName()))
                findViewById<ConstraintLayout>(R.id.info_layer).visibility = VISIBLE
            }
        }
        infoButton3 = findViewById<ImageButton>(R.id.Info_3)
        infoButton3.setOnClickListener {
            if (color2Name.isNotBlank()) {
                val colorName = color3Name.split("#")[0].split(" ").last()
                findViewById<TextView>(R.id.Layer_Farbname).text = colorName
                findViewById<TextView>(R.id.Layer_Info).text = getResources().getString(getResources().getIdentifier(colorName, "string", getPackageName()))
                findViewById<ConstraintLayout>(R.id.info_layer).visibility = VISIBLE
            }
        }
        cameraButton = findViewById<Button>(R.id.camera_capture_button)
        smileyBigImageView = findViewById<ImageView>(R.id.imageView)
        smileySmallImageView_1 = findViewById<ImageView>(R.id.imageView4)
        smileySmallImageView_2 = findViewById<ImageView>(R.id.imageView5)
        smileySmallImageView_3 = findViewById<ImageView>(R.id.imageView6)
        threadsRunning = true;
        Thread(checkDistance).start()
        Thread(updateUI).start()
    }

    @Synchronized
    private fun takeColor() {
        if (imageCapture == null) return

        val rgbColors = currentColor.split("-")

        calculateComplementaryColor(rgbColors)
        calculateAnalogueColors(rgbColors)

        currentState = States.SCAN
    }

    // Finding a complementary color is very simple in the RGB model. For any given color, for example, red (#FF0000),
    // you need to find the color, which, after being added to red, creates white (0xFFFFFF). Naturally, all you need to do,
    // is subtract red from white and get cyan (0xFFFFFF - 0xFF0000 = 0x00FFFF).
    @Synchronized
    private fun calculateComplementaryColor(rgbColors: List<String>) {
        val r = 255 - rgbColors[0].toInt()
        val g = 255 - rgbColors[1].toInt()
        val b = 255 - rgbColors[2].toInt()
        val complementaryColor = r.toString() + "," + g.toString() + "," + b.toString()
        Log.d(TAG, "Komplementär: $r, $g, $b")
        addRecommendedColor(complementaryColor)
    }

    @Synchronized
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

    @Synchronized
    private fun addRecommendedColor(color: String) {
        if (recommendedColors == null) {
            recommendedColors = mutableListOf(color)
        } else {
            recommendedColors!!.add(color)
        }
    }

    @Synchronized
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

    @Synchronized
    private fun calculateDistanceTo1(): Float {
        val rgbColors2 = currentColor.split("-")
        var distance = 999.0f
        if (recommendedColors!!.isEmpty()) {
            // TODO 1. zeile ausblenden
            distanceTo1 = 9999.0F
            previous_small_smiley_state_1 = 5
            return distanceTo1
        }
        val colorsToCompare = mutableListOf<String?>()
        colorsToCompare.add(recommendedColors?.getOrNull(0))
        colorsToCompare.add(recommendedColors?.getOrNull(1))
        colorsToCompare.add(recommendedColors?.getOrNull(2))

        for (recommended: String? in colorsToCompare) {
            if (recommended == null)  continue
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

    @Synchronized
    private fun calculateDistanceTo2(): Float {
        val rgbColors2 = currentColor.split("-")
        var distance = 999.0f
        if (recommendedColors!!.size <= 3) {
            // TODO 2. zeile ausblenden
            distanceTo2 = 9999.0F
            previous_small_smiley_state_2 = 5
            return distanceTo2
        }
        val colorsToCompare = mutableListOf<String?>()
        colorsToCompare.add(recommendedColors?.getOrNull(3))
        colorsToCompare.add(recommendedColors?.getOrNull(4))
        colorsToCompare.add(recommendedColors?.getOrNull(5))

        for (recommended: String? in colorsToCompare) {
            if (recommended == null)  continue
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

    @Synchronized
    private fun calculateDistanceTo3(): Float {
        val rgbColors2 = currentColor.split("-")
        var distance = 999.0f
        if (recommendedColors!!.size <= 6) {
            // TODO 3. zeile ausblenden
            distanceTo3 = 9999.0F
            previous_small_smiley_state_3 = 5
            return distanceTo3
        }
        val colorsToCompare = mutableListOf<String?>()
        colorsToCompare.add(recommendedColors?.getOrNull(6))
        colorsToCompare.add(recommendedColors?.getOrNull(7))
        colorsToCompare.add(recommendedColors?.getOrNull(8))

        for (recommended: String? in colorsToCompare) {
            if (recommended == null)  continue
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

   /* @Synchronized
    private fun calculateMinColorDistance(): Float {
        val rgbColors2 = currentColor.split("-")
        var distance = 999.0f
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
    }*/

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
                            //distance = calculateMinColorDistance()
                            distanceTo1 = calculateDistanceTo1()
                            distanceTo2 = calculateDistanceTo2()
                            distanceTo3 = calculateDistanceTo3()
                            Log.d(TAG, "Distance1: $distanceTo1, Distance2: $distanceTo2, Distance3: $distanceTo3")
                           // distance = Math.min(distanceTo3, Math.min(distanceTo1, distanceTo2))
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