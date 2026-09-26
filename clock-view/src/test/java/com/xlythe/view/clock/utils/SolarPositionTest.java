package com.xlythe.view.clock.utils;

import org.junit.Test;

import java.time.Instant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SolarPositionTest {
    private static final double TORONTO_LATITUDE = 43.65;
    private static final double TORONTO_LONGITUDE = -79.38333;

    @Test
    public void torontoSunsetIsAtTheApparentHorizon() {
        SolarPosition sun = toronto("2026-09-26T23:08:00Z");
        assertEquals(SolarPosition.HORIZON_DEGREES, sun.altitudeDegrees, 0.2);
        assertTrue(sun.hourAngleDegrees > 0);
        assertTrue(sun.transitionAlpha() > 0.95);
    }

    @Test
    public void summerTwilightLastsLongerThanAutumnTwilight() {
        SolarPosition summer = toronto("2026-06-22T02:18:00Z"); // 75 minutes after sunset
        SolarPosition autumn = toronto("2026-09-27T00:23:00Z"); // 75 minutes after sunset
        assertTrue(summer.altitudeDegrees > SolarPosition.NIGHT_DEGREES);
        assertTrue(summer.transitionAlpha() > 0);
        assertTrue(autumn.altitudeDegrees < SolarPosition.NIGHT_DEGREES);
        assertEquals(0, autumn.transitionAlpha(), 0);
    }

    @Test
    public void transitionArtworkIsAbsentAtNoonAndDeepNight() {
        assertEquals(0, toronto("2026-09-26T16:00:00Z").transitionAlpha(), 0);
        assertEquals(0, toronto("2026-09-27T05:00:00Z").transitionAlpha(), 0);
    }

    @Test
    public void midnightSunHasNoFalseSunset() {
        SolarPosition polarSummer = SolarPosition.at(
                Instant.parse("2026-06-21T00:00:00Z").toEpochMilli(), 70, 20);
        assertTrue(polarSummer.altitudeDegrees > SolarPosition.HORIZON_DEGREES);
        assertTrue(!polarSummer.crossesHorizon);
    }

    private static SolarPosition toronto(String utc) {
        return SolarPosition.at(Instant.parse(utc).toEpochMilli(),
                TORONTO_LATITUDE, TORONTO_LONGITUDE);
    }
}
