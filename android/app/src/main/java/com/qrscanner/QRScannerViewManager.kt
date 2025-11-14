package com.qrscanner

import com.facebook.react.uimanager.ViewGroupManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp
import com.facebook.react.common.MapBuilder
import com.facebook.react.bridge.ReadableArray

class QRScannerViewManager : ViewGroupManager<QRScannerView>() {

    override fun getName() = "QRScannerView"

    override fun createViewInstance(reactContext: ThemedReactContext): QRScannerView {
        return QRScannerView(reactContext)
    }

    override fun getExportedCustomDirectEventTypeConstants(): Map<String, Any>? {
        return MapBuilder.of(
            "onFrame", MapBuilder.of("registrationName", "onFrame")
        )
    }

    override fun getCommandsMap(): MutableMap<String, Int> {
        return mutableMapOf("frameComplete" to 1)
    }

    
    override fun receiveCommand(
        root: QRScannerView,
        commandId: Int,
        args: ReadableArray?
    ) {
        when (commandId) {
            1 -> root.isProcessingFrame = false   
        }
    }

    @ReactProp(name = "cameraFacing")
    fun setCameraType(view: QRScannerView?, type: String?) {
        view?.setCameraType(type ?: "back")
    }

    @ReactProp(name = "torch")
    fun setTorchMode(view: QRScannerView?, flashMode: String?) {
        view?.setTorch(flashMode)
    }

    @ReactProp(name = "zoom", defaultFloat = 0.0f)
    fun setZoom(view: QRScannerView?, zoom: Float) {
        view?.setZoom(zoom)
    }
}
