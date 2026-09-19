package com.xlythe.watchface.format

import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue
import static org.junit.Assert.fail

class SharedValuesTest {

    private static final Map<String, String> RESOLVED = [
            '${SUNRISE}': '(12 + [HOUR_0_23])',
            '${NOISE}'  : '(1)',
    ]

    @Test
    void aVariableCanBeNamedWithOrWithoutItsBraces() {
        assertEquals('${SUNRISE}', SharedValues.placeholder('SUNRISE'))
        assertEquals('${SUNRISE}', SharedValues.placeholder('${SUNRISE}'))
        assertEquals('SUNRISE', SharedValues.referenceName('${SUNRISE}'))
        assertEquals('SUNRISE', SharedValues.referenceName('SUNRISE'))
    }

    @Test
    void usesBecomeReferences() {
        assertEquals(['${SUNRISE}': '[REFERENCE.SUNRISE]'], SharedValues.asReferences(['SUNRISE']))
    }

    @Test
    void thePublisherCarriesTheValueOnAFloat() {
        String xml = SharedValues.publisherXml(RESOLVED, ['SUNRISE'])

        assertTrue("the expression is missing:\n$xml",
                xml.contains('<Transform target="scaleX" value="(12 + [HOUR_0_23])" />'))
        assertTrue("the reference is missing:\n$xml",
                xml.contains('<Reference name="SUNRISE" source="scaleX" defaultValue="0" />'))
        // alpha is an integer from 0 to 255 and would round whatever it carried.
        assertTrue("the value must not ride on alpha:\n$xml", !xml.contains('target="alpha"'))
    }

    /** A shared variable that reads another one inlines it, rather than chaining references. */
    @Test
    void aPublishedExpressionStandsAlone() {
        Map<String, String> resolved = [
                '${A}': '(1)',
                '${B}': '((1) + 2)',
        ]
        String xml = SharedValues.publisherXml(resolved, ['A', 'B'])
        assertTrue("B should hold A's value, not a reference to it:\n$xml",
                xml.contains('value="((1) + 2)"'))
        assertTrue("nothing published should read a reference:\n$xml", !xml.contains('[REFERENCE.'))
    }

    @Test
    void namingSomethingThatIsNotDefinedSaysSo() {
        try {
            SharedValues.publisherXml(RESOLVED, ['NOT_A_VARIABLE'])
            fail('Expected an unknown shared variable to be rejected')
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.message, expected.message.contains('${NOT_A_VARIABLE}'))
        }
    }

    @Test
    void thePublisherGoesInsideTheScene() {
        String template = '<WatchFace width="450" height="450">\n' +
                '    <Scene backgroundColor="#ff000000">\n' +
                '        <Group name="Face" x="0" y="0" width="450" height="450" />\n' +
                '    </Scene>\n</WatchFace>'
        String merged = SharedValues.insertIntoScene(template, SharedValues.publisherXml(RESOLVED, ['SUNRISE']))

        assertTrue("the publisher is outside the scene:\n$merged",
                merged.indexOf('<Scene') < merged.indexOf('SharedValues'))
        assertTrue("the publisher is after the face that reads it:\n$merged",
                merged.indexOf('SharedValues') < merged.indexOf('name="Face"'))
        assertTrue("the scene is no longer closed:\n$merged", merged.contains('</Scene>'))
    }

    @Test
    void aTemplateWithNoSceneSaysSo() {
        try {
            SharedValues.insertIntoScene('<WatchFace width="450" height="450" />', 'anything')
            fail('Expected a template with no Scene to be rejected')
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.message, expected.message.contains('Scene'))
        }
    }
}
