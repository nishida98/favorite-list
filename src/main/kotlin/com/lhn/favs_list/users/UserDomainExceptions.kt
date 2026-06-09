package com.lhn.favs_list.users

class EmailAlreadyInUseException(
    email: String,
    cause: Throwable? = null,
) : RuntimeException("Email is already in use: $email", cause)
