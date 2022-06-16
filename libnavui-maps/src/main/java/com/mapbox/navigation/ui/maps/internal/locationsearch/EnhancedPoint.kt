package com.mapbox.navigation.ui.maps.internal.locationsearch

import com.mapbox.geojson.Point
import java.util.function.Supplier

sealed class EnhancedPoint: Supplier<Point> {

    abstract fun getChmCoordinates(): Pair<Float, Float>?

    class KeyPoint(val point: Point, val chmCoords: Pair<Float, Float>): EnhancedPoint() {
        override fun get(): Point {
            return point
        }

        override fun getChmCoordinates(): Pair<Float, Float> {
            return chmCoords
        }
    }

    class MapPoint(val point: Point, var chmCoords: Pair<Float, Float>? = null): EnhancedPoint() {
        override fun get(): Point {
            return point
        }

        override fun getChmCoordinates(): Pair<Float, Float>? {
            return chmCoords
        }

        fun setChmCoordinates(coords: Pair<Float, Float>) {
            chmCoords = coords
        }
    }
}
