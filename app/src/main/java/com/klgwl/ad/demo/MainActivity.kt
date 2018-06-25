package com.klgwl.ad.demo

import android.Manifest
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.klgwl.ad.sdk.KlgAd
import com.klgwl.ad.sdk.SdkParam
import com.klgwl.ad.util.L
import com.klgwl.ad.util.RHttp

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        KlgAd.init(application, 1858505698, "7f27dfbe2a1d3891d6e08ceed2e61806", BuildConfig.DEBUG)

        requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 100)

        findViewById<View>(R.id.get).setOnClickListener {
            RHttp.get("http://service.klgwl.com/adv/adrequest") {
                L.e("call: onCreate -> $it")
            }
        }
        findViewById<View>(R.id.post).setOnClickListener {
            RHttp.post("http://service.klgwl.com/adv/adrequest", SdkParam().body()) {
                L.e("call: onCreate -> $it")
            }
        }
    }
}