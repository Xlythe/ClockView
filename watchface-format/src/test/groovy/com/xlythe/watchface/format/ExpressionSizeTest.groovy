package com.xlythe.watchface.format

import org.junit.Test

import static org.junit.Assert.assertTrue

/**
 * Watches how big the bundled variables get once they're resolved.
 *
 * <p>Watch Face Format has no variables of its own, so every reference to one is inlined. A
 * variable built from three others that are each built from three more grows by multiplication,
 * not addition, and the watch re-evaluates all of it on every tick that touches it. These bounds
 * are here so that growth is a decision rather than a surprise.
 */
class ExpressionSizeTest {

    private static final Map<String, Integer> LIMITS = [
            // Measured without the version overrides, so these are the format 1 shapes: the
            // date is counted from the calendar rather than taken off a timestamp, which is
            // longer to write and built only from sources that change once a minute or slower.
            // Format 3 and up get [MINUTES_SINCE_EPOCH] and come out smaller as well as slower.
            '${JULIAN_DAY}'                   : 200,
            '${SUNRISE_MILLIS_SINCE_MIDNIGHT}': 20_000,
            '${MOON_DECLINATION_DEG}'         : 12_000,
            '${MOON_RIGHT_ASCENSION_DEG}'     : 50_000,
            '${MOON_ALTITUDE_DEG}'            : 70_000,
            // The dearest of the lot: it inlines the altitude, and the altitude inlines the right
            // ascension. Reach for it once per face, not once per element.
            '${MOON_AZIMUTH_DEG}'             : 350_000,
            '${GET_TRANSITION_ALPHA}'         : 140_000,
            '${PERCENT_OF_DAY}'               : 160_000,
    ]

    @Test
    void noVariableGrowsPastWhatWeExpect() {
        String xml = ExpressionSizeTest.class
                .getResourceAsStream('/com/xlythe/watchface/format/variables/standard.xml')
                .getText('UTF-8')
        Map<String, String> resolved =
                TemplateProcessor.resolve(TemplateProcessor.parseVariables(xml, 'standard.xml'))

        LIMITS.each { name, limit ->
            int size = resolved[name].length()
            println "SIZE ${name} = ${size} (limit ${limit})"
            assertTrue("$name resolves to $size characters, past the $limit we expect. Inlining is"
                    + " multiplicative, so this is worth understanding before raising the bound.",
                    size <= limit)
        }
    }
}
