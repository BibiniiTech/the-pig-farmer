package com.example.smartswine.data

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.example.smartswine.utils.SecurityUtils
import java.security.MessageDigest

sealed class SecurityStatus {
    object Safe : SecurityStatus()
    data class Violation(val message: String) : SecurityStatus()
    data class Warning(val message: String) : SecurityStatus()
}

class SecurityManager(private val context: Context) {

    fun checkSecurity(): SecurityStatus {
        if (!isSignatureValid()) {
            val actualSignature = getActualSignature()
            return SecurityStatus.Violation(
                "Signature Mismatch.\nExpected: ${SecurityUtils.EXPECTED_SIGNATURE_HASH}\nActual: $actualSignature\n\nPlease ensure you are using the official version."
            )
        }

        if (isHookingFrameworkActive()) {
            return SecurityStatus.Violation("Active hooking framework (Xposed) detected. Please disable it to run SmartSwine.")
        }

        val luckyPatcherInstalled = SecurityUtils.isLuckyPatcherInstalled(context)
        if (luckyPatcherInstalled) {
            return SecurityStatus.Violation("Patching or game hacking tools (like Lucky Patcher) detected. Please uninstall them to run SmartSwine.")
        }

        if (isRooted()) {
            return SecurityStatus.Warning("Device is rooted. Some features may be unstable or insecure.")
        }

        if (!SecurityUtils.verifyInstaller(context)) {
            return SecurityStatus.Warning("App was not installed from an official store.")
        }

        return SecurityStatus.Safe
    }

    fun isRooted(): Boolean = SecurityUtils.isDeviceRooted()

    fun isSignatureValid(): Boolean = SecurityUtils.verifyAppSignature(context, SecurityUtils.EXPECTED_SIGNATURE_HASH)

    fun isHookingFrameworkActive(): Boolean = SecurityUtils.isXposedActive()

    private fun getActualSignature(): String {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
            }
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.signingCertificateHistory
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }
            signatures?.firstOrNull()?.let { sig ->
                val md = MessageDigest.getInstance("SHA-256")
                md.digest(sig.toByteArray()).joinToString(":") { String.format("%02X", it) }
            } ?: "No Signature Found"
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}
