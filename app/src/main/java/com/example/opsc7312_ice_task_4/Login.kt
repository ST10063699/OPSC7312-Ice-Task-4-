package com.example.opsc7312_ice_task_4

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class Login : AppCompatActivity() {

    private lateinit var loginUsername: EditText
    private lateinit var loginPassword: EditText
    private lateinit var loginBtn: Button
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        loginBtn = findViewById(R.id.LoginBtn)
        loginUsername = findViewById(R.id.LoginUsername)
        loginPassword = findViewById(R.id.LoginPassword)
        firebaseAuth = FirebaseAuth.getInstance()

        loginBtn.setOnClickListener {
            val email = loginUsername.text.toString().trim()
            val password = loginPassword.text.toString().trim()

            firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // User logged in successfully
                        val intent = Intent(this, MapsActivity::class.java)  // Correctly import MapsActivity
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, "Login failed. Please insert correct credentials.", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}
