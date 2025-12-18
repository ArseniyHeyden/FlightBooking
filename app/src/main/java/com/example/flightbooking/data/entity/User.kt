package com.example.flightbooking.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.security.MessageDigest

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val email: String,
    val phone: String,
    val passwordHash: String? = null,  // Изменено на nullable
    val passwordSalt: String? = null,  // Изменено на nullable
    val discountLevel: Int = 0,
    val totalTrips: Int = 0,
    val totalSpent: Double = 0.0
) {
    companion object {
        fun create(name: String, email: String, phone: String, password: String): User {
            val salt = generateSalt()
            val hash = hashPassword(password, salt)
            return User(
                name = name,
                email = email,
                phone = phone,
                passwordHash = hash,
                passwordSalt = salt
            )
        }

        private fun generateSalt(): String {
            val random = java.security.SecureRandom()
            val salt = ByteArray(16)
            random.nextBytes(salt)
            return bytesToHex(salt)
        }

        private fun hashPassword(password: String, salt: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val saltedPassword = password + salt
            val hash = digest.digest(saltedPassword.toByteArray(Charsets.UTF_8))
            return bytesToHex(hash)
        }

        fun verifyPassword(password: String, user: User): Boolean {
            val salt = user.passwordSalt ?: return false  // Проверка на null
            val hash = hashPassword(password, salt)
            return hash == user.passwordHash
        }

        private fun bytesToHex(bytes: ByteArray): String {
            val hexChars = CharArray(bytes.size * 2)
            for (i in bytes.indices) {
                val v = bytes[i].toInt() and 0xFF
                hexChars[i * 2] = "0123456789ABCDEF"[v ushr 4]
                hexChars[i * 2 + 1] = "0123456789ABCDEF"[v and 0x0F]
            }
            return String(hexChars)
        }
    }

    fun updatePassword(newPassword: String): User {
        val salt = generateSalt()
        val hash = hashPassword(newPassword, salt)
        return this.copy(passwordHash = hash, passwordSalt = salt)
    }
}