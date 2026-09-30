package com.shilapi.xcertplay.carwith

import org.junit.Assert.*
import org.junit.Test

class NativeCarWithPreflightTest {
    private val data = NativeCarWithPreflight.UsbInterface(255, 255, 0, true, true)
    private fun device(pid: Int = 0x2d00, permitted: Boolean = true,
        interfaces: List<NativeCarWithPreflight.UsbInterface> = listOf(data), vid: Int = 0x18d1) =
        NativeCarWithPreflight.UsbDevice(vid, pid, permitted, interfaces)
    private fun assess(devices: List<NativeCarWithPreflight.UsbDevice>) =
        NativeCarWithPreflight.assessUsb(28, true, devices)

    @Test fun android9UsesLegacyDiscoveryPermissions() {
        val p = NativeCarWithPreflight.discoveryPermissions(28)
        assertTrue(p.contains("android.permission.BLUETOOTH_ADMIN"))
        assertTrue(p.contains("android.permission.ACCESS_FINE_LOCATION"))
        assertFalse(p.contains("android.permission.BLUETOOTH_SCAN"))
        assertEquals(p, NativeCarWithPreflight.discoveryPermissions(30))
    }

    @Test fun android12SeparatesBluetoothFromWifiLocation() {
        val p = NativeCarWithPreflight.discoveryPermissions(31)
        assertTrue(p.containsAll(listOf("android.permission.BLUETOOTH_SCAN",
            "android.permission.BLUETOOTH_ADVERTISE", "android.permission.BLUETOOTH_CONNECT")))
        assertTrue(p.contains("android.permission.ACCESS_FINE_LOCATION"))
        assertFalse(p.contains("android.permission.BLUETOOTH_ADMIN"))
        assertFalse(p.contains("android.permission.NEARBY_WIFI_DEVICES"))
        assertEquals(p, NativeCarWithPreflight.discoveryPermissions(32))
    }

    @Test fun android13UsesNearbyWifiWithoutRequiringLocation() {
        val p = NativeCarWithPreflight.discoveryPermissions(33)
        assertTrue(p.contains("android.permission.NEARBY_WIFI_DEVICES"))
        assertFalse(p.contains("android.permission.ACCESS_FINE_LOCATION"))
    }

    @Test fun lowerAndroidIsAnExplicitUnsupportedResult() {
        assertTrue(NativeCarWithPreflight.discoveryPermissions(27).isEmpty())
        assertEquals(NativeCarWithPreflight.UsbState.ANDROID_VERSION_UNSUPPORTED,
            NativeCarWithPreflight.assessUsb(27, true, listOf(device())).state)
    }

    @Test fun missingHostServiceAndDeniedQueryAreDistinct() {
        assertEquals(NativeCarWithPreflight.UsbState.HOST_FEATURE_NOT_REPORTED,
            NativeCarWithPreflight.assessUsb(28, false, null).state)
        assertEquals(NativeCarWithPreflight.UsbState.SERVICE_UNAVAILABLE,
            NativeCarWithPreflight.assessUsb(28, true, null).state)
        assertEquals(NativeCarWithPreflight.UsbState.QUERY_UNAVAILABLE,
            NativeCarWithPreflight.assessUsb(28, true, null, true).state)
        assertEquals(NativeCarWithPreflight.UsbState.WAITING_FOR_DEVICE, assess(emptyList()).state)
    }

    @Test fun ordinaryUsbAndAoaAudioOnlyAreNotDataCandidates() {
        for (d in listOf(device(vid = 0x1234), device(pid = 0x2d02), device(pid = 0x2d03))) {
            assertEquals(NativeCarWithPreflight.UsbState.AOA_MODE_NOT_OBSERVED, assess(listOf(d)).state)
        }
    }

    @Test fun adbPairAndSplitInterfaceEndpointsAreNotAccessoryData() {
        val adb = NativeCarWithPreflight.UsbInterface(255, 66, 1, true, true)
        val split = listOf(data.copy(bulkOut = false), data.copy(bulkIn = false))
        for (interfaces in listOf(listOf(adb), split, emptyList())) {
            assertEquals(NativeCarWithPreflight.UsbState.AOA_DATA_ENDPOINTS_MISSING,
                assess(listOf(device(interfaces = interfaces))).state)
        }
    }

    @Test fun usbPermissionIsSeparateFromWirelessPermissionAndSessionSuccess() {
        assertEquals(NativeCarWithPreflight.UsbState.DEVICE_PERMISSION_REQUIRED,
            assess(listOf(device(permitted = false))).state)
        for (pid in listOf(0x2d00, 0x2d01, 0x2d04, 0x2d05)) {
            assertEquals(NativeCarWithPreflight.UsbState.AOA_DATA_INTERFACE_PRESENT,
                assess(listOf(device(pid = pid))).state)
        }
    }

    @Test fun authorizedCandidateWinsWithoutMistakingOtherDevicesForCarWith() {
        val result = assess(listOf(device(vid = 0x1234), device(permitted = false), device()))
        assertEquals(NativeCarWithPreflight.UsbState.AOA_DATA_INTERFACE_PRESENT, result.state)
        assertEquals(3, result.attachedDevices)
        assertEquals(2, result.aoaDataDevices)
        assertEquals(1, result.authorizedDataDevices)
    }
}
