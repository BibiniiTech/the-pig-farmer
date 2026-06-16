package com.example.smartswine.utils

import android.content.Context
import android.os.Build
import android.provider.Settings
import java.io.File
import android.content.pm.PackageManager
import android.content.pm.Signature as AndroidSignature
import java.security.MessageDigest
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import android.util.Base64
import android.util.Log
import java.util.jar.JarFile
import com.bibiniitech.smartswine.BuildConfig

object SecurityUtils {

    const val EXPECTED_SIGNATURE_HASH = "91:0F:09:2B:49:05:F3:FF:11:6E:B2:78:53:61:E1:DB:C6:9A:CC:E3:04:EB:65:81:B5:92:B4:EA:8A:65:65:25"

    /**
     * Checks if the device is likely rooted.
     */
    fun isDeviceRooted(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su"
        )
        for (path in paths) {
            try { if (File(path).exists()) return true } catch (e: Exception) { }
        }

        var process: Process? = null
        try {
            process = Runtime.getRuntime().exec(arrayOf("which", "su"))
            val reader = process.inputStream.bufferedReader()
            if (reader.readLine() != null) return true
        } catch (t: Throwable) {
        } finally {
            process?.destroy()
        }
        return false
    }

    /**
     * Checks if the app is running on an emulator.
     */
    fun isEmulator(): Boolean {
        val buildModel = Build.MODEL ?: ""
        return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || buildModel.contains("google_sdk")
                || buildModel.contains("Emulator")
                || buildModel.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.PRODUCT.contains("sdk_google")
                || Build.PRODUCT.contains("google_sdk")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("sdk_x86")
                || Build.PRODUCT.contains("vbox86p")
                || Build.PRODUCT.contains("emulator")
                || Build.PRODUCT.contains("simulator")
    }

    /**
     * Checks if Xposed hooking framework is active.
     * Modified to avoid false positives on OEM (Tecno/Infinix) system hooks.
     */
    fun isXposedActive(): Boolean {
        try {
            // Check for the Xposed bridge class specifically
            Class.forName("de.robv.android.xposed.XposedBridge")
            return true
        } catch (e: Exception) {
            // Not found
        }
        
        try {
            throw Exception("security_check")
        } catch (e: Exception) {
            for (element in e.stackTrace) {
                // Only flag if it's the actual Xposed or Substrate bridge
                val className = element.className
                if (className == "de.robv.android.xposed.XposedBridge" || 
                    className == "com.saurik.substrate.MS$2") {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Checks for known patching tools.
     */
    fun isLuckyPatcherInstalled(context: Context): Boolean {
        val patcherPackages = arrayOf(
            "com.dimonvideo.luckypatcher",
            "com.chelpus.lackypatch",
            "org.chelpus.luckypatcher",
            "com.chelpus.luckypatcher",
            "com.gameguardian.co.uk",
            "org.sbtools.gamehack"
        )
        val pm = context.packageManager
        for (pkg in patcherPackages) {
            try {
                pm.getPackageInfo(pkg, 0)
                return true
            } catch (e: PackageManager.NameNotFoundException) {
            }
        }
        return false
    }

    /**
     * Verifies if the app was installed from the Google Play Store.
     */
    fun verifyInstaller(context: Context): Boolean {
        if (BuildConfig.DEBUG) return true
        val installer = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getInstallerPackageName(context.packageName)
            }
        } catch (e: Exception) { null }
        return installer == "com.android.vending" || installer == "com.google.android.feedback"
    }

    /**
     * Secondary direct check. Now returns true if uncertain to prevent 
     * false positives on Play Store App Bundle delivery.
     */
    fun verifyApkSignatureDirect(context: Context, expectedSha256: String): Boolean {
        if (BuildConfig.DEBUG || expectedSha256.isBlank()) return true
        try {
            val apkPath = context.packageCodePath
            val jarFile = JarFile(apkPath, true)
            val je = jarFile.getJarEntry("AndroidManifest.xml") ?: return true
            
            val readBuffer = ByteArray(8192)
            jarFile.getInputStream(je).use { isStream ->
                while (isStream.read(readBuffer) != -1) { }
            }
            
            val certs = je.certificates
            if (certs != null && certs.isNotEmpty()) {
                val md = MessageDigest.getInstance("SHA-256")
                val currentSha256 = md.digest(certs[0].encoded).joinToString(":") { String.format("%02X", it) }
                return currentSha256.equals(expectedSha256, ignoreCase = true)
            }
        } catch (e: Exception) {
            Log.w("SecurityUtils", "Direct check skipped (Bundle/V2 signing): ${e.message}")
        }
        return true // Fallback to API check
    }

    /**
     * Main verification logic.
     */
    fun verifyAppSignature(context: Context, expectedSha256: String): Boolean {
        if (BuildConfig.DEBUG || expectedSha256.isBlank()) return true
        
        var apiValid = false
        try {
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

            val actualHashes = signatures?.map { getSHA256(it) } ?: emptyList()
            if (actualHashes.any { it.equals(expectedSha256, ignoreCase = true) }) {
                apiValid = true
            } else {
                Log.e("SecurityUtils", "Hash mismatch! Expected: $expectedSha256, Found: ${actualHashes.joinToString(", ")}")
            }
        } catch (e: Exception) {
            Log.e("SecurityUtils", "API check error: ${e.message}")
        }
        
        return apiValid && verifyApkSignatureDirect(context, expectedSha256)
    }

    private fun getSHA256(signature: AndroidSignature): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(signature.toByteArray()).joinToString(":") { String.format("%02X", it) }
    }

    fun verifyPurchase(base64PublicKey: String, signedData: String, signature: String): Boolean {
        if (signedData.isEmpty() || base64PublicKey.isEmpty() || signature.isEmpty()) return false
        return try {
            val key = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(Base64.decode(base64PublicKey, Base64.DEFAULT)))
            val sig = Signature.getInstance("SHA1withRSA")
            sig.initVerify(key)
            sig.update(signedData.toByteArray())
            sig.verify(Base64.decode(signature, Base64.DEFAULT))
        } catch (e: Exception) { false }
    }
}
