package com.xlythe.watchface.format

import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

/** Checks the actual expressions shipped for solar-angle scene transitions. */
class TransitionMathTest {
    private static final double LATITUDE = 51.5d
    private static final double LONGITUDE = 0d
    private final WffExpressions expressions = new WffExpressions()

    @Test
    void sunsetArtworkPeaksAtTheHorizon() {
        for (int day : [79, 172, 266, 355]) {
            double sunsetHour = value('${SUNSET_MILLIS_SINCE_MIDNIGHT}', day, 12d) / 3600000d
            assertTrue("sunset alpha on day $day", value('${GET_TRANSITION_ALPHA}', day, sunsetHour) > 245d)
            assertEquals("sunset phase on day $day", 1d, value('${IS_SUNSET}', day, sunsetHour), 0d)
        }
    }

    @Test
    void nauticalTwilightLastsLongerInSummer() {
        double summerSunset = value('${SUNSET_MILLIS_SINCE_MIDNIGHT}', 172, 12d) / 3600000d
        double equinoxSunset = value('${SUNSET_MILLIS_SINCE_MIDNIGHT}', 266, 12d) / 3600000d
        assertTrue(value('${GET_TRANSITION_ALPHA}', 172, summerSunset + 1.5d) > 0d)
        assertEquals(0d, value('${GET_TRANSITION_ALPHA}', 266, equinoxSunset + 1.5d), 0d)
    }

    @Test
    void dayAndDeepNightHaveNoTransitionArtwork() {
        for (int day : [79, 172, 266, 355]) {
            assertEquals(0d, value('${GET_TRANSITION_ALPHA}', day, 12d), 0d)
            assertEquals(0d, value('${GET_TRANSITION_ALPHA}', day, 0d), 0d)
        }
    }

    @Test
    void percentOfDayStillTracksTheOrb() {
        Map<Double, Double> expected = [
                0.0d : 0.49612d,
                4.0d : 0.01741d,
                12.0d: 0.49817d,
                20.0d: 0.97894d,
                22.0d: 0.22402d,
        ]
        expected.each { hour, percent ->
            assertEquals("orb at ${hour}:00", percent, value('${PERCENT_OF_DAY}', 172, hour), 0.002d)
        }
    }

    @Test
    void valuesStayInRangeAllDayEverySeason() {
        for (int day : [1, 79, 172, 266, 355]) {
            for (double hour = 0d; hour < 24d; hour += 0.5d) {
                double alpha = value('${GET_TRANSITION_ALPHA}', day, hour)
                double percent = value('${PERCENT_OF_DAY}', day, hour)
                assertTrue("alpha $alpha on day $day at $hour", alpha >= 0d && alpha <= 255d)
                assertTrue("percent $percent on day $day at $hour", percent >= 0d && percent <= 1d)
            }
        }
    }

    private double value(String name, int day, double hour) {
        return expressions.evaluate(name, LATITUDE, LONGITUDE, 2026, day, hour)
    }
}
