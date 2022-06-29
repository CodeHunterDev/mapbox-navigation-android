package com.mapbox.navigation.core.routeoptions

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.Bearing
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.internal.extensions.indexOfNextCoordinate
import com.mapbox.navigation.base.internal.utils._waypoints
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.routealternatives.RouteAlternativesController
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.OffRouteObserver
import com.mapbox.navigation.utils.internal.logE

private const val DEFAULT_REROUTE_BEARING_TOLERANCE = 90.0
private const val LOG_CATEGORY = "RouteOptionsUpdater"

/**
 * Updater is used for *Reroute* and *Route Alternatives* flow.
 *
 * It's used when turn-by-turn navigation goes off-route (see [OffRouteObserver])
 * and when route alternatives (see [RouteAlternativesController]) are requested.
 * For example, this is needed in order to filter the waypoints that have been completed.
 */
class RouteOptionsUpdater {

    /**
     * Provides a new [RouteOptions] instance based on the original request options, the current route progress and location matcher result.
     *
     * @return `RouteOptionsResult.Error` if a new [RouteOptions] instance cannot be combined based on the input given.
     * `RouteOptionsResult.Success` with a new [RouteOptions] instance if successfully combined.
     */
    fun update(
        routeOptions: RouteOptions?,
        routeProgress: RouteProgress?,
        locationMatcherResult: LocationMatcherResult?,
    ): RouteOptionsResult {
        if (routeOptions == null || routeProgress == null || locationMatcherResult == null) {
            val msg = "Cannot combine RouteOptions, invalid inputs. routeOptions, " +
                "routeProgress and locationMatcherResult cannot be null"
            logE(msg, LOG_CATEGORY)
            return RouteOptionsResult.Error(Throwable(msg))
        }

        val coordinatesList = routeOptions.coordinatesList()

        val (nextCoordinateIndex, remainingCoordinates) = indexOfNextCoordinate(
            routeProgress.navigationRoute._waypoints(),
            routeProgress.remainingWaypoints
        ).let {
            if (it == null) {
                val msg = "Index of next coordinate is not defined"
                logE(msg, LOG_CATEGORY)
                return RouteOptionsResult.Error(Throwable(msg))
            } else if (coordinatesList.lastIndex < it) {
                val msg = "Index of next coordinate is out of range of coordinates"
                logE(msg, LOG_CATEGORY)
                return RouteOptionsResult.Error(Throwable(msg))
            } else {
                return@let it to coordinatesList.size - it
            }
        }

        val optionsBuilder = routeOptions.toBuilder()

        try {
            val location = locationMatcherResult.enhancedLocation
            optionsBuilder
                .coordinatesList(
                    coordinatesList
                        .subList(nextCoordinateIndex, coordinatesList.size)
                        .toMutableList()
                        .apply {
                            add(0, Point.fromLngLat(location.longitude, location.latitude))
                        }
                )
                .bearingsList(
                    getUpdatedBearingList(
                        remainingCoordinates,
                        nextCoordinateIndex,
                        location.bearing.toDouble(),
                        routeOptions.bearingsList(),
                    )
                )
                .radiusesList(
                    let radiusesList@{
                        val radiusesList = routeOptions.radiusesList()
                        if (radiusesList.isNullOrEmpty()) {
                            return@radiusesList emptyList<Double?>()
                        }
                        mutableListOf<Double?>().also {
                            it.addAll(
                                radiusesList.subList(
                                    nextCoordinateIndex - 1, coordinatesList.size
                                )
                            )
                        }
                    }
                )
                .approachesList(
                    let approachesList@{
                        val approachesList = routeOptions.approachesList()
                        if (approachesList.isNullOrEmpty()) {
                            return@approachesList emptyList<String>()
                        }
                        mutableListOf<String>().also {
                            it.addAll(
                                approachesList.subList(
                                    nextCoordinateIndex - 1, coordinatesList.size
                                )
                            )
                        }
                    }
                )
                .snappingIncludeClosuresList(
                    let snappingClosures@{
                        val snappingClosures = routeOptions.snappingIncludeClosuresList()
                        if (snappingClosures.isNullOrEmpty()) {
                            return@snappingClosures emptyList<Boolean>()
                        }
                        mutableListOf<Boolean>().also {
                            it.addAll(
                                snappingClosures.subList(
                                    nextCoordinateIndex - 1, coordinatesList.size
                                )
                            )
                        }
                    }
                )
                .waypointNamesList(
                    getUpdatedWaypointsList(
                        routeOptions.waypointNamesList(),
                        routeOptions.waypointIndicesList(),
                        nextCoordinateIndex
                    )
                )
                .waypointTargetsList(
                    getUpdatedWaypointsList(
                        routeOptions.waypointTargetsList(),
                        routeOptions.waypointIndicesList(),
                        nextCoordinateIndex
                    )
                )
                .waypointIndicesList(
                    getUpdatedWaypointIndicesList(
                        routeOptions.waypointIndicesList(),
                        nextCoordinateIndex
                    )
                )

            if (
                routeOptions.profile() == DirectionsCriteria.PROFILE_DRIVING ||
                routeOptions.profile() == DirectionsCriteria.PROFILE_DRIVING_TRAFFIC
            ) {
                optionsBuilder.layersList(
                    mutableListOf(locationMatcherResult.zLevel).apply {
                        val legacyLayerList = routeOptions.layersList()
                        if (legacyLayerList != null) {
                            addAll(
                                legacyLayerList.subList(
                                    nextCoordinateIndex, coordinatesList.size
                                )
                            )
                        } else {
                            while (this.size < remainingCoordinates + 1) {
                                add(null)
                            }
                        }
                    }
                )
            }

            optionsBuilder.arriveBy(null)
            optionsBuilder.departAt(null)
        } catch (e: Exception) {
            logE("routeOptions=[$routeOptions]", LOG_CATEGORY)
            logE("locationMatcherResult=[$locationMatcherResult]", LOG_CATEGORY)
            logE("routeProgress=[$routeProgress]", LOG_CATEGORY)
            throw e
        }

        return RouteOptionsResult.Success(optionsBuilder.build())
    }

    private fun getUpdatedBearingList(
        remainingCoordinates: Int,
        nextCoordinateIndex: Int,
        currentAngle: Double,
        legacyBearingList: List<Bearing?>?,
    ): MutableList<Bearing?> {
        return ArrayList<Bearing?>().also { newList ->
            val originTolerance = legacyBearingList?.getOrNull(0)
                ?.degrees()
                ?: DEFAULT_REROUTE_BEARING_TOLERANCE
            newList.add(Bearing.builder().angle(currentAngle).degrees(originTolerance).build())

            if (legacyBearingList != null) {
                for (idx in nextCoordinateIndex..legacyBearingList.lastIndex) {
                    newList.add(legacyBearingList[idx])
                }
            }

            while (newList.size < remainingCoordinates + 1) {
                newList.add(null)
            }
        }
    }

    private fun getUpdatedWaypointIndicesList(
        waypointIndicesList: List<Int>?,
        nextCoordinateIndex: Int
    ): MutableList<Int> {
        if (waypointIndicesList.isNullOrEmpty()) {
            return mutableListOf()
        }
        return mutableListOf<Int>().apply {
            add(0)
            waypointIndicesList.forEach { value ->
                val newVal = value - nextCoordinateIndex + 1
                if (newVal > 0) {
                    add(newVal)
                }
            }
        }
    }

    private fun <T> getUpdatedWaypointsList(
        waypointsList: List<T>?,
        waypointIndicesList: List<Int>?,
        nextCoordinateIndex: Int
    ): MutableList<T> {
        if (waypointsList.isNullOrEmpty()) {
            return mutableListOf()
        }
        return mutableListOf<T>().also { updatedWaypointsList ->
            val updatedStartWaypointsListIndex = getUpdatedStartWaypointsListIndex(
                waypointIndicesList,
                nextCoordinateIndex
            )
            updatedWaypointsList.add(waypointsList[updatedStartWaypointsListIndex])
            updatedWaypointsList.addAll(
                waypointsList.subList(
                    updatedStartWaypointsListIndex + 1,
                    waypointsList.size
                )
            )
        }
    }

    private fun getUpdatedStartWaypointsListIndex(
        waypointIndicesList: List<Int>?,
        nextCoordinateIndex: Int
    ): Int {
        var updatedStartWaypointIndicesIndex = 0
        waypointIndicesList?.forEachIndexed { indx, waypointIndex ->
            if (waypointIndex < nextCoordinateIndex) {
                updatedStartWaypointIndicesIndex = indx
            }
        }
        return updatedStartWaypointIndicesIndex
    }

    /**
     * Describes a result of generating new options from the original request and the current route progress.
     */
    sealed class RouteOptionsResult {
        /**
         * Successful operation.
         *
         * @param routeOptions the recreated route option from the current route progress
         */
        data class Success(val routeOptions: RouteOptions) : RouteOptionsResult()

        /**
         * Failed operation.
         *
         * @param error reason
         */
        data class Error(val error: Throwable) : RouteOptionsResult()
    }
}
