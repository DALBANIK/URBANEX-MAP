package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.repository.MarkerRepository
import com.example.ui.auth.AuthManager
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.MainScreen
import com.example.ui.theme.UexMapTheme
import com.example.ui.viewmodel.MapViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UexMapTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val currentUser by AuthManager.authStateFlow().collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = {
                // AuthStateListener will automatically update currentUser
            }
        )
    } else {
        val databaseId = com.example.R.string.firestore_database_id

        val viewModel: MapViewModel = viewModel(
            key = user.uid,
            factory = viewModelFactory {
                initializer {
                    val app = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                    val dbId = app.getString(databaseId)
                    val db = FirebaseFirestore.getInstance(dbId)
                    val repository = MarkerRepository(db)
                    val shopRepo = com.example.data.repository.UserShopRepository(app, user.uid)
                    MapViewModel(
                        repository = repository,
                        currentUserId = user.uid,
                        currentUserEmail = user.email ?: "",
                        currentUserName = user.displayName ?: user.email ?: "Користувач",
                        userShopRepository = shopRepo
                    )
                }
            }
        )

        MainScreen(
            viewModel = viewModel,
            onSignOut = {
                // AuthStateListener triggers transition to AuthScreen
            }
        )
    }
}
