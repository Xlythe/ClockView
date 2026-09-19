package com.xlythe.watchface.format

import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue
import static org.junit.Assert.fail

class TemplateProcessorTest {
    @Test
    void parseVariables_wrapsInParenthesesAndEscapes() {
        Map<String, String> variables = TemplateProcessor.parseVariables('''
            <ItemList>
                <Item name="${IS_MORNING}">
                    <![CDATA[
                        [HOUR_0_23] < 12
                        && [TIMEZONE_ID] == "UTC"
                    ]]>
                </Item>
            </ItemList>''', 'vars.xml')

        assertEquals('([HOUR_0_23] &lt; 12 &amp;&amp; [TIMEZONE_ID] == &quot;UTC&quot;)', variables['${IS_MORNING}'])
    }

    @Test
    void parseVariables_rejectsMismatchedParentheses() {
        try {
            TemplateProcessor.parseVariables('<ItemList><Item name="${BAD}">((1 + 2)</Item></ItemList>', 'vars.xml')
            fail('Expected mismatched parentheses to be rejected')
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.message.contains('${BAD}'))
        }
    }

    @Test
    void resolve_substitutesNestedVariables() {
        Map<String, String> resolved = TemplateProcessor.resolve([
                '${TOTAL}': '(${A} + ${B})',
                '${A}'    : '(1)',
                '${B}'    : '(${A} * 2)',
        ])

        assertEquals('((1) + ((1) * 2))', resolved['${TOTAL}'])
    }

    @Test
    void resolve_rejectsCycles() {
        try {
            TemplateProcessor.resolve(['${A}': '(${B})', '${B}': '(${A})'])
            fail('Expected a cycle to be rejected')
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.message.contains('refers to itself'))
        }
    }

    @Test
    void expand_dropsOuterParenthesesForWholeAttributeValues() {
        String expanded = TemplateProcessor.expand(
                '<Group alpha="${ALPHA}"><Expression name="x">${ALPHA} + 1</Expression></Group>',
                ['${ALPHA}': '(255 * [STEP_PERCENT])'])

        assertEquals('<Group alpha="255 * [STEP_PERCENT]"><Expression name="x">(255 * [STEP_PERCENT]) + 1</Expression></Group>', expanded)
    }

    @Test
    void expand_leavesUnknownPlaceholders() {
        String expanded = TemplateProcessor.expand('<Group alpha="${MISSING}" />', [:])

        assertEquals(['${MISSING}'] as Set, TemplateProcessor.findPlaceholders(expanded))
    }

    @Test
    void withReplacements_doesNotModifyTheSharedVariables() {
        Map<String, String> shared = ['${RAINY}': '([WEATHER.IS_AVAILABLE] && [WEATHER.CONDITION] == 6)']

        Map<String, String> wff1 = TemplateProcessor.withReplacements(shared, ['[WEATHER.IS_AVAILABLE]': '0'])

        assertEquals('(0 && [WEATHER.CONDITION] == 6)', wff1['${RAINY}'])
        assertEquals('([WEATHER.IS_AVAILABLE] && [WEATHER.CONDITION] == 6)', shared['${RAINY}'])
    }

    @Test
    void stubWeatherDataSources_replacesEveryWeatherToken() {
        String stubbed = TemplateProcessor.stubWeatherDataSources(
                '[WEATHER.IS_AVAILABLE] && [WEATHER.HOURS.1.IS_DAY] && [WEATHER.TEMPERATURE] > 0 ' +
                        '&& [WEATHER.CONDITION_NAME] != "" && [WEATHER.DAYS.2.CHANCE_OF_PRECIPITATION] < 50 && [HOUR_0_23]')

        assertEquals('0 && 1 && 0 > 0 && &quot;&quot; != "" && 0 < 50 && [HOUR_0_23]', stubbed)
        assertTrue(TemplateProcessor.findDataSources(stubbed, 'WEATHER.').isEmpty())
    }

    @Test
    void parseTimeZoneCoordinates_usesLatitudeAndLongitude() {
        Map<String, String> coordinates = TemplateProcessor.parseTimeZoneCoordinates('''
            <Geo>
                <Location name="America/New_York" latitude="40.71417" longitude="-74.00639" />
                <Location name="Asia/Tokyo" latitude="35.68950" longitude="139.69171" />
            </Geo>''')

        assertEquals('(([TIMEZONE_ID] == &quot;America/New_York&quot;) ? 40.71417 : ([TIMEZONE_ID] == &quot;Asia/Tokyo&quot;) ? 35.68950 : 0)',
                coordinates['${LATITUDE}'])
        assertEquals('(([TIMEZONE_ID] == &quot;America/New_York&quot;) ? -74.00639 : ([TIMEZONE_ID] == &quot;Asia/Tokyo&quot;) ? 139.69171 : 0)',
                coordinates['${LONGITUDE}'])
        assertFalse(coordinates.values().any { it.contains('null') })
    }

    @Test
    void parse_acceptsDocumentsLargerThanTheJdkEntityLimit() {
        String bigExpression = '1 + ' * 60_000 + '1'
        Node root = TemplateProcessor.parse("<WatchFace><Expression>${bigExpression}</Expression></WatchFace>")

        assertTrue(root.text().length() > 200_000)
    }

    @Test
    void findDataSources_listsDistinctTokens() {
        assertEquals(['[WEATHER.CONDITION]', '[WEATHER.IS_AVAILABLE]'] as Set,
                TemplateProcessor.findDataSources('[WEATHER.IS_AVAILABLE] && [WEATHER.CONDITION] == [WEATHER.CONDITION] && [HOUR_0_23]', 'WEATHER.'))
    }

    @Test
    void bundledStandardVariables_resolveWithTimeZoneCoordinates() {
        Map<String, String> declared = new LinkedHashMap<>()
        declared.putAll(TemplateProcessor.parseVariables(resource('variables/standard.xml'), 'standard.xml'))
        declared.putAll(TemplateProcessor.parseTimeZoneCoordinates(resource('geo/timezones.xml')))

        Map<String, String> resolved = TemplateProcessor.resolve(declared)

        assertTrue(resolved.containsKey('${IS_SUNRISE}'))
        resolved.each { key, value -> assertTrue("${key} has unresolved placeholders", TemplateProcessor.findPlaceholders(value).isEmpty()) }
    }

    private static String resource(String path) {
        return TemplateProcessorTest.getResourceAsStream("/com/xlythe/watchface/format/${path}").getText('UTF-8')
    }
}
