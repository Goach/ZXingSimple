package com.goach.zxingsimple

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.utils.QRScanHelper
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"
    private lateinit var mQRScanHelper:QRScanHelper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mQRScanHelper = QRScanHelper(this)
        scanBtn.setOnClickListener {
            //获取权限省略
            mQRScanHelper.scanQRCode(TAG).subscribe({
                resultTv.text = it
            })
        }
    }
}
