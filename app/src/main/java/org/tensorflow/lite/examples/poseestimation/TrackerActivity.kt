package org.tensorflow.lite.examples.poseestimation

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Process
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.tensorflow.lite.examples.poseestimation.camera.CameraSource
import org.tensorflow.lite.examples.poseestimation.data.Device
import org.tensorflow.lite.examples.poseestimation.data.ErrorTypes
import org.tensorflow.lite.examples.poseestimation.data.LeftParts
import org.tensorflow.lite.examples.poseestimation.data.Person
import org.tensorflow.lite.examples.poseestimation.data.RightParts
import org.tensorflow.lite.examples.poseestimation.ml.*
import org.tensorflow.lite.examples.poseestimation.progressions.ProgressionState
import org.tensorflow.lite.examples.poseestimation.progressions.ProgressionStates
import org.tensorflow.lite.examples.poseestimation.progressions.ProgressionTypes
import org.tensorflow.lite.examples.poseestimation.sessions.DatabaseHandler
import org.tensorflow.lite.examples.poseestimation.sessions.RepetitionItem
import java.time.Instant
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class TrackerActivity : AppCompatActivity() {
        companion object {
        private const val FRAGMENT_DIALOG = "dialog"
    }

    /** A [SurfaceView] for camera preview.   */
    private lateinit var surfaceView: SurfaceView

    /** Default pose estimation model is 1 (MoveNet Thunder)
     * 0 == MoveNet Lightning model
     * 1 == MoveNet Thunder model
     *
     * Unused models:
     * 2 == MoveNet MultiPose model
     * 3 == PoseNet model
     **/
    private var modelPos = 1

    /** Default device is GPU */
    private var device = Device.GPU

    /** Beeps enabled by default */
    private var repetitionAudio = true

    var persons = listOf<Person>()
    private lateinit var dbHandler: DatabaseHandler
    private lateinit var tvScore: TextView
    private lateinit var tvFPS: TextView
    private lateinit var spnDevice: Spinner
    private lateinit var spnModel: Spinner
    private lateinit var swSkeleton: SwitchCompat
    private lateinit var vSkeletonOption: View
    private lateinit var swAudio: SwitchCompat
    private lateinit var vAudioOption: View
    private lateinit var repFeedback: TextView
    private lateinit var repCounter: TextView
    private lateinit var displayProgressionType: TextView
    private lateinit var mediaPlayer: MediaPlayer
    private var cameraSource: CameraSource? = null
    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted. Continue the action or workflow in your
                // app.
                openCamera()
            } else {
                // Explain to the user that the feature is unavailable because the
                // features requires a permission that the user has denied. At the
                // same time, respect the user's decision. Don't link to system
                // settings in an effort to convince the user to change their
                // decision.
                ErrorDialog.newInstance(getString(R.string.tfe_pe_request_permission))
                    .show(supportFragmentManager, FRAGMENT_DIALOG)
            }
        }
    private var changeModelListener = object : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(parent: AdapterView<*>?) {
            // do nothing
        }

        override fun onItemSelected(
            parent: AdapterView<*>?,
            view: View?,
            position: Int,
            id: Long
        ) {
            changeModel(position)
        }
    }

    private var changeDeviceListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            changeDevice(position)
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
            // do nothing
        }
    }

    private var setSkeletonListener =
        CompoundButton.OnCheckedChangeListener { _, isChecked ->
            showSkeleton(isChecked)
        }

    private var setAudioListener =
        CompoundButton.OnCheckedChangeListener { _, isChecked ->
            toggleAudio(isChecked)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tracker)

        // Prepare DatabaseHandler
        dbHandler = DatabaseHandler(this)

        val progressionTypeText = "${ProgressionTypes.fromInt(intent.extras?.getInt("progressionType")!!)} PUSH-UP"

        displayProgressionType = findViewById(R.id.tvProgressionType)
        displayProgressionType.text = progressionTypeText

        // keep screen on while app is running
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        tvScore = findViewById(R.id.tvScore)
        tvFPS = findViewById(R.id.tvFps)
        spnModel = findViewById(R.id.spnModel)
        spnDevice = findViewById(R.id.spnDevice)
        surfaceView = findViewById(R.id.surfaceView)

        // Switch for skeleton overlay
        swSkeleton = findViewById<SwitchCompat>(R.id.swSkeleton)
        vSkeletonOption = findViewById<View>(R.id.vSkeletonOption) // RelativeLayout

        // Switch for beeping
        swAudio = findViewById<SwitchCompat>(R.id.swAudio)
        vAudioOption = findViewById<View>(R.id.vAudioOption) // RelativeLayout

        repFeedback = findViewById(R.id.tvRepFeedback)
        repCounter = findViewById(R.id.tvRepCounter)
        initSpinner()
        spnModel.setSelection(modelPos)

        swSkeleton.setOnCheckedChangeListener(setSkeletonListener)
        swAudio.setOnCheckedChangeListener(setAudioListener)

        if (!isCameraPermissionGranted()) {
            requestPermission()
        }
        if (!this::mediaPlayer.isInitialized) {
            mediaPlayer = MediaPlayer.create(this, R.raw.badform)
        }

        // Verify orientation
        val orientation = resources.configuration.orientation
        updateSurfaceViewRotation(orientation)
    }

    override fun onStart() {
        super.onStart()
        openCamera()
    }

    override fun onResume() {
        cameraSource?.resume()
        super.onResume()
    }

    override fun onPause() {
        cameraSource?.close()
        cameraSource = null
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        dbHandler.close()
    }

    // Update surfaceView (camera) when changing orientation
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateSurfaceViewRotation(newConfig.orientation)
    }

    private fun updateSurfaceViewRotation(orientation: Int) {
        when (orientation) {
            Configuration.ORIENTATION_PORTRAIT -> {
                surfaceView.rotation = 0f
            }
            Configuration.ORIENTATION_LANDSCAPE -> {
                surfaceView.rotation = 0f
            }
        }
        surfaceView.requestLayout()
    }

    // check if permission is granted or not.
    private fun isCameraPermissionGranted(): Boolean {
        return checkPermission(
            Manifest.permission.CAMERA,
            Process.myPid(),
            Process.myUid()
        ) == PackageManager.PERMISSION_GRANTED
    }

    private var currentState = ProgressionState(
        sessionId = 0,
        reps = Triple(-1, 0, -1),
        feedback = listOf("Waiting for position..."),
        state = ProgressionStates.INITIALIZE,
        startingArmDist = 0f,
        errors = setOf(),
        goodForm = true,
        lowestArmDist = 9999999f,
        down = false,
        errorCounter = ErrorTypes(),
        headPointingUp = true,
    )

    private var startPlayed = false
    private fun replacePersons(newPersons : List<Person>) {
        persons = newPersons
        val progression = ProgressionTypes.fromInt(intent.extras?.getInt("progressionType")!!)
        val nextState = progression.processHeuristics(currentState, persons[0], dbHandler, mediaPlayer)
        runOnUiThread {
            repFeedback.text = ""
            nextState.feedback.forEach{
                repFeedback.append(it)
            }

            val repCounterText = nextState.reps.first
            repCounter.text = repCounterText.toString()

        }
        currentState = nextState
        
        // Beeping per repetition denoting good or poor performance
        if (repetitionAudio && currentState.state == ProgressionStates.GOINGUP && !startPlayed) {
            startPlayed = true
            mediaPlayer.reset()
            if (currentState.goodForm)
                mediaPlayer.setDataSource(resources.openRawResourceFd(R.raw.goodform))
            else
                mediaPlayer.setDataSource(resources.openRawResourceFd(R.raw.badform))
            mediaPlayer.prepare()
            mediaPlayer.start()
        } else if (startPlayed && currentState.state == ProgressionStates.START)
            startPlayed = false
    }

    private fun debugChangeText(text: String) {
        runOnUiThread {
            repFeedback.text = text
        }
    }

    // open camera
    private fun openCamera() {
        if (isCameraPermissionGranted()) {
            if (cameraSource == null) {
                cameraSource =
                    CameraSource(surfaceView, object : CameraSource.CameraSourceListener {
                        override fun onFPSListener(fps: Int) {
                            tvFPS.text = getString(R.string.tfe_pe_tv_fps, fps)
                        }

                        //TODO: Find a way to get rid of pose classifier.
                        override fun onDetectedInfo(
                            personScore: Float?,
                            poseLabels: List<Pair<String, Float>>?
                        ) {
                            tvScore.text = getString(R.string.tfe_pe_tv_score, personScore ?: 0f)
                        }

                    }, this).apply {
                        prepareCamera()
                    }
                lifecycleScope.launch(Dispatchers.Main) {
                    cameraSource?.initCamera(::replacePersons, intent.extras?.getInt("progressionType")!!) //progression checking starts with this function call
                }
            }
            createPoseEstimator()
        }
    }

    private fun convertPoseLabels(pair: Pair<String, Float>?): String {
        if (pair == null) return "empty"
        return "${pair.first} (${String.format("%.2f", pair.second)})"
    }

    // Initialize spinners to let user select model/accelerator.
    private fun initSpinner() {
        ArrayAdapter.createFromResource(
            this,
            R.array.tfe_pe_models_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            spnModel.adapter = adapter
            spnModel.onItemSelectedListener = changeModelListener
        }

        ArrayAdapter.createFromResource(
            this,
            R.array.tfe_pe_device_name, android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            spnDevice.adapter = adapter
            spnDevice.onItemSelectedListener = changeDeviceListener
        }
    }

    // Change model when app is running
    private fun changeModel(position: Int) {
        if (modelPos == position) return
        modelPos = position
        createPoseEstimator()
    }

    // Change device (accelerator) type when app is running
    private fun changeDevice(position: Int) {
        val targetDevice = when (position) {
            0 -> Device.CPU
            1 -> Device.GPU
            else -> Device.NNAPI
        }
        if (device == targetDevice) return
        device = targetDevice
        createPoseEstimator()
    }

    private fun createPoseEstimator() {
        val poseDetector = when (modelPos) {
            0 -> {
                // MoveNet Lightning (SinglePose)
                showDetectionScore(true)
                MoveNet.create(this, device, ModelType.Lightning)
            }
            1 -> {
                // MoveNet Thunder (SinglePose)
                showDetectionScore(true)
                MoveNet.create(this, device, ModelType.Thunder)
            }
            else -> {
                null
            }
        }
        poseDetector?.let { detector ->
            cameraSource?.setDetector(detector)
        }
    }

    // Show/hide the detection score.
    private fun showDetectionScore(isVisible: Boolean) {
        tvScore.visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    // Show/hide skeleton overlay
    private fun showSkeleton(isVisible: Boolean) {
//        val visibility = if (isVisible) View.VISIBLE else View.GONE
        VisualizationUtils.skeletonOverlay = isVisible
    }

    private fun toggleAudio(isEnabled: Boolean) {
        repetitionAudio = isEnabled
    }

    private fun requestPermission() {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) -> {
                // You can use the API that requires the permission.
                openCamera()
            }
            else -> {
                // You can directly ask for the permission.
                // The registered ActivityResultCallback gets the result of this request.
                requestPermissionLauncher.launch(
                    Manifest.permission.CAMERA
                )
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    /**
     * Shows an error message dialog.
     */
    class ErrorDialog : DialogFragment() {

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
            AlertDialog.Builder(activity)
                .setMessage(requireArguments().getString(ARG_MESSAGE))
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    // do nothing
                }
                .create()

        companion object {

            @JvmStatic
            private val ARG_MESSAGE = "message"

            @JvmStatic
            fun newInstance(message: String): ErrorDialog = ErrorDialog().apply {
                arguments = Bundle().apply { putString(ARG_MESSAGE, message) }
            }
        }
    }
}