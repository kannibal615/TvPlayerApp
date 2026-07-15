package com.smartvision.svplayer.ui.notifications

import com.smartvision.svplayer.data.notifications.AppNotification
import com.smartvision.svplayer.data.notifications.NotificationType
import com.smartvision.svplayer.data.notifications.excludeInstalledAppUpdates
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationsPolicyTest {
    @Test
    fun `wire values are classified into stable notification types`() {
        assertEquals(NotificationType.AppUpdate, NotificationType.fromWire("app_update"))
        assertEquals(NotificationType.PlaylistAdded, NotificationType.fromWire("playlist_added"))
        assertEquals(NotificationType.ImportantInfo, NotificationType.fromWire("important_info"))
        assertEquals(NotificationType.ImportantInfo, NotificationType.fromWire("legacy_unknown"))
    }

    @Test
    fun `sections and counters separate unread items from history`() {
        val items = listOf(
            notification(1, NotificationType.AppUpdate),
            notification(2, NotificationType.PlaylistAdded),
            notification(3, NotificationType.ImportantInfo),
            notification(4, NotificationType.PlaylistAdded, seen = true),
        )

        assertEquals(listOf(1L), filterNotifications(items, NotificationSection.Updates).map { it.id })
        assertEquals(listOf(2L), filterNotifications(items, NotificationSection.Playlists).map { it.id })
        assertEquals(listOf(4L), filterNotifications(items, NotificationSection.History).map { it.id })
        assertEquals(1, notificationCounts(items)[NotificationSection.Updates])
        assertEquals(1, notificationCounts(items)[NotificationSection.Playlists])
    }

    @Test
    fun `opening an unread item moves it from category to history`() {
        val unread = notification(8, NotificationType.ImportantInfo)
        val opened = unread.copy(seen = true, seenAt = "2026-07-14 12:00:00")

        assertEquals(emptyList<Long>(), filterNotifications(listOf(opened), NotificationSection.Important).map { it.id })
        assertEquals(listOf(8L), filterNotifications(listOf(opened), NotificationSection.History).map { it.id })
    }

    @Test
    fun `focus prefers next item then previous and finally category`() {
        val items = listOf(
            notification(10, NotificationType.ImportantInfo),
            notification(11, NotificationType.ImportantInfo),
            notification(12, NotificationType.ImportantInfo),
        )

        assertEquals(12L, nextNotificationFocusId(items, 1))
        assertEquals(11L, nextNotificationFocusId(items, 2))
        assertEquals(null, nextNotificationFocusId(items.take(1), 0))
    }

    @Test
    fun `installed application updates are excluded defensively`() {
        val items = listOf(
            notification(20, NotificationType.AppUpdate, sourceVersionCode = 100),
            notification(21, NotificationType.AppUpdate, sourceVersionCode = 101),
            notification(22, NotificationType.ImportantInfo),
        )

        assertEquals(listOf(21L, 22L), excludeInstalledAppUpdates(items, installedVersionCode = 100).map { it.id })
    }

    private fun notification(
        id: Long,
        type: NotificationType,
        seen: Boolean = false,
        sourceVersionCode: Int? = null,
    ) = AppNotification(
        id = id,
        title = "Notification $id",
        message = "Message",
        priority = "normal",
        createdAt = "2026-07-14 10:00:00",
        expiresAt = null,
        seen = seen,
        seenAt = if (seen) "2026-07-14 11:00:00" else null,
        type = type,
        sourceVersionCode = sourceVersionCode,
        details = null,
    )
}
