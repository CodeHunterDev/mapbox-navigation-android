package com.mapbox.navigation.ui.maps.internal.locationsearch

import com.mapbox.geojson.Point
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import java.util.PriorityQueue

class NearestNeighborCollector(val queryPoint: Point, private val capacity: Int) {

    private val distanceComparator by lazy {
        DistanceComparator(queryPoint)
    }

    private val priorityQueue by lazy {
        PriorityQueue(capacity, java.util.Collections.reverseOrder(distanceComparator))
    }

    private var distanceToFarthestPoint: Double = 0.0

    fun offerPoint(offeredPoint: Point) {
        val pointAdded = if (priorityQueue.size < this.capacity) {
            priorityQueue.add(offeredPoint)
        } else {
            if (priorityQueue.isNotEmpty()) {
                val distanceToNewPoint = TurfMeasurement.distance(queryPoint, offeredPoint, TurfConstants.UNIT_METERS)
                if (distanceToNewPoint < distanceToFarthestPoint) {
                    priorityQueue.poll()
                    priorityQueue.add(offeredPoint)
                    true
                } else {
                    false
                }
            } else {
                false
            }
        }

        if (pointAdded && priorityQueue.isNotEmpty()) {
            distanceToFarthestPoint = TurfMeasurement.distance(queryPoint, priorityQueue.peek()!!, TurfConstants.UNIT_METERS)
        }
    }

    fun getFarthestPoint(): Point? = priorityQueue.peek()

    fun toSortedList() = priorityQueue.toList().sortedWith(distanceComparator)
}
