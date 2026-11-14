package com.example.ui

import android.os.Bundle
import com.example.R
import com.journeyapps.barcodescanner.CaptureActivity
import com.journeyapps.barcodescanner.DecoratedBarcodeView

class CustomScannerActivity : CaptureActivity() {
    override fun initializeContent(): DecoratedBarcodeView {
        setContentView(R.layout.activity_custom_scanner)
        return findViewById(R.id.zxing_barcode_scanner)
    }
}
