package com.example.auth

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserProfile(
    val name: String,
    val email: String,
    val avatarUrl: String? = null,
    val isSignedIn: Boolean = false
)

class GoogleAuthManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("hasad_auth_prefs", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow(loadUser())
    val currentUser: StateFlow<UserProfile> = _currentUser.asStateFlow()

    private fun loadUser(): UserProfile {
        val isSignedIn = prefs.getBoolean(KEY_IS_SIGNED_IN, false)
        val name = prefs.getString(KEY_NAME, "") ?: ""
        val email = prefs.getString(KEY_EMAIL, "") ?: ""
        val avatar = prefs.getString(KEY_AVATAR, null)
        return UserProfile(
            name = if (name.isBlank()) "مستخدم حصاد" else name,
            email = if (email.isBlank()) "abodledam1@gmail.com" else email,
            avatarUrl = avatar,
            isSignedIn = isSignedIn
        )
    }

    fun signIn(name: String, email: String, avatarUrl: String? = null) {
        prefs.edit()
            .putBoolean(KEY_IS_SIGNED_IN, true)
            .putString(KEY_NAME, name)
            .putString(KEY_EMAIL, email)
            .putString(KEY_AVATAR, avatarUrl)
            .apply()

        _currentUser.value = UserProfile(
            name = name,
            email = email,
            avatarUrl = avatarUrl,
            isSignedIn = true
        )
    }

    fun signOut() {
        prefs.edit()
            .putBoolean(KEY_IS_SIGNED_IN, false)
            .apply()

        _currentUser.value = loadUser().copy(isSignedIn = false)
    }

    companion object {
        private const val KEY_IS_SIGNED_IN = "is_signed_in"
        private const val KEY_NAME = "user_name"
        private const val KEY_EMAIL = "user_email"
        private const val KEY_AVATAR = "user_avatar"
    }
}
