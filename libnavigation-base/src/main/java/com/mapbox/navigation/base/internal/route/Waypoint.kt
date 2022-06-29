package com.mapbox.navigation.base.internal.route

import androidx.annotation.IntDef
import com.mapbox.geojson.Point

data class Waypoint(
    val location: Point,
    @Type val type: Int,
    val name: String,
    val target: Point?,
) {
    companion object {
        const val REGULAR = 1
        const val SILENT = 2
        const val EV_CHARGING = 3
    }

    @Target(
        AnnotationTarget.PROPERTY,
        AnnotationTarget.VALUE_PARAMETER,
        AnnotationTarget.FUNCTION,
        AnnotationTarget.TYPE
    )
    @Retention(AnnotationRetention.BINARY)
    @IntDef(REGULAR, SILENT, EV_CHARGING)
    annotation class Type
}
