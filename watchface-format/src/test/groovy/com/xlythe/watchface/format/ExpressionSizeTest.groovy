package com.xlythe.watchface.format

import org.junit.Test

import static org.junit.Assert.assertTrue

/**
 * Watches how big the bundled variables get once they're resolved.
 *
 * <p>Watch Face Format has no variables of its own, so every reference to one is inlined. A
 * variable built from three others that are each built from three more grows by multiplication,
 * not addition, and the cost lands in the generated watchface.xml once per use. These bounds are
 * here so that growth is a decision rather than a surprise.
 */
class ExpressionSizeTest {

    private static final Map<String, Integer> LIMITS = [
            '${SUNRISE_MILLIS_SINCE_MIDNIGHT}': 20_000,
            '${MOON_DECLINATION_DEG}'         : 15_000,
            '${MOON_RIGHT_ASCENSION_DEG}'     : 55_000,
            '${MOON_ALTITUDE_DEG}'            : 80_000,
            // The dearest of the lot: it inlines the altitude, and the altitude inlines the right
            // ascension. Reach for it once per face, not once per element.
            '${MOON_AZIMUTH_DEG}'             : 400_000,
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
            assertTrue("$name resolves to $size characters, past the $limit we expect. Inlining is"
                    + " multiplicative, so this is worth understanding before raising the bound.",
                    size <= limit)
        }
    }
}
