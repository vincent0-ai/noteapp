package com.example.echowithin.data.local

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Helper for biometric (fingerprint/face) authentication.
 * Falls back to device credential (PIN/pattern/password) when biometric is unavailable.
 */
object BiometricHelper {

    /** Returns true if the device supports biometric or device-credential authentication. */
    fun canAuthenticate(context: Context): Boolean {
        val bm = BiometricManager.from(context)
        val result = bm.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        return result == BiometricManager.BIOMETRIC_SUCCESS
    }

    /** Returns true if the device has biometric hardware enrolled (fingerprint/face). */
    fun hasBiometricHardware(context: Context): Boolean {
        val bm = BiometricManager.from(context)
        val result = bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        return result == BiometricManager.BIOMETRIC_SUCCESS ||
               result == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED
    }

    /**
     * Shows the biometric prompt and calls [onSuccess] or [onError].
     * Must be called from a FragmentActivity (not a plain ComponentActivity).
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String = "Unlock Echo Within",
        subtitle: String = "Use your fingerprint or device credential",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                // User cancelled is not really an error
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                    errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                    errorCode != BiometricPrompt.ERROR_CANCELED
                ) {
                    onError(errString.toString())
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // Individual attempt failed — prompt stays open for retry
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        prompt.authenticate(promptInfo)
    }
}
