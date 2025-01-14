package com.mapbox.navigation.testing.factories

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.Incident
import com.mapbox.api.directions.v5.models.LegAnnotation
import com.mapbox.api.directions.v5.models.MaxSpeed
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point

fun createDirectionsResponse(
    uuid: String = "testUUID",
    routes: List<DirectionsRoute> = listOf(createDirectionsRoute())
): DirectionsResponse {
    val processedRoutes = routes.map {
        it.toBuilder().requestUuid(uuid).build()
    }
    return DirectionsResponse.builder()
        .uuid(uuid)
        .code("Ok")
        .routes(processedRoutes)
        .build()
}

fun createDirectionsRoute(
    legs: List<RouteLeg>? = listOf(createRouteLeg()),
    routeOptions: RouteOptions = createRouteOptions(),
    distance: Double = 5.0,
    duration: Double = 9.0,
    routeIndex: String = "0",
    requestUuid: String? = "testUUID"
): DirectionsRoute = DirectionsRoute.builder()
    .distance(distance)
    .duration(duration)
    .legs(legs)
    .routeOptions(routeOptions)
    .routeIndex(routeIndex)
    .requestUuid(requestUuid)
    .build()

fun createRouteLeg(
    annotation: LegAnnotation? = createRouteLegAnnotation(),
    incidents: List<Incident>? = null
): RouteLeg {
    return RouteLeg.builder().annotation(annotation).incidents(incidents).build()
}

fun createRouteLegAnnotation(
    congestion: List<String> = listOf("severe", "moderate"),
    congestionNumeric: List<Int> = listOf(90, 50),
    distance: List<Double> = listOf(10.0, 10.0),
    duration: List<Double> = listOf(2.0, 2.0),
    maxSpeed: List<MaxSpeed> = listOf(createMaxSpeed(40), createMaxSpeed(60)),
    speed: List<Double> = listOf(40.4, 60.7)
): LegAnnotation {
    return LegAnnotation.builder()
        .distance(distance)
        .duration(duration)
        .congestion(congestion)
        .congestionNumeric(congestionNumeric)
        .maxspeed(maxSpeed)
        .speed(speed)
        .build()
}

fun createMaxSpeed(
    speed: Int = 60,
    unit: String = "km/h"
): MaxSpeed = MaxSpeed.builder().speed(speed).unit(unit).build()

fun createIncident(
    id: String = "1",
    @Incident.IncidentType type: String = Incident.INCIDENT_CONSTRUCTION,
    endTime: String? = null
): Incident = Incident.builder()
    .id(id)
    .type(type)
    .endTime(endTime)
    .build()

fun createRouteOptions(
    // the majority of tests needs 2 waypoints
    coordinatesList: List<Point> = createCoordinatesList(2),
    profile: String = DirectionsCriteria.PROFILE_DRIVING,
    enableRefresh: Boolean = false,
): RouteOptions {
    return RouteOptions
        .builder()
        .coordinatesList(coordinatesList)
        .profile(profile)
        .enableRefresh(enableRefresh)
        .build()
}

fun createCoordinatesList(waypointCount: Int): List<Point> = MutableList(waypointCount) { index ->
    Point.fromLngLat(index.toDouble(), index.toDouble())
}
