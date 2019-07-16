package org.alphago.garbage.co

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
//import android.os.Environment
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
//import android.util.Log
//import android.view.View
import android.widget.MediaController
import android.widget.Toast
import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler
import com.github.hiteshsondhi88.libffmpeg.FFmpeg
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
//import org.jcodec.common.RunLength
import java.io.File


const val REQUEST_VIDEO_CAPTURE = 102
const val REQUEST_VIDEO_UPLOAD = 101
const val WRITE_REQUEST_CODE = 100
var permission_request = 0
var filePath : String? = ""
//var duration : Double? = null

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

        }
        settings.setOnClickListener{
            finish()
        }

    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when(requestCode){
            REQUEST_VIDEO_CAPTURE -> {
                if(resultCode == Activity.RESULT_OK && data!=null) {
                    filePath = getRealPathFromURI(data.data)
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(filePath)
                    val duration = (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)).toDouble()

                    videoView.setVideoURI(data.data)
                    videoView.setMediaController(MediaController(this))
                    videoView.start()

                    videoToImages(filePath, duration)
                }
            }

            REQUEST_VIDEO_UPLOAD -> {
                if(resultCode ==  Activity.RESULT_OK && data!=null) {
                    filePath = getRealPathFromURI(data.data)
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(filePath)
                    val duration = (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)).toDouble()

                    videoView.setVideoURI(data.data)
                    videoView.setMediaController(MediaController(this))
                    videoView.start()

                    videoToImages(filePath, duration)
                }
            }
            else -> {
                Toast.makeText(this, "Unrecognized request code $requestCode", Toast.LENGTH_LONG).show()
            }
        }
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


     fun videoToImages(inputPath: String?, duration: Double) {
        //val file = File()
        val outputPath = outputDir() + "/test%04d.jpg"
        var fps : Double = (25000/ duration)
        val cmd = arrayOf("-i", inputPath, "-vf", "fps=$fps", outputPath)
        FFmpeg.getInstance(this).execute(cmd, object: ExecuteBinaryResponseHandler(){
            override fun onStart() {

            }
            override fun onProgress(message: String?) {

            }
            override fun onSuccess(message: String?) {
                Toast.makeText(this@MainActivity, "video to images success", Toast.LENGTH_LONG).show()
            }
            override fun onFailure(message: String?) {
                Toast.makeText(this@MainActivity, "video to images failed", Toast.LENGTH_LONG).show()
            }
            override fun onFinish() {

            }
        })
    }

    private fun videoCapture() {
        val videoIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        if(videoIntent.resolveActivity(packageManager)!=null){
            startActivityForResult(videoIntent, REQUEST_VIDEO_CAPTURE)
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

    private fun getRealPathFromURI(contentURI: Uri): String? {
        val result: String?
        val cursor = contentResolver.query(contentURI, null, null, null, null)
        if (cursor == null) { // Source is Dropbox or other similar local file path
            result = contentURI.path
        } else {
            cursor.moveToFirst()
            val idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
            result = cursor.getString(idx)
            cursor.close()
        }
        return result
    }

    private fun outputDir(): String {
        val folder = File("/storage/emulated/0/Garbage/")
        folder.mkdirs()
        return folder.path
    }
}
