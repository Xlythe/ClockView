package com.xlythe.watchface.format

import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

/**
 * Pins what ${GET_TRANSITION_ALPHA} and ${PERCENT_OF_DAY} come to right around the clock.
 *
 * <p>These two are the dearest expressions the faces use, and the obvious ways to shorten them
 * are not equivalent. Replacing the pair of sunset branches with abs() looks right and changes
 * the answer in deep night from fully opaque to fully clear, which is the whole scene. Anything
 * that rewrites them has to leave every sample below where it is.
 */
class TransitionMathTest {
    private static final double LATITUDE = 51.5d
    private static final double LONGITUDE = 0d
    private static final int MIDSUMMER = 172

    private final WffExpressions mExpressions = new WffExpressions()

    /** Sunrise is about 03:45 UTC and sunset about 20:20 at this latitude in June. */
    @Test
    void theTransitionAlphaIsWhatItWas() {
        Map<Double, Double> expected = [
                0.0d : 255.0d,   // night, after midnight
                2.0d : 255.0d,
                3.0d : 73.905d,  // climbing into sunrise
                4.0d : 181.132d, // just past it
                6.0d : 0.0d,     // full day, away from either edge
                12.0d: 0.0d,
                19.0d: 0.0d,
                20.0d: 165.648d, // into sunset
                22.0d: 0.0d,     // night, before midnight - see the test below
        ]
        expected.each { hour, alpha ->
            assertEquals("the alpha at ${hour}:00", alpha, alphaAt(hour), 1.0d)
        }
    }

    @Test
    void thePercentOfDayIsWhatItWas() {
        Map<Double, Double> expected = [
                0.0d : 0.49612d, // about halfway through the night
                4.0d : 0.01741d, // just after sunrise, so early in the day
                12.0d: 0.49817d,
                20.0d: 0.97894d, // nearly sunset
                22.0d: 0.22402d, // into the night again
        ]
        expected.each { hour, percent ->
            assertEquals("the percent of day at ${hour}:00", percent, percentAt(hour), 0.002d)
        }
    }

    /** Whatever else changes, these have to stay in range or the artwork goes wrong. */
    @Test
    void bothStayInRangeAllDayEveryMonth() {
        for (int dayOfYear : [1, 79, 172, 266, 355]) {
            for (double hour = 0d; hour < 24d; hour += 0.5d) {
                double alpha = mExpressions.evaluate(
                        '${GET_TRANSITION_ALPHA}', LATITUDE, LONGITUDE, 2026, dayOfYear, hour)
                double percent = mExpressions.evaluate(
                        '${PERCENT_OF_DAY}', LATITUDE, LONGITUDE, 2026, dayOfYear, hour)
                assertTrue("alpha $alpha on day $dayOfYear at $hour", alpha >= 0d && alpha <= 255d)
                assertTrue("percent $percent on day $dayOfYear at $hour",
                        percent >= 0d && percent <= 1d)
            }
        }
    }

    /**
     * The alpha does not come back the same either side of midnight. Two hours after sunset it
     * reads 0 and four hours after it reads 255, because ${MILLIS_SINCE_SUNSET} goes negative
     * once the clock wraps and the clamp catches it at the far end.
     *
     * <p>This is pinned rather than corrected: it decides what the scene looks like all night,
     * and changing it is a decision about the artwork, not a tidy-up.
     */
    @Test
    void theAlphaJumpsAtMidnight() {
        assertEquals("two hours after sunset", 0.0d, alphaAt(22d), 1.0d)
        assertEquals("four hours after sunset, past midnight", 255.0d, alphaAt(0d), 1.0d)
    }

    private double alphaAt(double hour) {
        return mExpressions.evaluate('${GET_TRANSITION_ALPHA}', LATITUDE, LONGITUDE, 2026, MIDSUMMER, hour)
    }

    private double percentAt(double hour) {
        return mExpressions.evaluate('${PERCENT_OF_DAY}', LATITUDE, LONGITUDE, 2026, MIDSUMMER, hour)
    }
}
