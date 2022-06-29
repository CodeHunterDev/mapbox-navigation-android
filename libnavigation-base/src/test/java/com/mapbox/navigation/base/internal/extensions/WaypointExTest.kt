package com.mapbox.navigation.base.internal.extensions

import com.mapbox.navigation.base.internal.route.Waypoint
import io.mockk.every
import io.mockk.mockk
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

class WaypointExTest {

    @RunWith(Parameterized::class)
    class FilterTest(
        private val waypoints: List<Waypoint>,
        private val requestedWaypointsExpected: List<@Waypoint.Type Int>,
        private val legsWaypointsExpected: List<@Waypoint.Type Int>
    ) {
        companion object {
            @JvmStatic
            @Parameterized.Parameters
            fun data() = listOf(
                arrayOf(
                    provideWaypoints(Waypoint.REGULAR, Waypoint.SILENT, Waypoint.REGULAR),
                    listOf(Waypoint.REGULAR, Waypoint.SILENT, Waypoint.REGULAR),
                    listOf(Waypoint.REGULAR, Waypoint.REGULAR),
                ),
                arrayOf(
                    provideWaypoints(
                        Waypoint.REGULAR,
                        Waypoint.SILENT,
                        Waypoint.EV_CHARGING,
                        Waypoint.SILENT,
                        Waypoint.REGULAR
                    ),
                    listOf(Waypoint.REGULAR, Waypoint.SILENT, Waypoint.SILENT, Waypoint.REGULAR),
                    listOf(Waypoint.REGULAR, Waypoint.EV_CHARGING, Waypoint.REGULAR),
                ),
                arrayOf(
                    provideWaypoints(
                        Waypoint.REGULAR,
                        Waypoint.EV_CHARGING,
                        Waypoint.EV_CHARGING,
                        Waypoint.EV_CHARGING,
                        Waypoint.REGULAR
                    ),
                    listOf(Waypoint.REGULAR, Waypoint.REGULAR),
                    listOf(
                        Waypoint.REGULAR,
                        Waypoint.EV_CHARGING,
                        Waypoint.EV_CHARGING,
                        Waypoint.EV_CHARGING,
                        Waypoint.REGULAR
                    ),
                ),
                arrayOf(
                    provideWaypoints(
                        Waypoint.REGULAR,
                        Waypoint.SILENT,
                        Waypoint.REGULAR,
                        Waypoint.REGULAR,
                        Waypoint.EV_CHARGING,
                        Waypoint.REGULAR
                    ),
                    listOf(
                        Waypoint.REGULAR,
                        Waypoint.SILENT,
                        Waypoint.REGULAR,
                        Waypoint.REGULAR,
                        Waypoint.REGULAR
                    ),
                    listOf(
                        Waypoint.REGULAR,
                        Waypoint.REGULAR,
                        Waypoint.REGULAR,
                        Waypoint.EV_CHARGING,
                        Waypoint.REGULAR
                    ),
                ),
            )

            fun checkWaypoints(
                expectedWaypoints: List<@Waypoint.Type Int>,
                modified: List<Waypoint>,
                original: List<Waypoint>,
            ) {

                assertEquals(expectedWaypoints.size, modified.size)
                var bufferIndex = -1
                modified.forEachIndexed { index, waypoint ->
                    assertEquals(expectedWaypoints[index], waypoint.type)
                    assertTrue(original.contains(waypoint))
                    val idx = original.indexOf(waypoint)
                    assertTrue(idx > bufferIndex)
                    bufferIndex = idx
                }
            }
        }

        @Test
        fun testCases() {
            checkWaypoints(requestedWaypointsExpected, waypoints.requestedWaypoints(), waypoints)
            checkWaypoints(legsWaypointsExpected, waypoints.legsWaypoints(), waypoints)
        }
    }

    @RunWith(Parameterized::class)
    class IndexOfNextCoordinateTest(
        private val testDescription: String,
        private val waypoints: List<Waypoint>,
        private val remainingWaypoints: Int,
        private val expectedIndex: Int?
    ) {

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun data() = listOf(
                arrayOf(
                    "Next index: 1 for 2 relevant waypoints and remaining waypoint 1",
                    provideWaypoints(
                        Waypoint.REGULAR, Waypoint.REGULAR
                    ),
                    1,
                    1,
                ),
                arrayOf(
                    "Next index: 1 for 3 relevant waypoints and remaining waypoint 2",
                    provideWaypoints(
                        Waypoint.REGULAR, Waypoint.SILENT, Waypoint.REGULAR
                    ),
                    2,
                    1,
                ),
                arrayOf(
                    "Next index: 2 for 3 relevant waypoints and remaining waypoint 1",
                    provideWaypoints(
                        Waypoint.REGULAR, Waypoint.SILENT, Waypoint.REGULAR
                    ),
                    1,
                    2,
                ),
                arrayOf(
                    "Next index: 3 for 4 relevant waypoints and remaining waypoint 1",
                    provideWaypoints(
                        Waypoint.REGULAR, Waypoint.SILENT, Waypoint.SILENT, Waypoint.REGULAR
                    ),
                    1,
                    3,
                ),
                arrayOf(
                    "Next index: 1 for 2 relevant waypoints (1 is EV) and remaining waypoint 2",
                    provideWaypoints(
                        Waypoint.REGULAR, Waypoint.EV_CHARGING, Waypoint.REGULAR
                    ),
                    2,
                    1,
                ),
                arrayOf(
                    "Next index: 1 for 3 relevant waypoints (2 is EV) and remaining waypoint 4",
                    provideWaypoints(
                        Waypoint.REGULAR,
                        Waypoint.EV_CHARGING,
                        Waypoint.SILENT,
                        Waypoint.EV_CHARGING,
                        Waypoint.REGULAR
                    ),
                    4,
                    1,
                ),
                arrayOf(
                    "Next index: 1 for 3 relevant waypoints (2 is EV) and remaining waypoint 2",
                    provideWaypoints(
                        Waypoint.REGULAR,
                        Waypoint.EV_CHARGING,
                        Waypoint.SILENT,
                        Waypoint.EV_CHARGING,
                        Waypoint.REGULAR
                    ),
                    2,
                    2,
                ),
                arrayOf(
                    "Next index: null for non-valid case - 3 relevant waypoints (2 is EV) and " +
                        "remaining waypoint 7",
                    provideWaypoints(
                        Waypoint.REGULAR,
                        Waypoint.EV_CHARGING,
                        Waypoint.SILENT,
                        Waypoint.EV_CHARGING,
                        Waypoint.REGULAR
                    ),
                    7,
                    null,
                ),
                arrayOf(
                    "Next index: 0 for 3 relevant waypoints (2 is EV) and remaining waypoint 5",
                    provideWaypoints(
                        Waypoint.REGULAR,
                        Waypoint.EV_CHARGING,
                        Waypoint.SILENT,
                        Waypoint.EV_CHARGING,
                        Waypoint.REGULAR
                    ),
                    5,
                    0,
                ),
            )
        }

        @Test
        fun testCases() {
            assertEquals(
                testDescription,
                expectedIndex,
                indexOfNextCoordinate(waypoints, remainingWaypoints)
            )
        }
    }
}

private fun provideWaypoints(@Waypoint.Type vararg waypointType: Int): List<Waypoint> =
    waypointType.map { mapToType ->
        mockk { every { type } returns mapToType }
    }
