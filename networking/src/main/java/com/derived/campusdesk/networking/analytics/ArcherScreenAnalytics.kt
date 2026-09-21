package com.derived.campusdesk.networking.analytics

import co.archer.sdk.Archer

/**
 * Screen-view analytics for Archer. Call [setTab] / [push] / [pop] from the shell.
 * Mirrors Traject `ArcherScreenAnalytics`.
 */
object ArcherScreenAnalytics {
    private val lock = Any()
    private var lastScreen: String? = null
    private var lastAtMs = 0L
    private var stack: MutableList<String> = mutableListOf("App")

    /** Current screen path (used by API / event attribution). */
    val currentScreenName: String
        get() = synchronized(lock) { stack.joinToString(" › ") }

    /** Primary tabs / top-level shells — replaces the stack. */
    fun setTab(name: String) {
        synchronized(lock) { stack = mutableListOf(name) }
        track(name)
    }

    fun push(name: String) {
        synchronized(lock) {
            if (stack.lastOrNull() != name) {
                stack.add(name)
            }
        }
        track(name)
    }

    fun pop(name: String) {
        synchronized(lock) {
            val index = stack.lastIndexOf(name)
            if (index >= 0) {
                while (stack.size > index) {
                    stack.removeAt(stack.lastIndex)
                }
            }
            if (stack.isEmpty()) {
                stack = mutableListOf("App")
            }
        }
    }

    fun track(screen: String) {
        val now = System.currentTimeMillis()
        val shouldSkip = synchronized(lock) {
            val skip = lastScreen == screen && now - lastAtMs < 500L
            if (!skip) {
                lastScreen = screen
                lastAtMs = now
            }
            skip
        }
        if (shouldSkip) return
        Archer.analytics("screen_viewed", mapOf("screen" to screen))
    }
}
