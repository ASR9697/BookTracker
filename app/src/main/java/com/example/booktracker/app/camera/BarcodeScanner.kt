package com.example.booktracker.app.camera

import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class BarcodeAnalyzer(private val onIsbnScanned: (String) -> Unit) : ImageAnalysis.Analyzer {

    // ISBNs are physically EAN-13 barcodes; ML Kit has no ISBN *format*.
    // We scan EAN-13 and filter by value type below.
    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_EAN_13)
        .build()

    private val scanner = BarcodeScanning.getClient(options)

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    barcodes.firstOrNull { it.valueType == Barcode.TYPE_ISBN }
                        ?.rawValue
                        ?.let { isbn ->
                            Log.d("BarcodeAnalyzer", "Scanned ISBN: $isbn")
                            onIsbnScanned(isbn)
                        }
                }
                .addOnFailureListener {
                    Log.e("BarcodeAnalyzer", "Barcode scanning failed", it)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}
