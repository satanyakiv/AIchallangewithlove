package com.portfolio.ai_challenge.navigation

class AppNavigator(private val backStack: MutableList<Any>) {

    val currentRoute: Any? get() = backStack.lastOrNull()

    val stackSize: Int get() = backStack.size

    fun navigateTo(route: Any) {
        backStack.add(route)
    }

    fun navigateToDay(dayId: Int): Boolean {
        val route = routeForDay(dayId) ?: return false
        backStack.add(route)
        return true
    }

    fun goBack(): Boolean {
        if (backStack.size <= 1) return false
        backStack.removeLast()
        return true
    }
}
