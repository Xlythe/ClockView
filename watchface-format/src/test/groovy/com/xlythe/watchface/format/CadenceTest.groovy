package com.xlythe.watchface.format

import org.junit.Test

import static org.junit.Assert.assertTrue

/**
 * Checks which data sources reach the astronomy, because that is what decides how often the watch
 * works it out.
 *
 * <p>Watch Face Format re-evaluates an expression whenever any source inside it changes. A
 * sunrise built on [SECOND] is worked out once a second, and since it is inlined at every use,
 * once a second per use. Nothing here needs to know the time to better than a minute: the sun
 * moves a quarter of a degree in one and the moon a fifth of one.
 *
 * <p>Sizes are a poor guard for this - a short expression on a fast source costs far more than a
 * long one on a slow source - so what is asserted is the sources themselves.
 */
class CadenceTest {
    /** Sources that change faster than once a minute. */
    private static final List<String> FAST = ['[SECOND]', '[MILLISECOND]', '[SECOND_MILLISECOND]',
                                              '[UTC_TIMESTAMP]', '[SECONDS_SINCE_EPOCH]']

    /** What the sun needs, none of which should drag in a fast source. */
    private static final List<String> SOLAR = ['${SUNRISE_MILLIS_SINCE_MIDNIGHT}',
                                               '${SUNSET_MILLIS_SINCE_MIDNIGHT}',
                                               '${IS_SUNRISE}', '${IS_DAY}', '${IS_NIGHT}',
                                               '${GET_TRANSITION_ALPHA}', '${PERCENT_OF_DAY}']

    /**
     * Before format 3 the sun cannot be got entirely off the fast path: placing solar noon on the
     * local clock needs the UTC offset, [TIMEZONE_OFFSET] is text, and [UTC_TIMESTAMP] is the
     * only numeric route left. Everything else is slow, so this is the one leak, and it closes on
     * format 3 where [TIMEZONE_OFFSET_MINUTES] is an integer.
     */
    @Test
    void beforeFormatThreeOnlyTheOffsetIsFast() {
        Map<String, String> resolved = resolve(1)
        for (String name : SOLAR) {
            String expression = resolved[name]
            for (String source : FAST - '[UTC_TIMESTAMP]') {
                assertTrue("$name reads $source", !expression.contains(source))
            }
        }
        assertTrue('the offset should be the only thing pulling in the timestamp',
                resolved['${LOCAL_OFFSET}'].contains('[UTC_TIMESTAMP]'))
        assertTrue('the declination has no business reading a timestamp',
                !resolved['${SUN_DECLIN_DEG}'].contains('[UTC_TIMESTAMP]'))
    }

    @Test
    void fromFormatThreeTheSunIsEntirelySlow() {
        Map<String, String> resolved = resolve(3)
        for (String name : SOLAR) {
            String expression = resolved[name]
            assert expression != null: "$name is not defined"
            for (String source : FAST) {
                assertTrue("$name reads $source, which puts the whole chain on the per-second path",
                        !expression.contains(source))
            }
        }
    }

    /** And the moon with it: on format 3 one date serves both, so nothing is left on the fast path. */
    @Test
    void fromFormatThreeTheMoonIsEntirelySlowToo() {
        Map<String, String> resolved = resolve(3)
        for (String source : FAST) {
            assertTrue("\${MOON_ALTITUDE_DEG} reads $source",
                    !resolved['${MOON_ALTITUDE_DEG}'].contains(source))
            assertTrue("\${MOON_AZIMUTH_DEG} reads $source",
                    !resolved['${MOON_AZIMUTH_DEG}'].contains(source))
        }
    }

    /** Before that, sidereal time needs a UTC date, and only the moon pays for it. */
    @Test
    void beforeFormatThreeOnlyTheMoonPaysForTheTimestamp() {
        Map<String, String> resolved = resolve(1)
        assertTrue('the moon needs the UTC timestamp before format 3',
                resolved['${MOON_ALTITUDE_DEG}'].contains('[UTC_TIMESTAMP]'))
    }

    /** The clock the astronomy reads must not carry seconds. */
    @Test
    void theMinuteClockIsOnlyMinutes() {
        Map<String, String> resolved = resolve(1)
        for (String source : FAST) {
            assertTrue("${'$'}{CURRENT_MILLIS_TO_THE_MINUTE} reads $source",
                    !resolved['${CURRENT_MILLIS_TO_THE_MINUTE}'].contains(source))
        }
        // And the full-resolution one still exists for whatever genuinely wants a second.
        assertTrue('the per-second clock should still be available',
                resolved['${CURRENT_MILLIS_SINCE_MIDNIGHT}'].contains('[SECOND]'))
    }

    private static Map<String, String> resolve(int formatVersion) {
        String xml = CadenceTest.class
                .getResourceAsStream('/com/xlythe/watchface/format/variables/standard.xml')
                .getText('UTF-8')
        Map<String, String> declared = TemplateProcessor.parseVariables(xml, 'standard.xml')
        declared.putAll(StandardDates.forFormatVersion(formatVersion))
        return TemplateProcessor.resolve(declared)
    }
}
