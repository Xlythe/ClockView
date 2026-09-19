package com.xlythe.watchface.format

import org.junit.Test

import static org.junit.Assert.assertTrue

/**
 * Checks which data sources reach the astronomy, because that is what decides how often the watch
 * works it out.
 *
 * <p>Watch Face Format re-evaluates an expression whenever any source inside it changes. A
 * sunrise built on [SECOND] is worked out once a second, and since it is inlined at every use,
 * once a second per use. Nothing here needs the time to better than a minute: the sun moves a
 * quarter of a degree in one and the moon a fifth of one.
 *
 * <p>Sizes are a poor guard for this - a short expression on a fast source costs far more than a
 * long one on a slow source - so what is asserted is the sources themselves.
 */
class CadenceTest {
    /** Sources that change faster than once a minute. */
    private static final List<String> FAST = ['[SECOND]', '[MILLISECOND]', '[SECOND_MILLISECOND]',
                                              '[UTC_TIMESTAMP]', '[SECONDS_SINCE_EPOCH]']

    private static final List<String> SOLAR = ['${SUNRISE_MILLIS_SINCE_MIDNIGHT}',
                                               '${SUNSET_MILLIS_SINCE_MIDNIGHT}',
                                               '${IS_SUNRISE}', '${IS_DAY}', '${IS_NIGHT}',
                                               '${GET_TRANSITION_ALPHA}', '${PERCENT_OF_DAY}',
                                               '${LOCAL_OFFSET}', '${JULIAN_DAY}']

    private static final List<String> LUNAR = ['${MOON_DECLINATION_DEG}',
                                               '${MOON_ALTITUDE_DEG}', '${MOON_AZIMUTH_DEG}',
                                               '${IS_MOON_UP}', '${JULIAN_DAY_UTC}']

    /** Every format version, because the plugin swaps the date and the offset around by version. */
    private static final List<Integer> VERSIONS = [1, 2, 3, 4]

    @Test
    void theSunNeverReadsAFastSource() {
        for (int formatVersion : VERSIONS) {
            assertNothingFastIn(SOLAR, formatVersion)
        }
    }

    /**
     * The moon used to be the exception: sidereal time needs a UTC date, and before format 3 the
     * only numeric route to one was [UTC_TIMESTAMP]. Now the offset comes out of the time zone
     * table, so the UTC date is the local date less a number that changes twice a year.
     */
    @Test
    void norDoesTheMoon() {
        for (int formatVersion : VERSIONS) {
            assertNothingFastIn(LUNAR, formatVersion)
        }
    }

    @Test
    void theMinuteClockIsOnlyMinutes() {
        assertNothingFastIn(['${CURRENT_MILLIS_TO_THE_MINUTE}'], 1)
    }

    /** The full-resolution clock is still there for whatever genuinely wants a second. */
    @Test
    void thePerSecondClockIsStillAvailable() {
        assertTrue('nothing offers the second any more',
                resolve(1)['${CURRENT_MILLIS_SINCE_MIDNIGHT}'].contains('[SECOND]'))
    }

    private static void assertNothingFastIn(List<String> names, int formatVersion) {
        Map<String, String> resolved = resolve(formatVersion)
        for (String name : names) {
            String expression = resolved[name]
            assert expression != null: "$name is not defined"
            for (String source : FAST) {
                assertTrue("on format $formatVersion, $name reads $source, which re-evaluates it"
                        + " and everything holding it far more often than it can change",
                        !expression.contains(source))
            }
        }
    }

    private static Map<String, String> resolve(int formatVersion) {
        Map<String, String> declared = TemplateProcessor.parseVariables(
                read('variables/standard.xml'), 'standard.xml')
        declared.putAll(TemplateProcessor.parseTimeZoneCoordinates(read('geo/timezones.xml')))
        declared.putAll(StandardDates.forFormatVersion(formatVersion))
        return TemplateProcessor.resolve(declared)
    }

    private static String read(String path) {
        return CadenceTest.class
                .getResourceAsStream("/com/xlythe/watchface/format/${path}").getText('UTF-8')
    }
}
