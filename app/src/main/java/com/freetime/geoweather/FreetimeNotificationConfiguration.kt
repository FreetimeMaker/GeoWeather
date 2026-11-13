package com.freetime.geoweather

import com.freetime.notifications.DeveloperBlogSource
import com.freetime.notifications.GitHubReleaseSource
import com.freetime.notifications.NotificationCategory
import com.freetime.notifications.NotificationChannelOptions
import com.freetime.notifications.NotificationConfiguration

internal fun freetimeNotificationConfiguration(
    smallIcon: Int,
    updatesChannelName: String,
    changelogsChannelName: String,
    developerMessagesChannelName: String
): NotificationConfiguration = NotificationConfiguration(
    smallIcon = smallIcon,
    sources = listOf(
        GitHubReleaseSource(owner = "FreetimeMaker", repository = "GeoWeather"),
        DeveloperBlogSource()
    ),
    checkIntervalMinutes = 15,
    // Establish a silent baseline instead of notifying about historical posts on first launch.
    notifyOnFirstCheck = false,
    channels = mapOf(
        NotificationCategory.UPDATES to NotificationChannelOptions(name = updatesChannelName),
        NotificationCategory.CHANGELOGS to NotificationChannelOptions(name = changelogsChannelName),
        NotificationCategory.DEVELOPER_MESSAGES to NotificationChannelOptions(name = developerMessagesChannelName)
    )
)
