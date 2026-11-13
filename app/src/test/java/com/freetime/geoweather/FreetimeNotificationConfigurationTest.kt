package com.freetime.geoweather

import com.freetime.notifications.DeveloperBlogSource
import com.freetime.notifications.GitHubReleaseSource
import com.freetime.notifications.NotificationCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FreetimeNotificationConfigurationTest {
    private val configuration = freetimeNotificationConfiguration(
        smallIcon = 1,
        updatesChannelName = "App updates",
        changelogsChannelName = "Release notes",
        developerMessagesChannelName = "Developer messages"
    )

    @Test fun releasesBelongToGeoWeatherAndExcludePrereleases() {
        val source = configuration.sources.filterIsInstance<GitHubReleaseSource>().single()
        assertEquals("FreetimeMaker", source.owner)
        assertEquals("GeoWeather", source.repository)
        assertFalse(source.includePrereleases)
        assertEquals(2, configuration.sources.size)
    }

    @Test fun developerMessagesUseThePublicFreetimeBlog() {
        val source = configuration.sources.filterIsInstance<DeveloperBlogSource>().single()
        assertEquals("https://api.free-time.me/v2/blog/posts", source.apiUrl)
        assertEquals("https://blog.free-time.me", source.blogUrl)
        assertTrue(source.categories.isEmpty())
    }

    @Test fun pollingStartsWithASilentBaselineAndUsesTheAppIcon() {
        assertEquals(15L, configuration.checkIntervalMinutes)
        assertFalse(configuration.notifyOnFirstCheck)
        assertEquals(1, configuration.smallIcon)
    }

    @Test fun categoriesHaveSeparateLocalizedChannels() {
        assertEquals(NotificationCategory.entries.toSet(), configuration.enabledCategories)
        assertEquals("App updates", configuration.channels[NotificationCategory.UPDATES]?.name)
        assertEquals("Release notes", configuration.channels[NotificationCategory.CHANGELOGS]?.name)
        assertEquals("Developer messages", configuration.channels[NotificationCategory.DEVELOPER_MESSAGES]?.name)
        assertTrue(configuration.channels.values.all { it.importance == 3 })
    }
}
