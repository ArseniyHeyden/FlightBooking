package com.example.flightbooking.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.flightbooking.R
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {
    private lateinit var viewModel: FlightViewModel
    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[FlightViewModel::class.java]

        emailInput = view.findViewById(R.id.email_input)
        passwordInput = view.findViewById(R.id.password_input)
        val loginButton = view.findViewById<android.widget.Button>(R.id.login_button)
        val registerButton = view.findViewById<android.widget.Button>(R.id.register_button)
        val errorText = view.findViewById<android.widget.TextView>(R.id.error_text)

        setupObservers(errorText)
        setupListeners(loginButton, registerButton, errorText)
    }

    private fun setupObservers(errorText: android.widget.TextView) {
        // Наблюдаем за текущим пользователем
        viewModel.currentUser.observe(viewLifecycleOwner) { user ->
            if (user != null && user.id > 0) {
                // Пользователь авторизован - переходим на экран поиска
                Toast.makeText(context, "Добро пожаловать, ${user.name}!", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_login_to_search)
            }
        }

        // Наблюдаем за ошибками логина
        viewModel.loginError.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                errorText.text = it
                errorText.visibility = View.VISIBLE
            }
        }
    }

    private fun setupListeners(
        loginButton: android.widget.Button,
        registerButton: android.widget.Button,
        errorText: android.widget.TextView
    ) {
        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                errorText.text = "Заполните все поля"
                errorText.visibility = View.VISIBLE
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val user = viewModel.authenticateUser(email, password)
                if (user != null) {
                    viewModel.setCurrentUser(user)
                }
            }
        }

        registerButton.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }
    }

    override fun onResume() {
        super.onResume()
        // Очищаем поля при возврате на экран
        emailInput.text?.clear()
        passwordInput.text?.clear()
        viewModel.clearLoginError()
    }
}