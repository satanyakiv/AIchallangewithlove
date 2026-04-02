package com.portfolio.ai_challenge.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RouteForDayTest {

    @Test
    fun testRouteForDay_validDay4_returnsRouteDay4() {
        assertEquals(RouteDay4, routeForDay(4))
    }

    @Test
    fun testRouteForDay_validDay10_returnsRouteDay10Hub() {
        assertEquals(RouteDay10Hub, routeForDay(10))
    }

    @Test
    fun testRouteForDay_validDay15_returnsRouteDay15() {
        assertEquals(RouteDay15, routeForDay(15))
    }

    @Test
    fun testRouteForDay_allSupportedDays_returnNonNull() {
        val expected = mapOf(
            4 to RouteDay4,
            5 to RouteDay5,
            6 to RouteDay6,
            7 to RouteDay7,
            8 to RouteDay8,
            9 to RouteDay9,
            10 to RouteDay10Hub,
            11 to RouteDay11,
            12 to RouteDay12,
            13 to RouteDay13,
            14 to RouteDay14,
            15 to RouteDay15,
        )
        expected.forEach { (dayId, route) ->
            assertEquals(route, routeForDay(dayId), "Day $dayId should map to $route")
        }
    }

    @Test
    fun testRouteForDay_invalidDay0_returnsNull() {
        assertNull(routeForDay(0))
    }

    @Test
    fun testRouteForDay_invalidDay3_returnsNull() {
        assertNull(routeForDay(3))
    }

    @Test
    fun testRouteForDay_invalidDay100_returnsNull() {
        assertNull(routeForDay(100))
    }

    @Test
    fun testRouteForDay_negativeDay_returnsNull() {
        assertNull(routeForDay(-1))
    }
}
