package com.freetime.geoweather.shared

/**
 * Temporary source-compatibility bridge after merging the former shared module
 * into the Android application module. All resource IDs come from the app R class.
 */
class R private constructor() {
    typealias string = com.freetime.geoweather.R.string
    typealias drawable = com.freetime.geoweather.R.drawable
}
