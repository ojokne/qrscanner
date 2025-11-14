package com.qrscanner

import com.facebook.react.bridge.*
import com.facebook.react.uimanager.UIManagerModule

class QRScannerModule(private val context: ReactApplicationContext) : ReactContextBaseJavaModule(context) {

    @ReactMethod
    fun capture(tag: Int, promise: Promise) {
        val uiManager: UIManagerModule? =
            reactApplicationContext.getNativeModule(UIManagerModule::class.java)

        if (uiManager == null) {
            promise.reject("E_NO_UI_MANAGER", "UIManagerModule not available")
            return
        }

        val view = uiManager.resolveView(tag)
        if (view is QRScannerView) {
            view.capture(promise)
        } else {
            promise.reject("E_INVALID_TAG", "Invalid view tag or view not found")
        }
    }

    @ReactMethod
    fun frameComplete(viewTag: Int) {
        val uiManager = reactApplicationContext.getNativeModule(UIManagerModule::class.java)
        val view = uiManager?.resolveView(viewTag)

        if (view is QRScannerView) {
            view.isProcessingFrame = false
        }
    }

    override fun getName(): String = NAME

    companion object {
        private const val NAME = "QRScannerModule"
    }
}
