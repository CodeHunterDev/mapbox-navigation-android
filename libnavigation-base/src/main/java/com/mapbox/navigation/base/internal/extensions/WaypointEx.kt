@file:JvmName("WaypointEx")

package com.mapbox.navigation.base.internal.extensions

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.internal.route.Waypoint
import com.mapbox.navigation.base.trip.model.RouteProgress

/**
 * Return waypoints that are requested explicitly
 */
fun List<Waypoint>.requestedWaypoints(): List<Waypoint> =
    this.filter { it.type != Waypoint.EV_CHARGING }

/**
 * Return waypoints that tracking in [RouteProgress.currentLegProgress]#legIndex and based on
 * [DirectionsRoute.legs] index
 */
fun List<Waypoint>.legsWaypoints(): List<Waypoint> =
    this.filter { it.type != Waypoint.SILENT }

fun indexOfNextCoordinate(
    waypoints: List<Waypoint>,
    remainingWaypoints: Int
): Int? {
    if (remainingWaypoints > waypoints.size) {
        return null
    }
    val nextRequestedWaypoint = waypoints
        .subList(waypoints.size - remainingWaypoints, waypoints.size)
        .requestedWaypoints()
        .firstOrNull()
        ?: return null

    return waypoints
        .requestedWaypoints()
        .indexOf(nextRequestedWaypoint)
}
