package org.alphago.garbage.co

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.github.hiteshsondhi88.libffmpeg.FFmpeg
import com.github.hiteshsondhi88.libffmpeg.FFmpegLoadBinaryResponseHandler
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val auth: FirebaseAuth = FirebaseAuth.getInstance()



        /* FFMPEG library loaded into the device
            This is done once per app installation
        */
        try {
            FFmpeg.getInstance(this).loadBinary(object : FFmpegLoadBinaryResponseHandler {
                override fun onFailure() {
                    Log.e("FFMpeg", "Failed to load FFMpeg library.")
                }

                override fun onSuccess() {
                    Log.i("FFMpeg", "FFMpeg Library loaded!")
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

        } catch (e: Exception) {
            e.printStackTrace()
        }


        // checking if user has already used the app or first time running
        if (auth.currentUser!=null){
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
        else {
            val intent = Intent(this, SignIn::class.java)
            startActivity(intent)
            finish()
        }
    }
}
