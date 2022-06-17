package com.mapbox.navigation.ui.maps.internal.locationsearch

import com.mapbox.geojson.Point

object XB03PointToPixelMap {

    // CHM bounds
    /*
     Point.fromLngLat(134.8722536, 35.0032252), // north west
     Point.fromLngLat(135.7471644, 35.0032252), // north east
     Point.fromLngLat(135.7471644, 34.253293), // south east
     Point.fromLngLat(134.8722536, 34.253293), // south west
     */

    fun getPointCollections(): List<List<EnhancedPoint>> = listOf(
        pointToPixelForInterpolation,
        pointToPixelForInterpolation2
    )

    val pointToPixelForInterpolation2: List<EnhancedPoint> by lazy {
        listOf(
            EnhancedPoint.KeyPoint(Point.fromLngLat(135.296376, 34.413709), Pair(190.0f, 1193.0f)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.301912, 34.408723)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.30630850356116, 34.40573500172152)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.31031322233918, 34.40254050180957)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.31372179888464, 34.399031719010026)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.3166981258145, 34.39507528106214)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.3194479929101, 34.391177788049355)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.32219815463566, 34.38728050721432)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.3257254450907, 34.383639177315146)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.32973159134343, 34.3802572459963)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.33226446629243, 34.37604683188518)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.33413122856064, 34.3718098826353)),
            EnhancedPoint.KeyPoint(Point.fromLngLat(135.335067, 34.367225), Pair(190f,1295f))
        )
    }

    val pointToPixelForInterpolation: List<EnhancedPoint> by lazy {
        listOf(
            EnhancedPoint.KeyPoint(Point.fromLngLat(135.26354888814373, 34.438500471880225), Pair(190.0f, 1115.0f)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.26729722216783, 34.43489747681673)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.27143269522125, 34.43169180777495)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.27550312908502, 34.428617005239126)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.2795732634925, 34.42554206785929)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.28364309848703, 34.422466995660905)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.28771263411198, 34.41939178866937)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.2917818704107, 34.41631644691015)),
            EnhancedPoint.KeyPoint(Point.fromLngLat(135.296376, 34.413709), Pair(190.0f, 1193.0f)),

            EnhancedPoint.MapPoint(Point.fromLngLat(135.30153571191934, 34.415502500803086)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.30698698267128, 34.41623354196272)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.31254604370773, 34.4171395936453)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.31770275656007, 34.41913698290124)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.32209657672965, 34.42211206480083)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.3261666280631, 34.425452525714384)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.32978210516993, 34.42912703967447)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.33271005055866, 34.432921488449374)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.33541607553065, 34.436993669875044)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.3375814382039, 34.44120845855389)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.34233757614805, 34.443772467101724)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.34757166120423, 34.44538980221373)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.35172072901352, 34.448411693460926)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.35491999308022, 34.45231132854109)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.35781935472207, 34.4561750433119)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.36071005673364, 34.46024379720794)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.3638212126413, 34.46419186540004)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.367274, 34.467891)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.372093, 34.470181)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.37556553078954, 34.47393702541184)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.37763756696697, 34.478165135886364)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.37727658607497, 34.482674474856374)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.37846799417682, 34.48708282150821)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.37987166989663, 34.49167308855809)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.38125883185123, 34.496287127330405)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.38411024257616, 34.50033566876203)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.38836542646942, 34.5031906513975)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.3924609932627, 34.506199709364644)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.3972101816279, 34.5086433829396)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.4027267055564, 34.509666720826445)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.40787501454807, 34.51170531036246)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.41265382949538, 34.5138982810049)),
            EnhancedPoint.KeyPoint(Point.fromLngLat(135.4166698623002, 34.51715286622886), Pair(685.0f, 1193.0f)),

            EnhancedPoint.MapPoint(Point.fromLngLat(135.42004521533246, 34.52091406849284)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.42372098475894, 34.524549437135775)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.42613014645548, 34.528744338318134)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.428662, 34.532777)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.431641957322, 34.536730710529525)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.43440708500913, 34.54061624668257)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.43693382241486, 34.544743206388254)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.43980844776218, 34.5487473008449)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.4437509959282, 34.55204672901478)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.4453917319862, 34.55643045317768)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.44884249890123, 34.56006836407924)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.452953, 34.563207)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.45539642245308, 34.56730267405715)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.45706721763298, 34.57168874601794)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.45859594523313, 34.576176275243675)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.45800853419814, 34.58073834209151)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.46075849587007, 34.58480882568937)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.46355467379263, 34.588697943636475)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.46332605505924, 34.5933506955903)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.462748, 34.597988)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.461395, 34.602538)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.45759276884212, 34.605769057839105)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.4532119777108, 34.608573951285244)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.44794307119042, 34.61045284132922)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.44252060051764, 34.61196288140486)),
            EnhancedPoint.KeyPoint(Point.fromLngLat(135.43716885965827, 34.61344655043803), Pair(855.0f, 1030.0f)),

            EnhancedPoint.MapPoint(Point.fromLngLat(135.43541857873296, 34.617869471779855)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.43366052803458, 34.62238046669404)),
            EnhancedPoint.KeyPoint(Point.fromLngLat(135.4320750816063, 34.62669964452119), Pair(855.0f, 1010.0f)),

            EnhancedPoint.MapPoint(Point.fromLngLat(135.434718, 34.630798)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.43422546876656, 34.63542926719152)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.4351268908143, 34.64010685015972)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.43767350436244, 34.644094148606506)),
            EnhancedPoint.KeyPoint(Point.fromLngLat(135.44034202514675, 34.64827498171898),  Pair(800f, 1010.0f)),

            EnhancedPoint.MapPoint(Point.fromLngLat(135.442021, 34.652583)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.43772220525543, 34.65545767075316)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.43353995208128, 34.65855613855892)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.4297624373702, 34.66185494520802)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.42491943782437, 34.66418327103825)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.42147644754107, 34.66775765160598)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.4195353901103, 34.67207416532465)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.41764367699446, 34.67632796131387)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.4157477905149, 34.68059935614752)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.41385170843316, 34.68487072162431)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.41180633403846, 34.689051000154784)),
            EnhancedPoint.KeyPoint(Point.fromLngLat(135.4085670017836, 34.692791000730736),  Pair(800f, 665.0f)),

            EnhancedPoint.MapPoint(Point.fromLngLat(135.404270772023, 34.695821784837534)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.3991044234251, 34.69760153842241)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.39349178318247, 34.697239280802066)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.3883840375837, 34.69519621571538)),
            EnhancedPoint.KeyPoint(Point.fromLngLat(135.3829833028119, 34.69657143040748),  Pair(760f, 620.0f)),

            EnhancedPoint.MapPoint(Point.fromLngLat(135.37722733967075, 34.69664972496831)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.371554, 34.696653)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.36593296958566, 34.69719920699183)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.3610502018518, 34.69947474891815)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.3572323119788, 34.7027147418947)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.3533519043536, 34.70599524171251)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.34948350387725, 34.70922216606029)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.345449662829, 34.7124811215806)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.34034559842812, 34.71457425994662)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.3350472167275, 34.716153403764714)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.329447, 34.716838)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.323797380371, 34.715918074978)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.31840699243008, 34.714852860921994)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.31306830864108, 34.71379943256003)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.3074546944639, 34.71269502012801)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.3019416263332, 34.71167600316799)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.296405, 34.710871)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.2908352921629, 34.70974754427635)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.28526861044196, 34.70860456528489)),
            EnhancedPoint.MapPoint(Point.fromLngLat(135.27954719458808, 34.70792658266585)),
            EnhancedPoint.KeyPoint(Point.fromLngLat(135.2745724594273, 34.70588858834762), Pair(555f, 620f)),

            EnhancedPoint.MapPoint(Point.fromLngLat(135.27358603414842, 34.70134176784736)),
            EnhancedPoint.KeyPoint(Point.fromLngLat(135.2732434287031, 34.69668412888626), Pair(555f, 650f))
        )
    }

}
