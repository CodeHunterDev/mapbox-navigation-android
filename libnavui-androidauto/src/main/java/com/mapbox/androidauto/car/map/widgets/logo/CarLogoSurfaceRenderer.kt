package com.mapbox.androidauto.car.map.widgets.logo

import com.mapbox.androidauto.car.internal.extensions.getStyle
import com.mapbox.androidauto.car.internal.extensions.getStyleAsync
import com.mapbox.maps.LayerPosition
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.androidauto.MapboxCarMapObserver
import com.mapbox.maps.extension.androidauto.MapboxCarMapSurface

@OptIn(MapboxExperimental::class)
class CarLogoSurfaceRenderer(
    private val layerPosition: LayerPosition? = null
) : MapboxCarMapObserver {

    override fun onAttached(mapboxCarMapSurface: MapboxCarMapSurface) {
        val logoWidget = LogoWidget(mapboxCarMapSurface.carContext)
        mapboxCarMapSurface.getStyleAsync {
            it.addPersistentStyleCustomLayer(
                LogoWidget.LOGO_WIDGET_LAYER_ID,
                logoWidget.host,
                layerPosition
            )
        }
    }

    override fun onDetached(mapboxCarMapSurface: MapboxCarMapSurface) {
        mapboxCarMapSurface.getStyle()?.removeStyleLayer(LogoWidget.LOGO_WIDGET_LAYER_ID)
    }
}
