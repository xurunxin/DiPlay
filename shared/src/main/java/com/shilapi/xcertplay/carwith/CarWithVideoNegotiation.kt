package com.shilapi.xcertplay.carwith

/**
 * Event guard for a future real backend. Methods record externally confirmed events;
 * this class does not publish anything or configure a decoder.
 */
class CarWithVideoNegotiation(val plan: CarWithVideoPolicy.Plan) {
    enum class Result { ACCEPTED, NOT_PUBLISHED, RENEGOTIATION_REQUIRED }

    var publishedModes: Set<CarWithVideoPolicy.Mode> = emptySet()
        private set
    var negotiated: CarWithVideoPolicy.Stream? = null
        private set
    var decoderName: String? = null
        private set
    var configured: CarWithVideoPolicy.Stream? = null
        private set

    /** Only record modes a real protocol backend confirms it has published to the sender. */
    fun recordPublication(modes: Set<CarWithVideoPolicy.Mode>): Boolean {
        reset()
        if (plan.evidence != CarWithVideoPolicy.Evidence.CAPABILITY_CHECKED || modes.isEmpty() ||
            modes.any { mode -> !mode.fits(plan.limit) || plan.candidates.none { it.mode == mode } }) return false
        publishedModes = modes.toSet()
        negotiated = null
        configured = null
        decoderName = null
        return true
    }

    /** Final sender parameters must match a published mode and an actual decoder's format support. */
    fun recordNegotiated(stream: CarWithVideoPolicy.Stream): Result {
        negotiated = null
        configured = null
        decoderName = null
        if (publishedModes.isEmpty()) return Result.NOT_PUBLISHED
        if (!stream.mode.fits(plan.limit) || stream.mode !in publishedModes ||
            stream.profileLevel.profile <= 0 || stream.profileLevel.level <= 0) return Result.RENEGOTIATION_REQUIRED
        val candidate = plan.candidates.firstOrNull { candidate ->
            candidate.mode == stream.mode && candidate.profileLevels.any { it.profile == stream.profileLevel.profile } &&
                runCatching { candidate.decoder.supports(stream) }.getOrDefault(false)
        } ?: return Result.RENEGOTIATION_REQUIRED
        negotiated = stream
        decoderName = candidate.decoder.name
        return Result.ACCEPTED
    }

    /** Call after the selected decoder actually configures the negotiated stream, never for Surface scaling. */
    fun recordDecoderConfigured(stream: CarWithVideoPolicy.Stream, actualDecoderName: String): Boolean {
        configured = null
        if (stream != negotiated || actualDecoderName != decoderName) return false
        configured = stream
        return true
    }

    fun reset() {
        publishedModes = emptySet()
        negotiated = null
        decoderName = null
        configured = null
    }
}
