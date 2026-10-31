package com.hairstyle.changer.aitryon.utils

/**
 * Items unlocked by watching an ad stay unlocked only while the app process is alive.
 * Nothing is persisted: after the app is closed every locked item is locked again.
 */
object UnlockSession {

    private val unlocked = HashSet<String>()

    private fun key(group: UnlockConfig.Group, id: Int, title: String) = "${group.key}|$id|$title"

    @Synchronized
    fun isUnlocked(group: UnlockConfig.Group, id: Int, title: String): Boolean =
        key(group, id, title) in unlocked

    @Synchronized
    fun markUnlocked(group: UnlockConfig.Group, id: Int, title: String) {
        unlocked.add(key(group, id, title))
    }
}
