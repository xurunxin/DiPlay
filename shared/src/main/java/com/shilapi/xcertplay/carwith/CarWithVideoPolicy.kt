package com.shilapi.xcertplay.carwith

/** Receiver policy only: no discovery, identity, protocol publication or codec startup. */
object CarWithVideoPolicy {
    const val MIME = "video/avc"

    enum class Limit(val longEdge: Int, val shortEdge: Int) {
        HD720(1280, 720), FHD1080(1920, 1080);
        companion object {
            fun fromStored(value: String?): Limit = entries.firstOrNull { it.name == value } ?: HD720
        }
    }
    enum class Orientation { LANDSCAPE, PORTRAIT }
    enum class Evidence { CAPABILITY_CHECKED, UNVERIFIED_FALLBACK, NO_SUPPORTED_MODE }

    data class Mode(val width: Int, val height: Int, val fps: Int) {
        val valid: Boolean get() = width > 0 && height > 0 && fps in 1..60
        fun fits(limit: Limit): Boolean = valid && maxOf(width, height) <= limit.longEdge &&
            minOf(width, height) <= limit.shortEdge
    }
    /** Android reports the maximum level for a profile; this is not a required stream level. */
    data class ProfileLevel(val profile: Int, val level: Int)
    data class Stream(val mode: Mode, val profileLevel: ProfileLevel)

    interface Decoder {
        val name: String
        val hardwareAccelerated: Boolean?
        val widthAlignment: Int
        val heightAlignment: Int
        val profileLevels: List<ProfileLevel>
        /** Must check dimensions/rate and the complete AVC format, including profile/level. */
        fun supports(stream: Stream): Boolean
    }

    data class Candidate(
        val mode: Mode,
        val decoder: Decoder,
        val profileLevels: List<ProfileLevel>,
    )
    data class Plan(
        val limit: Limit,
        val evidence: Evidence,
        val candidates: List<Candidate>,
        val fallback: Mode? = null,
    ) {
        val selected: Mode? get() = candidates.firstOrNull()?.mode ?: fallback
    }

    /** Fixed bounded video presets; physical display pixels never determine the stream size. */
    fun select(
        decoders: List<Decoder>?,
        limit: Limit = Limit.HD720,
        requestedFps: Int = 30,
        orientation: Orientation = Orientation.LANDSCAPE,
        queryIncomplete: Boolean = false,
    ): Plan {
        require(requestedFps == 30 || requestedFps == 60)
        fun orient(width: Int, height: Int, fps: Int) = if (orientation == Orientation.PORTRAIT)
            Mode(height, width, fps) else Mode(width, height, fps)
        fun unknown() = Plan(limit, Evidence.UNVERIFIED_FALLBACK, emptyList(), orient(1280, 720, 30))
        if (decoders == null) return unknown()
        val rates = if (requestedFps == 60) listOf(60, 30) else listOf(30)
        val sizes = if (limit == Limit.FHD1080) listOf(1920 to 1080, 1280 to 720)
            else listOf(1280 to 720)
        val result = mutableListOf<Candidate>()
        var queryFailed = queryIncomplete
        // Avoid selecting software 1080p over a hardware-supported 720p on constrained receivers.
        for (decoder in decoders.sortedBy { when (it.hardwareAccelerated) { true -> 0; null -> 1; false -> 2 } }) {
            if (decoder.widthAlignment <= 0 || decoder.heightAlignment <= 0) {
                queryFailed = true
                continue
            }
            for ((width, height) in sizes) for (fps in rates) {
                val mode = orient(width, height, fps)
                // Do not round video dimensions into a different aspect ratio or exceed the cap.
                if (mode.width % decoder.widthAlignment != 0 || mode.height % decoder.heightAlignment != 0) continue
                val profiles = decoder.profileLevels.distinct().filter { pair ->
                    if (pair.profile <= 0 || pair.level <= 0) false else try {
                        decoder.supports(Stream(mode, pair))
                    } catch (_: Exception) { queryFailed = true; false }
                }
                if (profiles.isNotEmpty()) result += Candidate(mode, decoder, profiles)
            }
        }
        return when {
            result.isNotEmpty() -> Plan(limit, Evidence.CAPABILITY_CHECKED, result.toList())
            queryFailed -> unknown()
            else -> Plan(limit, Evidence.NO_SUPPORTED_MODE, emptyList())
        }
    }
}
