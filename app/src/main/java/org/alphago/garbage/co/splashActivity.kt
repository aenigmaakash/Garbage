package org.alphago.garbage.co

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth

class splashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        var auth: FirebaseAuth
        auth = FirebaseAuth.getInstance()

        if (auth.currentUser!=null){
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        else {
            val intent = Intent(this, signIn::class.java)
            startActivity(intent)
        }
    }
}
