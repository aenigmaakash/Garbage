package org.alphago.garbage.co

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_sign_in.*
import com.google.firebase.auth.FirebaseAuth
import android.util.Log
import android.content.Intent


class SignIn : AppCompatActivity() {

    private val mAuth = FirebaseAuth.getInstance()
    private val TAG = "Error:"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        progressBar.visibility = View.GONE
        signUpbtn.setOnClickListener {
            val emailId = email.text.toString().trim()
            val password = password.text.toString().trim()

            if(TextUtils.isEmpty(emailId)||TextUtils.isEmpty(password)){
                Toast.makeText(this, "Please enter valid info", Toast.LENGTH_LONG).show()
            }
            else {
                progressBar.visibility = View.VISIBLE
                mAuth.createUserWithEmailAndPassword(emailId, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            progressBar.visibility = View.INVISIBLE
                            signInFun(emailId, password)  // SignIn automatically after signUp
                        }
                        else {
                            Log.i("SignUp:Error", task.exception.toString())
                            Toast.makeText(this, task.exception.toString().substring(task.exception.toString().lastIndexOf(":")+1), Toast.LENGTH_LONG).show()
                            progressBar.visibility = View.INVISIBLE
                        }
                    }
            }
        }

        signInbtn.setOnClickListener {
            signInFun(email.text.toString().trim(),password.text.toString().trim())
        }
    }


    // Method to sign In
    private fun signInFun(emailId: String, password: String) {
        if (TextUtils.isEmpty(emailId) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please enter valid info", Toast.LENGTH_LONG).show()
        }
        else {
            progressBar.visibility = View.VISIBLE
            mAuth.signInWithEmailAndPassword(emailId, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = mAuth.currentUser
                        val intent = Intent(this, MainActivity::class.java)
                        progressBar.visibility = View.INVISIBLE
                        startActivity(intent)
                        finish()
                    }
                    else {
                        // If sign in fails, display a message to the user.
                        Log.i("SignIn:Error", task.exception.toString())
                        Toast.makeText(this, task.exception.toString().substring(task.exception.toString().lastIndexOf(":")+1), Toast.LENGTH_SHORT).show()
                        progressBar.visibility = View.INVISIBLE
                    }
                }
        }
    }
}
