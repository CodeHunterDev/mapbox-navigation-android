package com.mapbox.navigation.ui.maps.internal.locationsearch

import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.geojson.Point
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import com.mapbox.navigation.utils.internal.ifNonNull
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import java.util.function.Consumer
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class CityHighwayMapApi(private val locationSearchTree: LocationSearchTree<EnhancedPoint> = LocationSearchTree()) {

    private val jobControl = InternalJobControlFactory.createDefaultScopeJobControl()
    private var currentMapType: CityHighwayMap? = null

    fun setMap(mapType: CityHighwayMap) {
        locationSearchTree.clear()
        currentMapType = mapType

        val pointToPixelCollectionsToInterpolate = when (mapType) {
            CityHighwayMap.XB03 -> XB03PointToPixelMap.getPointCollections()
        }

        jobControl.scope.launch {
            pointToPixelCollectionsToInterpolate.forEach {
                LocationSearchUtil.interpolateScreenCoordinates(it)
            }
            pointToPixelCollectionsToInterpolate.flatten()
            locationSearchTree.addAll(pointToPixelCollectionsToInterpolate.flatten())
        }
    }

    // todo create extension function for using coroutines
    fun getMapData(currentLocation: Point?, resultConsumer: Consumer<Expected<CHMError,CHMResult>>) {
        // if point is null don't put a puck on the map just return the right CHM with the
        // traffic on it.

        val expected: Expected<CHMError,CHMResult> = ifNonNull(currentLocation) { targetLocation ->
            ifNonNull(locationSearchTree.getNearestNeighbor(targetLocation)) { neighbor ->
                ExpectedFactory.createValue(CHMResult(neighbor))
            } ?: ExpectedFactory.createError(CHMError("No location could be found.", null))
        } ?: ExpectedFactory.createError(CHMError("No location could be found.", null))

        resultConsumer.accept(expected)

        //return data needed to put a puck etc on the CHM and return for the
        //view class to render
    }

    fun cancel() {
        jobControl.job.cancelChildren()
        locationSearchTree.clear()
    }
}
// temporary, move this somewhere else
suspend fun CityHighwayMapApi.getMapData(currentLocation: Point?): Expected<CHMError,CHMResult> {
    return suspendCoroutine { continuation ->
        this.getMapData(currentLocation) { value ->
            continuation.resume(value)
        }
    }
}
