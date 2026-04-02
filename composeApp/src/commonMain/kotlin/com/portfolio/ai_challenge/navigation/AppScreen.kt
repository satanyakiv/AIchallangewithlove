package com.portfolio.ai_challenge.navigation

data object RouteMain
data object RouteDay4
data object RouteDay5
data object RouteDay6
data object RouteDay7
data object RouteDay8
data object RouteDay9
data object RouteDay10Hub
data object RouteDay10Sliding
data object RouteDay10Facts
data object RouteDay10Branching
data object RouteDay10Comparison
data object RouteDay11
data object RouteDay12
data object RouteDay13
data object RouteDay14
data object RouteDay15

private val dayRoutes: Map<Int, Any> = mapOf(
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

fun routeForDay(dayId: Int): Any? = dayRoutes[dayId]
