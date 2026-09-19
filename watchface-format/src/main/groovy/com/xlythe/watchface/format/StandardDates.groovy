package com.xlythe.watchface.format

/**
 * Picks how the date and the UTC offset are worked out, which decides how often the watch works
 * out everything downstream of them.
 *
 * <p>An expression is re-evaluated whenever a data source inside it changes. A source that
 * changes every millisecond therefore drags a whole chain onto the per-frame path however little
 * of it moves, and the sun and moon are the longest chains this plugin produces. What is
 * available to avoid that depends on the format version, so the definitions in standard.xml are
 * the ones that work everywhere and these replace them where something better exists.
 */
class StandardDates {
    /** [MINUTES_SINCE_EPOCH] and [TIMEZONE_OFFSET_MINUTES] arrived in Watch Face Format 3. */
    static final int SLOW_SOURCES_FORMAT_VERSION = 3

    private StandardDates() {}

    /**
     * Definitions to override for a format version, or nothing if it has no better source than
     * the ones standard.xml already uses.
     */
    static Map<String, String> forFormatVersion(int formatVersion) {
        if (formatVersion < SLOW_SOURCES_FORMAT_VERSION) {
            return [:]
        }
        return [
                // Exact, in UTC, and it only changes once a minute - so the sun and the moon can
                // share one date instead of the moon paying for [UTC_TIMESTAMP].
                '${JULIAN_DAY}'    : '([MINUTES_SINCE_EPOCH] / 1440 + 2440587.5)',
                '${JULIAN_DAY_UTC}': '([MINUTES_SINCE_EPOCH] / 1440 + 2440587.5)',
                // The offset a zone keeps, plus the hour it borrows in summer. [TIMEZONE_OFFSET]
                // has been readable since version 1 but returns "+1:30" rather than 90, and text
                // is no use in arithmetic.
                '${LOCAL_OFFSET}'  : '(([TIMEZONE_OFFSET_MINUTES] + ([IS_DAYLIGHT_SAVING_TIME] ? 60 : 0)) / 60)',
        ]
    }
}
