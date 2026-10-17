package cl.duoc.museo.utils

import android.content.Context
import android.content.SharedPreferences
import cl.duoc.museo.data.model.Role

class SessionManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("museo_session_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USER_ID = "active_user_id"
        private const val KEY_USER_NAME = "active_user_name"
        private const val KEY_USER_EMAIL = "active_user_email"
        private const val KEY_ROLE = "active_user_role"
        private const val KEY_AVATAR = "active_user_avatar"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }

    fun saveUserSession(
        userId: Long,
        name: String,
        email: String,
        role: Role,
        avatar: String = "avatar_1"
    ) {
        prefs.edit()
            .putLong(KEY_USER_ID, userId)
            .putString(KEY_USER_NAME, name)
            .putString(KEY_USER_EMAIL, email)
            .putString(KEY_ROLE, role.name)
            .putString(KEY_AVATAR, avatar)
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .apply()
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    fun getUserId(): Long = prefs.getLong(KEY_USER_ID, 0L)

    fun getUserName(): String = prefs.getString(KEY_USER_NAME, "Invitado") ?: "Invitado"

    fun getUserEmail(): String = prefs.getString(KEY_USER_EMAIL, "usuario@museo.org") ?: "usuario@museo.org"

    fun getUserRole(): Role {
        val roleStr = prefs.getString(KEY_ROLE, Role.CLIENT.name)
        return try {
            Role.valueOf(roleStr ?: Role.CLIENT.name)
        } catch (e: Exception) {
            Role.CLIENT
        }
    }

    fun getUserAvatar(): String = prefs.getString(KEY_AVATAR, "avatar_1") ?: "avatar_1"

    fun getShippingAddress(): String = getUserPreference("shipping_address", "")

    fun saveShippingAddress(address: String) {
        saveUserPreference("shipping_address", address)
    }

    fun getInterests(): Set<String> =
        prefs.getStringSet(userPreferenceKey("interests"), emptySet())?.toSet() ?: emptySet()

    fun saveInterests(interests: Set<String>) {
        prefs.edit().putStringSet(userPreferenceKey("interests"), interests.toSet()).apply()
    }

    fun areNotificationsEnabled(): Boolean = getUserPreference("notifications_enabled", true)

    fun setNotificationsEnabled(enabled: Boolean) {
        saveUserPreference("notifications_enabled", enabled)
    }

    fun isLargeTextEnabled(): Boolean = getUserPreference("large_text_enabled", false)

    fun setLargeTextEnabled(enabled: Boolean) {
        saveUserPreference("large_text_enabled", enabled)
    }

    fun getLanguage(): String = getUserPreference("language", "Español")

    fun setLanguage(language: String) {
        saveUserPreference("language", language)
    }

    fun clearSession() {
        prefs.edit()
            .remove(KEY_USER_ID)
            .remove(KEY_USER_NAME)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_ROLE)
            .remove(KEY_AVATAR)
            .remove(KEY_IS_LOGGED_IN)
            .apply()
    }

    private fun userPreferenceKey(name: String): String = "user_${getUserId()}_$name"

    private fun getUserPreference(key: String, defaultValue: String): String =
        prefs.getString(userPreferenceKey(key), defaultValue) ?: defaultValue

    private fun getUserPreference(key: String, defaultValue: Boolean): Boolean =
        prefs.getBoolean(userPreferenceKey(key), defaultValue)

    private fun saveUserPreference(key: String, value: String) {
        prefs.edit().putString(userPreferenceKey(key), value).apply()
    }

    private fun saveUserPreference(key: String, value: Boolean) {
        prefs.edit().putBoolean(userPreferenceKey(key), value).apply()
    }
}