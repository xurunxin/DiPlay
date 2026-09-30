package com.shilapi.xcertplay.carwith

/** Read-only platform checks. An AOA data interface is not an authenticated CarWith session. */
object NativeCarWithPreflight {
    const val MIN_API = 28

    enum class UsbState {
        ANDROID_VERSION_UNSUPPORTED, HOST_FEATURE_NOT_REPORTED, SERVICE_UNAVAILABLE,
        QUERY_UNAVAILABLE, WAITING_FOR_DEVICE, AOA_MODE_NOT_OBSERVED,
        AOA_DATA_ENDPOINTS_MISSING, DEVICE_PERMISSION_REQUIRED, AOA_DATA_INTERFACE_PRESENT,
    }

    data class UsbInterface(
        val deviceClass: Int,
        val subclass: Int,
        val protocol: Int,
        val bulkIn: Boolean,
        val bulkOut: Boolean,
    ) {
        val isAdb: Boolean get() = deviceClass == 255 && subclass == 66 && protocol == 1
        val hasDataPair: Boolean get() = !isAdb && bulkIn && bulkOut
    }

    data class UsbDevice(
        val vendorId: Int,
        val productId: Int,
        val permissionGranted: Boolean,
        val interfaces: List<UsbInterface>,
    ) {
        // AOA audio-only PIDs 0x2d02/0x2d03 do not expose an accessory data channel.
        val isAoaDataDevice: Boolean get() = vendorId == 0x18d1 &&
            productId in setOf(0x2d00, 0x2d01, 0x2d04, 0x2d05)
    }

    data class UsbAssessment(
        val state: UsbState,
        val attachedDevices: Int = 0,
        val aoaDataDevices: Int = 0,
        val authorizedDataDevices: Int = 0,
    )

    /** Standard BLE discovery/advertising plus Wi-Fi Direct permissions, not an SDK contract. */
    fun discoveryPermissions(api: Int): Set<String> {
        if (api < MIN_API) return emptySet()
        val bluetooth = if (api >= 31) setOf(
            "android.permission.BLUETOOTH_SCAN",
            "android.permission.BLUETOOTH_ADVERTISE",
            "android.permission.BLUETOOTH_CONNECT",
        ) else setOf("android.permission.BLUETOOTH", "android.permission.BLUETOOTH_ADMIN")
        val wifiDiscovery = if (api >= 33) "android.permission.NEARBY_WIFI_DEVICES"
            else "android.permission.ACCESS_FINE_LOCATION"
        return bluetooth + wifiDiscovery + setOf(
            "android.permission.ACCESS_WIFI_STATE", "android.permission.CHANGE_WIFI_STATE",
        )
    }

    fun assessUsb(
        api: Int,
        hostFeatureReported: Boolean,
        devices: List<UsbDevice>?,
        queryFailed: Boolean = false,
    ): UsbAssessment {
        if (api < MIN_API) return UsbAssessment(UsbState.ANDROID_VERSION_UNSUPPORTED)
        if (!hostFeatureReported) return UsbAssessment(UsbState.HOST_FEATURE_NOT_REPORTED)
        if (queryFailed) return UsbAssessment(UsbState.QUERY_UNAVAILABLE)
        if (devices == null) return UsbAssessment(UsbState.SERVICE_UNAVAILABLE)
        if (devices.isEmpty()) return UsbAssessment(UsbState.WAITING_FOR_DEVICE)
        val aoa = devices.filter { it.isAoaDataDevice }
        val withData = aoa.filter { device -> device.interfaces.any { it.hasDataPair } }
        val authorized = withData.count { it.permissionGranted }
        val state = when {
            aoa.isEmpty() -> UsbState.AOA_MODE_NOT_OBSERVED
            withData.isEmpty() -> UsbState.AOA_DATA_ENDPOINTS_MISSING
            authorized == 0 -> UsbState.DEVICE_PERMISSION_REQUIRED
            else -> UsbState.AOA_DATA_INTERFACE_PRESENT
        }
        return UsbAssessment(state, devices.size, aoa.size, authorized)
    }
}
