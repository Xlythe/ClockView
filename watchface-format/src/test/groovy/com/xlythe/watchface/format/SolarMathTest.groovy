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

    private final Map<String, String> mResolved = resolveStandardVariables()


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

    /** Resolves one variable for a place and a date, then works out what it comes to. */
    private double evaluate(String variable, double latitude, double longitude, int year, int dayOfYear) {
        String expression = mResolved[variable]
        assert expression != null: "$variable is not defined in standard.xml"
        expression = expression
                .replace('${LATITUDE}', "($latitude)")
                .replace('${LONGITUDE}', "($longitude)")
        // The platform's own sources, at noon UTC in a zone with no offset.
                .replace('[YEAR]', "($year)")
                .replace('[DAY_OF_YEAR]', "($dayOfYear)")
                .replace('[HOUR_0_23]', '(12)')
                .replace('[MINUTE]', '(0)')
                .replace('[SECOND]', '(0)')
                .replace('[MILLISECOND]', '(0)')
                .replace('[UTC_TIMESTAMP]', '(43200000)')
                .replace('&lt;', '<').replace('&gt;', '>').replace('&amp;', '&')
                .replace('&quot;', '"').replace('&apos;', "'")
        return new Evaluator(expression).evaluate()
    }

    private static Map<String, String> resolveStandardVariables() {
        String xml = SolarMathTest.class
                .getResourceAsStream('/com/xlythe/watchface/format/variables/standard.xml')
                .getText('UTF-8')
        Map<String, String> variables = TemplateProcessor.parseVariables(xml, 'standard.xml')
        return TemplateProcessor.resolve(variables)
    }

    /** Just enough of the Watch Face Format's arithmetic to work out a number. */
    private static class Evaluator {
        private final String mText
        private int mAt = 0

        Evaluator(String text) {
            mText = text
        }

        double evaluate() {
            double value = readSum()
            skipSpace()
            if (mAt < mText.length()) {
                throw new IllegalStateException("Unparsed from ${mAt}: ${mText.substring(mAt, Math.min(mAt + 40, mText.length()))}")
            }
            return value
        }

        private double readSum() {
            double value = readProduct()
            while (true) {
                skipSpace()
                if (take('+')) {
                    value += readProduct()
                } else if (take('-')) {
                    value -= readProduct()
                } else {
                    return value
                }
            }
        }

        private double readProduct() {
            double value = readTerm()
            while (true) {
                skipSpace()
                if (take('*')) {
                    value *= readTerm()
                } else if (take('/')) {
                    value /= readTerm()
                } else if (take('%')) {
                    value %= readTerm()
                } else {
                    return value
                }
            }
        }

        private double readTerm() {
            skipSpace()
            if (take('-')) {
                return -readTerm()
            }
            if (take('+')) {
                return readTerm()
            }
            if (take('(')) {
                double value = readSum()
                skipSpace()
                expect(')')
                return value
            }
            int start = mAt
            while (mAt < mText.length() && Character.isLetter(mText.charAt(mAt))) {
                mAt++
            }
            if (mAt > start) {
                return readCall(mText.substring(start, mAt))
            }
            while (mAt < mText.length()
                    && (Character.isDigit(mText.charAt(mAt)) || mText.charAt(mAt) == ('.' as char))) {
                mAt++
            }
            if (mAt == start) {
                throw new IllegalStateException("Expected a number at $mAt: ${mText.substring(start, Math.min(start + 40, mText.length()))}")
            }
            return Double.parseDouble(mText.substring(start, mAt))
        }

        private double readCall(String name) {
            skipSpace()
            expect('(')
            List<Double> arguments = []
            arguments.add(readSum())
            skipSpace()
            while (take(',')) {
                arguments.add(readSum())
                skipSpace()
            }
            expect(')')
            switch (name) {
                case 'sin': return Math.sin(arguments[0])
                case 'cos': return Math.cos(arguments[0])
                case 'tan': return Math.tan(arguments[0])
                case 'asin': return Math.asin(arguments[0])
                case 'acos': return Math.acos(arguments[0])
                case 'rad': return Math.toRadians(arguments[0])
                case 'deg': return Math.toDegrees(arguments[0])
                case 'abs': return Math.abs(arguments[0])
                case 'floor': return Math.floor(arguments[0])
                case 'round': return Math.round(arguments[0]) as double
                case 'sqrt': return Math.sqrt(arguments[0])
                case 'clamp': return Math.max(arguments[1], Math.min(arguments[2], arguments[0]))
                default: throw new IllegalStateException("No such function: $name")
            }
        }

        private void skipSpace() {
            while (mAt < mText.length() && Character.isWhitespace(mText.charAt(mAt))) {
                mAt++
            }
        }

        private boolean take(String expected) {
            skipSpace()
            if (mAt < mText.length() && mText.charAt(mAt) == expected.charAt(0)) {
                mAt++
                return true
            }
            return false
        }

        private void expect(String expected) {
            if (!take(expected)) {
                throw new IllegalStateException("Expected '$expected' at $mAt: ${mText.substring(Math.max(0, mAt - 20), Math.min(mAt + 20, mText.length()))}")
            }
        }
    }
}
