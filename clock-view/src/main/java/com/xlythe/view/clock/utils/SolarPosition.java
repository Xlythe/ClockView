package com.xlythe.view.clock.utils;

/** The Sun's geometric position for a moment and a point on Earth. */
public final class SolarPosition {
    public static final double HORIZON_DEGREES = -0.833;
    public static final double TRANSITION_START_DEGREES = 6.0;
    public static final double NIGHT_DEGREES = -12.0;

    public final double altitudeDegrees;
    public final double hourAngleDegrees;
    public final boolean crossesHorizon;

    private SolarPosition(double altitudeDegrees, double hourAngleDegrees, boolean crossesHorizon) {
        this.altitudeDegrees = altitudeDegrees;
        this.hourAngleDegrees = hourAngleDegrees;
        this.crossesHorizon = crossesHorizon;
    }

    /** NOAA solar equations, evaluated in UTC so device time zone and DST do not affect the Sun. */
    public static SolarPosition at(long epochMillis, double latitude, double longitude) {
        double century = (epochMillis / 86400000.0 + 2440587.5 - 2451545.0) / 36525.0;
        double meanLongitude = positiveMod(280.46646 + century *
                (36000.76983 + century * 0.0003032), 360.0);
        double meanAnomaly = 357.52911 + century * (35999.05029 - 0.0001537 * century);
        double eccentricity = 0.016708634 - century * (0.000042037 + 0.0000001267 * century);
        double center = Math.sin(Math.toRadians(meanAnomaly)) *
                (1.914602 - century * (0.004817 + 0.000014 * century))
                + Math.sin(Math.toRadians(2 * meanAnomaly)) * (0.019993 - 0.000101 * century)
                + Math.sin(Math.toRadians(3 * meanAnomaly)) * 0.000289;
        double apparentLongitude = meanLongitude + center - 0.00569
                - 0.00478 * Math.sin(Math.toRadians(125.04 - 1934.136 * century));
        double obliquity = 23 + (26 + (21.448 - century *
                (46.815 + century * (0.00059 - century * 0.001813))) / 60) / 60
                + 0.00256 * Math.cos(Math.toRadians(125.04 - 1934.136 * century));
        double declination = Math.toDegrees(Math.asin(Math.sin(Math.toRadians(obliquity))
                * Math.sin(Math.toRadians(apparentLongitude))));
        double y = Math.pow(Math.tan(Math.toRadians(obliquity / 2)), 2);
        double equationOfTime = 4 * Math.toDegrees(
                y * Math.sin(2 * Math.toRadians(meanLongitude))
                - 2 * eccentricity * Math.sin(Math.toRadians(meanAnomaly))
                + 4 * eccentricity * y * Math.sin(Math.toRadians(meanAnomaly))
                    * Math.cos(2 * Math.toRadians(meanLongitude))
                - 0.5 * y * y * Math.sin(4 * Math.toRadians(meanLongitude))
                - 1.25 * eccentricity * eccentricity * Math.sin(2 * Math.toRadians(meanAnomaly)));

        double utcMinutes = positiveMod(epochMillis / 60000.0, 1440.0);
        double solarMinutes = positiveMod(utcMinutes + equationOfTime + 4 * longitude, 1440.0);
        double hourAngle = solarMinutes / 4 - 180;
        double sineAltitude =
                Math.sin(Math.toRadians(latitude)) * Math.sin(Math.toRadians(declination))
                + Math.cos(Math.toRadians(latitude)) * Math.cos(Math.toRadians(declination))
                    * Math.cos(Math.toRadians(hourAngle));
        double altitude = Math.toDegrees(Math.asin(Math.max(-1, Math.min(1, sineAltitude))));
        double horizonCosine = (Math.sin(Math.toRadians(HORIZON_DEGREES))
                - Math.sin(Math.toRadians(latitude)) * Math.sin(Math.toRadians(declination)))
                / (Math.cos(Math.toRadians(latitude)) * Math.cos(Math.toRadians(declination)));
        return new SolarPosition(altitude, hourAngle,
                horizonCosine >= -1 && horizonCosine <= 1);
    }

    /** Alpha of the warm transition artwork, peaking when the Sun meets the horizon. */
    public double transitionAlpha() {
        if (altitudeDegrees >= HORIZON_DEGREES) {
            return clamp((TRANSITION_START_DEGREES - altitudeDegrees)
                    / (TRANSITION_START_DEGREES - HORIZON_DEGREES));
        }
        return clamp((altitudeDegrees - NIGHT_DEGREES)
                / (HORIZON_DEGREES - NIGHT_DEGREES));
    }

    private static double positiveMod(double value, double divisor) {
        return ((value % divisor) + divisor) % divisor;
    }

    private static double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }
}
