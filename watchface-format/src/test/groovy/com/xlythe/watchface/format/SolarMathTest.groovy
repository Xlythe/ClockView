package com.xlythe.watchface.format

import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

/**
 * Checks the sunrise and sunset maths in the bundled standard.xml.
 *
 * <p>The expressions under test are the ones that ship: standard.xml is read off the classpath,
 * resolved the way GenerateWatchFaceTask resolves it, and the resulting string is evaluated. A
 * re-implementation here would only prove that two copies of the algorithm agree.
 *
 * <p>What's asserted is astronomy rather than a table of times, so the test needs no almanac and
 * doesn't rot: the sun is up for twelve hours everywhere at an equinox, a day at the equator is
 * twelve hours all year, and sunrise and sunset sit either side of solar noon.
 */
class SolarMathTest {
    /** Sunrise is reckoned to the top of the disc through refraction, which adds a few minutes. */
    private static final double EQUINOX_DAY_HOURS = 12.1d
    private static final double TOLERANCE_MINUTES = 12d

    private final WffExpressions mExpressions = new WffExpressions()


    @Test
    void atAnEquinoxTheDayIsTwelveHoursAtEveryLatitude() {
        // 20 March 2026 and 23 September 2026.
        for (int dayOfYear : [79, 266]) {
            for (double latitude : [-60d, -35d, -10d, 0d, 10d, 35d, 60d]) {
                double hours = dayLengthHours(latitude, 0d, 2026, dayOfYear)
                assertEquals("day $dayOfYear at latitude $latitude lasts $hours hours",
                        EQUINOX_DAY_HOURS, hours, TOLERANCE_MINUTES / 60d)
            }
        }
    }

    @Test
    void atTheEquatorEveryDayIsTwelveHours() {
        for (int dayOfYear : [1, 79, 172, 266, 355]) {
            double hours = dayLengthHours(0d, 0d, 2026, dayOfYear)
            assertEquals("day $dayOfYear at the equator lasts $hours hours",
                    EQUINOX_DAY_HOURS, hours, TOLERANCE_MINUTES / 60d)
        }
    }

    @Test
    void sunriseAndSunsetSitEitherSideOfSolarNoon() {
        double noon = evaluate('${SOLAR_NOON_LST}', 51.5d, -0.13d, 2026, 172) * 24d
        double sunrise = sunriseHours(51.5d, -0.13d, 2026, 172)
        double sunset = sunsetHours(51.5d, -0.13d, 2026, 172)

        assertTrue("sunrise $sunrise is not before solar noon $noon", sunrise < noon)
        assertTrue("sunset $sunset is not after solar noon $noon", sunset > noon)
        assertEquals("sunrise and sunset are not symmetric about solar noon",
                noon - sunrise, sunset - noon, 1e-6d)
    }

    @Test
    void theFurtherNorthInJuneTheLongerTheDay() {
        double previous = 0d
        for (double latitude : [0d, 20d, 40d, 55d, 63d]) {
            double hours = dayLengthHours(latitude, 0d, 2026, 172)
            assertTrue("day at $latitude lasts $hours hours, no longer than $previous further south",
                    hours > previous)
            previous = hours
        }
    }

    /**
     * Midsummer in the north is midwinter in the south, so the two day lengths would add up to
     * exactly a day if the sun were a point on a geometric horizon. It's a disc seen through an
     * atmosphere, and sunrise is reckoned at a zenith of 90.833 degrees, which lengthens both
     * days - the more so the shallower the sun's approach, so the excess grows with latitude.
     */
    @Test
    void theHemispheresAreMirroredApartFromRefraction() {
        double previousExcess = 0d
        for (double latitude : [20d, 40d, 55d]) {
            double north = dayLengthHours(latitude, 0d, 2026, 172)
            double south = dayLengthHours(-latitude, 0d, 2026, 172)
            double excess = north + south - 24d

            assertTrue("$north hours at +$latitude and $south at -$latitude come to less than a day",
                    excess > 0d)
            assertTrue("refraction added $excess hours at $latitude degrees, which is too much",
                    excess < 1d)
            assertTrue("the excess shrank from $previousExcess to $excess going north",
                    excess > previousExcess)
            previousExcess = excess
        }
    }

    @Test
    void longitudeMovesSunriseByFourMinutesPerDegree() {
        // Same time zone, a degree of longitude apart: the sun arrives four minutes later.
        double atZero = sunriseHours(45d, 0d, 2026, 100)
        double atOne = sunriseHours(45d, -1d, 2026, 100)
        assertEquals("a degree of longitude moved sunrise by ${(atOne - atZero) * 60} minutes",
                4d / 60d, atOne - atZero, 1e-3d)
    }

    /**
     * Above the Arctic Circle in midsummer the sun never sets, and the hour angle is the arc
     * cosine of something outside -1..1. The expression yields NaN rather than a time, so a watch
     * face that reads it that far north shows neither sunrise nor sunset.
     */
    @Test
    void insideTheArcticCircleInJuneThereIsNoSunrise() {
        double sunrise = sunriseHours(80d, 0d, 2026, 172)
        assertTrue("sunrise at 80 degrees north in June evaluated to $sunrise", Double.isNaN(sunrise))
    }

    private double dayLengthHours(double latitude, double longitude, int year, int dayOfYear) {
        return sunsetHours(latitude, longitude, year, dayOfYear) -
                sunriseHours(latitude, longitude, year, dayOfYear)
    }

    private double sunriseHours(double latitude, double longitude, int year, int dayOfYear) {
        return evaluate('${SUNRISE_MILLIS_SINCE_MIDNIGHT}', latitude, longitude, year, dayOfYear) / 3600000d
    }

    private double sunsetHours(double latitude, double longitude, int year, int dayOfYear) {
        return evaluate('${SUNSET_MILLIS_SINCE_MIDNIGHT}', latitude, longitude, year, dayOfYear) / 3600000d
    }

    private double evaluate(String variable, double latitude, double longitude, int year, int dayOfYear) {
        return mExpressions.evaluate(variable, latitude, longitude, year, dayOfYear)
    }
}
