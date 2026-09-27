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

    /** The XML writer uses exactly the expressions supplied by the caller. */
    @Test
    void theXmlWriterUsesTheSuppliedExpression() {
        Map<String, String> resolved = [
                '${A}': '(1)',
                '${B}': '((1) + 2)',
        ]
        String xml = SharedValues.publisherXml(resolved, ['A', 'B'])
        assertTrue("B should hold the supplied expression:\n$xml",
                xml.contains('value="((1) + 2)"'))
    }

    @Test
    void publishersShareInputsInDependencyOrder() {
        Map<String, String> declared = [
                '${LATITUDE}': '([TIMEZONE_ID] == &quot;America/Toronto&quot; ? 43.65 : 0)',
                '${ALTITUDE}': '(${LATITUDE} + [MINUTE])',
                '${ALPHA}'   : '(255 * ${ALTITUDE})',
                '${POSITION}': '(${ALTITUDE} / 90)',
        ]
        SharedValues.Plan plan = SharedValues.plan(declared,
                ['ALPHA', 'ALTITUDE', 'LATITUDE'])

        assertEquals(['LATITUDE', 'ALTITUDE', 'ALPHA'], plan.publisherOrder)
        assertTrue(plan.publisherExpressions['${ALTITUDE}'].contains('[REFERENCE.LATITUDE]'))
        assertTrue(plan.publisherExpressions['${ALPHA}'].contains('[REFERENCE.ALTITUDE]'))
        assertTrue(plan.templateVariables['${POSITION}'].contains('[REFERENCE.ALTITUDE]'))
        assertEquals('[REFERENCE.ALPHA]', plan.templateVariables['${ALPHA}'])
        String xml = SharedValues.publisherXml(plan.publisherExpressions, plan.publisherOrder)
        assertTrue(xml.indexOf('name="LATITUDE"') < xml.indexOf('name="ALTITUDE"'))
        assertTrue(xml.indexOf('name="ALTITUDE"') < xml.indexOf('name="ALPHA"'))
    }

    @Test
    void cyclicPublishersAreRejected() {
        try {
            SharedValues.plan(['${A}': '(${B} + 1)', '${B}': '(${A} + 1)'], ['A', 'B'])
            fail('Expected a cyclic reference to be rejected')
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.message, expected.message.contains('depends on itself'))
        }
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
