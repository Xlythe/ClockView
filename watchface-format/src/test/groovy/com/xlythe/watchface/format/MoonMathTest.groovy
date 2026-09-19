package com.xlythe.watchface.format

import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

/**
 * Checks where standard.xml puts the moon in the sky.
 *
 * <p>Like the sunrise maths, the expressions that ship are the ones evaluated, and what's
 * asserted is astronomy rather than an almanac. The sharpest of these is the pair about full and
 * new moons: a full moon is opposite the sun, so it climbs highest around local midnight, and a
 * new moon sits with the sun, so it climbs highest around noon. Getting that right means the
 * ecliptic terms, the tilt to the equator, sidereal time and the hour angle all agree.
 */
class MoonMathTest {
    private static final double LONDON_LAT = 51.5d
    private static final double LONDON_LON = 0d

    private final WffExpressions mExpressions = new WffExpressions()

    @Test
    void theMoonStaysInTheSky() {
        for (int dayOfYear : (1..360).step(7)) {
            for (double hour : [0d, 6d, 12d, 18d]) {
                double altitude = altitude(LONDON_LAT, LONDON_LON, dayOfYear, hour)
                double azimuth = azimuth(LONDON_LAT, LONDON_LON, dayOfYear, hour)
                assertTrue("altitude $altitude on day $dayOfYear at $hour:00",
                        altitude >= -90d && altitude <= 90d)
                assertTrue("azimuth $azimuth on day $dayOfYear at $hour:00",
                        azimuth >= 0d && azimuth <= 360d)
            }
        }
    }

    /** The moon's orbit is tilted about five degrees to the ecliptic, and the ecliptic 23.4. */
    @Test
    void theMoonStaysWithinTheBandItCanReach() {
        double furthest = 0d
        for (int dayOfYear : (1..360).step(3)) {
            double declination = Math.abs(
                    mExpressions.evaluate('${MOON_DECLINATION_DEG}', LONDON_LAT, LONDON_LON, 2026, dayOfYear))
            furthest = Math.max(furthest, declination)
        }
        assertTrue("the moon reached a declination of $furthest degrees", furthest < 28.8d)
        assertTrue("the moon never got beyond $furthest degrees, which is too close to the equator",
                furthest > 17d)
    }

    /** It falls behind the sun by about thirteen degrees a day, which is why it rises later. */
    @Test
    void theMoonMovesThirteenDegreesADay() {
        double first = mExpressions.evaluate('${MOON_ECLIPTIC_LONGITUDE_DEG}', 0d, 0d, 2026, 100)
        double second = mExpressions.evaluate('${MOON_ECLIPTIC_LONGITUDE_DEG}', 0d, 0d, 2026, 101)
        double moved = ((second - first) % 360d + 360d) % 360d
        assertEquals("the moon moved $moved degrees in a day", 13.2d, moved, 2.5d)
    }

    @Test
    void aFullMoonIsHighestAroundMidnight() {
        int fullMoon = findMoonAtElongation(180d)
        double highest = hourOfHighestMoon(fullMoon)
        double fromMidnight = Math.min(highest, 24d - highest)
        assertTrue("the full moon on day $fullMoon was highest at ${highest}:00, "
                + "which is ${fromMidnight} hours from midnight", fromMidnight < 2.5d)
    }

    @Test
    void aNewMoonIsHighestAroundNoon() {
        int newMoon = findMoonAtElongation(0d)
        double highest = hourOfHighestMoon(newMoon)
        assertTrue("the new moon on day $newMoon was highest at ${highest}:00, not near noon",
                Math.abs(highest - 12d) < 2.5d)
    }

    /** Over a month the moon is above the horizon about half the time. */
    @Test
    void theMoonIsUpAboutHalfTheTime() {
        int up = 0
        int total = 0
        for (int dayOfYear : (100..130)) {
            for (int hour : (0..23)) {
                if (altitude(LONDON_LAT, LONDON_LON, dayOfYear, hour as double) > 0d) {
                    up++
                }
                total++
            }
        }
        double fraction = up / (double) total
        assertEquals("the moon was up for ${Math.round(fraction * 100)}% of the month",
                0.5d, fraction, 0.1d)
    }

    /** The day in 2026 when the moon sits the given angle away from the sun. */
    private int findMoonAtElongation(double wanted) {
        int best = 1
        double closest = 360d
        for (int dayOfYear : (1..60)) {
            double moon = mExpressions.evaluate('${MOON_ECLIPTIC_LONGITUDE_DEG}', 0d, 0d, 2026, dayOfYear)
            double sun = mExpressions.evaluate('${SUN_APP_LONG_DEG}', 0d, 0d, 2026, dayOfYear)
            double elongation = ((moon - sun) % 360d + 360d) % 360d
            double off = Math.abs(((elongation - wanted) % 360d + 540d) % 360d - 180d)
            if (off < closest) {
                closest = off
                best = dayOfYear
            }
        }
        assertTrue("no day in the window put the moon $wanted degrees from the sun", closest < 7d)
        return best
    }

    /** The hour, to a quarter, at which the moon climbs highest on a given day. */
    private double hourOfHighestMoon(int dayOfYear) {
        double bestHour = 0d
        double bestAltitude = -91d
        for (double hour = 0d; hour < 24d; hour += 0.25d) {
            double altitude = altitude(LONDON_LAT, LONDON_LON, dayOfYear, hour)
            if (altitude > bestAltitude) {
                bestAltitude = altitude
                bestHour = hour
            }
        }
        return bestHour
    }

    private double altitude(double latitude, double longitude, int dayOfYear, double hour) {
        return mExpressions.evaluate('${MOON_ALTITUDE_DEG}', latitude, longitude, 2026, dayOfYear, hour)
    }

    private double azimuth(double latitude, double longitude, int dayOfYear, double hour) {
        return mExpressions.evaluate('${MOON_AZIMUTH_DEG}', latitude, longitude, 2026, dayOfYear, hour)
    }
}
