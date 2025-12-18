package com.example.flightbooking

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.example.flightbooking.ui.FlightViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: FlightViewModel
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var navController: androidx.navigation.NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this)[FlightViewModel::class.java]

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        
        // Отключаем нижнюю панель по умолчанию
        bottomNav.visibility = android.view.View.GONE

        // Слушаем изменения пользователя
        viewModel.currentUser.observe(this) { user ->
            if (user != null && user.id > 0) {
                // Пользователь авторизован - показываем и настраиваем нижнюю панель
                bottomNav.visibility = android.view.View.VISIBLE
                bottomNav.setupWithNavController(navController)
            } else {
                // Пользователь не авторизован - скрываем нижнюю панель
                bottomNav.visibility = android.view.View.GONE
            }
        }

        // Предотвращаем навигацию к фрагментам без авторизации
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val isLoginOrRegister = destination.id == R.id.loginFragment || destination.id == R.id.registerFragment
            if (!isUserLoggedIn() && !isLoginOrRegister) {
                // Если пользователь не авторизован и пытается перейти не на login/register
                // Проверяем, что мы не уже на loginFragment, чтобы избежать бесконечного цикла
                if (destination.id != R.id.loginFragment) {
                    val navOptions = NavOptions.Builder()
                        .setPopUpTo(R.id.loginFragment, true)
                        .build()
                    navController.navigate(R.id.loginFragment, null, navOptions)
                }
            }
        }

        // Блокируем клики на элементы навигации, если пользователь не авторизован
        bottomNav.setOnItemSelectedListener { item ->
            if (!isUserLoggedIn()) {
                // Блокируем навигацию, если пользователь не авторизован
                return@setOnItemSelectedListener false
            }
            // Используем стандартную обработку навигации
            NavigationUI.onNavDestinationSelected(item, navController)
            true
        }
    }

    private fun isUserLoggedIn(): Boolean {
        val user = viewModel.currentUser.value
        return user != null && user.id > 0
    }
}