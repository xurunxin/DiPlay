package com.shilapi.xcertplay

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbManager
import android.os.Build
import com.shilapi.xcertplay.carwith.NativeCarWithPreflight

/** No discovery, permission prompts, device opening or AOA mode-switch requests. */
internal object NativeCarWithDiagnostics {
    fun report(context: Context): String {
        val api = Build.VERSION.SDK_INT
        val host = runCatching {
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_USB_HOST)
        }.getOrDefault(false)
        var failed = false
        val devices = if (api >= NativeCarWithPreflight.MIN_API && host) try {
            context.getSystemService(UsbManager::class.java)?.let { manager ->
                manager.deviceList.values.map { device ->
                    val interfaces = (0 until device.interfaceCount).map { index ->
                        val intf = device.getInterface(index)
                        val endpoints = (0 until intf.endpointCount).map(intf::getEndpoint)
                            .filter { it.type == UsbConstants.USB_ENDPOINT_XFER_BULK }
                        NativeCarWithPreflight.UsbInterface(
                            intf.interfaceClass, intf.interfaceSubclass, intf.interfaceProtocol,
                            endpoints.any { it.direction == UsbConstants.USB_DIR_IN },
                            endpoints.any { it.direction == UsbConstants.USB_DIR_OUT },
                        )
                    }
                    NativeCarWithPreflight.UsbDevice(
                        device.vendorId, device.productId, manager.hasPermission(device), interfaces,
                    )
                }
            }
        } catch (_: Exception) {
            failed = true
            null
        } else null
        val usb = NativeCarWithPreflight.assessUsb(api, host, devices, failed)
        val permissions = NativeCarWithPreflight.discoveryPermissions(api)
        val missing = permissions.filter { permission ->
            runCatching { context.checkSelfPermission(permission) }
                .getOrDefault(PackageManager.PERMISSION_DENIED) != PackageManager.PERMISSION_GRANTED
        }.sorted()
        return buildString {
            appendLine("Native CarWith: backend not integrated; discovery/authentication/first frame unverified")
            appendLine("Receiver platform: API=$api minimum=${NativeCarWithPreflight.MIN_API}")
            appendLine("USB preflight: ${usb.state}; attached=${usb.attachedDevices} AOA-data=${usb.aoaDataDevices} authorized=${usb.authorizedDataDevices}")
            appendLine("AOA interface presence does not verify CarWith compatibility or authentication.")
            appendLine("Standard wireless discovery missing permissions: ${missing.joinToString().ifEmpty { "none" }}")
            append("Permissions alone do not verify radio support, reachability or SDK requirements.")
        }
    }
}
