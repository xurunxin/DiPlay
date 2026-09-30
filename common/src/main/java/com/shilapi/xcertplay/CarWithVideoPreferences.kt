package com.shilapi.xcertplay

import android.content.Context
import com.shilapi.xcertplay.carwith.CarWithVideoPolicy

/** Separate from CarPlay sizing and identity preferences. Defaults to the lower-load 720p cap. */
internal object CarWithVideoPreferences {
    private const val PREFS = "diplay_carwith_video"
    private const val MAX_RESOLUTION = "maximum_resolution"

    fun loadLimit(context: Context): CarWithVideoPolicy.Limit = CarWithVideoPolicy.Limit.fromStored(
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(MAX_RESOLUTION, null),
    )

    fun saveLimit(context: Context, limit: CarWithVideoPolicy.Limit) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(MAX_RESOLUTION, limit.name).apply()
    }
}
