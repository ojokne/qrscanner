package com.qrscanner

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleObserver
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.uimanager.ThemedReactContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.YuvImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream
import com.facebook.react.uimanager.events.RCTEventEmitter
import com.facebook.react.bridge.ReactContext
import java.nio.ByteBuffer
import android.util.Base64

import java.nio.ByteOrder


class QRScannerView(context: ThemedReactContext) : FrameLayout(context), LifecycleObserver {

  companion object {
    private const val TAG = "QRScannerView"
    private const val RATIO_4_3_VALUE = 4.0 / 3.0
    private const val RATIO_16_9_VALUE = 16.0 / 9.0
  }

  private val currentContext: ThemedReactContext = context

  private var camera: Camera? = null
  private var preview: Preview? = null
  private var imageCapture: ImageCapture? = null
  private var viewFinder: PreviewView = PreviewView(context)
  private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
  private var cameraProvider: ProcessCameraProvider? = null
  private var currentCameraType = CameraSelector.LENS_FACING_BACK

  @Volatile
  var isProcessingFrame = false


  private fun getCurrentActivity(): Activity {
    return currentContext.currentActivity!!
  }

  init {
    viewFinder.layoutParams = LinearLayout.LayoutParams(
      LayoutParams.MATCH_PARENT,
      LayoutParams.MATCH_PARENT
    )
    installHierarchyFitter(viewFinder)
    addView(viewFinder)
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    if (hasPermissions()) {
      viewFinder.post { setupCamera() }
    } else {
      Log.w(TAG, "Permissions not granted; camera won't be started.")
    }
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    cameraExecutor.shutdown()
    cameraProvider?.unbindAll()
  }

  private fun installHierarchyFitter(view: ViewGroup) {
    if (context is ThemedReactContext) {
      view.setOnHierarchyChangeListener(object : OnHierarchyChangeListener {
        override fun onChildViewRemoved(parent: View?, child: View?) = Unit
        override fun onChildViewAdded(parent: View?, child: View?) {
          parent?.measure(
            MeasureSpec.makeMeasureSpec(measuredWidth, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(measuredHeight, MeasureSpec.EXACTLY)
          )
          parent?.layout(0, 0, parent.measuredWidth, parent.measuredHeight)
        }
      })
    }
  }

  private fun setupCamera() {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(getCurrentActivity())
    cameraProviderFuture.addListener(Runnable {
      cameraProvider = cameraProviderFuture.get()

      addTouchListeners()
      bindCameraUseCases()
    }, ContextCompat.getMainExecutor(getCurrentActivity()))
  }

    @SuppressLint("ClickableViewAccessibility")
private fun addTouchListeners() {
  // Pinch to zoom
  val scaleGestureDetector = ScaleGestureDetector(context,
    object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
      override fun onScale(detector: ScaleGestureDetector): Boolean {
        // current zoom ratio (Float)
        val currentZoom = (camera?.cameraInfo?.zoomState?.value?.zoomRatio ?: 1.0f)
        // desired new zoom ratio
        val scale = (currentZoom * detector.scaleFactor).toFloat()

        // clamp to supported range (use defaults if not available)
        val min = camera?.cameraInfo?.zoomState?.value?.minZoomRatio ?: 1.0f
        val max = camera?.cameraInfo?.zoomState?.value?.maxZoomRatio ?: 3.0f
        val clamped = scale.coerceIn(min, max)

        // pass a Float (not Double)
        camera?.cameraControl?.setZoomRatio(clamped)

        return true
      }
    })

  viewFinder.setOnTouchListener { _, event ->
    if (event.pointerCount > 1) {
      scaleGestureDetector.onTouchEvent(event)
      return@setOnTouchListener true
    }
    if (event.action == MotionEvent.ACTION_UP) {
      focusOnTouchPoint(event.x, event.y)
    }
    return@setOnTouchListener true
  }
}


  private fun focusOnTouchPoint(x: Float, y: Float) {
    val factory = viewFinder.meteringPointFactory
    val builder = FocusMeteringAction.Builder(factory.createPoint(x, y),
      FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
    camera?.cameraControl?.startFocusAndMetering(builder.build())
  }

  private var lastFrameTime = 0L

  private fun sendFrameToJS(bitmap: Bitmap) {
    if (isProcessingFrame) return  
    isProcessingFrame = true       

    val reactContext = context as? ReactContext ?: return
    val width = bitmap.width
    val height = bitmap.height

    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

    // Convert ARGB Ints to RGBA bytes safely
    val byteArray = ByteArray(pixels.size * 4)
    for (i in pixels.indices) {
        val color = pixels[i]
        byteArray[i * 4] = ((color shr 16) and 0xFF).toByte() // R
        byteArray[i * 4 + 1] = ((color shr 8) and 0xFF).toByte() // G
        byteArray[i * 4 + 2] = (color and 0xFF).toByte() // B
        byteArray[i * 4 + 3] = ((color shr 24) and 0xFF).toByte() // A
    }

    val base64 = Base64.encodeToString(byteArray, Base64.NO_WRAP)

    val event = Arguments.createMap().apply {
        putString("pixels", base64)
        putInt("width", width)
        putInt("height", height)
    }

    if (id != View.NO_ID) {
        reactContext.getJSModule(RCTEventEmitter::class.java)
            .receiveEvent(id, "onFrame", event)
    }
}


@SuppressLint("UnsafeOptInUsageError")
private fun bindCameraUseCases() {
    val display = viewFinder.display ?: return
    val metrics = DisplayMetrics().also { display.getRealMetrics(it) }
    val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
    val rotation = viewFinder.display.rotation
    val cameraSelector = CameraSelector.Builder().requireLensFacing(currentCameraType).build()
    
    preview = Preview.Builder()
        .setTargetAspectRatio(screenAspectRatio)
        .setTargetRotation(rotation)
        .build()
    
    imageCapture = ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .setTargetAspectRatio(screenAspectRatio)
        .setTargetRotation(rotation)
        .build()
    
    // ImageAnalysis for QR scanning
    val imageAnalysis = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()
    imageAnalysis.setAnalyzer(cameraExecutor, QRAnalyzer { bitmap ->
        sendFrameToJS(bitmap)
    })

    cameraProvider?.unbindAll()
    
    try {
        camera = cameraProvider?.bindToLifecycle(
            getCurrentActivity() as AppCompatActivity,
            cameraSelector,
            preview,
            imageCapture,
            imageAnalysis
        )
        preview?.setSurfaceProvider(viewFinder.surfaceProvider)
    } catch (exc: Exception) {
        Log.e(TAG, "Use case binding failed", exc)
    }
}

  private fun aspectRatio(width: Int, height: Int): Int {
    val previewRatio = max(width, height).toDouble() / min(width, height)
    if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
      return AspectRatio.RATIO_4_3
    }
    return AspectRatio.RATIO_16_9
  }

  fun capture(promise: Promise) {
    if (imageCapture == null) {
      promise.reject(TAG, "Camera not ready")
      return
    }

    val contentValues = ContentValues().apply {
      put(MediaStore.MediaColumns.DISPLAY_NAME, "IMG_" + System.currentTimeMillis())
      put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
    }

    val outputOptions = ImageCapture.OutputFileOptions
      .Builder(
        context.contentResolver,
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
      )
      .build()

    imageCapture?.takePicture(outputOptions, ContextCompat.getMainExecutor(getCurrentActivity()), object : ImageCapture.OnImageSavedCallback {
      override fun onError(ex: ImageCaptureException) {
        promise.reject(TAG, "takePicture failed: ${ex.message}")
      }

      override fun onImageSaved(output: ImageCapture.OutputFileResults) {
        try {
          val savedUri = output.savedUri?.toString() ?: ""
          val imageInfo = Arguments.createMap()
          imageInfo.putString("uri", savedUri)
          imageInfo.putInt("width", viewFinder.width)
          imageInfo.putInt("height", viewFinder.height)
          promise.resolve(imageInfo)
        } catch (ex: Exception) {
          promise.reject(TAG, "Error while reading saved photo: ${ex.message}")
        }
      }
    })
  }

  // Methods exposed via manager

  fun setTorch(mode: String?) {
    val cameraLocal = camera ?: return
    when (mode) {
      "on" -> cameraLocal.cameraControl.enableTorch(true)
      "off" -> cameraLocal.cameraControl.enableTorch(false)
      else -> cameraLocal.cameraControl.enableTorch(false)
    }
  }

  fun setZoom(value: Float) {
    camera?.cameraControl?.setLinearZoom(min(1.0f, value))
  }

  fun setCameraType(type: String) {
    val newCameraType = when (type) {
      "front" -> CameraSelector.LENS_FACING_FRONT
      else -> CameraSelector.LENS_FACING_BACK
    }
    val shouldRebindCamera = currentCameraType != newCameraType
    currentCameraType = newCameraType
    if (shouldRebindCamera) {
      bindCameraUseCases()
    }
  }

  private fun hasPermissions(): Boolean {
    val required = mutableListOf(Manifest.permission.CAMERA)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      required.add("android.permission.READ_MEDIA_IMAGES")
    } else {
      required.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    return required.all {
      ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
  }
}


class QRAnalyzer(private val onFrame: (Bitmap) -> Unit) : ImageAnalysis.Analyzer {
    override fun analyze(image: ImageProxy) {
        val bitmap = image.toBitmap()
        if (bitmap != null) onFrame(bitmap)
        image.close()
    }

    private fun ImageProxy.toBitmap(): Bitmap? {
        if (format != ImageFormat.YUV_420_888) return null

        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(android.graphics.Rect(0, 0, width, height), 50, out)
        val imageBytes = out.toByteArray()
        return android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }
}
