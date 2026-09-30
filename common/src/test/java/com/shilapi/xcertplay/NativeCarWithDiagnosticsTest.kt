package com.shilapi.xcertplay

import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28, 31, 33], manifest = Config.NONE)
class NativeCarWithDiagnosticsTest {
    private val context get() = RuntimeEnvironment.getApplication()

    @Test fun reportRunsOnAndroid9WithoutRequestingPermissionsOrClaimingAConnection() {
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_USB_HOST, false)
        val readOnly = object : ContextWrapper(context) {
            override fun checkSelfPermission(permission: String) = PackageManager.PERMISSION_DENIED
            override fun getSystemService(name: String): Any? {
                if (name == Context.USB_SERVICE) error("No USB probe needed without host feature")
                return super.getSystemService(name)
            }
        }
        val report = NativeCarWithDiagnostics.report(readOnly)
        assertTrue(report.contains("backend not integrated"))
        assertTrue(report.contains("first frame unverified"))
        assertTrue(report.contains("HOST_FEATURE_NOT_REPORTED"))
        val expected = when {
            Build.VERSION.SDK_INT >= 33 -> "NEARBY_WIFI_DEVICES"
            else -> "ACCESS_FINE_LOCATION"
        }
        assertTrue(report.contains(expected))
        assertEquals(Build.VERSION.SDK_INT >= 31, report.contains("BLUETOOTH_ADVERTISE"))
    }

    @Test fun deniedVendorUsbServiceBecomesAnUnknownDiagnosticInsteadOfCrashing() {
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_USB_HOST, true)
        val denied = object : ContextWrapper(context) {
            override fun getSystemService(name: String): Any? {
                if (name == Context.USB_SERVICE) throw SecurityException("secret-device-identifier")
                return super.getSystemService(name)
            }
        }
        val report = NativeCarWithDiagnostics.report(denied)
        assertTrue(report.contains("QUERY_UNAVAILABLE"))
        assertFalse(report.contains("secret-device-identifier"))
    }

    @Test fun missingVendorUsbServiceIsSeparateFromMissingHardwareFeature() {
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_USB_HOST, true)
        val missing = object : ContextWrapper(context) {
            override fun getSystemService(name: String): Any? =
                if (name == Context.USB_SERVICE) null else super.getSystemService(name)
        }
        assertTrue(NativeCarWithDiagnostics.report(missing).contains("SERVICE_UNAVAILABLE"))
    }
}
