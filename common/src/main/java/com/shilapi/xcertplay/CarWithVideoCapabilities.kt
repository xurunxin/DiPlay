package com.shilapi.xcertplay

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build
import com.shilapi.xcertplay.carwith.CarWithVideoPolicy

/** Public API28-compatible queries only; no MediaCodec instance or connection is created. */
internal object CarWithVideoCapabilities {
    fun plan(limit: CarWithVideoPolicy.Limit): CarWithVideoPolicy.Plan = try {
        val query = query()
        CarWithVideoPolicy.select(query.decoders, limit, queryIncomplete = query.incomplete)
    } catch (_: Exception) {
        CarWithVideoPolicy.select(null, limit)
    }

    private data class Query(val decoders: List<CarWithVideoPolicy.Decoder>, val incomplete: Boolean)

    private fun query(): Query {
        val decoders = mutableListOf<CarWithVideoPolicy.Decoder>()
        var incomplete = false
        for (codec in MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos) try {
            if (!codec.isEncoder && codec.supportedTypes.any { it.equals(CarWithVideoPolicy.MIME, true) }) {
                decoders += AndroidDecoder(codec, codec.getCapabilitiesForType(CarWithVideoPolicy.MIME))
            }
        } catch (_: Exception) {
            incomplete = true
        }
        return Query(decoders, incomplete)
    }

    internal class AndroidDecoder(
        codec: MediaCodecInfo,
        private val capabilities: MediaCodecInfo.CodecCapabilities,
    ) : CarWithVideoPolicy.Decoder {
        private val video = requireNotNull(capabilities.videoCapabilities)
        override val name = codec.name
        override val hardwareAccelerated: Boolean? = if (Build.VERSION.SDK_INT >= 29) codec.isHardwareAccelerated else null
        override val widthAlignment = video.widthAlignment
        override val heightAlignment = video.heightAlignment
        override val profileLevels = capabilities.profileLevels.map { CarWithVideoPolicy.ProfileLevel(it.profile, it.level) }

        override fun supports(stream: CarWithVideoPolicy.Stream): Boolean {
            val mode = stream.mode
            if (!mode.valid || !video.areSizeAndRateSupported(mode.width, mode.height, mode.fps.toDouble())) return false
            val format = MediaFormat.createVideoFormat(CarWithVideoPolicy.MIME, mode.width, mode.height).apply {
                setInteger(MediaFormat.KEY_FRAME_RATE, mode.fps)
                setInteger(MediaFormat.KEY_PROFILE, stream.profileLevel.profile)
                setInteger(MediaFormat.KEY_LEVEL, stream.profileLevel.level)
            }
            return capabilities.isFormatSupported(format)
        }
    }
}
