package com.mapbox.navigation.ui.maps.internal.locationsearch

import com.mapbox.geojson.Point
import com.mapbox.turf.TurfMeasurement

class LocationTreeNode(private val points: MutableList<Point>, private val capacity: Int = 5) {

    private val vantagePoint: Point by lazy {
        points.random()
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
                threshold = GeoSearchingUtil.selectThreshold(vantagePoint, points)
                when (val firstPastThreshold =
                    GeoSearchingUtil.partitionPoints(vantagePoint, points, threshold)) {
                    in 0 .. Int.MAX_VALUE -> {
                        closer = LocationTreeNode(points.subList(0, firstPastThreshold), capacity)
                        farther = LocationTreeNode(points.subList(firstPastThreshold, points.size), capacity)
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
        if (points.isNotEmpty()) {
            val firstNodeSearched = getChildNodeForPoint(collector.queryPoint)
            firstNodeSearched?.collectNearestNeighbors(collector)

            val distanceFromVantagePointToQueryPoint = TurfMeasurement.distance(vantagePoint, collector.queryPoint)
            val distanceFromQueryPointToFarthestPoint = if (collector.getFarthestPoint() != null) {
                TurfMeasurement.distance(collector.queryPoint, collector.getFarthestPoint()!!)
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
            points.forEach {
                collector.offerPoint(it)
            }
        }
    }

    private fun getChildNodeForPoint(point: Point): LocationTreeNode? {
        return if (TurfMeasurement.distance(vantagePoint, point)  <= threshold) {
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

}
