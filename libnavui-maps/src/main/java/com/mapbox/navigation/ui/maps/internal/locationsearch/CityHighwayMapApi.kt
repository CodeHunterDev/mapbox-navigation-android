package com.mapbox.navigation.ui.maps.internal.locationsearch

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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

class CityHighwayMapApi(
    private val locationSearchTree: LocationSearchTree<EnhancedPoint> = LocationSearchTree(),
    private val chmImageProvider: () -> Bitmap
) {

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
            //pointToPixelCollectionsToInterpolate.flatten()
            locationSearchTree.addAll(pointToPixelCollectionsToInterpolate.flatten().distinct())
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

    fun getMapImage(currentLocation: Point?, resultConsumer: Consumer<Expected<CHMError, CityHighwayBitmap>>) {
        getMapData(currentLocation) { expectedResult ->
            expectedResult.fold(
                {
                    ExpectedFactory.createValue<CHMError, CityHighwayBitmap>(CityHighwayBitmap(chmImageProvider.invoke()))
                },{
                    ifNonNull(it.pointPixelData.getChmCoordinates()) { imageCoordinates ->
                        updateCHMImage(imageCoordinates.first, imageCoordinates.second, chmImageProvider.invoke()).run {
                            ExpectedFactory.createValue<CHMError, CityHighwayBitmap>(CityHighwayBitmap(this))
                        }
                    } ?: ExpectedFactory.createValue<CHMError, CityHighwayBitmap>(CityHighwayBitmap(chmImageProvider.invoke()))
            }).apply {
                resultConsumer.accept(this)
            }
        }
    }

    private fun updateCHMImage(x: Float, y: Float, bitmap: Bitmap): Bitmap {
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#ffcc00")
            style = Paint.Style.FILL
        }
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        canvas.drawCircle(x, y, 10f, paint)
        return bitmap
    }



    fun cancel() {
        jobControl.job.cancelChildren()
        locationSearchTree.clear()
    }
}
// temporary, move this somewhere else
suspend fun CityHighwayMapApi.getCityHighwayMapData(currentLocation: Point?): Expected<CHMError,CHMResult> {
    return suspendCoroutine { continuation ->
        this.getMapData(currentLocation) { value ->
            continuation.resume(value)
        }
    }
}

suspend fun CityHighwayMapApi.getCityHighwayMapImage(currentLocation: Point?): Expected<CHMError, CityHighwayBitmap> {
    return suspendCoroutine { continuation ->
        this.getMapImage(currentLocation) { value ->
            continuation.resume(value)
        }
    }
}
