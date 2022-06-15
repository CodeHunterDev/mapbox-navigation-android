package com.mapbox.navigation.ui.maps.internal.locationsearch

import com.mapbox.geojson.Point

class LocationTree(private val capacity: Int = 5) {

    private var rootNode: LocationTreeNode? = null

    fun size() = rootNode?.size() ?: 0

    fun isEmpty() = size() == 0

    fun add(point: Point) {
        addAll(listOf(point))
    }

    fun addAll(points: List<Point>) {
        if (rootNode == null) {
            rootNode = LocationTreeNode(points.toMutableList(), capacity)
        } else {
            if (points.isNotEmpty()) {
                points.forEach {
                    rootNode?.add(it)
                }
                rootNode?.initializeNode()
            }
        }
    }

    fun remove(point: Point) {
        removeAll(listOf(point))
    }

    fun removeAll(points: List<Point>) {
        var pointRemoved = false
        rootNode?.let { theRootNode ->
            points.forEach {
                pointRemoved = theRootNode.remove(it) || pointRemoved
            }

            if (pointRemoved) {
                theRootNode.initializeNode()
            }
        }
    }

    fun clear() {
        rootNode = null
    }

    fun getNearestNeighbor(target: Point) = getNearestNeighbors(target, 1).firstOrNull()

    fun getNearestNeighbors(target: Point, maxResults: Int): List<Point> {
        return if (rootNode == null) {
            listOf()
        } else {
            val collector = NearestNeighborCollector(target, maxResults)
            rootNode?.collectNearestNeighbors(collector)
            collector.toSortedList()
        }
    }
}
