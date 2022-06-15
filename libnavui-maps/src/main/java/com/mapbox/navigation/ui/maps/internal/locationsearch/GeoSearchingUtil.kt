package com.mapbox.navigation.ui.maps.internal.locationsearch

import android.R.attr
import com.mapbox.geojson.Point
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import java.util.Collections
import java.util.Random

internal object GeoSearchingUtil {

    // fun selectSampledMedianDistanceThreshold(origin: Point, points: List<Point>): Double {
    //     points.shuffled().take(35)
    //
    //     // val features = points.map { Feature.fromGeometry(it) }
    //     // val center = TurfMeasurement.center(FeatureCollection.fromFeatures(features)).geometry() as Point
    //     // return TurfMeasurement.distance(origin, center, TurfConstants.UNIT_METERS)
    // }

    fun selectThreshold(origin: Point, points: List<Point>): Double {
        return if (points.isNotEmpty()) {
            var left = 0
            var right = points.size - 1
            val medianIndex = points.size / 2
            val random = Random()
            while (left != right) {
                val pivotIndex =
                    left + if (right - left == 0) 0 else random.nextInt(right - left)
                val pivotDistance = TurfMeasurement.distance(origin, points[pivotIndex], TurfConstants.UNIT_METERS)
                Collections.swap(points, pivotIndex, right)
                var storeIndex = left

                for (i in left until right) {
                    if (TurfMeasurement.distance(origin, points[i], TurfConstants.UNIT_METERS) < pivotDistance) {
                        Collections.swap(points, storeIndex++, i)
                    }
                }
                Collections.swap(points, right, storeIndex)
                if (storeIndex == medianIndex) {
                    break
                } else if (storeIndex < medianIndex) {
                    left = storeIndex + 1
                } else {
                    right = storeIndex - 1
                }
            }

            TurfMeasurement.distance(origin, points[medianIndex])
        } else {
            0.0
        }
    }


    fun partitionPoints(vantagePoint: Point, points: List<Point>, threshold: Double): Int {
        var i = 0
        var j = points.size - 1

        // This is, essentially, a single swapping quicksort iteration
        while (i <= j) {
            if (TurfMeasurement.distance(vantagePoint, points[i], TurfConstants.UNIT_METERS) > threshold) {
                while (j >= i) {
                    if (TurfMeasurement.distance(vantagePoint, points[j], TurfConstants.UNIT_METERS) <= threshold) {
                        Collections.swap(points, i, j--)
                        break
                    }
                    j--
                }
            }
            i++
        }

        val firstIndexPastThreshold = if (TurfMeasurement.distance(
                vantagePoint,
                points[i - 1],
                TurfConstants.UNIT_METERS
            ) > threshold
        ) {
            i - 1
        } else {
            i
        }

        return if (TurfMeasurement.distance(vantagePoint, points.first()) <= threshold && TurfMeasurement.distance(vantagePoint, points.last()) > threshold) {
            firstIndexPastThreshold
        } else {
            -1
        }
    }
}
