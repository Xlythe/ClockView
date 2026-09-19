package com.xlythe.watchface.format

import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue
import static org.junit.Assert.fail

class ComplicationSlotExpanderTest {
    private static final String CHIP = '''
        <ComplicationSlot slotId="${COMPLICATION_ID}" x="${POS_X}" y="${POS_Y}" width="${WIDTH}" height="${HEIGHT}">
            <Group name="Layout" x="0" y="0" width="${WIDTH}" height="${HEIGHT}">
                ${BACKGROUND_GROUP}
                <PartImage x="${IMG_POS_X_SINGLE}" y="${IMG_POS_Y_SINGLE}" width="${IMG_WIDTH_SINGLE}" height="${IMG_HEIGHT_SINGLE}" tintColor="${COLOR}" />
                <PartImage x="${IMG_POS_X_SPLIT}" y="${IMG_POS_Y_SPLIT}" width="${IMG_WIDTH_SPLIT}" height="${IMG_HEIGHT_SPLIT}" tintColor="${AMBIENT_COLOR}" />
            </Group>
        </ComplicationSlot>'''

    private final ComplicationSlotExpander expander =
            new ComplicationSlotExpander([chip: CHIP], '#FFFFFFFF', '#FF808080')

    @Test
    void expand_replacesTagWithLayout() {
        Node root = expander.expand(TemplateProcessor.parse('''
            <Scene>
                <com.xlythe.ComplicationSlot slotId="1" x="200" y="200" width="160" height="160" type="chip" complicationDrawableStyle="line" />
            </Scene>'''), 2)

        Node slot = (Node) root.children()[0]
        assertEquals('ComplicationSlot', slot.name())
        assertEquals('1', slot.attribute('slotId'))

        List<Node> images = slot.depthFirst().findAll { it instanceof Node && it.name() == 'PartImage' }
        // 160x160: iconSize = (int)(160 * 0.33) = 52, single icon centered at (int)(80 - 26) = 54.
        assertEquals(['54', '54', '52', '52', '#FFFFFFFF'],
                ['x', 'y', 'width', 'height', 'tintColor'].collect { images[0].attribute(it) })
        // Split icon sits above the text: (int)(80 - 52) = 28.
        assertEquals(['54', '28', '#FF808080'], ['x', 'y', 'tintColor'].collect { images[1].attribute(it) })

        Node stroke = slot.depthFirst().find { it instanceof Node && it.name() == 'Stroke' }
        assertTrue(['#FFFFFFFF', '#FF808080'].contains(stroke.attribute('color')))
    }

    @Test
    void expand_prefersColorsFromTheTag() {
        Node root = expander.expand(TemplateProcessor.parse('''
            <Scene>
                <com.xlythe.ComplicationSlot slotId="2" x="0" y="0" width="120" height="60" type="chip"
                    complicationDrawableStyle="empty" color="[CONFIGURATION.themeColor.0]" ambientColor="#FF000000" />
            </Scene>'''), 2)

        List<String> tints = root.depthFirst().findAll { it instanceof Node && it.name() == 'PartImage' }.collect { it.attribute('tintColor') }
        assertEquals(['[CONFIGURATION.themeColor.0]', '#FF000000'], tints)
        assertFalse(root.depthFirst().any { it instanceof Node && it.name() == 'Stroke' })
    }

    @Test
    void expand_drawsFilledBackgroundsAsEllipses() {
        Node root = expander.expand(TemplateProcessor.parse('''
            <Scene>
                <com.xlythe.ComplicationSlot slotId="3" x="0" y="0" width="100" height="100" type="chip" complicationDrawableStyle="fill" />
            </Scene>'''), 2)

        // Arc only allows a Stroke child, so fills need a shape that accepts Fill.
        assertFalse(root.depthFirst().any { it instanceof Node && it.name() == 'Arc' })
        List<Node> ellipses = root.depthFirst().findAll { it instanceof Node && it.name() == 'Ellipse' }
        assertEquals(2, ellipses.size())
        assertEquals(['#FF808080', '#FFFFFFFF'], ellipses.collect { ((Node) it.children()[0]).attribute('color') })
    }

    @Test
    void bundledTemplates_expandToValidLookingSlots() {
        Map<String, String> bundled = [:]
        for (String type : ['chip', 'background']) {
            bundled[type] = ComplicationSlotExpanderTest.getResourceAsStream(
                    "/com/xlythe/watchface/format/templates/complication_${type}.xml").getText('UTF-8')
        }
        ComplicationSlotExpander bundledExpander = new ComplicationSlotExpander(bundled, '#FFFFFFFF', '#FF808080')

        for (String type : ['chip', 'background']) {
            Node root = bundledExpander.expand(TemplateProcessor.parse(
                    "<Scene><com.xlythe.ComplicationSlot slotId=\"1\" x=\"10\" y=\"20\" width=\"120\" height=\"120\" type=\"${type}\" complicationDrawableStyle=\"line\" /></Scene>"), 2)
            String printed = TemplateProcessor.print(root)

            assertTrue("${type} left placeholders: ${TemplateProcessor.findPlaceholders(printed)}", TemplateProcessor.findPlaceholders(printed).isEmpty())
            // Font requires a family in every WFF version.
            assertTrue("${type} has a Font without a family", root.depthFirst().findAll { it instanceof Node && it.name() == 'Font' }.every { it.attribute('family') })
            assertFalse("${type} still hardcodes a 160px slot", printed.contains('"160"'))
            // WFF renders whitespace inside text, so text must sit against its tags.
            assertFalse("${type} has padded template text", (printed =~ /<Template>\s|\s<Parameter|>\s+<\/Template>/).find())
        }
    }

    /**
     * What each complication type carries, from Wear's Complication reference.
     *
     * <p>A type only reports the data its own kind has: asking SHORT_TEXT for a small image or
     * MONOCHROMATIC_IMAGE for a title gets null, and the branch that wanted it never draws. Nothing
     * catches that - the validator does not know the complication sources, so a layout that asks
     * for the wrong thing passes validation and then shows an empty slot on the watch.
     *
     * <p>Every image source has an {@code _AMBIENT} companion, which the reference lists for some
     * types and not others. They are allowed wherever the image itself is: a provider that does not
     * offer one leaves it null, and the layout falls back to the full-res image.
     */
    private static final Map<String, List<String>> SOURCES_BY_TYPE = [
            'EMPTY'              : [],
            'SHORT_TEXT'         : ['MONOCHROMATIC_IMAGE', 'TEXT', 'TITLE'],
            'LONG_TEXT'          : ['MONOCHROMATIC_IMAGE', 'SMALL_IMAGE', 'TEXT', 'TITLE'],
            'MONOCHROMATIC_IMAGE': ['MONOCHROMATIC_IMAGE'],
            'SMALL_IMAGE'        : ['SMALL_IMAGE', 'IMAGE_STYLE'],
            'PHOTO_IMAGE'        : ['PHOTO_IMAGE'],
            'RANGED_VALUE'       : ['MONOCHROMATIC_IMAGE', 'TEXT', 'TITLE', 'RANGED_VALUE_MIN',
                                    'RANGED_VALUE_MAX', 'RANGED_VALUE_VALUE', 'RANGED_VALUE_COLORS',
                                    'RANGED_VALUE_COLORS_INTERPOLATE'],
            'GOAL_PROGRESS'      : ['MONOCHROMATIC_IMAGE', 'TEXT', 'TITLE', 'GOAL_PROGRESS_VALUE',
                                    'GOAL_PROGRESS_TARGET_VALUE', 'GOAL_PROGRESS_COLORS',
                                    'GOAL_PROGRESS_COLORS_INTERPOLATE'],
            'WEIGHTED_ELEMENTS'  : ['MONOCHROMATIC_IMAGE', 'TEXT', 'TITLE', 'WEIGHTED_ELEMENTS_COLORS',
                                    'WEIGHTED_ELEMENTS_WEIGHTS', 'WEIGHTED_ELEMENTS_BACKGROUND_COLOR'],
    ]

    @Test
    void bundledTemplates_onlyAskATypeForWhatItCarries() {
        for (String layout : ['chip', 'background']) {
            eachComplication(layout, 5) { String type, String xml ->
                List<String> allowed = SOURCES_BY_TYPE[type]
                assertTrue("${layout} has a layout for ${type}, which is not a complication type",
                        allowed != null)
                List<String> permitted = allowed + allowed.findAll { it.endsWith('IMAGE') }
                        .collect { "${it}_AMBIENT".toString() }
                Set<String> used = (xml =~ /\[COMPLICATION\.([A-Z_0-9]+)]/).collect { it[1] } as Set
                assertTrue("${layout}'s ${type} layout reads ${used - permitted}, which a ${type}"
                        + " complication never reports", (used - permitted).isEmpty())
            }
        }
    }

    @Test
    void bundledTemplates_giveEveryTypeTheyAdvertiseALayout() {
        for (String layout : ['chip', 'background']) {
            Node slot = expandBundled(layout, 5)
            Set<String> advertised = slot.attribute('supportedTypes').toString().split(' ') as Set
            Set<String> laidOut = slot.children()
                    .findAll { it instanceof Node && it.name() == 'Complication' }
                    .collect { it.attribute('type').toString() } as Set
            assertEquals("${layout} advertises types it does not draw", advertised, laidOut)
        }
    }

    /**
     * Google Play validates every watchface.xml in a bundle against one format version, so a format
     * 1 bundle may not so much as name a type that arrived in 2.
     */
    @Test
    void bundledTemplates_dropWhatTheFormatVersionHasNeverHeardOf() {
        String v1 = TemplateProcessor.print(expandBundled('chip', 1))
        for (String later : ['GOAL_PROGRESS', 'WEIGHTED_ELEMENTS', 'WeightedStroke', 'isAutoSize']) {
            assertFalse("format 1 output names ${later}", v1.contains(later))
        }

        String v2 = TemplateProcessor.print(expandBundled('chip', 2))
        assertTrue('format 2 has weighted strokes and should use them', v2.contains('WeightedStroke'))
        assertFalse('isAutoSize arrived in format 3', v2.contains('isAutoSize'))

        assertTrue('format 3 should let text shrink to fit',
                TemplateProcessor.print(expandBundled('chip', 3)).contains('isAutoSize="TRUE"'))
    }

    /** A gauge in the provider's own colors: a battery knows better than we do when to turn red. */
    @Test
    void rangedValues_takeTheProvidersColorsWhenItOffersThem() {
        String xml = null
        eachComplication('chip', 2) { String type, String printed ->
            if (type == 'RANGED_VALUE') {
                xml = printed
            }
        }
        assertTrue('no weighted stroke on the gauge', xml.contains(
                '<WeightedStroke colors="[COMPLICATION.RANGED_VALUE_COLORS]"'))
        assertTrue('the gauge should fall back to the slot color',
                xml.contains('<Stroke color="#FFFFFFFF"'))
        assertTrue('nothing chooses between them',
                xml.contains('[COMPLICATION.RANGED_VALUE_COLORS] != null'))
    }

    @Test
    void everyDrawnThingStaysInsideItsSlot() {
        for (String layout : ['chip', 'background']) {
            Node slot = expandBundled(layout, 5)
            for (Node part : slot.depthFirst().findAll {
                it instanceof Node && it.name() in ['PartText', 'PartImage', 'PartDraw']
            }) {
                int x = part.attribute('x').toString().toInteger()
                int y = part.attribute('y').toString().toInteger()
                int w = part.attribute('width').toString().toInteger()
                int h = part.attribute('height').toString().toInteger()
                assertTrue("${layout} draws a ${part.name()} at ${x},${y} ${w}x${h}, which leaves a"
                        + ' 120x120 slot', x >= 0 && y >= 0 && x + w <= 120 && y + h <= 120)
            }
        }
    }

    @Test
    void text_rejectsAnAreaOrScaleThatIsNotThere() {
        for (String tag : ['<com.xlythe.ComplicationText expression="[COMPLICATION.TEXT]" area="middle" />',
                           '<com.xlythe.ComplicationText expression="[COMPLICATION.TEXT]" area="text" scale="huge" />',
                           '<com.xlythe.ComplicationArc kind="pie" />']) {
            try {
                new ComplicationSlotExpander([chip: "<ComplicationSlot slotId=\"\${COMPLICATION_ID}\">${tag}</ComplicationSlot>"],
                        '#FFFFFFFF', '#FFFFFFFF').expand(TemplateProcessor.parse(
                        '<Scene><com.xlythe.ComplicationSlot slotId="1" x="0" y="0" width="120" height="120" type="chip" /></Scene>'), 5)
                fail("Expected ${tag} to be rejected")
            } catch (IllegalArgumentException expected) {
                assertTrue(expected.message, expected.message.contains('expected'))
            }
        }
    }

    private static Node expandBundled(String layout, int formatVersion) {
        Map<String, String> bundled = [:]
        for (String type : ['chip', 'background']) {
            bundled[type] = ComplicationSlotExpanderTest.getResourceAsStream(
                    "/com/xlythe/watchface/format/templates/complication_${type}.xml").getText('UTF-8')
        }
        Node root = new ComplicationSlotExpander(bundled, '#FFFFFFFF', '#FF808080').expand(
                TemplateProcessor.parse("<Scene><com.xlythe.ComplicationSlot slotId=\"1\" x=\"10\" y=\"20\""
                        + " width=\"120\" height=\"120\" type=\"${layout}\" complicationDrawableStyle=\"line\" /></Scene>"),
                formatVersion)
        return (Node) root.children()[0]
    }

    private static void eachComplication(String layout, int formatVersion, Closure<?> body) {
        for (Node complication : expandBundled(layout, formatVersion).children()
                .findAll { it instanceof Node && it.name() == 'Complication' }) {
            body(complication.attribute('type').toString(), TemplateProcessor.print(complication))
        }
    }

    @Test
    void expand_rejectsUnknownTypes() {
        try {
            expander.expand(TemplateProcessor.parse(
                    '<Scene><com.xlythe.ComplicationSlot slotId="1" x="0" y="0" width="10" height="10" type="ring" /></Scene>'), 2)
            fail('Expected an unknown type to be rejected')
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.message.contains('ring'))
        }
    }

    @Test
    void expand_keepsMixedContentOrder() {
        Node root = expander.expand(TemplateProcessor.parse(
                '<PartText><Text><Font><Template>%s  hours <Parameter expression="[HOUR_0_23]" /></Template></Font></Text></PartText>'), 2)

        Node template = (Node) root.depthFirst().find { it instanceof Node && it.name() == 'Template' }
        assertEquals(2, template.children().size())
        assertEquals('%s hours', template.children()[0])
        assertEquals('Parameter', ((Node) template.children()[1]).name())
    }
}
