package org.alphago.garbage.co

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.github.hiteshsondhi88.libffmpeg.FFmpeg
import com.github.hiteshsondhi88.libffmpeg.FFmpegLoadBinaryResponseHandler
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException
import com.google.firebase.auth.FirebaseAuth

class splashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        var auth: FirebaseAuth
        auth = FirebaseAuth.getInstance()

        var noSupport = false

        try {
            FFmpeg.getInstance(this).loadBinary(object : FFmpegLoadBinaryResponseHandler {
                override fun onFailure() {
                    Log.e("FFMpeg", "Failed to load FFMpeg library.")
                }

                override fun onSuccess() {
                    Log.i("FFMpeg", "FFMpeg Library loaded!")
                    //Toast.makeText(this@splashActivity, "success", Toast.LENGTH_LONG).show()
                }

                override fun onStart() {
                    Log.i("FFMpeg", "FFMpeg Started")
                }

                override fun onFinish() {
                    Log.i("FFMpeg", "FFMpeg Stopped")
                }
            })
        } catch (e: FFmpegNotSupportedException) {
            e.printStackTrace()
            noSupport = true
        } catch (e: Exception) {
            e.printStackTrace()
        }


        if (auth.currentUser!=null){
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
        else {
            val intent = Intent(this, signIn::class.java)
            startActivity(intent)
            finish()
        }
    }
}
