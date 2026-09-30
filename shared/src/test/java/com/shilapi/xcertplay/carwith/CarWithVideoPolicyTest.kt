package com.shilapi.xcertplay.carwith

import com.shilapi.xcertplay.carwith.CarWithVideoPolicy.Mode
import com.shilapi.xcertplay.carwith.CarWithVideoPolicy.ProfileLevel
import com.shilapi.xcertplay.carwith.CarWithVideoPolicy.Stream
import com.shilapi.xcertplay.carwith.CarWithVideoPolicy.Decoder
import com.shilapi.xcertplay.carwith.CarWithVideoPolicy.Limit
import com.shilapi.xcertplay.carwith.CarWithVideoPolicy.Evidence
import com.shilapi.xcertplay.carwith.CarWithVideoPolicy.Orientation
import org.junit.Assert.*
import org.junit.Test

class CarWithVideoPolicyTest {
    private val baseline = ProfileLevel(1, 512)
    private val high = ProfileLevel(8, 2048)
    private val hd = Mode(1280, 720, 30)
    private val fullHd = Mode(1920, 1080, 30)

    private class TestDecoder(
        override val name: String = "test.decoder",
        override val hardwareAccelerated: Boolean? = true,
        override val widthAlignment: Int = 2,
        override val heightAlignment: Int = 2,
        override val profileLevels: List<ProfileLevel> = listOf(ProfileLevel(1, 512), ProfileLevel(8, 2048)),
        val check: (Stream) -> Boolean = { it.mode.valid && it.mode.width <= 1920 && it.mode.height <= 1080 && it.mode.fps <= 30 && it.profileLevel.level in 1..2048 },
    ) : Decoder {
        override fun supports(stream: Stream) = check(stream)
    }

    @Test fun defaultUserCapKeepsA4kDecoderAt720p() {
        val d = TestDecoder(check = { true })
        val plan = CarWithVideoPolicy.select(listOf(d))
        assertEquals(hd, plan.selected)
        assertTrue(plan.candidates.all { it.mode.fits(Limit.HD720) })
        assertEquals(Evidence.CAPABILITY_CHECKED, plan.evidence)
    }

    @Test fun user1080pCapSelectsSupported1080pAndRetains720pAlternative() {
        val plan = CarWithVideoPolicy.select(listOf(TestDecoder()), Limit.FHD1080)
        assertEquals(fullHd, plan.selected)
        assertEquals(listOf(fullHd, hd), plan.candidates.map { it.mode })
        assertEquals(listOf(baseline, high), plan.candidates.first().profileLevels)
    }

    @Test fun actualHeightLimitFallsBackTo720pWithoutRounding1080IntoAnotherAspectRatio() {
        val plan = CarWithVideoPolicy.select(listOf(TestDecoder(heightAlignment = 16)), Limit.FHD1080)
        assertEquals(hd, plan.selected)
        assertFalse(plan.candidates.any { it.mode.height == 1072 || it.mode.height == 1088 })
    }

    @Test fun declared1080pCapDoesNotOverrideADecoderLimitedTo720p() {
        val d = TestDecoder(check = { it.mode.width <= 1280 && it.mode.height <= 720 })
        assertEquals(hd, CarWithVideoPolicy.select(listOf(d), Limit.FHD1080).selected)
    }

    @Test fun portraitSelectionChecksActualRotatedDimensions() {
        val d = TestDecoder(check = { it.mode == Mode(720, 1280, 30) })
        val plan = CarWithVideoPolicy.select(listOf(d), orientation = Orientation.PORTRAIT)
        assertEquals(Mode(720, 1280, 30), plan.selected)
        assertTrue(plan.selected!!.fits(Limit.HD720))
        assertEquals(Evidence.NO_SUPPORTED_MODE, CarWithVideoPolicy.select(listOf(d)).evidence)
    }

    @Test fun unsupported60FpsFallsBackTo30WithoutClaiming60Support() {
        val plan = CarWithVideoPolicy.select(listOf(TestDecoder()), requestedFps = 60)
        assertEquals(hd, plan.selected)
        assertTrue(plan.candidates.all { it.mode.fps == 30 })
    }

    @Test fun noSupported30FpsIsUnavailableRatherThanAnInventedFallback() {
        val d = TestDecoder(check = { it.mode.fps <= 24 })
        val plan = CarWithVideoPolicy.select(listOf(d))
        assertEquals(Evidence.NO_SUPPORTED_MODE, plan.evidence)
        assertNull(plan.selected)
    }

    @Test fun missingCapabilitiesUseUnverified720p30EvenWhen1080pWasRequested() {
        val plan = CarWithVideoPolicy.select(null, Limit.FHD1080, 60)
        assertEquals(hd, plan.selected)
        assertEquals(Evidence.UNVERIFIED_FALLBACK, plan.evidence)
        assertTrue(plan.candidates.isEmpty())
        assertFalse(CarWithVideoNegotiation(plan).recordPublication(setOf(hd)))
    }

    @Test fun emptyDecoderListAndEmptyProfilesDoNotClaimCompatibility() {
        for (decoders in listOf(emptyList(), listOf(TestDecoder(profileLevels = emptyList())))) {
            assertEquals(Evidence.NO_SUPPORTED_MODE, CarWithVideoPolicy.select(decoders).evidence)
        }
    }

    @Test fun vendorQueryExceptionAndInvalidAlignmentRemainExplicitlyUnverified() {
        for (d in listOf(TestDecoder(check = { throw IllegalArgumentException("vendor query failed") }),
            TestDecoder(widthAlignment = 0))) {
            assertEquals(Evidence.UNVERIFIED_FALLBACK, CarWithVideoPolicy.select(listOf(d)).evidence)
        }
    }

    @Test fun failingVendorCodecDoesNotHideAnotherUsableDecoder() {
        val d = TestDecoder(check = { throw IllegalArgumentException() })
        assertEquals(Evidence.CAPABILITY_CHECKED, CarWithVideoPolicy.select(listOf(d, TestDecoder())).evidence)
    }

    @Test fun incompleteEnumerationIsUnknownUnlessAnotherDecoderValidatesAMode() {
        assertEquals(Evidence.UNVERIFIED_FALLBACK,
            CarWithVideoPolicy.select(emptyList(), queryIncomplete = true).evidence)
        assertEquals(Evidence.CAPABILITY_CHECKED,
            CarWithVideoPolicy.select(listOf(TestDecoder()), queryIncomplete = true).evidence)
    }

    @Test fun hardware720pIsPreferredOverSoftware1080p() {
        val software = TestDecoder(name = "software", hardwareAccelerated = false)
        val hardware = TestDecoder(name = "hardware", check = { it.mode == hd })
        val plan = CarWithVideoPolicy.select(listOf(software, hardware), Limit.FHD1080)
        assertEquals(hd, plan.selected)
        assertEquals("hardware", plan.candidates.first().decoder.name)
    }

    @Test fun unsupportedProfileIsNotAdvertisedEvenIfSizeAndRateFit() {
        val d = TestDecoder(check = { it.profileLevel.profile == baseline.profile })
        val plan = CarWithVideoPolicy.select(listOf(d))
        assertEquals(listOf(baseline), plan.candidates.first().profileLevels)
    }

    private fun session() = CarWithVideoNegotiation(CarWithVideoPolicy.select(listOf(TestDecoder()), Limit.FHD1080))

    @Test fun selectionAloneIsNeitherPublicationNorNegotiationNorDecoderConfiguration() {
        val state = session()
        assertTrue(state.publishedModes.isEmpty())
        assertNull(state.negotiated)
        assertNull(state.configured)
        assertEquals(CarWithVideoNegotiation.Result.NOT_PUBLISHED, state.recordNegotiated(Stream(hd, baseline)))
        assertFalse(state.recordDecoderConfigured(Stream(hd, baseline), "test.decoder"))
    }

    @Test fun oversizedOrAbnormalAspectRatioOrUnpublishedRotatedStreamNeedsRenegotiation() {
        val state = session()
        assertTrue(state.recordPublication(setOf(hd, fullHd)))
        for (mode in listOf(Mode(1920, 1152, 30), Mode(4096, 2160, 30), Mode(1920, 240, 30),
            Mode(720, 1280, 30), Mode(0, 720, 30), Mode(-1280, 720, 30), Mode(1280, 720, 60))) {
            assertEquals(CarWithVideoNegotiation.Result.RENEGOTIATION_REQUIRED, state.recordNegotiated(Stream(mode, baseline)))
            assertNull(state.negotiated)
            assertNull(state.configured)
        }
    }

    @Test fun profileOrLevelOutsideDecoderSupportIsRejectedBeforeConfiguration() {
        val state = session(); state.recordPublication(setOf(hd))
        for (pair in listOf(ProfileLevel(16, 512), ProfileLevel(1, 4096), ProfileLevel(1, 0))) {
            assertEquals(CarWithVideoNegotiation.Result.RENEGOTIATION_REQUIRED, state.recordNegotiated(Stream(hd, pair)))
        }
    }

    @Test fun decoderConfigurationCannotSubstituteAnotherCodecOrSurfaceScaledDimensions() {
        val state = session(); state.recordPublication(setOf(hd))
        val stream = Stream(hd, baseline)
        assertEquals(CarWithVideoNegotiation.Result.ACCEPTED, state.recordNegotiated(stream))
        assertNull(state.configured)
        assertFalse(state.recordDecoderConfigured(Stream(fullHd, baseline), "test.decoder"))
        assertFalse(state.recordDecoderConfigured(stream, "other.decoder"))
        assertTrue(state.recordDecoderConfigured(stream, "test.decoder"))
        assertEquals(stream, state.configured)
    }

    @Test fun republishingOrResetClearsPriorNegotiationAndConfiguration() {
        val state = session(); state.recordPublication(setOf(hd))
        val stream = Stream(hd, baseline)
        state.recordNegotiated(stream); state.recordDecoderConfigured(stream, "test.decoder")
        assertTrue(state.recordPublication(setOf(fullHd)))
        assertNull(state.negotiated); assertNull(state.configured)
        state.reset(); assertTrue(state.publishedModes.isEmpty())
    }

    @Test fun arbitraryPublicationAndCorruptStoredLimitsAreRejectedSafely() {
        assertFalse(session().recordPublication(setOf(Mode(1920, 1152, 30))))
        assertFalse(session().recordPublication(emptySet()))
        assertEquals(Limit.HD720, Limit.fromStored("4K"))
        assertEquals(Limit.HD720, Limit.fromStored(null))
    }

    @Test fun invalidRepublishingClearsPreviouslyTrustedState() {
        val state = session(); state.recordPublication(setOf(hd))
        state.recordNegotiated(Stream(hd, baseline))
        assertFalse(state.recordPublication(setOf(Mode(1920, 1152, 30))))
        assertTrue(state.publishedModes.isEmpty())
        assertNull(state.negotiated)
    }
}
