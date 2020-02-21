package com.zt.test

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.zt.test.dlnacling.MainActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), View.OnClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btn_dlnaCling.setOnClickListener(this)
        btn_dlnaClingTest.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_dlnaCling -> startActivity(Intent(this, MainActivity::class.java))
//            R.id.btn_dlnaClingTest ->
        }
    }
}