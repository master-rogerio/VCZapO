package com.pdm.vczap_o.auth.data

import android.os.Build
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat

class BiometricAuthenticator(
    private val activity: FragmentActivity
) {
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private lateinit var biometricPrompt: BiometricPrompt



    fun isBiometricAvailable(): BiometricAuthStatus {
        return try {
            // VERIFICAR PERMISSÕES PRIMEIRO
            Log.d("BiometricAuthenticator", "=== PERMISSION CHECK ===")

            val hasBiometricPermission = ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.USE_BIOMETRIC
            ) == PackageManager.PERMISSION_GRANTED

            Log.d("BiometricAuthenticator", "USE_BIOMETRIC permission granted: $hasBiometricPermission")

            if (!hasBiometricPermission) {
                Log.e("BiometricAuthenticator", "❌ USE_BIOMETRIC permission not granted")
                return BiometricAuthStatus.NOT_AVAILABLE
            }


            val manager = BiometricManager.from(activity)

            // VERIFICAÇÃO DETALHADA DE HARDWARE
            Log.d("BiometricAuthenticator", "=== BIOMETRIC HARDWARE CHECK ===")

            // Verificar se tem hardware biométrico
            val hasHardware = manager.canAuthenticate(BIOMETRIC_STRONG)
            Log.d("BiometricAuthenticator", "Has BIOMETRIC_STRONG hardware: $hasHardware")

            // Verificar se tem credenciais do dispositivo
            val hasDeviceCredentials = manager.canAuthenticate(DEVICE_CREDENTIAL)
            Log.d("BiometricAuthenticator", "Has DEVICE_CREDENTIAL: $hasDeviceCredentials")

            // Verificar combinação
            val combinedCheck = manager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            Log.d("BiometricAuthenticator", "Combined check result: $combinedCheck")

            when (combinedCheck) {
                BiometricManager.BIOMETRIC_SUCCESS -> {
                    Log.d("BiometricAuthenticator", "✅ BIOMETRIC_SUCCESS - Biometric is ready")
                    BiometricAuthStatus.READY
                }
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                    Log.e("BiometricAuthenticator", "❌ BIOMETRIC_ERROR_NO_HARDWARE - No biometric hardware")
                    BiometricAuthStatus.NOT_AVAILABLE
                }
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                    Log.w("BiometricAuthenticator", "⚠️ BIOMETRIC_ERROR_HW_UNAVAILABLE - Hardware temporarily unavailable")
                    BiometricAuthStatus.TEMPORARILY_UNAVAILABLE
                }
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                    Log.w("BiometricAuthenticator", "⚠️ BIOMETRIC_ERROR_NONE_ENROLLED - No biometric enrolled")
                    BiometricAuthStatus.AVAILABLE_BUT_NOT_ENROLLED
                }
                else -> {
                    // CORREÇÃO: Mapear códigos específicos que podem não estar nas constantes
                    when (combinedCheck) {
                        0 -> {
                            Log.d("BiometricAuthenticator", "✅ Code 0 - BIOMETRIC_SUCCESS")
                            BiometricAuthStatus.READY
                        }

                        -1 -> {
                            Log.e(
                                "BiometricAuthenticator",
                                "❌ Code -1 - BIOMETRIC_ERROR_NO_HARDWARE"
                            )
                            BiometricAuthStatus.NOT_AVAILABLE
                        }

                        -2 -> {
                            Log.w(
                                "BiometricAuthenticator",
                                "⚠️ Code -2 - BIOMETRIC_ERROR_NONE_ENROLLED"
                            )
                            BiometricAuthStatus.AVAILABLE_BUT_NOT_ENROLLED
                        }

                        -3 -> {
                            Log.w(
                                "BiometricAuthenticator",
                                "⚠️ Code -3 - BIOMETRIC_ERROR_HW_UNAVAILABLE"
                            )
                            BiometricAuthStatus.TEMPORARILY_UNAVAILABLE
                        }

                        else -> {
                            Log.e(
                                "BiometricAuthenticator",
                                "❓ Unknown biometric status code: $combinedCheck"
                            )
                            BiometricAuthStatus.NOT_AVAILABLE
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("BiometricAuthenticator", "💥 Error checking biometric availability: ${e.message}", e)
            BiometricAuthStatus.NOT_AVAILABLE
        }
    }

    fun promptBiometricAuth(
        title: String,
        subtitle: String,
        negativeButtonText: String,
        onSuccess: () -> Unit,
        onError: (errorCode: Int, errString: CharSequence) -> Unit,
        onFailed: () -> Unit
    ) {
        try {
            val executor = ContextCompat.getMainExecutor(activity)
            biometricPrompt = BiometricPrompt(activity, executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        onSuccess()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        onError(errorCode, errString)
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        onFailed()
                    }
                }
            )

            promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButtonText(negativeButtonText)
                .build()

            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            // Log do erro para debug
            Log.e("BiometricAuthenticator", "Error creating biometric prompt: ${e.message}")
            onError(-1, "Erro ao criar prompt biométrico: ${e.message}")
        }
    }
}

enum class BiometricAuthStatus {
    READY,
    NOT_AVAILABLE,
    TEMPORARILY_UNAVAILABLE,
    AVAILABLE_BUT_NOT_ENROLLED
}