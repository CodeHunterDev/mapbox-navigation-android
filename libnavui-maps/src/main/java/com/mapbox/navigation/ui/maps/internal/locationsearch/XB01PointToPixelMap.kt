package com.mapbox.navigation.ui.maps.internal.locationsearch

import com.mapbox.geojson.Feature
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.turf.TurfMeasurement
import com.mapbox.turf.TurfMisc

object XB01PointToPixelMap {
    // Tokyo

    // CHM bounds
    /*
     Point.fromLngLat(139.4967988, 35.9198808), // north west
     Point.fromLngLat(140.121725, 35.9198808), // north east
     Point.fromLngLat(140.121725, 35.3366116), // south east
     Point.fromLngLat(139.4967988, 35.3366116), // south west
     */

    fun getBounds(): Feature {
        return Feature.fromGeometry(
            LineString.fromLngLats(
            listOf(
                Point.fromLngLat(139.4967988, 35.9198808), // north west
                Point.fromLngLat(140.121725, 35.9198808), // north east
                Point.fromLngLat(140.121725, 35.3366116), // south east
                Point.fromLngLat(139.4967988, 35.3366116), // south west
                Point.fromLngLat(139.4967988, 35.9198808) // close to north west
            )
        ))
    }

    fun getPointCollections(): List<List<EnhancedPoint>> = listOf(
//        pointToPixelForInterpolation,
//        pointToPixelForInterpolation2,
        //pointToPixelForInterpolation3,
        pointToPixelForInterpolation4,
//        pointToPixelForInterpolation5,
        pointToPixelForInterpolation6
    )

    val pointToPixelForInterpolation6: List<EnhancedPoint> by lazy {
        listOf(
            EnhancedPoint.KeyPoint(Point.fromLngLat(140.100278, 35.660428), Pair(1500f, 850f)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.10463461168874, 35.657588988740166)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.1070033792565, 35.65327807400259)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.10875080530894, 35.64899722692373)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.11115853198558, 35.644808765972535)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.11469594034148, 35.641269385956086)),
            EnhancedPoint.KeyPoint(Point.fromLngLat(140.121707, 35.635674), Pair(1650f, 1000f)),
        )
    }

    val pointToPixelForInterpolation5: List<EnhancedPoint> by lazy {
        listOf(
            EnhancedPoint.KeyPoint(Point.fromLngLat(139.7957892431928, 35.63947950042642), Pair(980f, 935f)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.79278366760937, 35.6433180002178)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.7896157573235, 35.64731316188828)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.78615644387276, 35.65090638810382)),
            EnhancedPoint.KeyPoint(Point.fromLngLat(139.783036, 35.653966), Pair(955f, 910f))
        )
    }

//1500.0 y pos 850.0  //intersection with keiyo
    val pointToPixelForInterpolation4: List<EnhancedPoint> by lazy {
        listOf(
            EnhancedPoint.KeyPoint(Point.fromLngLat(139.947825, 35.687562), Pair(1350f, 850f)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.9523903794291, 35.69035021680864)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.95750301889106, 35.69247997413077)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.963261, 35.692137)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.9685439403147, 35.69073784219612)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.9738429865759, 35.689360336027065)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.97916941022171, 35.68796229193639)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.98449538673054, 35.686563359013064)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.98979146607022, 35.68513554545122)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.9948164527124, 35.68291167450362)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.999693, 35.680527)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.00284035657378, 35.6767829472764)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.0054077878199, 35.672799074325106)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.01001788578822, 35.66989421440224)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.0145102917547, 35.667180873468986)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.019013, 35.664479)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.023738, 35.662031)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.02882506357946, 35.66014062883558)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.03430114933755, 35.658903210672406)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.039297, 35.656781)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.04403923654306, 35.65402339176295)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.04899983474888, 35.651641203977675)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.05436747681992, 35.65001168361462)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.05981535387707, 35.64904384745562)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.06495986993568, 35.64737077080455)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.06905280960177, 35.64419324048409)),
            EnhancedPoint.KeyPoint(Point.fromLngLat(140.07390507058378, 35.641753258694145), Pair(1435f, 920f)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.079322, 35.643043)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.08340182663466, 35.64629439506633)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.0874605228988, 35.649589727616394)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.09158147882124, 35.65290436558827)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.096272, 35.655642)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.09990680652754, 35.65928294339027)), //Pair(1500f, 850f)
            EnhancedPoint.MapPoint(Point.fromLngLat(140.102555518523, 35.66336307195332)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.107858, 35.664959)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.112443157269, 35.66760291726735)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.116887, 35.670398)),
            EnhancedPoint.KeyPoint(Point.fromLngLat(140.12226484989444, 35.67158599876446), Pair(1660f, 705f)),
        )
    }

    val pointToPixelForInterpolation3: List<EnhancedPoint> by lazy {
        listOf(
            EnhancedPoint.KeyPoint(Point.fromLngLat(139.791604, 35.518599), Pair(900f, 1285f)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.79259286633683, 35.522118743286704)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.7937975115769, 35.52659277472807)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.794945, 35.531116)),
            EnhancedPoint.KeyPoint(Point.fromLngLat(139.79518644955266, 35.53568859680539), Pair(945f, 1235f)), //
            EnhancedPoint.MapPoint(Point.fromLngLat(139.7933241282795, 35.54002452456237)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.7904125200011, 35.544154480508745)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.787578, 35.548143)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.78468442103204, 35.55220929146595)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.781805, 35.55622)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.778096, 35.559565)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.77327848652308, 35.562125523153696)),
            EnhancedPoint.KeyPoint(Point.fromLngLat(139.76831568356857, 35.56458216456649), Pair(945f, 1175f)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.76338922216834, 35.56698856054812)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.759688, 35.570365)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.7582347430038, 35.57482679507058)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.75683426102853, 35.57935484754131)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.75544118362265, 35.583974575004675)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.75519182152706, 35.58872269568587)),
            EnhancedPoint.KeyPoint(Point.fromLngLat(139.75516406677778, 35.59348096017464), Pair(885f, 1115f)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.75522, 35.598239)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.75526448248064, 35.6028200370654)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.7552467982209, 35.607488055391514)),
            EnhancedPoint.KeyPoint(Point.fromLngLat(139.75637812050815, 35.61201000010181), Pair(885f, 1040f)), //
            EnhancedPoint.MapPoint(Point.fromLngLat(139.76035792494918, 35.61535942293978)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.7644156677166, 35.61847345847887)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.76847372648552, 35.621587357956685)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.772524, 35.624708)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.7772625917183, 35.627494680704544)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.782108078483, 35.630122178100834)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.78686191307582, 35.632691412315914)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.79168540516255, 35.635321426553176)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.796358940977, 35.63784439924975)), //intersection to small exp way near chuo
            EnhancedPoint.MapPoint(Point.fromLngLat(139.801164, 35.640413)),
            EnhancedPoint.KeyPoint(Point.fromLngLat(139.80648869375642, 35.642347950017026), Pair(1005f, 928f)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.81192072953598, 35.64409215050457)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.81716599286477, 35.64570253504851)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.82274506022458, 35.646309935123064)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.82842886896486, 35.64684044433478)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.83418977814827, 35.64741405330116)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.8399890017795, 35.64757031643002)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.84568629291564, 35.64720482663393)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.8512409471033, 35.64687214298478)),
            EnhancedPoint.KeyPoint(Point.fromLngLat(139.85698511895322, 35.64648001565101), Pair(1170f, 928f)),//
            EnhancedPoint.MapPoint(Point.fromLngLat(139.8621992175645, 35.644892075466515)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.86727581944066, 35.64302577033544)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.87235187684783, 35.64115870024297)),
            EnhancedPoint.KeyPoint(Point.fromLngLat(139.87743660991637, 35.639307507094465), Pair(1215f, 963f)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.88265500165943, 35.637697133252324)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.8882789484418, 35.638492306050885)),
            EnhancedPoint.KeyPoint(Point.fromLngLat(139.8924423748377, 35.64150805641941), Pair(1240f, 963f)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.89636088378643, 35.644779621501904)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.900279713643, 35.648051059660276)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.90436028377263, 35.65145724094441)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.908385, 35.654818)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.91244182016555, 35.65820004829349)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.91640238791425, 35.66158787539984)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.91979012712412, 35.665142771861156)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.923197, 35.668772)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.92700027200948, 35.672196757864484)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.9308624364915, 35.6755337209248)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.93463179716977, 35.67890020114241)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.93892554405517, 35.682089702346346)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.94337886979417, 35.68483699621751)),
            EnhancedPoint.KeyPoint(Point.fromLngLat(139.9478250774336, 35.68756223045331), Pair(1350f, 850f)), //  intersection of B and Gaikan
//            EnhancedPoint.MapPoint(Point.fromLngLat(139.9524928313749, 35.69041286397442)),
//            EnhancedPoint.MapPoint(Point.fromLngLat(139.95771046778145, 35.692513612458036)),
//            EnhancedPoint.MapPoint(Point.fromLngLat(139.963261, 35.692137)),
//            EnhancedPoint.MapPoint(Point.fromLngLat(139.9685439403147, 35.69073784219612)),
//            EnhancedPoint.MapPoint(Point.fromLngLat(139.9738429865759, 35.689360336027065)),
//            EnhancedPoint.MapPoint(Point.fromLngLat(139.97916941022171, 35.68796229193639)),
//            EnhancedPoint.MapPoint(Point.fromLngLat(139.98449538673054, 35.686563359013064)),
//            EnhancedPoint.MapPoint(Point.fromLngLat(139.98979146607022, 35.68513554545122)),
//            EnhancedPoint.MapPoint(Point.fromLngLat(139.9948164527124, 35.68291167450362)),
//            EnhancedPoint.MapPoint(Point.fromLngLat(139.999693, 35.680527)),
//            EnhancedPoint.MapPoint(Point.fromLngLat(140.00284035657378, 35.6767829472764)),
//            EnhancedPoint.MapPoint(Point.fromLngLat(140.0054077878199, 35.672799074325106)),
//            EnhancedPoint.MapPoint(Point.fromLngLat(140.01001788578822, 35.66989421440224)),
//            EnhancedPoint.MapPoint(Point.fromLngLat(140.0145102917547, 35.667180873468986)),
//            EnhancedPoint.MapPoint(Point.fromLngLat(140.019013, 35.664479)),
//            EnhancedPoint.MapPoint(Point.fromLngLat(140.023738, 35.662031)),
//            EnhancedPoint.MapPoint(Point.fromLngLat(140.02882506357946, 35.66014062883558)),
//            EnhancedPoint.MapPoint(Point.fromLngLat(140.03430114933755, 35.658903210672406)),
//            EnhancedPoint.MapPoint(Point.fromLngLat(140.039297, 35.656781)),
//            EnhancedPoint.MapPoint(Point.fromLngLat(140.04403923654306, 35.65402339176295)),
//            EnhancedPoint.MapPoint(Point.fromLngLat(140.04899983474888, 35.651641203977675)),
//            EnhancedPoint.MapPoint(Point.fromLngLat(140.05436747681992, 35.65001168361462)),
//            EnhancedPoint.MapPoint(Point.fromLngLat(140.05981535387707, 35.64904384745562)),
//            EnhancedPoint.MapPoint(Point.fromLngLat(140.06495986993568, 35.64737077080455)),
//            EnhancedPoint.MapPoint(Point.fromLngLat(140.06905280960177, 35.64419324048409)),
//            EnhancedPoint.MapPoint(Point.fromLngLat(140.07390507058378, 35.641753258694145)),
//            EnhancedPoint.MapPoint(Point.fromLngLat(140.079322, 35.643043)),
//            EnhancedPoint.MapPoint(Point.fromLngLat(140.08340182663466, 35.64629439506633)),
//            EnhancedPoint.MapPoint(Point.fromLngLat(140.0874605228988, 35.649589727616394)),
//            EnhancedPoint.MapPoint(Point.fromLngLat(140.09158147882124, 35.65290436558827)),
//            EnhancedPoint.MapPoint(Point.fromLngLat(140.096272, 35.655642)),
//            EnhancedPoint.KeyPoint(Point.fromLngLat(140.099908894798, 35.65928678739391), Pair(1350f, 850f)),
        )
    }

     val pointToPixelForInterpolation2: List<EnhancedPoint> by lazy {
         listOf(
             EnhancedPoint.KeyPoint(Point.fromLngLat(139.791604, 35.518599), Pair(900f, 1285f)),
             EnhancedPoint.MapPoint(Point.fromLngLat(139.788879, 35.513586)),
             EnhancedPoint.MapPoint(Point.fromLngLat(139.7841577548979, 35.51089744654628)),
             EnhancedPoint.MapPoint(Point.fromLngLat(139.7795627166098, 35.50824390793768)),
             EnhancedPoint.MapPoint(Point.fromLngLat(139.77484664095886, 35.50563735034486)),
             EnhancedPoint.MapPoint(Point.fromLngLat(139.76991093054576, 35.503248218532995)),
             EnhancedPoint.MapPoint(Point.fromLngLat(139.7650879476831, 35.50105749938771)),
             EnhancedPoint.MapPoint(Point.fromLngLat(139.76018844130508, 35.49889903725097)),
             EnhancedPoint.MapPoint(Point.fromLngLat(139.75529319115512, 35.49673439165533)),
             EnhancedPoint.MapPoint(Point.fromLngLat(139.75040369913896, 35.494561323752606)),
             EnhancedPoint.KeyPoint(Point.fromLngLat(139.74554709088986, 35.49239190666143), Pair(780f, 1400f)),
             EnhancedPoint.MapPoint(Point.fromLngLat(139.74069722152905, 35.49023612702606)),
             EnhancedPoint.MapPoint(Point.fromLngLat(139.73575251012176, 35.48806698277547)),
             EnhancedPoint.MapPoint(Point.fromLngLat(139.73080334862232, 35.48590479191795)),
             EnhancedPoint.MapPoint(Point.fromLngLat(139.72585157916762, 35.483748309276514)),
             EnhancedPoint.MapPoint(Point.fromLngLat(139.72091456509645, 35.481594460523844)),
             EnhancedPoint.MapPoint(Point.fromLngLat(139.71598617475445, 35.47942776284659)),
             EnhancedPoint.MapPoint(Point.fromLngLat(139.71105727349163, 35.47724743067013)),
             EnhancedPoint.MapPoint(Point.fromLngLat(139.70612569936978, 35.47505047782384)),
             EnhancedPoint.MapPoint(Point.fromLngLat(139.70125045488405, 35.47287703322947)),
             EnhancedPoint.MapPoint(Point.fromLngLat(139.69614904247626, 35.47061661847929)),
             EnhancedPoint.MapPoint(Point.fromLngLat(139.69101951034972, 35.46836024298886)),
             EnhancedPoint.MapPoint(Point.fromLngLat(139.68615443846383, 35.46619835650247)),
             EnhancedPoint.KeyPoint(Point.fromLngLat(139.68147689209417, 35.463786112560186), Pair(530f, 1400f)),
             EnhancedPoint.MapPoint(Point.fromLngLat(139.67784146835402, 35.460216479910905)),
             EnhancedPoint.MapPoint(Point.fromLngLat(139.67516350902454, 35.45624345103427)),
             EnhancedPoint.MapPoint(Point.fromLngLat(139.672514259314, 35.45224881370118)),
             EnhancedPoint.MapPoint(Point.fromLngLat(139.6698825268991, 35.44829471775352)),
             EnhancedPoint.MapPoint(Point.fromLngLat(139.6675241001604, 35.444007850895495)),
             EnhancedPoint.MapPoint(Point.fromLngLat(139.66765295704207, 35.439491918782586)),
             EnhancedPoint.MapPoint(Point.fromLngLat(139.6698231709887, 35.4353523522905)),
             EnhancedPoint.MapPoint(Point.fromLngLat(139.67113642701233, 35.430935953745724)),
             EnhancedPoint.KeyPoint(Point.fromLngLat(139.67112332842987, 35.42633237426695), Pair(500f, 1400f)),
             EnhancedPoint.MapPoint(Point.fromLngLat(139.67072507603802, 35.42174384616793)),
             EnhancedPoint.MapPoint(Point.fromLngLat(139.67010401127283, 35.41717381390722)),
             EnhancedPoint.MapPoint(Point.fromLngLat(139.66616253325066, 35.41373201069654)),
             EnhancedPoint.MapPoint(Point.fromLngLat(139.66083127007414, 35.412418592840815)),
             EnhancedPoint.MapPoint(Point.fromLngLat(139.6562604080645, 35.41502880311372)),
             EnhancedPoint.MapPoint(Point.fromLngLat(139.6516438616201, 35.41782700429576)),
             EnhancedPoint.MapPoint(Point.fromLngLat(139.646062, 35.418199)),
             EnhancedPoint.MapPoint(Point.fromLngLat(139.6408223845242, 35.41665945112451)),
             EnhancedPoint.MapPoint(Point.fromLngLat(139.63533774441166, 35.415194781392394)),
             EnhancedPoint.MapPoint(Point.fromLngLat(139.63030987916412, 35.41310908167814)),
             EnhancedPoint.MapPoint(Point.fromLngLat(139.62622266358125, 35.409957000877746)),
             EnhancedPoint.MapPoint(Point.fromLngLat(139.62343840607957, 35.40600828444059)),
             EnhancedPoint.MapPoint(Point.fromLngLat(139.62062942976212, 35.401935833670684)),
             EnhancedPoint.MapPoint(Point.fromLngLat(139.61780369503916, 35.39780521133476)),
             EnhancedPoint.MapPoint(Point.fromLngLat(139.61647582721338, 35.393239624121215)),
             EnhancedPoint.MapPoint(Point.fromLngLat(139.618426, 35.388913)),
             EnhancedPoint.MapPoint(Point.fromLngLat(139.62211200051277, 35.385390333549566)),
             EnhancedPoint.MapPoint(Point.fromLngLat(139.62666925295179, 35.38271990815844)),
             EnhancedPoint.MapPoint(Point.fromLngLat(139.6319556328938, 35.38085065130036)),
             EnhancedPoint.MapPoint(Point.fromLngLat(139.63743091291872, 35.379665449012926)),
             EnhancedPoint.MapPoint(Point.fromLngLat(139.64224328289978, 35.377152712971096)),
             EnhancedPoint.MapPoint(Point.fromLngLat(139.64338441260438, 35.37269896920446)),
             EnhancedPoint.MapPoint(Point.fromLngLat(139.64350942540872, 35.3681409924804)),
             EnhancedPoint.KeyPoint(Point.fromLngLat(139.64373543912504, 35.36357418942545), Pair(450f, 1445f)),
             EnhancedPoint.MapPoint(Point.fromLngLat(139.64393378523616, 35.358857082169656)),
             EnhancedPoint.MapPoint(Point.fromLngLat(139.64416706882264, 35.35421606925415)),
             EnhancedPoint.MapPoint(Point.fromLngLat(139.6444072616972, 35.34950885693444)),
             EnhancedPoint.MapPoint(Point.fromLngLat(139.64471231935326, 35.34487475084729)),
             EnhancedPoint.KeyPoint(Point.fromLngLat(139.64468436530098, 35.34022427319847), Pair(450f, 1475f)),
         )
     }

    val pointToPixelForInterpolation: List<EnhancedPoint> by lazy {
        listOf(
            EnhancedPoint.KeyPoint(Point.fromLngLat(139.791604, 35.518599), Pair(900f, 1285f)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.79687440512663, 35.51668904475685)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.80152965349603, 35.513964812634036)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.80583687545993, 35.5110305352397)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.8101437826156, 35.508096104758536)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.81445037500376, 35.50516152121836)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.818756652665, 35.50222678464692)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.8230626156401, 35.49929189507197)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.82736826396967, 35.496356852521274)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.83167359769445, 35.49342165702257)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.83598280442143, 35.49048773715759)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.84029767860105, 35.487555704680986)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.84461223792735, 35.484623518616466)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.84892648244087, 35.48169117899189)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.85324041218226, 35.478758685835054)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.85755402719204, 35.4758260391738)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.8618673275108, 35.47289323903596)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.8661803131791, 35.46996028544934)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.87049298423747, 35.46702717844176)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.87480375713338, 35.46409237605334)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.87913907581674, 35.46119533653849)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.88346225402267, 35.45829623530968)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.88775526208119, 35.455364299198386)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.8920479572645, 35.45243221111089)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.89634033961306, 35.44949997107472)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.90063240916737, 35.44656757911739)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.90492416596788, 35.443635035266446)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.9092156100551, 35.44070233954942)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.91374762703344, 35.437950361185365)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.918648196875, 35.43588061367041)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.92317650147504, 35.432935750606745)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.9270987183858, 35.42941903824267)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.9308763029487, 35.426008200692344)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.9348705583603, 35.42284997220032)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.93959931928333, 35.42021983323077)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.94449083381068, 35.41807310406041)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.94943350807029, 35.415856306286145)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.954532, 35.4136)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.959174, 35.410904)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.96294117019883, 35.407493947671846)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.96582, 35.403493)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.96764933355004, 35.39901715803431)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.96868785032123, 35.39456771258702)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.97048852114594, 35.39012845821664)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.97344676560846, 35.38615831280797)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.9767107385583, 35.382391349153)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.97919834597246, 35.378154406386415)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.98118778952576, 35.373797034390975)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.984449, 35.369946)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.98905800044037, 35.367266667047474)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.9943627921376, 35.36577622893744)),
            EnhancedPoint.MapPoint(Point.fromLngLat(139.999428, 35.363816)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.00364590633762, 35.360847727715175)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.0083501827117, 35.35809595186307)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.01377373520697, 35.3568093052269)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.01958725496127, 35.35706012374683)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.02532277569773, 35.35773140742848)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.0309116535604, 35.357863538690204)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.03654699750254, 35.35771762176733)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.04205179498572, 35.35855422244616)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.04763354362038, 35.35921599860255)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.053327, 35.359233)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.05901440483842, 35.35912474467167)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.06459997307456, 35.3590512193889)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.07033924321615, 35.35908682881253)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.07588275326572, 35.3597574449574)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.08128673791177, 35.36101634155877)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.0868933786049, 35.36218333129008)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.09254472171568, 35.362697566347705)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.09830325764898, 35.36259580629173)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.10405092649754, 35.362314976329664)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.10960146567032, 35.362074640943234)),
            EnhancedPoint.MapPoint(Point.fromLngLat(140.11518433330278, 35.36190900050119)),
            EnhancedPoint.KeyPoint(Point.fromLngLat(140.122126, 35.363022), Pair(1090f,1470f))
        )
    }
}
