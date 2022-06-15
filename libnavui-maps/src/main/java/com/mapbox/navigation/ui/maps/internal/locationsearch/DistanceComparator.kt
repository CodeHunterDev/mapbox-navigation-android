package com.mapbox.navigation.ui.maps.internal.locationsearch

import com.mapbox.geojson.Point
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement

class DistanceComparator(private val origin: Point): Comparator<Point> {

    override fun compare(p0: Point, p1: Point): Int {
        return TurfMeasurement.distance(origin, p0, TurfConstants.UNIT_METERS)
            .compareTo(TurfMeasurement.distance(origin, p1, TurfConstants.UNIT_METERS))
    }
}
