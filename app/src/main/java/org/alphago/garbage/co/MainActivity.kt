package org.alphago.garbage.co

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.View
import android.widget.MediaController
import android.widget.Toast
import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler
import com.github.hiteshsondhi88.libffmpeg.FFmpeg
import kotlinx.android.synthetic.main.activity_main.*
const val REQUEST_VIDEO_CAPTURE = 102
const val REQUEST_VIDEO_UPLOAD = 101
const val WRITE_REQUEST_CODE = 100
var permission_request = 0
var filePath = ""
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        setupPermissions()
        takeVideo.setOnClickListener {
            videoCapture()

        }

        upVideo.setOnClickListener {
            //uploadVideo()
            if(permission_request == 0){
                Toast.makeText(this,"Please allow to write to storage!", Toast.LENGTH_LONG).show()
                setupPermissions()
                //uploadVideo()
            }
            else {
                uploadVideo()
            }
            //Toast.makeText(this,"Permissions should have been taken", Toast.LENGTH_LONG).show()
            //uploadVideo()
        }

    }

    private fun videoCapture() {
        val videoIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        if(videoIntent.resolveActivity(packageManager)!=null){
            startActivityForResult(videoIntent, REQUEST_VIDEO_CAPTURE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when(requestCode){
            REQUEST_VIDEO_CAPTURE -> {
                if(resultCode == Activity.RESULT_OK && data!=null) {
                    videoView.setVideoURI(data.data)
                    videoView.setMediaController(MediaController(this))
                    videoView.start()
                    videoToImages(data.data.path)
                    filePath = data.data.path
                    Toast.makeText(this, "video uri : " + filePath, Toast.LENGTH_LONG).show()
                }
            }

            REQUEST_VIDEO_UPLOAD -> {
                if(resultCode ==  Activity.RESULT_OK && data!=null) {
                    videoView.setVideoURI(data.data)
                    videoView.setMediaController(MediaController(this))
                    videoView.start()
                    videoToImages(data.data.path)
                    filePath = data.data.path
                    Toast.makeText(this, "upload uri : " + filePath, Toast.LENGTH_LONG).show()
                }
            }
            else -> {
                Toast.makeText(this, "Unrecognized request code $requestCode", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun uploadVideo(){
        val upIntent = Intent(Intent.ACTION_PICK)
        upIntent.type = "video/*"
        startActivityForResult(upIntent, REQUEST_VIDEO_UPLOAD)
    }

    private fun setupPermissions() {
        val permission = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.READ_EXTERNAL_STORAGE)
        val permission2 = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if(permission!=PackageManager.PERMISSION_GRANTED || permission2!=PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this@MainActivity, arrayOf<String>(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), WRITE_REQUEST_CODE)

        }
        else
            permission_request = 1
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            WRITE_REQUEST_CODE -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    permission_request = 0
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_LONG).show()
                } else {
                    permission_request = 1
                }
            }
        }
    }


    private fun videoToImages(inputPath: String?) {
        val outputPath = filePath
        val cmdCondition = " select=eq(pict_type\\,I) "
        val cmd = arrayOf("-i", inputPath, " -vf ", cmdCondition, " -vsync", " vfr ", outputPath + "image%04d.jpg")
        FFmpeg.getInstance(this).execute(cmd, object: ExecuteBinaryResponseHandler(){
            override fun onStart() {

            }
            override fun onProgress(message: String?) {

            }
            override fun onSuccess(message: String?) {

            }
            override fun onFailure(message: String?) {

            }
            override fun onFinish() {

            }
        })


    }

}
