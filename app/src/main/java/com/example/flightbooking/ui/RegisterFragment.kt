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
import com.example.flightbooking.data.AppDatabase
import com.example.flightbooking.data.entity.User
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class RegisterFragment : Fragment() {
    private lateinit var viewModel: FlightViewModel
    private lateinit var nameInput: TextInputEditText
    private lateinit var emailInput: TextInputEditText
    private lateinit var phoneInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var confirmPasswordInput: TextInputEditText
    private lateinit var errorText: android.widget.TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[FlightViewModel::class.java]
        
        nameInput = view.findViewById(R.id.name_input)
        emailInput = view.findViewById(R.id.email_input)
        phoneInput = view.findViewById(R.id.phone_input)
        passwordInput = view.findViewById(R.id.password_input)
        confirmPasswordInput = view.findViewById(R.id.confirm_password_input)
        val registerButton = view.findViewById<android.widget.Button>(R.id.register_button)
        errorText = view.findViewById(R.id.error_text)
        
        // Наблюдаем за текущим пользователем для автоматического перехода после регистрации
        viewModel.currentUser.observe(viewLifecycleOwner) { user ->
            if (user != null && user.id > 0) {
                // Пользователь авторизован - переходим на экран поиска
                Toast.makeText(context, "Добро пожаловать, ${user.name}!", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_register_to_search)
            }
        }

        registerButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val phone = phoneInput.text.toString().trim()
            val password = passwordInput.text.toString()
            val confirmPassword = confirmPasswordInput.text.toString()

            if (validateInput(name, email, phone, password, confirmPassword)) {
                lifecycleScope.launch {
                    registerUser(name, email, phone, password)
                }
            }
        }
    }

    private fun validateInput(
        name: String,
        email: String,
        phone: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        return when {
            name.isEmpty() -> {
                showError("Введите имя")
                false
            }
            email.isEmpty() -> {
                showError("Введите email")
                false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                showError("Введите корректный email")
                false
            }
            phone.isEmpty() -> {
                showError("Введите телефон")
                false
            }
            password.isEmpty() -> {
                showError("Введите пароль")
                false
            }
            password.length < 6 -> {
                showError("Пароль должен быть не менее 6 символов")
                false
            }
            password != confirmPassword -> {
                showError("Пароли не совпадают")
                false
            }
            else -> {
                errorText.visibility = View.GONE
                true
            }
        }
    }

    private fun showError(message: String) {
        errorText.text = message
        errorText.visibility = View.VISIBLE
    }

    private suspend fun registerUser(name: String, email: String, phone: String, password: String) {
        val database = AppDatabase.getDatabase(requireContext())
        val userDao = database.userDao()

        // Проверяем, существует ли пользователь
        if (userDao.emailExists(email)) {
            showError("Пользователь с таким email уже существует")
            return
        }

        // Создаем нового пользователя
        val user = User.create(name, email, phone, password)
        val userId = userDao.insert(user)

        if (userId > 0) {
            // Получаем созданного пользователя из базы
            val createdUser = userDao.getUserSuspend(userId.toInt())
            if (createdUser != null) {
                // Автоматически авторизуем пользователя
                viewModel.setCurrentUser(createdUser)
                Toast.makeText(context, "Регистрация успешна!", Toast.LENGTH_SHORT).show()
                // Переход на экран поиска произойдет через observer currentUser
            } else {
                Toast.makeText(context, "Регистрация успешна! Войдите в систему", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
        } else {
            showError("Ошибка при регистрации")
        }
    }

    override fun onResume() {
        super.onResume()
        // Очищаем поля при возврате на экран
        nameInput.text?.clear()
        emailInput.text?.clear()
        phoneInput.text?.clear()
        passwordInput.text?.clear()
        confirmPasswordInput.text?.clear()
        errorText.visibility = View.GONE
    }
}