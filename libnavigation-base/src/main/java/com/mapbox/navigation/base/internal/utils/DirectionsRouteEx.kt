@file:JvmName("DirectionsRouteEx")

package com.mapbox.navigation.base.internal.utils

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.navigation.base.internal.route.Waypoint
import com.mapbox.navigation.base.utils.ifNonNull

/**
 * Compare routes as geometries (if exist) or as a names of [LegStep] of the [DirectionsRoute].
 *
 * **This check does not compare route annotations!**
 */
fun DirectionsRoute.isSameRoute(compare: DirectionsRoute?): Boolean {
    if (this === compare) {
        return true
    }

    if (compare == null) {
        return false
    }

    ifNonNull(this.geometry(), compare.geometry()) { g1, g2 ->
        return g1 == g2
    }

    ifNonNull(this.stepsNamesAsString(), compare.stepsNamesAsString()) { s1, s2 ->
        return s1 == s2
    }

    return false
}

private fun DirectionsRoute.stepsNamesAsString(): String? =
    this.legs()
        ?.joinToString { leg ->
            leg.steps()?.joinToString { step -> step.name() ?: "" } ?: ""
        }

internal fun List<com.mapbox.navigator.Waypoint>.mapToSkd(): List<Waypoint> =
    map { nativeWaypoint ->
        Waypoint(
            location = nativeWaypoint.location,
            type = nativeWaypoint.type.mapToSdk(),
            name = nativeWaypoint.name,
            target = nativeWaypoint.target,
        )
    }

@Waypoint.Type
private fun com.mapbox.navigator.WaypointType.mapToSdk(): Int =
    when (this) {
        com.mapbox.navigator.WaypointType.REGULAR -> Waypoint.REGULAR
        com.mapbox.navigator.WaypointType.SILENT -> Waypoint.SILENT
        com.mapbox.navigator.WaypointType.EV_CHARGING -> Waypoint.EV_CHARGING
    }
