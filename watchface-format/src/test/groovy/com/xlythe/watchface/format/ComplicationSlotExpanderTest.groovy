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
