package org.alphago.garbage.co

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.EditText
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_sign_in.*
import org.w3c.dom.Text
import com.google.firebase.auth.FirebaseAuth
//import jdk.nashorn.internal.runtime.ECMAException.getException
import com.google.firebase.auth.FirebaseUser
//import org.junit.experimental.results.ResultMatchers.isSuccessful
import com.google.firebase.auth.AuthResult
import com.google.android.gms.tasks.Task
import android.support.annotation.NonNull
import com.google.android.gms.tasks.OnCompleteListener
import android.R.attr.password
import android.support.v4.app.FragmentActivity
import android.util.Log
import android.R.attr.password
import android.content.Intent


class signIn : AppCompatActivity() {

    val mAuth = FirebaseAuth.getInstance();
    val TAG = "Error:"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)



        val textemail = findViewById<EditText>(R.id.email)
        val textpass = findViewById<EditText>(R.id.password)

        signUpbtn.setOnClickListener {
            var email = textemail.text.toString().trim()
            var pass = textpass.text.toString().trim()

            if(TextUtils.isEmpty(email)||TextUtils.isEmpty(pass)){
                Toast.makeText(this, "Please enter valid info", Toast.LENGTH_LONG).show()
            }
            else {
                progressBar.visibility = View.VISIBLE
                mAuth.createUserWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            // Sign in success, update UI with the signed-in user's information
                            Toast.makeText(
                                this, "Sign Up Successful",
                                Toast.LENGTH_LONG
                            ).show()
                            signInFun(textemail, textpass)


                        } else {
                            // If sign in fails, display a message to the user.

                            Toast.makeText(
                                this, "Authentication failed.",
                                Toast.LENGTH_LONG
                            ).show()

                        }

                        // ...
                    }
                progressBar.visibility = View.INVISIBLE


            }

        }

        signInbtn.setOnClickListener {
            signInFun(textemail,textpass)
        }





    }

    fun signInFun(textEmail: EditText, textPass:EditText) {
        var email = textEmail.text.toString().trim()
        var pass = textPass.text.toString().trim()

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(pass)) {
            Toast.makeText(this, "Please enter valid info", Toast.LENGTH_LONG).show()
        } else {
            progressBar.visibility = View.VISIBLE
            mAuth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Toast.makeText(this, "Sign In Successful", Toast.LENGTH_LONG).show()
                        val user = mAuth.currentUser
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish()

                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "signInWithEmail:failure", task.exception)
                        Toast.makeText(
                            this, "Authentication failed.",
                            Toast.LENGTH_SHORT
                        ).show()

                    }

                    // ...
                }
            progressBar.visibility = View.INVISIBLE

        }
    }
}
