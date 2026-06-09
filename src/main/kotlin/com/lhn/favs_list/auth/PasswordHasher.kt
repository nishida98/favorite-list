package com.lhn.favs_list.auth

interface PasswordHasher {
    fun hash(rawPassword: String): String

    fun matches(rawPassword: String, passwordHash: String): Boolean
}
