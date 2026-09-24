package com.xlythe.watchface.format

import org.junit.Test

import java.time.LocalDate
import java.time.ZoneId

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

/**
 * Checks the bundled time zone table against the JDK's own copy of the IANA database.
 *
 * <p>The table is the only thing telling the watch where it is - Watch Face Format has no
 * location source - so a wrong row moves sunrise, and a wrong offset moves the moon across the
 * sky. Both were generated, and generated data is worth checking against something that did not
 * generate it.
 */
class TimeZoneTableTest {

    private static final List<List<String>> ROWS = parse()

    @Test
    void everyZoneIsRealAndIsNamedOnce() {
        Set<String> seen = []
        for (List<String> row : ROWS) {
            String zone = row[0]
            assertTrue("$zone appears twice", seen.add(zone))
            try {
                ZoneId.of(zone)
            } catch (Exception e) {
                // Bougainville split from Port Moresby after some JDK bundles were cut.
                assertTrue("$zone is not a time zone this JDK knows", zone == 'Asia/Bougainville')
            }
        }
        assertTrue("only ${ROWS.size()} zones in the table", ROWS.size() > 350)
    }

    /** Zones that have kept the same standard offset for decades, so any JDK agrees about them. */
    private static final Map<String, Integer> SETTLED = [
            'Europe/London'      : 0,
            'Europe/Paris'       : 60,
            'Europe/Moscow'      : 180,
            'Asia/Kolkata'       : 330,
            'Asia/Tokyo'         : 540,
            'Australia/Sydney'   : 600,
            'Pacific/Auckland'   : 720,
            'America/New_York'   : -300,
            'America/Chicago'    : -360,
            'America/Denver'     : -420,
            'America/Los_Angeles': -480,
            'Pacific/Honolulu'   : -600,
            'America/Sao_Paulo'  : -180,
            'Africa/Johannesburg': 120,
            'Asia/Kathmandu'     : 345,
            'Europe/Dublin'      : 0,
    ]

    /**
     * Current standard offsets and the values still reported by JDK 19's tzdb 2022c. The table
     * was checked against IANA 2026d; these exact differences are historical JDK data, not table
     * errors. Keep the current values pinned so this exception cannot conceal a bad edit.
     */
    private static final Map<String, List<Integer>> CHANGED_SINCE_2022C = [
            'America/Asuncion'     : [-180, -240],
            'America/Chihuahua'    : [-360, -420],
            'America/Godthab'      : [-120, -180],
            'America/Nuuk'         : [-120, -180],
            'America/Ojinaga'      : [-360, -420],
            'America/Scoresbysund' : [-120, -60],
            'Antarctica/Casey'     : [480, 660],
            'Antarctica/Vostok'    : [300, 360],
            'Asia/Almaty'         : [300, 360],
            'Asia/Amman'          : [180, 120],
            'Asia/Damascus'       : [180, 120],
            'Asia/Qostanay'       : [300, 360],
    ]

    @Test
    void theSettledZonesAreExactlyRight() {
        Map<String, Integer> table = ROWS.collectEntries { [(it[0]): it[3] as Integer] }
        SETTLED.each { zone, expected ->
            assertEquals(zone, expected, table[zone])
        }
    }

    @Test
    void recentlyChangedZonesKeepTheirCurrentOffsets() {
        Map<String, Integer> table = ROWS.collectEntries { [(it[0]): it[3] as Integer] }
        CHANGED_SINCE_2022C.each { zone, offsets ->
            assertEquals(zone, offsets[0], table[zone])
        }
    }

    @Test
    void dublinUsesTheSameDaylightConventionAsTheWatch() {
        Node dublin = (Node) TemplateProcessor.parse(read()).children().find {
            it instanceof Node && it.attribute('name') == 'Europe/Dublin'
        }
        assertTrue('Dublin is missing from the table', dublin != null)
        int base = dublin.attribute('utcOffsetMinutes') as int
        int daylight = (dublin.attribute('dstMinutes') ?: '60') as int
        TimeZone zone = TimeZone.getTimeZone('Europe/Dublin')
        for (int month : [1, 7]) {
            Date date = Date.from(LocalDate.of(2026, month, 15).atStartOfDay(ZoneId.of('Europe/Dublin')).toInstant())
            int actual = zone.getOffset(date.time) / 60000
            int computed = base + (zone.inDaylightTime(date) ? daylight : 0)
            assertEquals("Dublin month $month", actual, computed)
        }
    }

    @Test
    void everyOffsetIsAWholeQuarterHourAndOnEarth() {
        for (List<String> row : ROWS) {
            int minutes = row[3] as Integer
            assertTrue("${row[0]} is ${minutes} minutes from UTC", minutes >= -720 && minutes <= 840)
            assertTrue("${row[0]} is ${minutes} minutes from UTC, which is not a quarter hour",
                    minutes % 15 == 0)
        }
    }

    /** Compare with the JDK while recognizing only the exact changes its older tzdb may lack. */
    @Test
    void theTableAgreesWithTheDatabaseAlmostEverywhere() {
        List<String> differing = []
        int checked = 0
        for (List<String> row : ROWS) {
            ZoneId id
            try {
                id = ZoneId.of(row[0])
            } catch (Exception ignored) {
                continue
            }
            Integer expected = null
            for (int month : [1, 7]) {
                def instant = LocalDate.of(2026, month, 15).atStartOfDay(id).toInstant()
                if (!id.rules.isDaylightSavings(instant)) {
                    expected = (int) (id.rules.getOffset(instant).totalSeconds / 60)
                    break
                }
            }
            if (expected == null) {
                continue
            }
            checked++
            int table = row[3] as Integer
            if (expected.intValue() != table) {
                List<Integer> currentAndOld = CHANGED_SINCE_2022C[row[0]]
                if (currentAndOld == null || currentAndOld[0] != table || currentAndOld[1] != expected) {
                    differing.add("${row[0]} table=${table} jdk=${expected}".toString())
                }
            }
        }
        assertTrue("only $checked offsets were checkable", checked > 300)
        assertTrue("${differing.size()} of $checked zones differ beyond known JDK tzdata drift: ${differing}",
                differing.isEmpty())
    }

    @Test
    void coordinatesAreOnTheGlobe() {
        for (List<String> row : ROWS) {
            double latitude = row[1] as double
            double longitude = row[2] as double
            assertTrue("${row[0]} sits at latitude $latitude", latitude >= -90d && latitude <= 90d)
            assertTrue("${row[0]} sits at longitude $longitude", longitude >= -180d && longitude <= 180d)
        }
    }

    /** The curated zones come first so that trimming the table keeps the ones worth having. */
    @Test
    void theCuratedZonesLeadTheTable() {
        assertEquals('Pacific/Midway', ROWS[0][0])
        assertEquals('America/Los_Angeles', ROWS[3][0])
        assertTrue('the IANA zones should follow the curated ones',
                ROWS[200][0] != null && ROWS.size() > 200)
    }

    @Test
    void trimmingKeepsTheTopOfTheTable() {
        Map<String, String> all = TemplateProcessor.parseTimeZoneCoordinates(read())
        Map<String, String> few = TemplateProcessor.parseTimeZoneCoordinates(read(), 3, '0', '0')

        assertTrue('trimming should shorten the lookup',
                few['${LATITUDE}'].length() < all['${LATITUDE}'].length())
        assertTrue('the first zone should survive', few['${LATITUDE}'].contains('Pacific/Midway'))
        assertTrue('a zone past the limit should not', !few['${LATITUDE}'].contains('America/Denver'))
    }

    @Test
    void theFallbackIsUsedForZonesOffTheTable() {
        Map<String, String> few = TemplateProcessor.parseTimeZoneCoordinates(read(), 1, '51.5', '-0.13')
        assertTrue('the fallback latitude is missing', few['${LATITUDE}'].endsWith('51.5)'))
        assertTrue('the fallback longitude is missing', few['${LONGITUDE}'].endsWith('-0.13)'))
    }

    /** Only the zones that shift by something other than an hour need naming. */
    @Test
    void daylightSavingNamesOnlyTheOddOnes() {
        String daylight = TemplateProcessor.parseTimeZoneCoordinates(read())['${TIMEZONE_DAYLIGHT_MINUTES}']
        assertTrue('Lord Howe shifts half an hour', daylight.contains('Australia/Lord_Howe'))
        assertTrue('Troll shifts two hours', daylight.contains('Antarctica/Troll'))
        assertTrue('an ordinary zone should fall through to 60', !daylight.contains('Europe/London'))
        assertTrue('a zone with no daylight saving should fall through too',
                !daylight.contains('Asia/Tokyo'))
    }

    private static List<List<String>> parse() {
        List<List<String>> rows = []
        read().eachLine { String line ->
            def match = line =~ /<Location name="([^"]+)" latitude="([-0-9.]+)" longitude="([-0-9.]+)" utcOffsetMinutes="(-?\d+)"/
            if (match.find()) {
                rows.add([match.group(1), match.group(2), match.group(3), match.group(4)])
            }
        }
        return rows
    }

    private static String read() {
        return TimeZoneTableTest.class
                .getResourceAsStream('/com/xlythe/watchface/format/geo/timezones.xml').getText('UTF-8')
    }
}
