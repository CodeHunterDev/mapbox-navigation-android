package com.mapbox.navigation.ui.maps.internal.locationsearch

import com.mapbox.geojson.Point
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import java.util.concurrent.CopyOnWriteArrayList

class LocationTreeNode(private val points: MutableList<Point>, private val capacity: Int = 32, private val distanceCalcFunction: (Point, Point) ->  Double) {

    private val vantagePoint: Point by lazy {
        points.random()
    }

    private val sortedPoints = {
        points.sortedBy { distanceCalcFunction(vantagePoint, it) }
    }

    private var threshold = 0.0

    private var closer: LocationTreeNode? = null
    private var farther: LocationTreeNode? = null

    init {
        initializeNode()
    }

    internal fun initializeNode() {
        if (points.isEmpty()) {
            if (closer?.size() == 0 || farther?.size() == 0) {
                // One of the child nodes has become empty, and needs to be pruned.
                addAllPointsToCollection(points)
                closer = null
                farther = null
                initializeNode()
            } else {
                closer?.initializeNode()
                farther?.initializeNode()
            }
        } else {
            if (points.size > capacity) {
                threshold = distanceCalcFunction(vantagePoint, sortedPoints()[points.size / 2])
                when (val firstPastThreshold =
                    partitionPoints(vantagePoint, sortedPoints(), threshold)) {
                    in 0 .. Int.MAX_VALUE -> {
                        closer = LocationTreeNode(sortedPoints().subList(0, firstPastThreshold).toMutableList(), capacity, distanceCalcFunction)
                        farther = LocationTreeNode(sortedPoints().subList(firstPastThreshold, points.size).toMutableList(), capacity, distanceCalcFunction)
                        points.clear()
                    }
                    else -> {
                        closer = null
                        farther = null
                    }
                }
            }
        }
    }

    fun size(): Int {
        return if (this.points.isEmpty()) {
            (closer?.size() ?: 0) + (farther?.size() ?: 0)
        } else {
            this.points.size
        }
    }

    fun add(point: Point) {
        if (points.isEmpty()) {
            getChildNodeForPoint(point)?.add(point)
        } else {
            points.add(point)
        }
    }

    fun remove(point: Point): Boolean {
        return if (points.isEmpty()) {
            getChildNodeForPoint(point)?.remove(point) ?: false
        } else {
            points.remove(point)
        }
    }

    fun collectNearestNeighbors(collector: NearestNeighborCollector) {
        if (sortedPoints().isEmpty()) {
            val firstNodeSearched = getChildNodeForPoint(collector.queryPoint)
            firstNodeSearched?.collectNearestNeighbors(collector)

            val distanceFromVantagePointToQueryPoint = distanceCalcFunction(vantagePoint, collector.queryPoint) //TurfMeasurement.distance(vantagePoint, collector.queryPoint, TurfConstants.UNIT_METERS)
            val distanceFromQueryPointToFarthestPoint = if (collector.getFarthestPoint() != null) {
                distanceCalcFunction(collector.queryPoint, collector.getFarthestPoint()!!)
            } else {
                Double.MAX_VALUE
            }

            if (firstNodeSearched == closer) {
                val distanceFromQueryPointToThreshold = threshold - distanceFromVantagePointToQueryPoint

                if (distanceFromQueryPointToFarthestPoint > distanceFromQueryPointToThreshold) {
                    farther?.collectNearestNeighbors(collector)
                }
            } else {
                val distanceFromQueryPointToThreshold = distanceFromVantagePointToQueryPoint - threshold

                if (distanceFromQueryPointToThreshold <= distanceFromQueryPointToFarthestPoint) {
                    closer?.collectNearestNeighbors(collector)
                }
            }
        } else {
            sortedPoints().forEach {
                collector.offerPoint(it)
            }
        }
    }

    private fun getChildNodeForPoint(point: Point): LocationTreeNode? {
        return if (distanceCalcFunction(vantagePoint, point)  <= threshold) {
            closer
        } else {
            farther
        }
    }

    private fun addAllPointsToCollection(collection: MutableList<Point>) {
        if (points.isEmpty()) {
            closer?.addAllPointsToCollection(collection)
            farther?.addAllPointsToCollection(collection)
        } else {
            collection.addAll(points)
        }
    }

    private fun partitionPoints(vantagePoint: Point, points: List<Point>, threshold: Double): Int {
        return points.map {
            Pair(it, distanceCalcFunction(vantagePoint, it))
        }.sortedBy { it.second }.indexOfFirst { it.second > threshold }
    }
}
