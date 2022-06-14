package com.mapbox.navigation.examples.core

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.mapbox.android.core.FileUtils
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.android.gestures.Utils
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.extension.observable.eventdata.MapLoadingErrorEventData
import com.mapbox.maps.extension.style.layers.generated.CircleLayer
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSource
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.delegates.listeners.OnMapLoadErrorListener
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.OnMapLongClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.RouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.utils.DecodeUtils.completeGeometryToLineString
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.history.ReplayEventBase
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.examples.core.databinding.LayoutActivityRoutelineExampleBinding
import com.mapbox.navigation.ui.maps.NavigationStyles
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.TOP_LEVEL_ROUTE_LINE_LAYER_ID
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineColorResources
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import com.mapbox.navigation.utils.internal.ifNonNull
import com.mapbox.navigation.utils.internal.parallelMap
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import com.mapbox.turf.TurfMisc
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

/**
 * This class demonstrates the usage of the route line and route arrow API's. There is
 * boiler plate code for a basic navigation experience. The turn by turn navigation is simulated.
 * The route line and arrow specific code is indicated with inline comments.
 */
class MapboxRouteLineAndArrowActivity : AppCompatActivity(), OnMapLongClickListener {
    private val routeClickPadding = Utils.dpToPx(30f)
    private val ONE_HUNDRED_MILLISECONDS = 100
    private val mapboxReplayer = MapboxReplayer()
    private val replayRouteMapper = ReplayRouteMapper()
    private val replayProgressObserver = ReplayProgressObserver(mapboxReplayer)
    private var trafficGradientSoft = false

    private val viewBinding: LayoutActivityRoutelineExampleBinding by lazy {
        LayoutActivityRoutelineExampleBinding.inflate(layoutInflater)
    }

    private val mapboxMap: MapboxMap by lazy {
        viewBinding.mapView.getMapboxMap()
    }

    private val navigationLocationProvider by lazy {
        NavigationLocationProvider()
    }

    private val locationComponent by lazy {
        viewBinding.mapView.location.apply {
            setLocationProvider(navigationLocationProvider)
            enabled = true
        }
    }

    private val mapboxNavigation by lazy {
        MapboxNavigationProvider.create(
            NavigationOptions.Builder(this)
                .accessToken(getMapboxAccessTokenFromResources())
                .locationEngine(ReplayLocationEngine(mapboxReplayer))
                .build()
        )
    }

    private val mapCamera by lazy {
        viewBinding.mapView.camera
    }

    // RouteLine: Route line related colors can be customized via the RouteLineColorResources.
    private val routeLineColorResources by lazy {
        RouteLineColorResources.Builder()
            .routeLineTraveledColor(Color.LTGRAY)
            .routeLineTraveledCasingColor(Color.GRAY)
            .build()
    }

    // RouteLine: Various route line related options can be customized here including applying
    // route line color customizations. If using the default colors the RouteLineColorResources
    // does not need to be set as seen here, the defaults will be used internally by the builder.
    private val routeLineResources: RouteLineResources by lazy {
        RouteLineResources.Builder()
            .routeLineColorResources(routeLineColorResources)
            .build()
    }

    // RouteLine: Additional route line options are available through the MapboxRouteLineOptions.
    // Notice here the withRouteLineBelowLayerId option. The map is made up of layers. In this
    // case the route line will be placed below the "road-label" layer which is a good default
    // for the most common Mapbox navigation related maps. You should consider if this should be
    // changed for your use case especially if you are using a custom map style.
    //
    // Also noteworthy is the 'withVanishingRouteLineEnabled' option. This feature will change
    // the color of the route line behind the puck during navigation. The color can be customized
    // using the RouteLineColorResources::routeLineTraveledColor and
    // RouteLineColorResources::routeLineTraveledCasingColor options. The color options support
    // an alpha value to render the line transparent which is the default.
    //
    // To use the vanishing route line feature it is also necessary to register an
    // OnIndicatorPositionChangedListener and a RouteProgressObserver. There may be reasons to use
    // a RouteProgressObserver even if you are not using the vanishing route line feature.
    // The OnIndicatorPositionChangedListener is only useful and required when enabling the
    // vanishing route line feature.
    //
    // Examples are below.
    private val options: MapboxRouteLineOptions by lazy {
        MapboxRouteLineOptions.Builder(this)
            .withRouteLineResources(routeLineResources)
            .withRouteLineBelowLayerId("road-label-navigation")
            .withVanishingRouteLineEnabled(false)
            //.displaySoftGradientForTraffic(trafficGradientSoft)
            .build()
    }

    // RouteLine: This class is responsible for rendering route line related mutations generated
    // by the MapboxRouteLineApi class.
    private val routeLineView by lazy {
        MapboxRouteLineView(options)
    }

    // RouteLine: This class is responsible for generating route line related data which must be
    // rendered by the MapboxRouteLineView class in order to visualize the route line on the map.
    private val routeLineApi: MapboxRouteLineApi by lazy {
        MapboxRouteLineApi(options)
    }

    // RouteArrow: This class is responsible for generating data related to maneuver arrows. The
    // data generated must be rendered by the MapboxRouteArrowView in order to apply mutations to
    // the map.
    private val routeArrowApi: MapboxRouteArrowApi by lazy {
        MapboxRouteArrowApi()
    }

    // RouteArrow: Customization of the maneuver arrow(s) can be done using the
    // RouteArrowOptions. Here the above layer ID is used to determine where in the map layer
    // stack the arrows appear. Above the layer of the route traffic line is being used here. Your
    // use case may necessitate adjusting this to a different layer position.
    private val routeArrowOptions by lazy {
        RouteArrowOptions.Builder(this)
            .withAboveLayerId(TOP_LEVEL_ROUTE_LINE_LAYER_ID)
            .build()
    }

    // RouteArrow: This class is responsible for rendering the arrow related mutations generated
    // by the MapboxRouteArrowApi class.
    private val routeArrowView: MapboxRouteArrowView by lazy {
        MapboxRouteArrowView(routeArrowOptions)
    }

    var indexCounterZZ = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        init()

        viewBinding.moveDown.setOnClickListener {
            redCircleYPos += 5f
            updateCHMImage(
                redCircleXPos,
                redCircleYPos
            )
            Log.e("foobar", "circle x pos $redCircleXPos y pos $redCircleYPos")
        }

        viewBinding.moveLeft.setOnClickListener {
            redCircleXPos -= 5f
            updateCHMImage(
                redCircleXPos,
                redCircleYPos
            )
            Log.e("foobar", "circle x pos $redCircleXPos y pos $redCircleYPos")
        }

        viewBinding.moveRight.setOnClickListener {
            redCircleXPos += 5f
            updateCHMImage(
                redCircleXPos,
                redCircleYPos
            )
            Log.e("foobar", "circle x pos $redCircleXPos y pos $redCircleYPos")
        }

        viewBinding.moveUp.setOnClickListener {
            redCircleYPos -= 5f
            updateCHMImage(
                redCircleXPos,
                redCircleYPos
            )
            Log.e("foobar", "circle x pos $redCircleXPos y pos $redCircleYPos")
        }

        viewBinding.btnDo.setOnClickListener {
            // viewBinding.mapImage.invalidate()
            //
            // val mapImage = BitmapFactory.decodeResource(
            //     getResources(),
            //     R.drawable.xbo3
            // ).copy(Bitmap.Config.ARGB_8888, true)
            //
            //
            // val canvas = Canvas(mapImage)
            // val paint = Paint().apply {
            //     isAntiAlias = true
            //     color = Color.parseColor("#00ff0000")
            //     style = Paint.Style.FILL
            // }
            //
            // //val m: Matrix = viewBinding.mapImage.getImageMatrix()
            // //val drawableRect = RectF(0f, 0f, 606f, 540f)
            // //val viewRect = RectF(0f, 0f, bmWidth, bmHeight)
            // //m.setRectToRect(drawableRect, viewRect, Matrix.ScaleToFit.CENTER)
            //
            //
            // canvas.drawBitmap(mapImage, 0f, 0f, null)
            // canvas.drawCircle(redCircleXPos, (viewBinding.mapImage.height / 2).toFloat() + redCircleYPos, 50f, paint)
            // viewBinding.mapImage.setImageBitmap(mapImage)



            // if (redCircleXPos >= 700) {
            //     redCircleYPos -= 20f
            // }

            if (indexCounterZZ < pointToPixelMapRevised.size) {
                updateCHMImage(
                    //redCircleXPos,
                    //redCircleYPos
                    pointToPixelMapRevised[indexCounterZZ].second.first,
                    pointToPixelMapRevised[indexCounterZZ].second.second
                )
                redCircleXPos = pointToPixelMapRevised[indexCounterZZ].second.first
                redCircleYPos = pointToPixelMapRevised[indexCounterZZ].second.second
                indexCounterZZ++
            }

            Log.e("foobar", "circle x pos $redCircleXPos y pos $redCircleYPos  point= ${pointToPixelMapRevised[indexCounterZZ].first}")
            // locationRecorderOn = !locationRecorderOn
            // if (!locationRecorderOn) {
            //     printLocationRecordings()
            // }
        }


        viewBinding.btnDimCHM.setOnClickListener {
            if (viewBinding.mapImage.isVisible) {
                viewBinding.mapImage.visibility = View.INVISIBLE
            } else {
                viewBinding.mapImage.visibility = View.VISIBLE
            }




        //     InternalJobControlFactory.createDefaultScopeJobControl().scope.launch {
        //         val minStart = async {
        //             fillerCoordinates.mapIndexed { index, point ->
        //                 Pair(index, TurfMeasurement.distance(point, Point.fromLngLat(135.256397, 34.443042), TurfConstants.UNIT_METERS))
        //             }.minByOrNull { it.second }
        //         }
        //
        //         val minEnd = async {
        //             fillerCoordinates.mapIndexed { index, point ->
        //                 Pair(index, TurfMeasurement.distance(point, Point.fromLngLat(135.293218, 34.415231), TurfConstants.UNIT_METERS))
        //             }.minByOrNull { it.second }
        //         }
        //
        //         val ms = minStart.await()
        //         val me = minEnd.await()
        //
        //         ifNonNull(ms, me) { m1, m2 ->
        //             val totalDist = TurfMeasurement.distance(fillerCoordinates[m1.first], fillerCoordinates[m2.first], TurfConstants.UNIT_METERS)
        //             val indexDiff = m2.first - m1.first // 255
        //             Log.e("foobar", "$m1, $m2, $indexDiff")
        //
        //             val y = 350
        //             var counter = 0
        //             for (i in (m1.first..m2.first)) {
        //                 Log.e("foobar", "${fillerCoordinates[i]} -- x=185 y=${y + counter++}")
        //
        //             }
        //         }
        //     }
        }

        viewBinding.startNavigation.visibility = View.VISIBLE
    }

    fun updateCHMImage(x: Float, y: Float) {
        viewBinding.mapImage.invalidate()

        val mapImage = BitmapFactory.decodeResource(
            getResources(),
            R.drawable.xbo3
        ).copy(Bitmap.Config.ARGB_8888, true)


        val canvas = Canvas(mapImage)
        val paint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#55ffcc00")
            style = Paint.Style.FILL
        }

        // this was to make up for a mistake in the original y value
        val yShim = 0//742

        canvas.drawBitmap(mapImage, 0f, 0f, null)
        canvas.drawCircle(x, yShim + y, 50f, paint)
        viewBinding.mapImage.setImageBitmap(mapImage)

        //Log.e("foobar", "imageHeight div2 is ${viewBinding.mapImage.height / 2} y = $y")
    }

    // var lastCHMUpdate = 0
    // val updateThreshold = 1000
    fun updateCHM(currentPoint: Point) {

        //val lastUpdateDelta = System.currentTimeMillis() - lastCHMUpdate
        if (viewBinding.mapImage.visibility == View.VISIBLE) {

            //ifNonNull(routeProgress.currentLegProgress?.currentStepProgress?.stepPoints?.first()) { currentPoint ->
                //not optimized
                job.scope.launch {

                    val closestPointDef = async {
                        pointToPixelMap.parallelMap({
                            val dist = TurfMeasurement.distance(
                                it.first,
                                currentPoint,
                                TurfConstants.UNIT_METERS
                            )
                            Pair(it, dist)
                        }, this).minByOrNull { it.second }
                    }

                    Log.e("foobar", "distance from current point to closest point ${TurfMeasurement.distance(currentPoint, pointToPixelMap[1].first, TurfConstants.UNIT_METERS)}")

                    ifNonNull(closestPointDef.await()) { closestPointer ->
                        if (closestPointer.second <= 47) {
                            updateCHMImage(closestPointer.first.second.first.toFloat(), closestPointer.first.second.second.toFloat() + 25)
                            Log.e("foobar", "would update chm with ${closestPointer.first.second.first} , ${closestPointer.first.second.second}")
                        }
                    }
                }
                //val pointToPixelMap.minByOrNull { TurfMeasurement.distance(it.first, currentPoint, TurfConstants.UNIT_METERS) }
            //}
        }
        //lastCHMUpdate = System.currentTimeMillis()
    }
    //135.256397, 34.443042  y=350 x=185   , 135.293218, 34.415231  y=450 x=185

    var redCircleYPos = 350f
    var redCircleXPos = 185f
    val bmWidth = 1080f
    val bmHeight = 1485f

    private fun init() {
        initGradientSelector()
        initNavigation()
        initStyle()
        initListeners()
        locationComponent.locationPuck = LocationPuck2D(
            null,
            ContextCompat.getDrawable(
                this@MapboxRouteLineAndArrowActivity,
                R.drawable.mapbox_navigation_puck_icon
            ),
            null,
            null
        )
    }

    private fun initGradientSelector() {
        viewBinding.gradientOptionHard.setOnClickListener {
            trafficGradientSoft = false
        }
        viewBinding.gradientOptionSoft.setOnClickListener {
            trafficGradientSoft = true
        }
    }

    // RouteLine: This is one way to keep the route(s) appearing on the map in sync with
    // MapboxNavigation. When this observer is called the route data is used to draw route(s)
    // on the map.
    private val routesObserver: RoutesObserver = RoutesObserver { result ->
        // RouteLine: wrap the DirectionRoute objects and pass them
        // to the MapboxRouteLineApi to generate the data necessary to draw the route(s)
        // on the map.
        val routeLines = result.routes.map { RouteLine(it, null) }

        routeLineApi.setRoutes(
            routeLines
        ) { value ->
            // RouteLine: The MapboxRouteLineView expects a non-null reference to the map style.
            // the data generated by the call to the MapboxRouteLineApi above must be rendered
            // by the MapboxRouteLineView in order to visualize the changes on the map.
            mapboxMap.getStyle()?.apply {
                routeLineView.renderRouteDrawData(this, value)
            }
        }
    }

    // RouteLine: This listener is necessary only when enabling the vanishing route line feature
    // which changes the color of the route line behind the puck during navigation. If this
    // option is set to `false` (the default) in MapboxRouteLineOptions then it is not necessary
    // to use this listener.
    private val onPositionChangedListener = OnIndicatorPositionChangedListener { point ->
        val result = routeLineApi.updateTraveledRouteLine(point)
        mapboxMap.getStyle()?.apply {
            // Render the result to update the map.
            routeLineView.renderRouteLineUpdate(this, result)
        }
    }

    private val routeProgressObserver = RouteProgressObserver { routeProgress ->
        // RouteLine: This line is only necessary if the vanishing route line feature
        // is enabled.
        routeLineApi.updateWithRouteProgress(routeProgress) { result ->
            mapboxMap.getStyle()?.apply {
                routeLineView.renderRouteLineUpdate(this, result)
            }
        }

        // RouteArrow: The next maneuver arrows are driven by route progress events.
        // Generate the next maneuver arrow update data and pass it to the view class
        // to visualize the updates on the map.
        val arrowUpdate = routeArrowApi.addUpcomingManeuverArrow(routeProgress)
        mapboxMap.getStyle()?.apply {
            // Render the result to update the map.
            routeArrowView.renderManeuverUpdate(this, arrowUpdate)
        }
    }

    // RouteLine: Below is a demonstration of selecting different routes. On a map click, a call
    // will be made to look for a route line on the map based on the map touch point. If a route is
    // found and it is not already the primary route, the selected route will designated the primary
    // route and MapboxNavigation will be updated.
    private val mapClickListener = OnMapClickListener { point ->
        mapboxMap.getStyle()?.apply {
            // Since this listener is reacting to all map touches, if the primary and alternative
            // routes aren't visible it's assumed the touch isn't related to selecting an
            // alternative route.
            val primaryLineVisibility = routeLineView.getPrimaryRouteVisibility(this)
            val alternativeRouteLinesVisibility = routeLineView.getAlternativeRoutesVisibility(this)
            if (
                primaryLineVisibility == Visibility.VISIBLE &&
                alternativeRouteLinesVisibility == Visibility.VISIBLE
            ) {
                routeLineApi.findClosestRoute(
                    point,
                    mapboxMap,
                    routeClickPadding
                ) { result ->
                    result.onValue { value ->
                        if (value.route != routeLineApi.getPrimaryRoute()) {
                            val reOrderedRoutes = routeLineApi.getRoutes()
                                .filter { it != value.route }
                                .toMutableList()
                                .also {
                                    it.add(0, value.route)
                                }
                            mapboxNavigation.setRoutes(reOrderedRoutes)
                        }
                    }
                }
            }
        }

        false
    }

    @SuppressLint("MissingPermission")
    private fun initNavigation() {
        mapboxNavigation.registerLocationObserver(locationObserver)
        mapboxNavigation.registerRouteProgressObserver(replayProgressObserver)
        mapboxNavigation.registerRoutesObserver(routesObserver)

        // The lines below are related to the navigation simulator.
        mapboxReplayer.pushRealLocation(this, 0.0)
        mapboxReplayer.playbackSpeed(4.0)
        mapboxReplayer.play()
    }

    private val locationObserver = object : LocationObserver {
        override fun onNewRawLocation(rawLocation: Location) {}
        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            navigationLocationProvider.changePosition(
                locationMatcherResult.enhancedLocation,
                locationMatcherResult.keyPoints,
            )
            updateCamera(locationMatcherResult.enhancedLocation)
            //updateCHM(Point.fromLngLat(locationMatcherResult.enhancedLocation.longitude, locationMatcherResult.enhancedLocation.latitude))
            //recordLocation(Point.fromLngLat(locationMatcherResult.enhancedLocation.longitude, locationMatcherResult.enhancedLocation.latitude))
            val currentLocPoint = Point.fromLngLat(locationMatcherResult.enhancedLocation.longitude, locationMatcherResult.enhancedLocation.latitude)

            job.scope.launch(Dispatchers.Main) {
                getClosestPixelCoords(currentLocPoint)?.apply {
                    updateCHMImage(this.first, this.second)
                }
            }
        }
    }

    private fun updateCamera(location: Location) {
        val mapAnimationOptionsBuilder = MapAnimationOptions.Builder()
        mapCamera.easeTo(
            CameraOptions.Builder()
                .center(Point.fromLngLat(location.longitude, location.latitude))
                .bearing(location.bearing.toDouble())
                .pitch(45.0)
                .zoom(17.0)
                .padding(EdgeInsets(1000.0, 0.0, 0.0, 0.0))
                .build(),
            mapAnimationOptionsBuilder.build()
        )
    }

    private fun getMapboxAccessTokenFromResources(): String {
        return getString(this.resources.getIdentifier("mapbox_access_token", "string", packageName))
    }

    @SuppressLint("MissingPermission")
    private fun initStyle() {
        mapboxMap.loadStyleUri(
            NavigationStyles.NAVIGATION_DAY_STYLE,
            { style: Style ->
                // Get the last known location and move the map to that location.
                // mapboxNavigation.navigationOptions.locationEngine.getLastLocation(
                //     locationEngineCallback
                // )
                //viewBinding.mapView.gestures.addOnMapLongClickListener(this)
                val route = loadRoute()
                val routeOrigin = getRouteOrigin(route)
                val cameraOptions = CameraOptions.Builder().center(routeOrigin).zoom(14.0).build()
                mapboxMap.setCamera(cameraOptions)
                mapboxNavigation.setRoutes(listOf(route))

                initPointLayer(style)
                initPointHighlightLayer(style)
                //addPointToPixelMapPoints(style)
                //addPointToPixelMapPoints(recordedPoints)
                addPointToPixelMapPoints(preRecordedPoints)

                val pointToHighlightIndex = 76
                highLightPointOnMap(preRecordedPoints[pointToHighlightIndex])
                Log.e("foobar", "highlighted point is at ${preRecordedPoints[pointToHighlightIndex]}")

                redCircleXPos = pointToPixelMapRevised.last().second.first
                redCircleYPos = pointToPixelMapRevised.last().second.second
                updateCHMImage(redCircleXPos, redCircleYPos)
            },
            object : OnMapLoadErrorListener {
                override fun onMapLoadError(eventData: MapLoadingErrorEventData) {
                    Log.e(
                        MapboxRouteLineAndArrowActivity::class.java.simpleName,
                        "Error loading map - error type: " +
                            "${eventData.type}, message: ${eventData.message}"
                    )
                }
            }
        )
    }

    override fun onMapLongClick(point: Point): Boolean {
        vibrate()
        viewBinding.startNavigation.visibility = View.GONE
        viewBinding.optionTrafficGradient.visibility = View.GONE
        val currentLocation = navigationLocationProvider.lastLocation
        if (currentLocation != null) {
            val originPoint = Point.fromLngLat(
                currentLocation.longitude,
                currentLocation.latitude
            )
            findRoute(originPoint, point)
            viewBinding.routeLoadingProgressBar.visibility = View.VISIBLE
        }
        return false
    }

    fun findRoute(origin: Point?, destination: Point?) {
        val routeOptions = RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .applyLanguageAndVoiceUnitOptions(this)
            .coordinatesList(listOf(origin, destination))
            .layersList(listOf(mapboxNavigation.getZLevel(), null))
            .alternatives(true)
            .build()
        mapboxNavigation.requestRoutes(
            routeOptions,
            routesReqCallback
        )
    }

    private val routesReqCallback: RouterCallback = object : RouterCallback {
        override fun onRoutesReady(routes: List<DirectionsRoute>, routerOrigin: RouterOrigin) {
            mapboxNavigation.setRoutes(routes)
            if (routes.isNotEmpty()) {
                viewBinding.routeLoadingProgressBar.visibility = View.INVISIBLE
                viewBinding.startNavigation.visibility = View.VISIBLE
            }
        }

        override fun onCanceled(routeOptions: RouteOptions, routerOrigin: RouterOrigin) {
            viewBinding.routeLoadingProgressBar.visibility = View.INVISIBLE
        }

        override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {
            viewBinding.routeLoadingProgressBar.visibility = View.INVISIBLE
        }
    }

    @SuppressLint("MissingPermission")
    private fun vibrate() {
        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    ONE_HUNDRED_MILLISECONDS.toLong(),
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            vibrator.vibrate(ONE_HUNDRED_MILLISECONDS.toLong())
        }
    }

    @SuppressLint("MissingPermission")
    private fun initListeners() {
        viewBinding.startNavigation.setOnClickListener {
            val route = mapboxNavigation.getRoutes().firstOrNull()
            if (route != null) {
                mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
                mapboxNavigation.startTripSession()
                viewBinding.startNavigation.visibility = View.INVISIBLE
                locationComponent.addOnIndicatorPositionChangedListener(onPositionChangedListener)

                // RouteLine: Hiding the alternative routes when navigation starts.
                mapboxMap.getStyle()?.apply {
                    routeLineView.hideAlternativeRoutes(this)
                }

                startSimulation(route)
            }
        }
        viewBinding.mapView.gestures.addOnMapClickListener(mapClickListener)
    }

    // Starts the navigation simulator
    private fun startSimulation(route: DirectionsRoute) {
        mapboxReplayer.stop()
        mapboxReplayer.clearEvents()
        val replayData: List<ReplayEventBase> = replayRouteMapper.mapDirectionsRouteGeometry(route)
        mapboxReplayer.pushEvents(replayData)
        mapboxReplayer.seekTo(replayData[0])
        mapboxReplayer.play()
    }

    override fun onStart() {
        super.onStart()
        mapboxNavigation.registerLocationObserver(locationObserver)
        mapboxNavigation.registerRouteProgressObserver(replayProgressObserver)
        mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.registerRoutesObserver(routesObserver)
    }

    override fun onStop() {
        super.onStop()
        locationComponent.removeOnIndicatorPositionChangedListener(onPositionChangedListener)
        mapboxNavigation.unregisterRouteProgressObserver(replayProgressObserver)
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.unregisterLocationObserver(locationObserver)
        mapboxNavigation.unregisterRoutesObserver(routesObserver)
    }

    override fun onDestroy() {
        super.onDestroy()
        routeLineApi.cancel()
        routeLineView.cancel()
        mapboxReplayer.finish()
        mapboxNavigation.onDestroy()
    }

    private val locationEngineCallback = MyLocationEngineCallback(this)

    private class MyLocationEngineCallback(activity: MapboxRouteLineAndArrowActivity) :
        LocationEngineCallback<LocationEngineResult> {

        private val activityRef: WeakReference<MapboxRouteLineAndArrowActivity> by lazy {
            WeakReference(activity)
        }

        override fun onSuccess(result: LocationEngineResult?) {
            val location = result?.lastLocation
            val activity = activityRef.get()
            if (location != null && activity != null) {
                val point = Point.fromLngLat(location.longitude, location.latitude)
                val cameraOptions = CameraOptions.Builder().center(point).zoom(13.0).build()
                activity.mapboxMap.setCamera(cameraOptions)
                activity.navigationLocationProvider.changePosition(location, listOf(), null, null)
            }
        }

        override fun onFailure(exception: Exception) {
        }
    }

    private val fillerCoordinates: List<Point> by lazy {
        getRouteFillerCoordinates(loadRoute())
    }

    fun getRouteOrigin(route: DirectionsRoute) =
        route.completeGeometryToLineString().coordinates().first()

    fun loadRoute(): DirectionsRoute {
        val routeAsJson = readRawFileText(this, R.raw.temp_delete_me_route)
        return DirectionsRoute.fromJson(routeAsJson)
    }

    fun readRawFileText(context: Context, res: Int): String =
        context.resources.openRawResource(res).bufferedReader().use { it.readText() }

    fun getRouteFillerCoordinates(route: DirectionsRoute): List<Point> {
        val distThreshold = 76
        val routeGeometry = route.completeGeometryToLineString()
        val newPoints = mutableListOf<Point>(routeGeometry.coordinates().first())
        var endPointIndex = 1
        while (endPointIndex < routeGeometry.coordinates().size) {

            val lastFillPoint = getFillPoint(newPoints.last(), routeGeometry.coordinates()[endPointIndex])
            newPoints.add(lastFillPoint)
            if (TurfMeasurement.distance(lastFillPoint, routeGeometry.coordinates()[endPointIndex], TurfConstants.UNIT_METERS) <= distThreshold) {
                endPointIndex++
            }
        }
        return newPoints
    }

    private tailrec fun getFillPoint(startPoint: Point, endPoint: Point): Point {
        // rough estimate 60 mph is roughly 26 meters per second
        // the rational being route progress is every second
        val distThreshold = 76
        return if (TurfMeasurement.distance(startPoint, endPoint, TurfConstants.UNIT_METERS) <= distThreshold) {
            endPoint
        } else {
            getFillPoint(startPoint, TurfMeasurement.midpoint(startPoint, endPoint))
        }
    }

    private val job by lazy {
        InternalJobControlFactory.createDefaultScopeJobControl()
    }

    private val pointToPixelMapRevised: List<Pair<Point, Pair<Float, Float>>> by lazy {
        listOf(
            Pair(Point.fromLngLat(135.26354888814373, 34.438500471880225), Pair(190.0f, 1095.0f)),
            Pair(Point.fromLngLat(135.26729722216783, 34.43489747681673), Pair(190.0f, 1106.875f)),
            Pair(Point.fromLngLat(135.27143269522125, 34.43169180777495), Pair(190.0f, 1118.75f)),
            Pair(Point.fromLngLat(135.27550312908502, 34.428617005239126), Pair(190.0f, 1130.625f)),
            Pair(Point.fromLngLat(135.2795732634925, 34.42554206785929), Pair(190.0f, 1142.5f)),
            Pair(Point.fromLngLat(135.28364309848703, 34.422466995660905), Pair(190.0f, 1154.375f)),
            Pair(Point.fromLngLat(135.28771263411198, 34.41939178866937), Pair(190.0f, 1166.25f)),
            Pair(Point.fromLngLat(135.2917818704107, 34.41631644691015), Pair(190.0f, 1178.125f)),
            Pair(Point.fromLngLat(135.296376, 34.413709), Pair(190.0f, 1193.0f)),

            Pair(Point.fromLngLat(135.30153571191934, 34.415502500803086), Pair(205.0f, 1193.0f)),
            Pair(Point.fromLngLat(135.30698698267128, 34.41623354196272), Pair(220.0f, 1193.0f)),
            Pair(Point.fromLngLat(135.31254604370773, 34.4171395936453), Pair(235.0f, 1193.0f)),
            Pair(Point.fromLngLat(135.31770275656007, 34.41913698290124), Pair(250.0f, 1193.0f)),
            Pair(Point.fromLngLat(135.32209657672965, 34.42211206480083), Pair(265.0f, 1193.0f)),
            Pair(Point.fromLngLat(135.3261666280631, 34.425452525714384), Pair(280.0f, 1193.0f)),
            Pair(Point.fromLngLat(135.32978210516993, 34.42912703967447), Pair(295.0f, 1193.0f)),
            Pair(Point.fromLngLat(135.33271005055866, 34.432921488449374), Pair(310.0f, 1193.0f)),
            Pair(Point.fromLngLat(135.33541607553065, 34.436993669875044), Pair(325.0f, 1193.0f)),
            Pair(Point.fromLngLat(135.3375814382039, 34.44120845855389), Pair(340.0f, 1193.0f)),
            Pair(Point.fromLngLat(135.34233757614805, 34.443772467101724), Pair(355.0f, 1193.0f)),
            Pair(Point.fromLngLat(135.34757166120423, 34.44538980221373), Pair(370.0f, 1193.0f)),
            Pair(Point.fromLngLat(135.35172072901352, 34.448411693460926), Pair(385.0f, 1193.0f)),
            Pair(Point.fromLngLat(135.35491999308022, 34.45231132854109), Pair(400.0f, 1193.0f)),
            Pair(Point.fromLngLat(135.35781935472207, 34.4561750433119), Pair(415.0f, 1193.0f)),
            Pair(Point.fromLngLat(135.36071005673364, 34.46024379720794), Pair(430.0f, 1193.0f)),
            Pair(Point.fromLngLat(135.3638212126413, 34.46419186540004), Pair(445.0f, 1193.0f)),
            Pair(Point.fromLngLat(135.367274, 34.467891), Pair(460.0f, 1193.0f)),
            Pair(Point.fromLngLat(135.372093, 34.470181), Pair(475.0f, 1193.0f)),
            Pair(Point.fromLngLat(135.37556553078954, 34.47393702541184), Pair(490.0f, 1193.0f)),
            Pair(Point.fromLngLat(135.37763756696697, 34.478165135886364), Pair(505.0f, 1193.0f)),
            Pair(Point.fromLngLat(135.37727658607497, 34.482674474856374), Pair(520.0f, 1193.0f)),
            Pair(Point.fromLngLat(135.37846799417682, 34.48708282150821), Pair(535.0f, 1193.0f)),
            Pair(Point.fromLngLat(135.37987166989663, 34.49167308855809), Pair(550.0f, 1193.0f)),
            Pair(Point.fromLngLat(135.38125883185123, 34.496287127330405), Pair(565.0f, 1193.0f)),
            Pair(Point.fromLngLat(135.38411024257616, 34.50033566876203), Pair(580.0f, 1193.0f)),
            Pair(Point.fromLngLat(135.38836542646942, 34.5031906513975), Pair(595.0f, 1193.0f)),
            Pair(Point.fromLngLat(135.3924609932627, 34.506199709364644), Pair(610.0f, 1193.0f)),
            Pair(Point.fromLngLat(135.3972101816279, 34.5086433829396), Pair(625.0f, 1193.0f)),
            Pair(Point.fromLngLat(135.4027267055564, 34.509666720826445), Pair(640.0f, 1193.0f)),
            Pair(Point.fromLngLat(135.40787501454807, 34.51170531036246), Pair(655.0f, 1193.0f)),
            Pair(Point.fromLngLat(135.41265382949538, 34.5138982810049), Pair(670.0f, 1193.0f)),
            Pair(Point.fromLngLat(135.4166698623002, 34.51715286622886), Pair(685.0f, 1193.0f)),

            Pair(Point.fromLngLat(135.42004521533246, 34.52091406849284), Pair(690.0f, 1188.6875f)),
            Pair(Point.fromLngLat(135.42372098475894, 34.524549437135775), Pair(695.0f, 1183.375f)),
            Pair(Point.fromLngLat(135.42613014645548, 34.528744338318134), Pair(700.0f, 1178.0625f)),
            Pair(Point.fromLngLat(135.428662, 34.532777), Pair(705.0f, 1172.75f)),
            Pair(Point.fromLngLat(135.431641957322, 34.536730710529525), Pair(710.0f, 1167.4375f)),
            Pair(Point.fromLngLat(135.43440708500913, 34.54061624668257), Pair(715.0f, 1162.125f)),
            Pair(Point.fromLngLat(135.43693382241486, 34.544743206388254), Pair(720.0f, 1159.8125f)),
            Pair(Point.fromLngLat(135.43980844776218, 34.5487473008449), Pair(725.0f, 1154.5f)),
            Pair(Point.fromLngLat(135.4437509959282, 34.55204672901478), Pair(730.0f, 1150.1875f)),
            Pair(Point.fromLngLat(135.4453917319862, 34.55643045317768), Pair(735.0f, 1144.875f)),
            Pair(Point.fromLngLat(135.44884249890123, 34.56006836407924), Pair(740.0f, 1139.5625f)),
            Pair(Point.fromLngLat(135.452953, 34.563207), Pair(745.0f, 1128.25f)),
            Pair(Point.fromLngLat(135.45539642245308, 34.56730267405715), Pair(750.0f, 1133.9375f)),
            Pair(Point.fromLngLat(135.45706721763298, 34.57168874601794), Pair(755.0f, 1127.625f)),
            Pair(Point.fromLngLat(135.45859594523313, 34.576176275243675), Pair(760.0f, 1120.3125f)),
            Pair(Point.fromLngLat(135.45800853419814, 34.58073834209151), Pair(765.0f, 1115.0f)),
            Pair(Point.fromLngLat(135.46075849587007, 34.58480882568937), Pair(770.0f, 1109.6875f)),
            Pair(Point.fromLngLat(135.46355467379263, 34.588697943636475), Pair(775.0f, 1104.375f)),
            Pair(Point.fromLngLat(135.46332605505924, 34.5933506955903), Pair(780.0f, 1101.0625f)),
            Pair(Point.fromLngLat(135.462748, 34.597988), Pair(785.0f, 1089.75f)),
            Pair(Point.fromLngLat(135.461395, 34.602538), Pair(790.0f, 1082.4375f)),
            Pair(Point.fromLngLat(135.45759276884212, 34.605769057839105), Pair(795.0f, 1077.125f)),
            Pair(Point.fromLngLat(135.4532119777108, 34.608573951285244), Pair(800.0f, 1071.8125f)),
            Pair(Point.fromLngLat(135.44794307119042, 34.61045284132922), Pair(805.0f, 1066.5f)),
            Pair(Point.fromLngLat(135.44252060051764, 34.61196288140486), Pair(810.0f, 1061.1875f)),
            Pair(Point.fromLngLat(135.43716885965827, 34.61344655043803), Pair(815.0f, 1055.875f)),
            Pair(Point.fromLngLat(135.43541857873296, 34.617869471779855), Pair(820.0f, 1050.5625f)),
            Pair(Point.fromLngLat(135.43366052803458, 34.62238046669404), Pair(825.0f, 1044.25f)),
            Pair(Point.fromLngLat(135.4320750816063, 34.62669964452119), Pair(830.0f, 1039.9375f)),
            Pair(Point.fromLngLat(135.434718, 34.630798), Pair(835.0f, 1034.625f)),
            Pair(Point.fromLngLat(135.43422546876656, 34.63542926719152), Pair(840.0f, 1029.3125f)),
            Pair(Point.fromLngLat(135.4351268908143, 34.64010685015972), Pair(845.0f, 1024.0f)),

            Pair(Point.fromLngLat(135.43767350436244, 34.644094148606506), Pair(845.0f, 1015.0f)),
            Pair(Point.fromLngLat(135.44034202514675, 34.64827498171898), Pair(845.0f, 1010.0f)),
            Pair(Point.fromLngLat(135.442021, 34.652583), Pair(845.0f, 1005.0f)),


        )
    }

    private val pointToPixelMap: List<Pair<Point, Pair<Float, Float>>> by lazy {
        listOf(
            Pair(Point.fromLngLat(135.26354888814373, 34.438500471880225), Pair(185f, 350.0f)),
            Pair(Point.fromLngLat(135.26729722216783, 34.43489747681673), Pair(185f, 361.11f)),
            Pair(Point.fromLngLat(135.27143269522125, 34.43169180777495), Pair(185f, 372.11f)),
            Pair(Point.fromLngLat(135.27550312908502, 34.428617005239126), Pair(185f, 383.22f)),
            Pair(Point.fromLngLat(135.2795732634925, 34.42554206785929), Pair(185f, 394.33f)),
            Pair(Point.fromLngLat(135.28364309848703, 34.422466995660905), Pair(185f, 405.44f)),
            Pair(Point.fromLngLat(135.28771263411198, 34.41939178866937), Pair(185f, 416.55f)),
            Pair(Point.fromLngLat(135.2917818704107, 34.41631644691015), Pair(185f, 427.66f)),
            Pair(Point.fromLngLat(135.296376, 34.413709), Pair(185f, 438.77f)),
            Pair(Point.fromLngLat(135.30153571191934, 34.415502500803086), Pair(185f, 449.88f)),
            //
            Pair(Point.fromLngLat(135.30698698267128, 34.41623354196272), Pair(200f, 449f)),
            Pair(Point.fromLngLat(135.31254604370773, 34.4171395936453), Pair(215f, 449f)),
            Pair(Point.fromLngLat(135.31770275656007, 34.41913698290124), Pair(230f, 449f)),
            Pair(Point.fromLngLat(135.32209657672965, 34.42211206480083), Pair(245f, 449f)),
            Pair(Point.fromLngLat(135.3261666280631, 34.425452525714384), Pair(260f, 449f)),
            Pair(Point.fromLngLat(135.32978210516993, 34.42912703967447), Pair(275f, 449f)),
            Pair(Point.fromLngLat(135.33271005055866, 34.432921488449374), Pair(290f, 449f)),
            Pair(Point.fromLngLat(135.33541607553065, 34.436993669875044), Pair(305f, 449f)),
            Pair(Point.fromLngLat(135.3375814382039, 34.44120845855389), Pair(320f, 449f)),
            Pair(Point.fromLngLat(135.34233757614805, 34.443772467101724), Pair(335f, 449f)),
            Pair(Point.fromLngLat(135.34757166120423, 34.44538980221373), Pair(350f, 449f)),
            Pair(Point.fromLngLat(135.35172072901352, 34.448411693460926), Pair(365f, 449f)),
            Pair(Point.fromLngLat(135.35491999308022, 34.45231132854109), Pair(380f, 449f)),
            Pair(Point.fromLngLat(135.35781935472207, 34.4561750433119), Pair(395f, 449f)),
            Pair(Point.fromLngLat(135.36071005673364, 34.46024379720794), Pair(410f, 449f)),
            Pair(Point.fromLngLat(135.3638212126413, 34.46419186540004), Pair(425f, 449f)),
            Pair(Point.fromLngLat(135.367274, 34.467891), Pair(440f, 449f)),
            Pair(Point.fromLngLat(135.372093, 34.470181), Pair(455f, 449f)),
            Pair(Point.fromLngLat(135.37556553078954, 34.47393702541184), Pair(455f, 449f)),
            Pair(Point.fromLngLat(135.37763756696697, 34.478165135886364), Pair(470f, 449f)),
            Pair(Point.fromLngLat(135.37727658607497, 34.482674474856374), Pair(485f, 449f)),
            Pair(Point.fromLngLat(135.37846799417682, 34.48708282150821), Pair(500f, 449f)),
            Pair(Point.fromLngLat(135.37987166989663, 34.49167308855809), Pair(515f, 449f)),
            Pair(Point.fromLngLat(135.38125883185123, 34.496287127330405), Pair(530f, 449f)),
            Pair(Point.fromLngLat(135.38411024257616, 34.50033566876203), Pair(545f, 449f)),
            Pair(Point.fromLngLat(135.38836542646942, 34.5031906513975), Pair(560f, 449f)),
            Pair(Point.fromLngLat(135.3924609932627, 34.506199709364644), Pair(575f, 449f)),
            Pair(Point.fromLngLat(135.3972101816279, 34.5086433829396), Pair(590f, 449f)),
            Pair(Point.fromLngLat(135.4027267055564, 34.509666720826445), Pair(605f, 449f)),
            Pair(Point.fromLngLat(135.40787501454807, 34.51170531036246), Pair(620f, 449f)),
            Pair(Point.fromLngLat(135.41265382949538, 34.5138982810049), Pair(635f, 440f)),
            Pair(Point.fromLngLat(135.4166698623002, 34.51715286622886), Pair(650f, 449f)),
            Pair(Point.fromLngLat(135.42004521533246, 34.52091406849284), Pair(665f, 449f)),

            Pair(Point.fromLngLat(135.42372098475894, 34.524549437135775), Pair(673f, 443.5f)),
            Pair(Point.fromLngLat(135.42613014645548, 34.528744338318134), Pair(681f, 438f)),
            Pair(Point.fromLngLat(135.428662, 34.532777), Pair(689f, 432.5f)),
            Pair(Point.fromLngLat(135.431641957322, 34.536730710529525), Pair(697f, 427f)),
            Pair(Point.fromLngLat(135.43440708500913, 34.54061624668257), Pair(705f, 421.5f)),
            Pair(Point.fromLngLat(135.43693382241486, 34.544743206388254), Pair(713f, 416f)),
            Pair(Point.fromLngLat(135.43980844776218, 34.5487473008449), Pair(721f, 410.5f)),
            Pair(Point.fromLngLat(135.4437509959282, 34.55204672901478), Pair(729f, 410.5f)),
            Pair(Point.fromLngLat(135.4453917319862, 34.55643045317768), Pair(737f, 399.5f)),
            Pair(Point.fromLngLat(135.44884249890123, 34.56006836407924), Pair(745f, 394.5f)),
            Pair(Point.fromLngLat(135.452953, 34.563207), Pair(753f, 388.5f)),
            Pair(Point.fromLngLat(135.45539642245308, 34.56730267405715), Pair(761f, 383f)),
            Pair(Point.fromLngLat(135.45706721763298, 34.57168874601794), Pair(769f, 377.5f)),
            Pair(Point.fromLngLat(135.45859594523313, 34.576176275243675), Pair(777f, 372f)),
            Pair(Point.fromLngLat(135.45800853419814, 34.58073834209151), Pair(785f, 366.5f)),
            Pair(Point.fromLngLat(135.46075849587007, 34.58480882568937), Pair(793f, 361f)),
            Pair(Point.fromLngLat(135.46355467379263, 34.588697943636475), Pair(801f, 355.5f)),
            Pair(Point.fromLngLat(135.46332605505924, 34.5933506955903), Pair(809f, 350f)),
            Pair(Point.fromLngLat(135.462748, 34.597988), Pair(817f, 344.5f)),
            Pair(Point.fromLngLat(135.461395, 34.602538), Pair(825f, 339f)),


            Pair(Point.fromLngLat(135.45759276884212, 34.605769057839105), Pair(830.66f, 315.7f)),
            Pair(Point.fromLngLat(135.4532119777108, 34.608573951285244), Pair(837.32f, 292.4f)),
            Pair(Point.fromLngLat(135.44794307119042, 34.61045284132922), Pair(845f, 269f)),

            Pair(Point.fromLngLat(135.44252060051764, 34.61196288140486), Pair(820f, 269f)),
            Pair(Point.fromLngLat(135.43716885965827, 34.61344655043803), Pair(795f, 269f)),



            Pair(Point.fromLngLat(135.43541857873296, 34.617869471779855), Pair(795f, 249f)),
            Pair(Point.fromLngLat(135.43366052803458, 34.62238046669404),  Pair(795f, 229f)),
            Pair(Point.fromLngLat(135.4320750816063, 34.62669964452119),  Pair(795f, 209f)),
            Pair(Point.fromLngLat(135.434718, 34.630798),  Pair(795f, 189f)),
            Pair(Point.fromLngLat(135.43422546876656, 34.63542926719152),  Pair(795f, 169f)),
            Pair(Point.fromLngLat(135.4351268908143, 34.64010685015972),  Pair(795f, 149f)),
            Pair(Point.fromLngLat(135.43767350436244, 34.644094148606506),  Pair(795f, 129f)),
            Pair(Point.fromLngLat(135.44034202514675, 34.64827498171898),  Pair(795f, 109f)),
            Pair(Point.fromLngLat(135.442021, 34.652583),  Pair(795f, 89f)),
            Pair(Point.fromLngLat(135.43772220525543, 34.65545767075316),  Pair(795f, 69f)),
            Pair(Point.fromLngLat(135.43353995208128, 34.65855613855892),  Pair(795f, 49f)),
            Pair(Point.fromLngLat(135.4297624373702, 34.66185494520802),  Pair(795f, 29f)),
            Pair(Point.fromLngLat(135.42491943782437, 34.66418327103825),  Pair(795f, 9f)),
            Pair(Point.fromLngLat(135.42147644754107, 34.66775765160598),  Pair(795f, -11f)),
            Pair(Point.fromLngLat(135.4195353901103, 34.67207416532465),  Pair(795f, -31f)),
            Pair(Point.fromLngLat(135.41764367699446, 34.67632796131387),  Pair(795f, -51f)),


            Pair(Point.fromLngLat(135.4157477905149, 34.68059935614752), Pair(790f, -59.75f)),
            Pair(Point.fromLngLat(135.41385170843316, 34.68487072162431), Pair(785f, -68.5f)),
            Pair(Point.fromLngLat(135.41180633403846, 34.689051000154784), Pair(780f, -77.25f)),
            Pair(Point.fromLngLat(135.4085670017836, 34.692791000730736), Pair(775f, -86f)),
            Pair(Point.fromLngLat(135.404270772023, 34.695821784837534), Pair(770f, -94.75f)),
            Pair(Point.fromLngLat(135.3991044234251, 34.69760153842241), Pair(765f, -103.5f)),
            Pair(Point.fromLngLat(135.39349178318247, 34.697239280802066), Pair(760f, -112.5f)),
            Pair(Point.fromLngLat(135.3883840375837, 34.69519621571538), Pair(755f, -121.5f)),


            Pair(Point.fromLngLat(135.3829833028119, 34.69657143040748), Pair(755f, -121.5f)),
            Pair(Point.fromLngLat(135.37722733967075, 34.69664972496831), Pair(755f, -121.5f)),
            Pair(Point.fromLngLat(135.371554, 34.696653), Pair(745f, -121.5f)),
            Pair(Point.fromLngLat(135.36593296958566, 34.69719920699183), Pair(735f, -121.5f)),
            Pair(Point.fromLngLat(135.3610502018518, 34.69947474891815), Pair(725f, -121.5f)),
            Pair(Point.fromLngLat(135.3572323119788, 34.7027147418947), Pair(715f, -121.5f)),
            Pair(Point.fromLngLat(135.3533519043536, 34.70599524171251), Pair(705f, -121.5f)),
            Pair(Point.fromLngLat(135.34948350387725, 34.70922216606029), Pair(695f, -121.5f)),
            Pair(Point.fromLngLat(135.345449662829, 34.7124811215806), Pair(685f, -121.5f)),
            Pair(Point.fromLngLat(135.34034559842812, 34.71457425994662), Pair(675f, -121.5f)),
            Pair(Point.fromLngLat(135.3350472167275, 34.716153403764714), Pair(665f, -121.5f)),
            Pair(Point.fromLngLat(135.329447, 34.716838), Pair(655f, -121.5f)),
            Pair(Point.fromLngLat(135.323797380371, 34.715918074978), Pair(645f, -121.5f)),
            Pair(Point.fromLngLat(135.31840699243008, 34.714852860921994), Pair(635f, -121.5f)),
            Pair(Point.fromLngLat(135.31306830864108, 34.71379943256003), Pair(625f, -121.5f)),
            Pair(Point.fromLngLat(135.3074546944639, 34.71269502012801), Pair(615f, -121.5f)),
            Pair(Point.fromLngLat(135.3019416263332, 34.71167600316799), Pair(605f, -121.5f)),
            Pair(Point.fromLngLat(135.296405, 34.710871), Pair(595f, -121.5f)),
            Pair(Point.fromLngLat(135.2908352921629, 34.70974754427635), Pair(585f, -121.5f)),
            Pair(Point.fromLngLat(135.28526861044196, 34.70860456528489), Pair(575f, -121.5f)),


            Pair(Point.fromLngLat(135.27954719458808, 34.70792658266585), Pair(565f, -121.5f)),
            Pair(Point.fromLngLat(135.2745724594273, 34.70588858834762), Pair(565f, -107.67f)),
            Pair(Point.fromLngLat(135.27358603414842, 34.70134176784736), Pair(565f, -94.34f)),
            Pair(Point.fromLngLat(135.2732434287031, 34.69668412888626), Pair(565f, -81.5f))
        )
    }

    private suspend fun getClosestPixelCoords(point: Point): Pair<Float, Float>? {
        return coroutineScope {
            val startTime = System.currentTimeMillis()
            val distancesDef = async(Dispatchers.Default) {
                pointToPixelMap.map {
                    val dist = TurfMeasurement.distance(point, it.first, TurfConstants.UNIT_METERS)
                    //Log.e("foobar", "distance from $point to ${it.first} is $dist")
                    Pair(dist, it.second)
                }
            }

            // val distances = pointToPixelMap.parallelMap({
            //     val dist = TurfMeasurement.distance(point, it.first, TurfConstants.UNIT_METERS)
            //     Pair(dist, it.second)
            // }, job.scope)

            Log.e("foobar", "Time to get closest = ${System.currentTimeMillis() - startTime}")
            val distances = distancesDef.await()
            val closestCoordsDef = async(Dispatchers.Default) {
                distances.minByOrNull { it.first }?.second
            }
            closestCoordsDef.await()?.also {
                Log.e("foobar", "returning coords x=${it.first} y=${it.second}")
            }
        }
    }

    private val preRecordedPoints by lazy {
        listOf(
            // Point.fromLngLat(135.263263, 34.43838),
            // Point.fromLngLat(135.2592237703461, 34.44150361991096),
            Point.fromLngLat(135.26354888814373, 34.438500471880225),
            Point.fromLngLat(135.26729722216783, 34.43489747681673),
            Point.fromLngLat(135.27143269522125, 34.43169180777495),
            Point.fromLngLat(135.27550312908502, 34.428617005239126),
            Point.fromLngLat(135.2795732634925, 34.42554206785929),
            Point.fromLngLat(135.28364309848703, 34.422466995660905),
            Point.fromLngLat(135.28771263411198, 34.41939178866937),
            Point.fromLngLat(135.2917818704107, 34.41631644691015),
            Point.fromLngLat(135.296376, 34.413709),
            Point.fromLngLat(135.30153571191934, 34.415502500803086),
            Point.fromLngLat(135.30698698267128, 34.41623354196272),
            Point.fromLngLat(135.31254604370773, 34.4171395936453),
            Point.fromLngLat(135.31770275656007, 34.41913698290124),
            Point.fromLngLat(135.32209657672965, 34.42211206480083),
            Point.fromLngLat(135.3261666280631, 34.425452525714384),
            Point.fromLngLat(135.32978210516993, 34.42912703967447),
            Point.fromLngLat(135.33271005055866, 34.432921488449374),
            Point.fromLngLat(135.33541607553065, 34.436993669875044),
            Point.fromLngLat(135.3375814382039, 34.44120845855389),
            Point.fromLngLat(135.34233757614805, 34.443772467101724),
            Point.fromLngLat(135.34757166120423, 34.44538980221373),
            Point.fromLngLat(135.35172072901352, 34.448411693460926),
            Point.fromLngLat(135.35491999308022, 34.45231132854109),
            Point.fromLngLat(135.35781935472207, 34.4561750433119),
            Point.fromLngLat(135.36071005673364, 34.46024379720794),
            Point.fromLngLat(135.3638212126413, 34.46419186540004),
            Point.fromLngLat(135.367274, 34.467891),
            Point.fromLngLat(135.372093, 34.470181),
            Point.fromLngLat(135.37556553078954, 34.47393702541184),
            Point.fromLngLat(135.37763756696697, 34.478165135886364),
            Point.fromLngLat(135.37727658607497, 34.482674474856374),
            Point.fromLngLat(135.37846799417682, 34.48708282150821),
            Point.fromLngLat(135.37987166989663, 34.49167308855809),
            Point.fromLngLat(135.38125883185123, 34.496287127330405),
            Point.fromLngLat(135.38411024257616, 34.50033566876203),
            Point.fromLngLat(135.38836542646942, 34.5031906513975),
            Point.fromLngLat(135.3924609932627, 34.506199709364644),
            Point.fromLngLat(135.3972101816279, 34.5086433829396),
            Point.fromLngLat(135.4027267055564, 34.509666720826445),
            Point.fromLngLat(135.40787501454807, 34.51170531036246),
            Point.fromLngLat(135.41265382949538, 34.5138982810049),
            Point.fromLngLat(135.4166698623002, 34.51715286622886),
            Point.fromLngLat(135.42004521533246, 34.52091406849284),
            Point.fromLngLat(135.42372098475894, 34.524549437135775),
            Point.fromLngLat(135.42613014645548, 34.528744338318134),
            Point.fromLngLat(135.428662, 34.532777),
            Point.fromLngLat(135.431641957322, 34.536730710529525),
            Point.fromLngLat(135.43440708500913, 34.54061624668257),
            Point.fromLngLat(135.43693382241486, 34.544743206388254),
            Point.fromLngLat(135.43980844776218, 34.5487473008449),
            Point.fromLngLat(135.4437509959282, 34.55204672901478),
            Point.fromLngLat(135.4453917319862, 34.55643045317768),
            Point.fromLngLat(135.44884249890123, 34.56006836407924),
            Point.fromLngLat(135.452953, 34.563207),
            Point.fromLngLat(135.45539642245308, 34.56730267405715),
            Point.fromLngLat(135.45706721763298, 34.57168874601794),
            Point.fromLngLat(135.45859594523313, 34.576176275243675),
            Point.fromLngLat(135.45800853419814, 34.58073834209151),
            Point.fromLngLat(135.46075849587007, 34.58480882568937),
            Point.fromLngLat(135.46355467379263, 34.588697943636475),
            Point.fromLngLat(135.46332605505924, 34.5933506955903),
            Point.fromLngLat(135.462748, 34.597988),
            Point.fromLngLat(135.461395, 34.602538),
            Point.fromLngLat(135.45759276884212, 34.605769057839105),
            Point.fromLngLat(135.4532119777108, 34.608573951285244),
            Point.fromLngLat(135.44794307119042, 34.61045284132922),
            Point.fromLngLat(135.44252060051764, 34.61196288140486),
            Point.fromLngLat(135.43716885965827, 34.61344655043803),
            Point.fromLngLat(135.43541857873296, 34.617869471779855),
            Point.fromLngLat(135.43366052803458, 34.62238046669404),
            Point.fromLngLat(135.4320750816063, 34.62669964452119),
            Point.fromLngLat(135.434718, 34.630798),
            Point.fromLngLat(135.43422546876656, 34.63542926719152),
            Point.fromLngLat(135.4351268908143, 34.64010685015972),
            Point.fromLngLat(135.43767350436244, 34.644094148606506),
            Point.fromLngLat(135.44034202514675, 34.64827498171898),
            Point.fromLngLat(135.442021, 34.652583),
            Point.fromLngLat(135.43772220525543, 34.65545767075316),
            Point.fromLngLat(135.43353995208128, 34.65855613855892),
            Point.fromLngLat(135.4297624373702, 34.66185494520802),
            Point.fromLngLat(135.42491943782437, 34.66418327103825),
            Point.fromLngLat(135.42147644754107, 34.66775765160598),
            Point.fromLngLat(135.4195353901103, 34.67207416532465),
            Point.fromLngLat(135.41764367699446, 34.67632796131387),
            Point.fromLngLat(135.4157477905149, 34.68059935614752),
            Point.fromLngLat(135.41385170843316, 34.68487072162431),
            Point.fromLngLat(135.41180633403846, 34.689051000154784),
            Point.fromLngLat(135.4085670017836, 34.692791000730736),
            Point.fromLngLat(135.404270772023, 34.695821784837534),
            Point.fromLngLat(135.3991044234251, 34.69760153842241),
            Point.fromLngLat(135.39349178318247, 34.697239280802066),
            Point.fromLngLat(135.3883840375837, 34.69519621571538),
            Point.fromLngLat(135.3829833028119, 34.69657143040748),
            Point.fromLngLat(135.37722733967075, 34.69664972496831),
            Point.fromLngLat(135.371554, 34.696653),
            Point.fromLngLat(135.36593296958566, 34.69719920699183),
            Point.fromLngLat(135.3610502018518, 34.69947474891815),
            Point.fromLngLat(135.3572323119788, 34.7027147418947),
            Point.fromLngLat(135.3533519043536, 34.70599524171251),
            Point.fromLngLat(135.34948350387725, 34.70922216606029),
            Point.fromLngLat(135.345449662829, 34.7124811215806),
            Point.fromLngLat(135.34034559842812, 34.71457425994662),
            Point.fromLngLat(135.3350472167275, 34.716153403764714),
            Point.fromLngLat(135.329447, 34.716838),
            Point.fromLngLat(135.323797380371, 34.715918074978),
            Point.fromLngLat(135.31840699243008, 34.714852860921994),
            Point.fromLngLat(135.31306830864108, 34.71379943256003),
            Point.fromLngLat(135.3074546944639, 34.71269502012801),
            Point.fromLngLat(135.3019416263332, 34.71167600316799),
            Point.fromLngLat(135.296405, 34.710871),
            Point.fromLngLat(135.2908352921629, 34.70974754427635),
            Point.fromLngLat(135.28526861044196, 34.70860456528489),
            Point.fromLngLat(135.27954719458808, 34.70792658266585),
            Point.fromLngLat(135.2745724594273, 34.70588858834762),
            Point.fromLngLat(135.27358603414842, 34.70134176784736),
            Point.fromLngLat(135.2732434287031, 34.69668412888626),
        )
    }

    private val LINE_END_LAYER_ID = "DRAW_UTIL_LINE_END_LAYER_ID"
    private val LINE_END_SOURCE_ID = "DRAW_UTIL_LINE_END_SOURCE_ID"
    private fun initPointLayer(style: Style) {
        if (!style.styleSourceExists(LINE_END_SOURCE_ID)) {
            geoJsonSource(LINE_END_SOURCE_ID) {}.bindTo(style)
        }

        if (!style.styleLayerExists(LINE_END_LAYER_ID)) {
            CircleLayer(LINE_END_LAYER_ID, LINE_END_SOURCE_ID)
                .circleRadius(5.0)
                .circleOpacity(1.0)
                .circleColor(Color.BLACK)
                .bindTo(style)
        }
    }


    private val POINT_HIGHLIGHT_LAYER_ID = "POINT_HIGHLIGHT_LAYER_ID"
    private val POINT_HIGHLIGHT_SOURCE_ID = "POINT_HIGHLIGHT_SOURCE_ID"
    private fun initPointHighlightLayer(style: Style) {
        if (!style.styleSourceExists(POINT_HIGHLIGHT_LAYER_ID)) {
            geoJsonSource(POINT_HIGHLIGHT_SOURCE_ID) {}.bindTo(style)
        }

        if (!style.styleLayerExists(POINT_HIGHLIGHT_LAYER_ID)) {
            CircleLayer(POINT_HIGHLIGHT_LAYER_ID, POINT_HIGHLIGHT_SOURCE_ID)
                .circleRadius(5.0)
                .circleOpacity(1.0)
                .circleColor(Color.RED)
                .bindTo(style)
        }
    }

    private fun highLightPointOnMap(point: Point) {
        (mapboxMap.getStyle()!!.getSource(POINT_HIGHLIGHT_SOURCE_ID) as GeoJsonSource).apply {
            this.featureCollection(FeatureCollection.fromFeatures(listOf(Feature.fromGeometry(point))))
        }
    }

    private fun addPointToPixelMapPoints(points: List<Point>) {
        val features = points.map { Feature.fromGeometry(it) }

        (mapboxMap.getStyle()!!.getSource(LINE_END_SOURCE_ID) as GeoJsonSource).apply {
            this.featureCollection(FeatureCollection.fromFeatures(features))
        }
    }

    private fun recordLocation(point: Point) {
        if (locationRecorderOn) {
            if (locationRecordings.isEmpty()) {
                locationRecordings.add(point)
            } else {
                val dist = TurfMeasurement.distance(locationRecordings.last(), point, TurfConstants.UNIT_METERS)
                if (dist >= 300.0) {
                    locationRecordings.add(point)
                }
            }
            updateRecordingsOnMap()
        }
    }

    private fun updateRecordingsOnMap() {
        val features = locationRecordings.map { Feature.fromGeometry(it) }
        (mapboxMap.getStyle()!!.getSource(LINE_END_SOURCE_ID) as GeoJsonSource).apply {
            this.featureCollection(FeatureCollection.fromFeatures(features))
        }
    }
    var locationRecordings = mutableListOf<Point>()
    var locationRecorderOn = false

    private fun printLocationRecordings() {
        locationRecordings.forEach {
            Log.e("foobar", "Point.fromLngLat(${it.longitude()}, ${it.latitude()}),")
        }
    }
}
