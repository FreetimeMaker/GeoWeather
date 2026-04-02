# Fix for Issue #45: Losing location / Incorrect Notifications

## Problem Analysis

Users experienced incorrect notifications when a location was selected in the background. The issue occurred because:

1. **Multiple notifications for all enabled locations**: The notification system sent alerts for ALL locations with `notificationsEnabled=true`, instead of just the currently viewed location.
2. **No tracking of selected location**: There was no mechanism to distinguish which location the user was currently viewing or should receive notifications for.
3. **Duplicate location entries**: When a user searched for "Marina del Rey" (which has 3 different GPS coordinates in the API), they could accidentally create multiple entries or the wrong coordinates could be saved.

## Solution Implemented

### Changes Made:

1. **LocationEntity.kt** - Added `selected: Boolean` field
   - Tracks which location is currently selected for viewing
   - Added unique index constraint on `(latitude, longitude)` to prevent duplicate location entries with the same coordinates

2. **LocationDao.kt** - Added new query methods
   - `getSelectedLocation()`: Retrieves the currently selected location
   - `deselectAllLocations()`: Clears the selected flag from all locations

3. **LocationDatabase.kt** - Updated schema version
   - Bumped version from 4 to 5 to trigger database migration for new `selected` field

4. **WeatherNotificationWorker.kt** - Modified to use selected location only
   - Now only sends notifications for the currently selected location
   - Prevents multiple notifications for different locations
   - Uses `getSelectedLocation()` instead of `getAllLocationsSync()`

5. **WeatherChangeWorker.kt** - Modified to use selected location only
   - Sends weather change alerts only for the selected location
   - Prevents duplicate alerts for unviewed locations

6. **MainActivity.kt** - Updated location navigation
   - When a new location is added, it's automatically marked as selected
   - When a location detail is opened, it's marked as selected
   - Previous selections are cleared (only one location can be selected at a time)

## How It Works

**User Flow:**
1. User searches for "Marina del Rey" and sees 3 options with different GPS coordinates
2. User selects the correct Marina del Rey location
3. The location is added to database and marked as `selected=true`
4. When opening location detail, the system confirms this location is marked as selected
5. Notification worker runs and sends alerts ONLY for the selected location
6. Notifications show correct weather data for the viewed location

**Benefits:**
- No more incorrect notifications for wrong locations
- Clear separation between viewing a location and receiving notifications
- Prevents duplicate location entries with identical coordinates
- Temperature shown in background notifications matches foreground display

## Testing Recommendations

1. Add a location for "Marina del Rey"
2. Enable notifications for that location
3. Go to background, verify notifications show correct temperature
4. Return to app and verify foreground temperature matches notification temperature
5. Add a different location and verify previous location's notifications stop
6. Try to add the same coordinates twice - should fail due to unique constraint
