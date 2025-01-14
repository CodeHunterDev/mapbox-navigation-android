package com.mapbox.androidauto.car.map.widgets.compass

import com.mapbox.androidauto.car.internal.extensions.getStyle
import com.mapbox.androidauto.car.internal.extensions.getStyleAsync
import com.mapbox.androidauto.car.map.widgets.logo.LogoWidget
import com.mapbox.maps.LayerPosition
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.androidauto.MapboxCarMapObserver
import com.mapbox.maps.extension.androidauto.MapboxCarMapSurface
import com.mapbox.maps.plugin.delegates.listeners.OnCameraChangeListener

@OptIn(MapboxExperimental::class)
class CarCompassSurfaceRenderer(
    private val layerPosition: LayerPosition? = null
) : MapboxCarMapObserver {

    private var mapboxMap: MapboxMap? = null
    private var compassWidget: CompassWidget? = null
    private val onCameraChangeListener = OnCameraChangeListener { _ ->
        mapboxMap?.cameraState?.bearing?.toFloat()?.let {
            compassWidget?.updateBearing(it)
        }
    }

    override fun onAttached(mapboxCarMapSurface: MapboxCarMapSurface) {
        val compassWidget = CompassWidget(mapboxCarMapSurface.carContext)
        val mapboxMap = mapboxCarMapSurface.mapSurface.getMapboxMap().also { mapboxMap = it }
        this.compassWidget = compassWidget
        mapboxCarMapSurface.getStyleAsync {
            it.addPersistentStyleCustomLayer(
                CompassWidget.COMPASS_WIDGET_LAYER_ID,
                compassWidget.host,
                layerPosition
            )
        }
        mapboxMap.addOnCameraChangeListener(onCameraChangeListener)
    }

    override fun onDetached(mapboxCarMapSurface: MapboxCarMapSurface) {
        mapboxCarMapSurface.mapSurface.getMapboxMap()
            .removeOnCameraChangeListener(onCameraChangeListener)
        mapboxCarMapSurface.getStyle()?.removeStyleLayer(LogoWidget.LOGO_WIDGET_LAYER_ID)
        compassWidget = null
        mapboxMap = null
    }
}
