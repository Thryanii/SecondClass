package com.thryan.secondclass.core.result

import kotlinx.serialization.Serializable

@Serializable
data class SignInfo(val id: String, val signInTime: String = "", val signOutTime: String = "")

