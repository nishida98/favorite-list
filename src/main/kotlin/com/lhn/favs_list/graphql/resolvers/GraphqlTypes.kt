package com.lhn.favs_list.graphql.resolvers

import com.lhn.favs_list.users.UserProfile

data class RegisterUserInput(
    val email: String,
    val name: String,
    val nickname: String,
    val password: String,
)

data class RegisterUserPayload(
    val user: UserProfile,
)

data class LoginInput(
    val email: String,
    val password: String,
)

data class LoginPayload(
    val accessToken: String,
    val tokenType: String,
    val expiresIn: Int,
    val user: UserProfile,
)

data class UpdateMeInput(
    val name: String? = null,
    val nickname: String? = null,
)

data class LogoutPayload(
    val success: Boolean,
)

data class DeleteMePayload(
    val success: Boolean,
)
