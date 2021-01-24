package org.alphago.garbage.co

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.MediaStore
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.util.Log
import android.view.View
import android.widget.MediaController
import android.widget.Toast
import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler
import com.github.hiteshsondhi88.libffmpeg.FFmpeg
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import java.io.File

const val NO_OF_IMAGES = 25
const val REQUEST_VIDEO_CAPTURE = 102
const val REQUEST_VIDEO_UPLOAD = 101
const val PERMISSION_REQUEST = 210
const val REQUEST_CHECK_SETTINGS = 201


class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var mLocationRequestHighAccuracy: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var finalLocation: Location? = null
    private var requestingLocationUpdates = 0
    private val updateInterval = (2 * 1000).toLong()  /* 10 secs */
    private val fastestInterval: Long = 2000 /* 2 sec */
    private var filePath: String? = ""
    private lateinit var myRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        uploadCon.visibility = View.INVISIBLE
        ffmpegPrgress.visibility = View.INVISIBLE
        uploadProgress.visibility = View.INVISIBLE

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        myRef = FirebaseDatabase.getInstance().reference

        if(checkPermissions()){
            requestMultiplePermissions()
        }

        createLocationCallback()

        chooseVideo.setOnClickListener {
            if(checkPermissions()){
                requestMultiplePermissions()
            }
            else
                chooseVideoOption()
        }
        uploadCon.setOnClickListener {
            uploadProgress.visibility = View.VISIBLE
            requestLocation()


        }
        settings.setOnClickListener {
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
                    callFFMPEG(duration)
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
                    callFFMPEG(duration)
                }
            }

            REQUEST_CHECK_SETTINGS -> {
                when(resultCode) {
                    Activity.RESULT_OK -> {
                        Log.i("Location", "Location settings change agreed")
                        startLocationUpdates()

                    }
                    Activity.RESULT_CANCELED -> {
                        Log.i("Location", "Location settings not changed")
                        requestingLocationUpdates = 0
                        Toast.makeText(this, "Location permission required to identify the garbage location!", Toast.LENGTH_LONG).show()
                    }
                }
            }

            else -> {
                Toast.makeText(this, "Unrecognized request code $requestCode", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQUEST -> {
                if (grantResults.isEmpty()) {
                }
                else {
                    for(i in 0 until (grantResults.size)) {
                        if(grantResults[i] != PackageManager.PERMISSION_GRANTED){
                            Toast.makeText(this@MainActivity, "Please allow all the permissions", Toast.LENGTH_LONG).show()
                            Log.i("Permissions", "$i permission denied")
                        }
                        else {
                            Log.i("Permissions", "$i permission granted")
                        }
                    }
                }
            }
        }
    }




    /* Video options */

    private fun chooseVideoOption() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Video Choice")
            .setCancelable(true)
            .setMessage("Capture a video using your camera or select a video from gallery")
            .setPositiveButton("Capture", DialogInterface.OnClickListener{
                dialog, id -> videoCapture()
            })
            .setNegativeButton("Select", DialogInterface.OnClickListener{
                dialog, id -> videoSelect()
            })
            .create()
            .show()
    }

    private fun videoCapture() {
        val videoIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        if(videoIntent.resolveActivity(packageManager)!=null){
            startActivityForResult(videoIntent, REQUEST_VIDEO_CAPTURE)
        }
    }

    private fun videoSelect(){
        val upIntent = Intent(Intent.ACTION_PICK)
        upIntent.type = "video/*"
        startActivityForResult(upIntent, REQUEST_VIDEO_UPLOAD)
    }




    /* Path generation methods */

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




    /* Permissions and their handling methods */

    private fun requestMultiplePermissions() {
        val readPermission = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.READ_EXTERNAL_STORAGE)
        val writePermission = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val fineLocationPermission = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_FINE_LOCATION)
        val courseLocationPermission = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_COARSE_LOCATION)

        if(readPermission!=PackageManager.PERMISSION_GRANTED || writePermission!=PackageManager.PERMISSION_GRANTED || fineLocationPermission!=PackageManager.PERMISSION_GRANTED || courseLocationPermission!=PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQUEST)
        }
    }

    private fun checkPermissions(): Boolean{
        val readPermission = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.READ_EXTERNAL_STORAGE)
        val writePermission = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val fineLocationPermission = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_FINE_LOCATION)
        val courseLocationPermission = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_COARSE_LOCATION)
        return (readPermission!=PackageManager.PERMISSION_GRANTED || writePermission!=PackageManager.PERMISSION_GRANTED || fineLocationPermission!=PackageManager.PERMISSION_GRANTED || courseLocationPermission!=PackageManager.PERMISSION_GRANTED)

    }




    /* FFMPEG for splitting video to images */

    private fun callFFMPEG(duration: Double) {

        //Toast.makeText(this@MainActivity, "Starting video to images", Toast.LENGTH_SHORT).show()
        videoToImages(filePath, duration)

    }

    private fun videoToImages(inputPath: String?, duration: Double) {
        //val file = File()
        val outputPath = outputDir() + "/test%02d.jpg"
        var fps : Double = (((NO_OF_IMAGES-1)*1000)/ duration)
        val cmd = arrayOf("-i", inputPath, "-vf", "fps=$fps", outputPath)
        FFmpeg.getInstance(this).execute(cmd, object: ExecuteBinaryResponseHandler(){
            override fun onStart() {
                ffmpegPrgress.visibility = View.VISIBLE
            }
            override fun onProgress(message: String?) {
               // Log.i("FFMPEG Progress", message)
            }
            override fun onSuccess(message: String?) {
                Toast.makeText(this@MainActivity, "Please upload the files", Toast.LENGTH_LONG).show()
                uploadCon.visibility = View.VISIBLE
            }
            override fun onFailure(message: String?) {
                Toast.makeText(this@MainActivity, "Video to images failed", Toast.LENGTH_LONG).show()
            }
            override fun onFinish() {
                ffmpegPrgress.visibility = View.INVISIBLE
            }
        })
    }




    /* Location Methods */

    @SuppressLint("MissingPermission")
    private fun requestLocation(){
        mLocationRequestHighAccuracy = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(updateInterval)
            .setFastestInterval(fastestInterval)

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(mLocationRequestHighAccuracy)
        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener { 
            locationSettingsResponse ->
            Log.i("Location settings", "All settings satisfied")
            startLocationUpdates()
        }

        task.addOnFailureListener {
            if(it is ResolvableApiException){
                requestingLocationUpdates = 0
                try {
                    it.startResolutionForResult(this@MainActivity, REQUEST_CHECK_SETTINGS)
                }
                catch (sendEx: IntentSender.SendIntentException){
                    Log.i("Location Settings", "Error in changing settings")
                }
            }
        }
    }

    private fun createLocationCallback() {
        locationCallback = object: LocationCallback() {
            override fun onLocationResult(location: LocationResult?) {
                super.onLocationResult(location)
                finalLocation = location!!.lastLocation
                if(finalLocation!=null){
                    //Toast.makeText(this@MainActivity, finalLocation!!.latitude.toString(), Toast.LENGTH_LONG).show()
                    uploadImages()
                    stopLocationUpdates()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (requestingLocationUpdates == 1) startLocationUpdates()
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {}
        else
            fusedLocationClient.requestLocationUpdates(mLocationRequestHighAccuracy, locationCallback, Looper.myLooper())
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun locationPath(): String {
        if(finalLocation!=null)
             return ("Lat" + finalLocation!!.latitude.toString() + "Lon" + finalLocation!!.longitude.toString())
        else
             return "NoLocation"
    }




    private var imagesLeft = NO_OF_IMAGES

    private fun uploadImages() {
        while (finalLocation==null);
        var fileString: String

        for(i in 1 .. NO_OF_IMAGES){
            if (i<10){
                fileString = outputDir() + "/test0" + i + ".jpg"
                uploadImageTask(fileString)
            }
            else {
                fileString = outputDir() + "/test" + i + ".jpg"
                uploadImageTask(fileString)
            }

        }
        myRef.child("directory").child("locationPath").setValue(locationPath())
    }

    private fun uploadImageTask(fileString: String) {
        val mStorage = FirebaseStorage.getInstance()
        val storageRef = mStorage.reference
        val storageLocation = storageRef.child(locationPath())
        val fileUri = Uri.fromFile(File(fileString))
        val fileRef = storageLocation.child(fileUri.lastPathSegment)
        val metadata = StorageMetadata.Builder()
            .setContentType("image/jpg")
            .build()
        val uploadTask = fileRef.putFile(fileUri, metadata)

        uploadTask.addOnFailureListener {
            Log.i("Image Upload Failure", fileUri.lastPathSegment)
        }
        uploadTask.addOnSuccessListener {
            Log.i("Image Upload Success", fileUri.lastPathSegment)
            imagesLeft--
            imgCnt.text = "$imagesLeft left of $NO_OF_IMAGES"
            if(imagesLeft == 0){
                imgCnt.text = "Upload Completed Successfully"
                uploadProgress.visibility = View.INVISIBLE
            }

        }
        uploadTask.addOnProgressListener {
            val progress = (100.0 * it.bytesTransferred) / it.totalByteCount
            uploadProgress.progress = progress.toInt()
        }
    }
}
