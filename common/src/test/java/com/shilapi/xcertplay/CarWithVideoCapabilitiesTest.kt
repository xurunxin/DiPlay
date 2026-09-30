package com.shilapi.xcertplay

import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Build
import com.shilapi.xcertplay.carwith.CarWithVideoPolicy
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.MediaCodecInfoBuilder
import org.robolectric.shadows.MediaCodecInfoBuilder.CodecCapabilitiesBuilder
import org.robolectric.shadows.ShadowMediaCodecList

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28, 31, 33], manifest = Config.NONE)
class CarWithVideoCapabilitiesTest {
    private val baseline = MediaCodecInfo.CodecProfileLevel().apply {
        profile = MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline
        level = MediaCodecInfo.CodecProfileLevel.AVCLevel31
    }

    private fun codec(encoder: Boolean = false): MediaCodecInfo {
        val format = MediaFormat.createVideoFormat(CarWithVideoPolicy.MIME, 1280, 720).apply {
            setString("frame-rate-range", "1-30")
        }
        val caps = CodecCapabilitiesBuilder.newBuilder().setMediaFormat(format)
            .setProfileLevels(arrayOf(baseline))
            .setColorFormats(intArrayOf(MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface))
            .setIsEncoder(encoder).build()
        return MediaCodecInfoBuilder.newBuilder().setName("test.avc.decoder")
            .setIsHardwareAccelerated(true).setIsEncoder(encoder).setCapabilities(caps).build()
    }

    @Test fun actualAndroidCapabilitiesReject1080pAndSelect720p() {
        ShadowMediaCodecList.addCodec(codec())
        val plan = CarWithVideoCapabilities.plan(CarWithVideoPolicy.Limit.FHD1080)
        assertEquals(CarWithVideoPolicy.Evidence.CAPABILITY_CHECKED, plan.evidence)
        assertEquals(CarWithVideoPolicy.Mode(1280, 720, 30), plan.selected)
        assertEquals(listOf(CarWithVideoPolicy.ProfileLevel(baseline.profile, baseline.level)), plan.candidates.first().profileLevels)
        assertEquals(if (Build.VERSION.SDK_INT >= 29) true else null, plan.candidates.first().decoder.hardwareAccelerated)
    }

    @Test fun actualAndroidFormatCheckRejectsUnsupportedSizeRateAndProfile() {
        val info = codec()
        val decoder = CarWithVideoCapabilities.AndroidDecoder(info, info.getCapabilitiesForType(CarWithVideoPolicy.MIME))
        fun supported(width: Int, height: Int, fps: Int, profile: Int = baseline.profile, level: Int = baseline.level) =
            decoder.supports(CarWithVideoPolicy.Stream(CarWithVideoPolicy.Mode(width, height, fps), CarWithVideoPolicy.ProfileLevel(profile, level)))
        assertTrue(supported(1280, 720, 30))
        assertFalse(supported(1920, 1152, 30))
        assertFalse(supported(1280, 720, 60))
        assertFalse(supported(1280, 720, 30, MediaCodecInfo.CodecProfileLevel.AVCProfileHigh))
        assertFalse(supported(-1280, 720, 30))
    }

    @Test fun anEncoderIsNotUsedAsAReceiverDecoder() {
        ShadowMediaCodecList.addCodec(codec(encoder = true))
        assertEquals(CarWithVideoPolicy.Evidence.NO_SUPPORTED_MODE,
            CarWithVideoCapabilities.plan(CarWithVideoPolicy.Limit.HD720).evidence)
    }
}
