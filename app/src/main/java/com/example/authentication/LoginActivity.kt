package com.example.authentication

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.authentication.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth


class LoginActivity : AppCompatActivity() {
  private lateinit var binding: ActivityLoginBinding
  private lateinit var firebaseAuth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        binding.loginBtn.setOnClickListener{
          val email = binding.loginEmail.text.toString().trim()
          val password = binding.loginPassword.text.toString()

          val validationMessage = when {
            email.isEmpty() -> {
              binding.loginEmail.error = "Email required"
              "Please provide your email."
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
              binding.loginEmail.error = "Invalid email"
              "Enter a valid email address."
            }
            password.isEmpty() -> {
              binding.loginPassword.error = "Password required"
              "Please enter your password."
            }
            password.length < 6 -> {
              binding.loginPassword.error = "Too short"
              "Password must be at least 6 characters."
            }
            else -> null
          }

          if(validationMessage != null) {
            Toast.makeText(this, validationMessage, Toast.LENGTH_SHORT).show()
            return@setOnClickListener
          }

          binding.loginEmail.error = null
          binding.loginPassword.error = null

            firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener {
              if (it.isSuccessful) {
                Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
              } else {
                val errorMessage = it.exception?.localizedMessage ?: "Login failed. Check your credentials and try again."
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
              }
            }
        }
        binding.SignUpRedirectText.setOnClickListener {
          val signupIntent = Intent(this, SignupActivity::class.java)
          startActivity(signupIntent)
        }
    }
}
