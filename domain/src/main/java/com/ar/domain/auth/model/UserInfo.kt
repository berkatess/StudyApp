package com.ar.domain.auth.model

data class UserInfo(
    val uid: String,
    val email: String?,
    val isAnonymous: Boolean
)
