package com.thryan.secondclass.ui.login

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.ViewModel

import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.thryan.secondclass.core.Webvpn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("StaticFieldLeak")
class LoginViewModel(private val context: Context, private val navController: NavHostController) :
    ViewModel() {

    private val TAG = "LoginViewModel"

    val sharedPref = context.getSharedPreferences(
        "com.thryan.secondclass.PREFERENCE", Context.MODE_PRIVATE
    )

    private val _uiState = MutableStateFlow(LoginState("", "", "", false, ""))
    val uiState: StateFlow<LoginState> = _uiState.asStateFlow()

    private suspend fun update(uiStates: LoginState) = _uiState.emit(uiStates)

    fun send(intent: LoginIntent) = viewModelScope.launch { onHandle(intent) }

    private suspend fun onHandle(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.Login -> {
                login()
            }

            is LoginIntent.GetPreference -> {
                update(
                    uiState.value.copy(
                        account = sharedPref.getString("account", "")!!,
                        password = sharedPref.getString("password", "")!!
                    )
                )
            }

            is LoginIntent.UpdateAccount -> {
                update(uiState.value.copy(account = intent.account))
            }

            is LoginIntent.UpdatePassword -> {
                update(uiState.value.copy(password = intent.password))
            }

            is LoginIntent.UpdateSCAccount -> {
                update(uiState.value.copy(scAccount = intent.scAccount))
            }
            is LoginIntent.CloseDialog -> {
                update(uiState.value.copy(showDialog = false))
            }
        }
    }


    suspend fun login() {
        val twfid = sharedPref.getString("twfid", "")
        val (account, password, scAccount) = uiState.value
        //检查缓存的twfid是否可用
        if (!twfid.isNullOrEmpty() && Webvpn.checkLogin(twfid)) {
            withContext(Dispatchers.Main) {
                navController.navigate("page?twfid=${twfid}&account=${scAccount.ifEmpty { account }}") {
                    launchSingleTop = true
                }
            }
        } else {
            val response = Webvpn.login(account, password)
            //登录
            if (response.message == "请求成功") {
                with(sharedPref.edit()) {
                    putString("twfid", response.data)
                    putString("account", account)
                    putString("password", password)
                    apply()
                }
                withContext(Dispatchers.Main) {
                    navController.navigate("page?twfid=${response.data}&account=${scAccount.ifEmpty { account }}") {
                        launchSingleTop = true
                    }
                }
            } else {
                update(uiState.value.copy(showDialog = true, message = response.message))
            }
        }
    }

}
