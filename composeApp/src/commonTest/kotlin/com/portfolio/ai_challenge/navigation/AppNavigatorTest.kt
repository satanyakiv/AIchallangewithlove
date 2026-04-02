package com.portfolio.ai_challenge.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppNavigatorTest {

    private fun createNavigator(vararg initial: Any): AppNavigator {
        val stack = mutableListOf(*initial)
        return AppNavigator(stack)
    }

    // --- navigateTo ---

    @Test
    fun testNavigateTo_addsRouteToStack() {
        val nav = createNavigator(RouteMain)
        nav.navigateTo(RouteDay4)
        assertEquals(RouteDay4, nav.currentRoute)
        assertEquals(2, nav.stackSize)
    }

    @Test
    fun testNavigateTo_multipleRoutes_stackGrows() {
        val nav = createNavigator(RouteMain)
        nav.navigateTo(RouteDay4)
        nav.navigateTo(RouteDay5)
        assertEquals(3, nav.stackSize)
        assertEquals(RouteDay5, nav.currentRoute)
    }

    // --- navigateToDay ---

    @Test
    fun testNavigateToDay_validDay_returnsTrue() {
        val nav = createNavigator(RouteMain)
        assertTrue(nav.navigateToDay(11))
        assertEquals(RouteDay11, nav.currentRoute)
    }

    @Test
    fun testNavigateToDay_invalidDay_returnsFalse() {
        val nav = createNavigator(RouteMain)
        assertFalse(nav.navigateToDay(99))
        assertEquals(RouteMain, nav.currentRoute)
        assertEquals(1, nav.stackSize)
    }

    @Test
    fun testNavigateToDay_invalidDay_stackUnchanged() {
        val nav = createNavigator(RouteMain)
        nav.navigateToDay(0)
        assertEquals(1, nav.stackSize)
    }

    // --- goBack ---

    @Test
    fun testGoBack_multipleEntries_removesLast() {
        val nav = createNavigator(RouteMain, RouteDay7)
        assertTrue(nav.goBack())
        assertEquals(RouteMain, nav.currentRoute)
        assertEquals(1, nav.stackSize)
    }

    @Test
    fun testGoBack_singleEntry_returnsFalse() {
        val nav = createNavigator(RouteMain)
        assertFalse(nav.goBack())
        assertEquals(RouteMain, nav.currentRoute)
        assertEquals(1, nav.stackSize)
    }

    @Test
    fun testGoBack_emptyStack_returnsFalse() {
        val nav = createNavigator()
        assertFalse(nav.goBack())
    }

    @Test
    fun testGoBack_deepStack_popsOneByOne() {
        val nav = createNavigator(RouteMain, RouteDay10Hub, RouteDay10Sliding)
        nav.goBack()
        assertEquals(RouteDay10Hub, nav.currentRoute)
        nav.goBack()
        assertEquals(RouteMain, nav.currentRoute)
        assertFalse(nav.goBack())
    }

    // --- currentRoute ---

    @Test
    fun testCurrentRoute_emptyStack_returnsNull() {
        val nav = createNavigator()
        assertEquals(null, nav.currentRoute)
    }

    @Test
    fun testCurrentRoute_afterNavigate_returnsNewRoute() {
        val nav = createNavigator(RouteMain)
        nav.navigateTo(RouteDay12)
        assertEquals(RouteDay12, nav.currentRoute)
    }

    // --- full navigation flow ---

    @Test
    fun testFullFlow_navigateAndGoBack_returnsToMain() {
        val nav = createNavigator(RouteMain)
        nav.navigateToDay(10)
        nav.navigateTo(RouteDay10Branching)
        assertEquals(RouteDay10Branching, nav.currentRoute)
        assertEquals(3, nav.stackSize)

        nav.goBack()
        assertEquals(RouteDay10Hub, nav.currentRoute)

        nav.goBack()
        assertEquals(RouteMain, nav.currentRoute)
    }
}
