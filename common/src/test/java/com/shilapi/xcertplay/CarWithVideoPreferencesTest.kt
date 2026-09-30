package com.shilapi.xcertplay

import com.shilapi.xcertplay.carwith.CarWithVideoPolicy
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28, 31, 33], manifest = Config.NONE)
class CarWithVideoPreferencesTest {
    @Test fun limitDefaultsTo720pAndPersistsIndependentlyOfCarPlay() {
        val context = RuntimeEnvironment.getApplication()
        assertEquals(CarWithVideoPolicy.Limit.HD720, CarWithVideoPreferences.loadLimit(context))
        val carPlayScale = AirPlayPersistence.loadDisplayScaleTenths(context)
        CarWithVideoPreferences.saveLimit(context, CarWithVideoPolicy.Limit.FHD1080)
        assertEquals(CarWithVideoPolicy.Limit.FHD1080, CarWithVideoPreferences.loadLimit(context))
        assertEquals(carPlayScale, AirPlayPersistence.loadDisplayScaleTenths(context))
    }

    @Test fun publicCodecQueryRunsOnApi28AndDoesNotPretendToStartAConnection() {
        val plan = CarWithVideoCapabilities.plan(CarWithVideoPolicy.Limit.HD720)
        assertTrue(plan.selected == null || plan.selected!!.fits(CarWithVideoPolicy.Limit.HD720))
        val state = com.shilapi.xcertplay.carwith.CarWithVideoNegotiation(plan)
        assertTrue(state.publishedModes.isEmpty())
        assertNull(state.negotiated)
        assertNull(state.configured)
    }
}
