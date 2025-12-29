package com.example.authentication

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.authentication.MedTrackApp
import com.example.authentication.databinding.ActivitySignupBinding
import com.example.authentication.session.UserSessionManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch


class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        binding.signupBtn.setOnClickListener{
          val email = binding.signupEmail.text.toString().trim()
          val password = binding.signupPassword.text.toString()
          val confirmpass = binding.signupConfirm.text.toString()

          val validationMessage = when {
            email.isEmpty() -> {
              binding.signupEmail.error = "Email required"
              "Please provide your email."
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
              binding.signupEmail.error = "Invalid email"
              "Enter a valid email address."
            }
            password.isEmpty() -> {
              binding.signupPassword.error = "Password required"
              "Please create a password."
            }
            password.length < 6 -> {
              binding.signupPassword.error = "Too short"
              "Password must be at least 6 characters."
            }
            confirmpass.isEmpty() -> {
              binding.signupConfirm.error = "Confirm password"
              "Please confirm your password."
            }
            password != confirmpass -> {
              binding.signupConfirm.error = "Mismatch"
              "Passwords do not match."
            }
            else -> null
          }

          if(validationMessage != null) {
            Toast.makeText(this, validationMessage, Toast.LENGTH_SHORT).show()
            return@setOnClickListener
          }

          binding.signupEmail.error = null
          binding.signupPassword.error = null
          binding.signupConfirm.error = null

          val role = if (binding.radioCaretaker.isChecked) "caretaker" else "patient"

          firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
              if (task.isSuccessful) {
                  val userId = firebaseAuth.currentUser?.uid
                  if (userId != null) {
                      val app = application as MedTrackApp
                      lifecycleScope.launch {
                          app.userRepository.upsertUser(userId, email, role)
                          if (role == "patient") {
                              UserSessionManager.setActivePatient(this@SignupActivity, userId)
                          } else {
                              UserSessionManager.clearActivePatient(this@SignupActivity)
                          }
                      }
                      Toast.makeText(this, "Account created as $role! Please log in.", Toast.LENGTH_SHORT).show()
                      val intent = Intent(this, LoginActivity::class.java)
                      startActivity(intent)
                      finish()
                  }
              } else {
                  val errorMessage = task.exception?.localizedMessage ?: "Signup failed. Please try again."
                  Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
              }
          }
        }
      binding.loginRedirectText.setOnClickListener {
        val loginIntent = Intent(this, LoginActivity::class.java)
        startActivity(loginIntent)
      }
    }
}
