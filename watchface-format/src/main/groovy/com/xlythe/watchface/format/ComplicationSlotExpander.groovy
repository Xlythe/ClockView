package com.xlythe.watchface.format

/**
 * Replaces the {@code com.xlythe.*} complication tags with full Watch Face Format layouts,
 * mirroring ClockView's {@code ComplicationView}.
 *
 * <pre>
 * &lt;com.xlythe.ComplicationSlot slotId="1" x="200" y="200" width="160" height="160"
 *     type="chip" complicationDrawableStyle="line" color="#FFFFFFFF" ambientColor="#FFFFFFFF" /&gt;
 * </pre>
 *
 * {@code type} names a layout - {@code chip} and {@code background} are bundled, and
 * {@code complicationTemplates} adds more; {@code complicationDrawableStyle} is {@code fill},
 * {@code line}, {@code dot} or {@code empty}. {@code color} and {@code ambientColor} are optional
 * and accept any WFF color, including configuration references such as
 * {@code [CONFIGURATION.themeColor.1]}.
 *
 * <p>{@code contentColor} and {@code ambientContentColor} colour what goes inside the ring, and
 * default to the ring's own colour. A slot drawn {@code fill} wants them set to something that
 * reads against the disc, since otherwise the text is the colour of what is behind it.
 *
 * <p>A layout is written in terms of three smaller tags. Each knows the slot it sits in, so a
 * layout names a role rather than repeating geometry, and each expands to both an ambient and a
 * full-res rendering, so a layout never says the same thing twice:
 *
 * <pre>
 * &lt;com.xlythe.ComplicationText expression="[COMPLICATION.TEXT]" area="value" scale="large" /&gt;
 * &lt;com.xlythe.ComplicationImage source="MONOCHROMATIC_IMAGE" area="icon" /&gt;
 * &lt;com.xlythe.ComplicationArc kind="ranged" /&gt;
 * </pre>
 *
 * See {@link #areas} for the areas a layout may ask for and {@link #fontSize} for the scales.
 *
 * <p>Watch Face Format grew over five versions and a bundle is validated against one of them, so
 * the tags emit what the version in hand allows: a weighted stroke and the provider's own colors
 * from 2, text that shrinks to fit rather than ellipsing from 3.
 */
final class ComplicationSlotExpander {
    static final String TAG = 'com.xlythe.ComplicationSlot'
    static final String TEXT_TAG = 'com.xlythe.ComplicationText'
    static final String IMAGE_TAG = 'com.xlythe.ComplicationImage'
    static final String ARC_TAG = 'com.xlythe.ComplicationArc'

    /** GOAL_PROGRESS, WEIGHTED_ELEMENTS and {@code <WeightedStroke>} arrived in format 2. */
    private static final int WEIGHTED_FORMAT_VERSION = 2

    /** Text that shrinks to fit its box, rather than ellipsing, arrived in format 3. */
    private static final int AUTO_SIZE_FORMAT_VERSION = 3

    /**
     * How strongly the part of a gauge the value has not reached is drawn, out of 255.
     *
     * <p>It serves twice, and the two are the same picture: the lap already completed under the
     * one past a goal, and the empty part of a gauge whose provider sent no colors. Holding it
     * back rather than thickening what sits on top keeps the two rings concentric - a thicker lap
     * has to grow somewhere, and outwards is the bezel, so it grows inwards and stops sharing a
     * center line with the gauge it is drawn over.
     */
    private static final int TRACK_ALPHA = 105

    /**
     * What is drawn over the part of a color ramp the value has not reached.
     *
     * <p>A provider's ramp describes its whole range - the color at seven tenths of the way round
     * is the color of a reading at seven tenths - so the ramp is drawn in full and the remainder
     * shaded, rather than the ramp being squeezed into the part that is filled. Shading is
     * translucent black because the face behind the gauge is unknown: on the dark faces watches
     * mostly wear it fades the remainder to a tint of what is coming, and on a light one it
     * darkens it, and either way the filled part is the vivid one.
     */
    private static final String UNFILLED_RAMP_SHADE = '#B3000000'

    /**
     * The part of a band's gauge the value has not reached.
     *
     * <p>A band draws its gauge as pills laid end to end rather than as a fill over a track, so
     * the remainder is a shape of its own and wants a color of its own: a grey just off black,
     * which reads as empty on a dark face without disappearing into it.
     */
    private static final String UNFILLED_PILL_COLOR = '#FF303030'

    /** The gap between two pills on a band, as a share of the band's thickness. */
    private static final double PILL_MARGIN = 0.2d

    /**
     * How thick a band is, and how far in from the slot's edge it sits, as shares of the slot.
     *
     * <p>Proportions rather than pixels, because a slot is as wide as the face and a face is
     * whatever size its author chose. A band sits inside whatever the face draws around its rim -
     * tick marks, numerals - rather than on top of it: out at the rim there is no room for the
     * band's own text, which then runs off the screen, and the marks are what the band would have
     * to overlap to find any. A face with a bare rim can pass a smaller inset and take it.
     */
    private static final double DEFAULT_BAND_THICKNESS = 0.056d
    private static final double DEFAULT_BAND_INSET = 0.114d

    /** Below this, a font size is too small to read on a watch. Also WFF's own autosize floor. */
    private static final int MIN_FONT_SIZE = 12

    private int formatVersion = 1

    /** The slot whose layout is being expanded, for the tags nested inside it. */
    private Geometry geometry
    private String color
    private String ambientColor
    private String contentColor
    private String ambientContentColor

    private final Map<String, String> templates
    private final String defaultColor
    private final String defaultAmbientColor

    /**
     * @param templates layout template text keyed by slot type ({@code chip}, {@code background})
     */
    ComplicationSlotExpander(Map<String, String> templates, String defaultColor, String defaultAmbientColor) {
        this.templates = templates
        this.defaultColor = defaultColor
        this.defaultAmbientColor = defaultAmbientColor
    }

    /** @param formatVersion gates the complication types and elements the output may name. */
    Node expand(Node root, int formatVersion) {
        this.formatVersion = formatVersion
        return (Node) replaceCustomTags(root)
    }

    private Object replaceCustomTags(Object node) {
        if (!(node instanceof Node)) {
            return node
        }
        Node element = (Node) node
        switch (element.name()) {
            case TAG: return expandSlot(element)
            case TEXT_TAG: return expandText(element)
            case IMAGE_TAG: return expandImage(element)
            case ARC_TAG: return expandArc(element)
        }

        List<Object> newChildren = element.children().collect { replaceCustomTags(it) }
        element.children().clear()
        boolean mixedContent = newChildren.size() > 1 && newChildren.any { !(it instanceof Node) }
        for (Object child : newChildren) {
            if (child instanceof Node) {
                element.append((Node) child)
            } else if (mixedContent) {
                // e.g. <Template>%s<Parameter expression="..." /></Template> must keep its order.
                element.children().add(TemplateProcessor.normalizeWhitespace(child.toString()))
            } else {
                element.value = TemplateProcessor.normalizeWhitespace(child.toString())
            }
        }
        return element
    }

    private Node expandSlot(Node node) {
        def slotId = node.attribute('slotId')
        def x = node.attribute('x')
        def y = node.attribute('y')
        def width = node.attribute('width')
        def height = node.attribute('height')
        def drawableStyle = node.attribute('complicationDrawableStyle') // One of fill, line, dot, or empty
        if (slotId == null || x == null || y == null || width == null || height == null) {
            throw new IllegalArgumentException(
                    "${TAG} requires slotId, x, y, width and height, but was ${node.attributes()}")
        }

        color = node.attribute('color') ?: defaultColor
        ambientColor = node.attribute('ambientColor') ?: defaultAmbientColor
        // What goes inside the ring, as opposed to the ring itself. The two are the same unless
        // the slot is filled, where drawing the text in the color of the disc behind it would
        // hide it. There is no working the contrast out here: the color may be a configuration
        // reference, which is a name at build time and a color only on the watch.
        contentColor = node.attribute('contentColor') ?: color
        ambientContentColor = node.attribute('ambientContentColor') ?: ambientColor
        geometry = new Geometry(width.toString().toInteger(), height.toString().toInteger(),
                arcSpan(node))
        int w = geometry.w
        int h = geometry.h

        // An arc slot's track follows its own span rather than closing into a ring, and the four
        // styles say how solid it is rather than what shape it is.
        String bgShapeAmbient = ''
        String bgShapeFullres = ''
        if (geometry.arc != null) {
            String dashes = drawableStyle == 'dot' ? ' dashIntervals="6 3"' : ''
            bgShapeAmbient = geometry.arc.shape("<Stroke color=\"${ambientColor}\" cap=\"ROUND\" thickness=\"${geometry.arc.thickness}\"${dashes} />")
            bgShapeFullres = geometry.arc.shape("<Stroke color=\"${color}\" cap=\"ROUND\" thickness=\"${geometry.arc.thickness}\"${dashes} />")
            if (!['fill', 'line', 'dot', 'empty', null].contains(drawableStyle)) {
                throw new IllegalArgumentException(
                        "Unknown complicationDrawableStyle '${drawableStyle}' for ${TAG} ${slotId}; expected fill, line, dot or empty")
            }
        } else if (drawableStyle == 'fill') {
            bgShapeAmbient = "<Ellipse x=\"0\" y=\"0\" width=\"${width}\" height=\"${height}\"><Fill color=\"${ambientColor}\" /></Ellipse>"
            bgShapeFullres = "<Ellipse x=\"0\" y=\"0\" width=\"${width}\" height=\"${height}\"><Fill color=\"${color}\" /></Ellipse>"
        } else if (drawableStyle == 'line' || drawableStyle == null) {
            bgShapeAmbient = arc(w, h, 4, "<Stroke color=\"${ambientColor}\" cap=\"ROUND\" thickness=\"4\" />")
            bgShapeFullres = arc(w, h, 4, "<Stroke color=\"${color}\" cap=\"ROUND\" thickness=\"4\" />")
        } else if (drawableStyle == 'dot') {
            bgShapeAmbient = arc(w, h, 4, "<Stroke color=\"${ambientColor}\" cap=\"ROUND\" thickness=\"4\" dashIntervals=\"6 3\" />")
            bgShapeFullres = arc(w, h, 4, "<Stroke color=\"${color}\" cap=\"ROUND\" thickness=\"4\" dashIntervals=\"6 3\" />")
        } else if (drawableStyle != 'empty') {
            throw new IllegalArgumentException(
                    "Unknown complicationDrawableStyle '${drawableStyle}' for ${TAG} ${slotId}; expected fill, line, dot or empty")
        }

        // An arc slot's track carries a gauge drawn over it in the same colour, so it is held back
        // to read as the track rather than as part of the sweep. A ring slot has its content
        // inside it instead of on top, so it stays at full strength.
        int trackAlpha = (geometry.arc != null && drawableStyle != 'fill') ? 110 : 255
        String bgGroup = ''
        if (drawableStyle != 'empty') {
            bgGroup = """
            <!-- Draw the background -->
            <Group name="Background" x="0" y="0" width="${width}" height="${height}">
                <!-- Ambient background -->
                <PartDraw x="0" y="0" width="${width}" height="${height}" alpha="0">
                    <Variant mode="AMBIENT" target="alpha" value="${trackAlpha}" />
                    ${bgShapeAmbient}
                </PartDraw>
                <!-- Full-res background -->
                <PartDraw x="0" y="0" width="${width}" height="${height}" alpha="${trackAlpha}">
                    <Variant mode="AMBIENT" target="alpha" value="0" />
                    ${bgShapeFullres}
                </PartDraw>
            </Group>
"""
        }

        Map<String, Object> replacements = [
                '${COMPLICATION_ID}'        : slotId,
                '${POS_X}'                  : x,
                '${POS_Y}'                  : y,
                '${WIDTH}'                  : width,
                '${HEIGHT}'                 : height,
                '${BACKGROUND_GROUP}'       : bgGroup,
                '${DEFAULT_PROVIDER_POLICY}': defaultProviderPolicy(node),
                // Arc slots only. A layout that does not use these is unaffected by them.
                '${BOUNDING_ARC}'           : geometry.arc == null ? '' : geometry.arc.bounding(),
                '${ARC_CENTER_X}'           : "${geometry.arc?.centerX}",
                '${ARC_CENTER_Y}'           : "${geometry.arc?.centerY}",
                '${ARC_WIDTH}'              : "${geometry.arc?.width}",
                '${ARC_HEIGHT}'             : "${geometry.arc?.height}",
                '${ARC_START_ANGLE}'        : "${geometry.arc?.startAngle}",
                '${ARC_END_ANGLE}'          : "${geometry.arc?.endAngle}",
                '${ARC_THICKNESS}'          : "${geometry.arc?.thickness}",
                '${ARC_DIRECTION}'          : "${geometry.arc?.direction}",
                // Single pane icon E.g. <Compare expression="icon">
                '${IMG_POS_X_SINGLE}'       : "${geometry.iconXSingle}",
                '${IMG_POS_Y_SINGLE}'       : "${geometry.iconYSingle}",
                '${IMG_WIDTH_SINGLE}'       : "${geometry.iconSize}",
                '${IMG_HEIGHT_SINGLE}'      : "${geometry.iconSize}",
                // Split pane icon E.g. <Compare expression="icon_text">, <Compare expression="icon_title">
                '${IMG_POS_X_SPLIT}'        : "${geometry.iconXSplit}",
                '${IMG_POS_Y_SPLIT}'        : "${geometry.iconYSplit}",
                '${IMG_WIDTH_SPLIT}'        : "${geometry.iconSize}",
                '${IMG_HEIGHT_SPLIT}'       : "${geometry.iconSize}",
                // Single pane text / title E.g. <Compare expression="text">, <Compare expression="title">
                '${TEXT_POS_X_SINGLE}'      : "${geometry.textXSingle}",
                '${TEXT_POS_Y_SINGLE}'      : "${geometry.textYSingle}",
                '${TEXT_WIDTH_SINGLE}'      : "${geometry.textWidthSingle}",
                '${TEXT_HEIGHT_SINGLE}'     : "${geometry.textHeightSingle}",
                // Split pane text / title with icon E.g. <Compare expression="icon_text">
                '${TEXT_POS_X_SPLIT_ICON}'  : "${geometry.textXSplitIcon}",
                '${TEXT_POS_Y_SPLIT_ICON}'  : "${geometry.textYSplitIcon}",
                '${TEXT_WIDTH_SPLIT_ICON}'  : "${geometry.textWidthSplitIcon}",
                '${TEXT_HEIGHT_SPLIT_ICON}' : "${geometry.textHeightSplitIcon}",
                // Split pane text_title (no icon) E.g. <Compare expression="text_title">
                '${TITLE_POS_X_SPLIT_TEXT}' : "${geometry.titleXSplitText}",
                '${TITLE_POS_Y_SPLIT_TEXT}' : "${geometry.titleYSplitText}",
                '${TITLE_WIDTH_SPLIT_TEXT}' : "${geometry.titleWidthSplitText}",
                '${TITLE_HEIGHT_SPLIT_TEXT}': "${geometry.titleHeightSplitText}",
                '${TEXT_POS_X_SPLIT_TEXT}'  : "${geometry.textXSplitText}",
                '${TEXT_POS_Y_SPLIT_TEXT}'  : "${geometry.textYSplitText}",
                '${TEXT_WIDTH_SPLIT_TEXT}'  : "${geometry.textWidthSplitText}",
                '${TEXT_HEIGHT_SPLIT_TEXT}' : "${geometry.textHeightSplitText}",
                // Background slot E.g. complication_background.xml
                '${CENTER_X}'               : "${w / 2}",
                '${CENTER_Y}'               : "${h / 2}",
                '${RING_WIDTH}'             : "${w - 4}",
                '${RING_HEIGHT}'            : "${h - 4}",
                '${ICON_SIZE}'              : "${geometry.backgroundIconSize}",
                '${ICON_X}'                 : "${(int) ((w - geometry.backgroundIconSize) / 2)}",
                '${ICON_Y}'                 : "${(int) ((h - geometry.backgroundIconSize) / 2)}",
        ]

        def type = node.attribute('type')
        String template = templates.get(type)
        if (template == null) {
            throw new IllegalArgumentException("Unknown ComplicationSlot type ${type}")
        }
        template = supportedTypes(template)

        String content = template
        replacements.each { String key, Object value -> content = content.replace(key, value.toString()) }
        // Colors last: they may appear inside ${BACKGROUND_GROUP} and the template itself.
        content = content.replace('${AMBIENT_COLOR}', ambientColor).replace('${COLOR}', color)

        // Tidy the layout's text the same way as the rest of the document. WFF renders whitespace
        // inside text, e.g. around the %s in <Template>.
        return (Node) replaceCustomTags(TemplateProcessor.parse(content))
    }

    /**
     * Reads the span of a slot that hugs the bezel rather than sitting in a box.
     *
     * <pre>
     * &lt;com.xlythe.ComplicationSlot slotId="5" x="0" y="0" width="450" height="450" type="arc"
     *     startAngle="200" endAngle="250" /&gt;
     * </pre>
     *
     * <p>A slot still needs a box, because Watch Face Format asks every ComplicationSlot for one;
     * for an arc it is the box the arc is drawn in, usually the whole face. The angles are
     * degrees from twelve o'clock, which is where the format puts zero.
     */
    private static ArcSpan arcSpan(Node node) {
        String start = attribute(node, 'startAngle')
        String end = attribute(node, 'endAngle')
        if (start == null && end == null) {
            return null
        }
        if (start == null || end == null) {
            throw new IllegalArgumentException("${TAG} ${node.attribute('slotId')} needs both" +
                    ' startAngle and endAngle, or neither')
        }
        int width = node.attribute('width').toString().toInteger()
        int height = node.attribute('height').toString().toInteger()
        int across = Math.min(width, height)
        int thickness = attribute(node, 'thickness')?.toInteger() ?: (int) (across * DEFAULT_BAND_THICKNESS)
        int inset = attribute(node, 'inset')?.toInteger() ?: (int) (across * DEFAULT_BAND_INSET)
        String direction = attribute(node, 'direction') ?: 'CLOCKWISE'
        if (!['CLOCKWISE', 'COUNTER_CLOCKWISE'].contains(direction)) {
            throw new IllegalArgumentException("Unknown ${TAG} direction '${direction}';" +
                    ' expected CLOCKWISE or COUNTER_CLOCKWISE')
        }
        return new ArcSpan(width, height, start as double, end as double, thickness, inset, direction)
    }

    /** The providers Wear OS ships, any of which a slot may ask for before the user chooses. */
    private static final List<String> SYSTEM_PROVIDERS = ['APP_SHORTCUT', 'DATE', 'DAY_OF_WEEK',
                                                          'DAY_AND_DATE', 'FAVORITE_CONTACT',
                                                          'NEXT_EVENT', 'STEP_COUNT',
                                                          'SUNRISE_SUNSET', 'TIME_AND_DATE',
                                                          'UNREAD_NOTIFICATION_COUNT',
                                                          'WATCH_BATTERY', 'WORLD_CLOCK', 'EMPTY']

    /** The types that only exist from format 2, and so can only be asked for from format 2. */
    private static final List<String> WEIGHTED_TYPES = ['GOAL_PROGRESS', 'WEIGHTED_ELEMENTS']

    /**
     * What a slot shows before the user has chosen anything.
     *
     * <pre>
     * &lt;com.xlythe.ComplicationSlot ... defaultProvider="WATCH_BATTERY"
     *     defaultProviderType="RANGED_VALUE" /&gt;
     * </pre>
     *
     * <p>A watch face whose slots start empty looks unfinished on the first run, and the user has to
     * go and find the editor to fix it. Naming a system provider costs nothing and the user can
     * still change it.
     *
     * <p>A type the format version has never heard of cannot be asked for either, so the whole
     * policy is dropped there and the slot starts empty as it would have anyway.
     */
    private String defaultProviderPolicy(Node node) {
        String provider = attribute(node, 'defaultProvider')
        String type = attribute(node, 'defaultProviderType')
        if (provider == null && type == null) {
            return ''
        }
        if (provider == null || type == null) {
            throw new IllegalArgumentException("${TAG} ${node.attribute('slotId')} needs both" +
                    ' defaultProvider and defaultProviderType, or neither')
        }
        if (!SYSTEM_PROVIDERS.contains(provider)) {
            throw new IllegalArgumentException("Unknown defaultProvider '${provider}';" +
                    " expected one of ${SYSTEM_PROVIDERS.join(', ')}")
        }
        if (formatVersion < WEIGHTED_FORMAT_VERSION && WEIGHTED_TYPES.contains(type)) {
            return ''
        }
        return "<DefaultProviderPolicy defaultSystemProvider=\"${provider}\"" +
                " defaultSystemProviderType=\"${type}\" />"
    }

    /**
     * Drops the complication types and their layouts that the format version in hand has never
     * heard of.
     *
     * <p>Google Play validates a bundle against one format version, so naming GOAL_PROGRESS in a
     * supportedTypes list fails the whole bundle on format 1 rather than being ignored there. The
     * layout goes with it: a {@code <Complication>} for a type the runtime cannot report is dead
     * weight in a file the memory footprint check reads.
     */
    private String supportedTypes(String template) {
        if (formatVersion >= WEIGHTED_FORMAT_VERSION) {
            return template
        }
        String stripped = template
        for (String type : ['GOAL_PROGRESS', 'WEIGHTED_ELEMENTS']) {
            stripped = stripped.replace(" ${type}", '')
            int start = stripped.indexOf("<Complication type=\"${type}\">")
            if (start >= 0) {
                int end = stripped.indexOf('</Complication>', start)
                if (end < 0) {
                    throw new IllegalArgumentException(
                            "A ${type} complication was opened and never closed")
                }
                stripped = stripped.substring(0, start) + stripped.substring(end + '</Complication>'.length())
            }
        }
        return stripped
    }

    /**
     * Draws one of the complication's strings.
     *
     * <pre>
     * &lt;com.xlythe.ComplicationText expression="[COMPLICATION.TEXT]" area="value"
     *     scale="large" weight="MEDIUM" maxLines="1" dim="false" /&gt;
     * </pre>
     *
     * <p>Only {@code expression} and {@code area} are required. {@code dim} holds a label back from
     * the value it belongs to; it does that with alpha rather than a paler color, because the color
     * may be a configuration reference the build cannot see into.
     *
     * <p>{@code autoSizeScale} is the scale to use from format 3, where text shrinks to fit its
     * box; before that {@code scale} is used. A value wants to be as large as its box allows, and
     * how large that is depends on the string: "72°" fits a chip at a size "10,482" does not. A
     * runtime that can shrink is given the larger size and left to fit the long ones; a runtime
     * that can only ellipsize is given the size the long ones fit at.
     *
     * <p>{@code curved="true"} bends the line along the slot's own arc, for a slot that is a band
     * around the bezel and has no inside to put a line in.
     */
    private Node expandText(Node node) {
        String expression = required(node, 'expression')
        int[] area = area(node)
        int size = fontSize(node, 'medium')
        String weight = attribute(node, 'weight') ?: 'NORMAL'
        String maxLines = attribute(node, 'maxLines') ?: '1'
        int alpha = 'true' == attribute(node, 'dim') ? 170 : 255
        boolean curved = 'true' == attribute(node, 'curved')
        if (curved && geometry.arc == null) {
            throw new IllegalArgumentException(
                    "<${node.name()}> is curved, but its slot has no startAngle to curve along")
        }

        String box = "x=\"${area[0]}\" y=\"${area[1]}\" width=\"${area[2]}\" height=\"${area[3]}\""
        // Text fits its box by shrinking where the runtime can, and by ellipsing where it cannot.
        String fit = formatVersion >= AUTO_SIZE_FORMAT_VERSION
                ? "ellipsis=\"TRUE\" maxLines=\"${maxLines}\" isAutoSize=\"TRUE\""
                : "ellipsis=\"TRUE\" maxLines=\"${maxLines}\""
        // TextCircular carries the angles itself and lays one line along them, so it takes an
        // alignment rather than a line count.
        //
        // The line straddles the oval it is laid on, so the band's own oval - its center line -
        // is what centres the line in the band. The format's reference says what a TextCircular's
        // width and height are but never where the glyphs sit against them, so this is measured
        // rather than read: a hairline and a run of capitals drawn on one oval came back with the
        // capitals spanning a cap height either side of the hairline. Pulling the oval in to make
        // room, which is what this did before, drops the text half a type-height below the band.
        String shape = curved
                ? "<TextCircular ${geometry.arc.textAttributes()}" +
                  " align=\"${attribute(node, 'align') ?: 'CENTER'}\" ellipsis=\"TRUE\""
                : "<Text ${fit}"
        String closing = curved ? '</TextCircular>' : '</Text>'
        Closure<String> part = { String textColor, int shown, int inAmbient ->
            """<PartText ${box} alpha="${shown}">
                    <Variant mode="AMBIENT" target="alpha" value="${inAmbient}" />
                    <Localization calendar="GREGORIAN" />
                    ${shape}>
                        <Font family="SYNC_TO_DEVICE" size="${size}" weight="${weight}" color="${textColor}">
                            <Template>%s<Parameter expression="${escapeAttribute(expression)}" /></Template>
                        </Font>
                    ${closing}
                </PartText>"""
        }

        // One rendering does for both modes when the two colors agree, which is the common case.
        String parts = contentColor == ambientContentColor
                ? part(contentColor, alpha, alpha)
                : part(ambientContentColor, 0, alpha) + part(contentColor, alpha, 0)
        return parse("""<Group name="Text" x="0" y="0" width="${geometry.w}" height="${geometry.h}">
                ${parts}
            </Group>""")
    }

    /**
     * Draws one of the complication's images.
     *
     * <pre>
     * &lt;com.xlythe.ComplicationImage source="MONOCHROMATIC_IMAGE" area="icon" /&gt;
     * </pre>
     *
     * <p>{@code source} is {@code MONOCHROMATIC_IMAGE}, {@code SMALL_IMAGE} or
     * {@code PHOTO_IMAGE}. A monochromatic image is a single-color glyph the watch face is expected
     * to tint; the other two carry their own color and are left alone.
     *
     * <p>Each source has an {@code _AMBIENT} companion that a provider may or may not supply, so
     * ambient falls back to the full-res image rather than going blank.
     */
    private Node expandImage(Node node) {
        String source = required(node, 'source')
        int[] area = area(node)
        String box = "x=\"${area[0]}\" y=\"${area[1]}\" width=\"${area[2]}\" height=\"${area[3]}\""
        // PHOTO_IMAGE has no ambient companion: it is a photograph, and ambient is not the place
        // for one. The slot simply shows nothing there.
        boolean hasAmbient = source != 'PHOTO_IMAGE'
        String tint = source == 'MONOCHROMATIC_IMAGE' ? " tintColor=\"${contentColor}\"" : ''
        String ambientTint = source == 'MONOCHROMATIC_IMAGE' ? " tintColor=\"${ambientContentColor}\"" : ''

        String fullRes = """<PartImage ${box}${tint} alpha="255">
                    <Variant mode="AMBIENT" target="alpha" value="0" />
                    <Image resource="[COMPLICATION.${source}]" />
                </PartImage>"""
        if (!hasAmbient) {
            return parse("""<Group name="Image" x="0" y="0" width="${geometry.w}" height="${geometry.h}">
                ${fullRes}
            </Group>""")
        }
        String ambient = """<Condition>
                    <Expressions>
                        <Expression name="has_ambient">[COMPLICATION.${source}_AMBIENT] != null</Expression>
                    </Expressions>
                    <Compare expression="has_ambient">
                        <PartImage ${box}${ambientTint} alpha="0">
                            <Variant mode="AMBIENT" target="alpha" value="255" />
                            <Image resource="[COMPLICATION.${source}_AMBIENT]" />
                        </PartImage>
                    </Compare>
                    <Default>
                        <PartImage ${box}${ambientTint} alpha="0">
                            <Variant mode="AMBIENT" target="alpha" value="255" />
                            <Image resource="[COMPLICATION.${source}]" />
                        </PartImage>
                    </Default>
                </Condition>"""
        return parse("""<Group name="Image" x="0" y="0" width="${geometry.w}" height="${geometry.h}">
                ${ambient}
                ${fullRes}
            </Group>""")
    }

    /**
     * Draws the ring that makes a number mean something.
     *
     * <pre>
     * &lt;com.xlythe.ComplicationArc kind="ranged" /&gt;
     * </pre>
     *
     * <p>{@code kind} is {@code ranged}, {@code goal} or {@code weighted}. All three draw over the
     * slot's own ring, so that ring reads as the track and this as what has been filled.
     */
    private Node expandArc(Node node) {
        String kind = required(node, 'kind')
        switch (kind) {
            case 'ranged': return parse(progressRing('RANGED_VALUE', rangedFraction()))
            case 'goal': return parse(goalRings())
            case 'weighted': return parse(weightedRing())
            default: throw new IllegalArgumentException(
                    "Unknown ${ARC_TAG} kind '${kind}'; expected ranged, goal or weighted")
        }
    }

    /**
     * A sweep from twelve o'clock, which is where Watch Face Format puts zero degrees, clockwise to
     * the fraction given. A band draws its gauge as pills instead; see {@link #bandGauge}.
     *
     * <p>endAngle takes a number rather than an expression - which is the only reason an Arc
     * accepts a Transform at all - so the sweep is applied as one.
     *
     * <p>From format 2 the full-res gauge takes the provider's own colors when it offers any: a
     * battery complication knows better than we do that it should turn red. The ramp is laid over
     * the whole range and the part past the value shaded, so the fill ends in the color the value
     * has reached; squeezed into the fill instead, the ramp would end in its last color however
     * little there was of it. Format 4 can pick that one color out of the ramp
     * ({@code extractColorFromColors}), but the earlier formats cannot, and the shading works on
     * all of them. Ambient stays one flat color, because a screen held lit for hours is the wrong
     * place to ask for a gradient.
     *
     * <p>A ring closes on itself and has no free end to round: a cap there is drawn back over the
     * ramp's first color as a bite out of it, so the ramp and the shade are both cut square and
     * the shade starts on the value exactly. A ring has only a hairline behind it, so it is given
     * a track of its own.
     *
     * @param type the complication type, naming its {@code _COLORS} sources.
     * @param fraction an expression between 0 and 1, already clamped.
     * @param alpha how strongly to draw it, so a lap already done can sit behind the next.
     * @param lapped whether a lap is already drawn underneath this one, which is what a passed
     *        goal is. Shading the part not reached would darken that lap rather than the face, so
     *        a lapped ring sweeps its ramp instead, and takes no track of its own.
     */
    private String progressRing(String type, String fraction, int alpha = 255,
                                boolean lapped = false) {
        if (geometry.arc != null) {
            return bandGauge(fraction)
        }
        int thickness = 8
        String geometryAttributes = "centerX=\"${geometry.w / 2}\" centerY=\"${geometry.h / 2}\"" +
                " width=\"${geometry.w - thickness}\" height=\"${geometry.h - thickness}\""
        String reached = "360 * (${fraction})"
        // Number rather than double for the two angles: a primitive double takes two local
        // variable slots, and Groovy miscounts them when a closure mixes primitives with objects,
        // which the JVM rejects when it verifies the generated doCall ("Bad local variable type").
        Closure<String> ring = { String stroke, int shown, int inAmbient, Number startAngle, Number endAngle, String transform ->
            """<PartDraw x="0" y="0" width="${geometry.w}" height="${geometry.h}" alpha="${shown}">
                    <Variant mode="AMBIENT" target="alpha" value="${inAmbient}" />
                    <Arc startAngle="${startAngle}" endAngle="${endAngle}" ${geometryAttributes}>
                        ${transform}
                        ${stroke}
                    </Arc>
                </PartDraw>"""
        }
        Closure<String> sweep = { String stroke, int shown, int inAmbient ->
            ring(stroke, shown, inAmbient, 0, 0, "<Transform target=\"endAngle\" value=\"${reached}\" />")
        }
        Closure<String> flat = { String c -> "<Stroke color=\"${c}\" cap=\"ROUND\" thickness=\"${thickness}\" />" }
        // A lap drawn over another lap has that one behind it as its track, so it wants no second.
        String track = !lapped && alpha == 255
                ? ring(flat(color), TRACK_ALPHA, 0, 0, 360, '')
                : ''
        String fullRes = track + sweep(flat(color), alpha, 0)
        if (formatVersion >= WEIGHTED_FORMAT_VERSION) {
            String ramp = "<WeightedStroke colors=\"[COMPLICATION.${type}_COLORS]\"" +
                    " interpolate=\"[COMPLICATION.${type}_COLORS_INTERPOLATE]\"" +
                    " cap=\"BUTT\" thickness=\"${thickness}\" />"
            String shade = "<Stroke color=\"${UNFILLED_RAMP_SHADE}\" cap=\"BUTT\" thickness=\"${thickness}\" />"
            String colored = lapped
                    ? sweep(ramp, alpha, 0)
                    : ring(ramp, alpha, 0, 0, 360, '') +
                      ring(shade, alpha, 0, 0, 360,
                              "<Transform target=\"startAngle\" value=\"${reached}\" />")
            fullRes = """<Condition>
                    <Expressions>
                        <Expression name="provider_colors">[COMPLICATION.${type}_COLORS] != null</Expression>
                    </Expressions>
                    <Compare expression="provider_colors">
                        ${colored}
                    </Compare>
                    <Default>
                        ${fullRes}
                    </Default>
                </Condition>"""
        }
        return """<Group name="Progress" x="0" y="0" width="${geometry.w}" height="${geometry.h}">
                ${sweep(flat(ambientColor), 0, 255)}
                ${fullRes}
            </Group>"""
    }

    /**
     * A band's gauge: two pills laid end to end with a little margin between them, the value
     * from the start of the band and the remainder after it, each rounded at both ends.
     *
     * <p>Neither is drawn over the other, and there is no track underneath, so the band never
     * shows one color through another. That also rules out the provider's color ramp, which
     * only means anything as a gradient: the fill is the slot's own color.
     *
     * <p>A round cap bulges half a thickness past each end of the arc it is drawn on, so a pill
     * is an arc a whole thickness shorter than the length it covers. The band's outline is the
     * track the slot would otherwise draw - its span, capped round - and the pills share it out:
     * the value's arc runs from the start as far as the fraction reaches of whatever is left
     * once the caps and the margin are paid for, and the remainder's arc starts that much after
     * it and runs to the end. At nothing the value is not drawn and at everything the remainder
     * is not, rather than either shrinking to a dot.
     *
     * @param fraction an expression between 0 and 1, already clamped.
     * @param restColor what the remainder is drawn in.
     * @param restAlpha how strongly, so a lap already done can be told from the one being drawn.
     */
    private String bandGauge(String fraction, String restColor = UNFILLED_PILL_COLOR,
                             int restAlpha = 255) {
        ArcSpan band = geometry.arc
        double from = band.startAngle
        double to = band.sweepEnd()
        double radius = (band.width + band.height) / 4d
        double capsAndMargin = Math.toDegrees((1 + PILL_MARGIN) * band.thickness / radius)
        String reach = String.format(Locale.ROOT, '%.2f', Math.max(0d, band.span() - capsAndMargin))
        String offset = String.format(Locale.ROOT, '%.2f', capsAndMargin)
        String reached = "${from} + ${reach} * (${fraction})"
        Closure<String> pill = { String stroke, int shown, int inAmbient, String transform ->
            """<PartDraw x="0" y="0" width="${geometry.w}" height="${geometry.h}" alpha="${shown}">
                    <Variant mode="AMBIENT" target="alpha" value="${inAmbient}" />
                    <Arc startAngle="${from}" endAngle="${to}" ${band.geometryAttributes()}>
                        ${transform}
                        ${stroke}
                    </Arc>
                </PartDraw>"""
        }
        Closure<String> flat = { String c -> "<Stroke color=\"${c}\" cap=\"ROUND\" thickness=\"${band.thickness}\" />" }
        String valueEnd = "<Transform target=\"endAngle\" value=\"${reached}\" />"
        String restStart = "<Transform target=\"startAngle\" value=\"${reached} + ${offset}\" />"
        String value = pill(flat(ambientColor), 0, 255, valueEnd) + pill(flat(color), 255, 0, valueEnd)
        String rest = pill(flat(restColor), restAlpha, restAlpha, restStart)
        return """<Group name="Progress" x="0" y="0" width="${geometry.w}" height="${geometry.h}">
                <Condition>
                    <Expressions>
                        <Expression name="none"><![CDATA[(${fraction}) <= 0]]></Expression>
                        <Expression name="all"><![CDATA[(${fraction}) >= 1]]></Expression>
                    </Expressions>
                    <Compare expression="none">
                        ${pill(flat(restColor), restAlpha, restAlpha, '')}
                    </Compare>
                    <Compare expression="all">
                        ${pill(flat(ambientColor), 0, 255, '')}
                        ${pill(flat(color), 255, 0, '')}
                    </Compare>
                    <Default>
                        ${value}
                        ${rest}
                    </Default>
                </Condition>
            </Group>"""
    }

    /**
     * A goal is allowed to be passed - that is rather the point of one - and neither a ring nor a
     * band has anywhere to put more than one full pass.
     *
     * <p>So a passed goal is drawn as two laps: the one done, whole and held back, and however far
     * into the next one the value has got, drawn over it at full strength. The lap being drawn is
     * the reading, and the one behind it is what has already been counted.
     *
     * <p>Telling them apart by thickness instead keeps both at full strength, which is two bright
     * rings of nearly the same size and no way to see which is which; and the thicker one has to
     * grow inwards, because outwards is the bezel, so it stops sharing a center line with the ring
     * it covers and reads as having slipped.
     */
    private String goalRings() {
        String overshoot = 'clamp([COMPLICATION.GOAL_PROGRESS_VALUE]' +
                ' / [COMPLICATION.GOAL_PROGRESS_TARGET_VALUE] - 1, 0, 1)'
        // A band's pills cannot be laid over one another, so there the lap already done is the
        // remainder of the next one, in the slot's own color held back, rather than a lap beneath.
        String passed = geometry.arc != null
                ? bandGauge(overshoot, color, TRACK_ALPHA)
                : progressRing('GOAL_PROGRESS', '1', TRACK_ALPHA) +
                  progressRing('GOAL_PROGRESS', overshoot, 255, true)
        String reaching = geometry.arc != null
                ? bandGauge(goalFraction())
                : progressRing('GOAL_PROGRESS', goalFraction())
        return """<Group name="Goal" x="0" y="0" width="${geometry.w}" height="${geometry.h}">
                <Condition>
                    <Expressions>
                        <Expression name="passed"><![CDATA[[COMPLICATION.GOAL_PROGRESS_VALUE] > [COMPLICATION.GOAL_PROGRESS_TARGET_VALUE]]]></Expression>
                    </Expressions>
                    <Compare expression="passed">
                        ${passed}
                    </Compare>
                    <Default>
                        ${reaching}
                    </Default>
                </Condition>
            </Group>"""
    }

    /**
     * One ring divided into the provider's elements, each taking the share of the turn its
     * weight asks for.
     *
     * <p>A weighted stroke does the dividing, so the whole ring is one Arc. The segments are
     * capped round and set a gap apart, which is what the provider's background color is for:
     * Wear's own data model names it the color between the elements. Round, because a band has
     * rounded ends and a square segment sitting in one leaves a crescent of track showing past
     * the color; and because the format caps every segment of a weighted stroke the same way,
     * which makes the ends of the band match at the cost of every element becoming a lozenge.
     *
     * <p>A round cap is drawn half a stroke past the end of its segment, so two facing caps close
     * a whole stroke's worth of gap between them. The gap asked for therefore carries that stroke
     * on top of the gap wanted, or the segments meet and the division disappears. It stays a
     * share of the stroke, so it is a hairline on a thin ring, but it is capped at a share of the
     * sweep, because a bezel band is thick and short and a gap sized to its thickness would take
     * most of its length.
     *
     * <p>That backdrop is drawn underneath, a full turn, since the segments cover whatever
     * they cover.
     *
     * <p>A band is the exception to both. Its elements are pills, like its other gauges, so they
     * sit a margin apart however short the band is - the gap is always the two caps and the
     * margin, never less - and nothing is drawn beneath them to show through the gaps.
     */
    private String weightedRing() {
        ArcSpan band = geometry.arc
        int thickness = band != null ? band.thickness : 8
        double from = band != null ? band.startAngle : 0d
        double to = band != null ? band.startAngle + band.span() : 360d
        String geometryAttributes = band != null
                ? band.geometryAttributes()
                : "centerX=\"${geometry.w / 2}\" centerY=\"${geometry.h / 2}\"" +
                  " width=\"${geometry.w - thickness}\" height=\"${geometry.h - thickness}\""
        double radius = band != null
                ? (band.width + band.height) / 4d
                : Math.min(geometry.w, geometry.h) / 2d
        double gap = band != null
                ? Math.toDegrees((1 + PILL_MARGIN) * thickness / radius)
                : Math.min(Math.toDegrees(1.75d * thickness / radius), 0.06d * (to - from))
        String discreteGap = String.format(Locale.ROOT, '%.1f', gap)
        String backdrop = band != null ? '' : """<Condition>
                    <Expressions>
                        <Expression name="has_background">[COMPLICATION.WEIGHTED_ELEMENTS_BACKGROUND_COLOR] != null</Expression>
                    </Expressions>
                    <Compare expression="has_background">
                        <PartDraw x="0" y="0" width="${geometry.w}" height="${geometry.h}" alpha="255">
                            <Variant mode="AMBIENT" target="alpha" value="0" />
                            <Arc startAngle="${from}" endAngle="${to}" ${geometryAttributes}>
                                <Stroke color="[COMPLICATION.WEIGHTED_ELEMENTS_BACKGROUND_COLOR]" cap="ROUND" thickness="${thickness}" />
                            </Arc>
                        </PartDraw>
                    </Compare>
                </Condition>"""
        return """<Group name="Weighted" x="0" y="0" width="${geometry.w}" height="${geometry.h}">
                <PartDraw x="0" y="0" width="${geometry.w}" height="${geometry.h}" alpha="0">
                    <Variant mode="AMBIENT" target="alpha" value="255" />
                    <Arc startAngle="${from}" endAngle="${to}" ${geometryAttributes}>
                        <Stroke color="${ambientColor}" cap="ROUND" thickness="${thickness}" />
                    </Arc>
                </PartDraw>
                ${backdrop}
                <PartDraw x="0" y="0" width="${geometry.w}" height="${geometry.h}" alpha="255">
                    <Variant mode="AMBIENT" target="alpha" value="0" />
                    <Arc startAngle="${from}" endAngle="${to}" ${geometryAttributes}>
                        <WeightedStroke colors="[COMPLICATION.WEIGHTED_ELEMENTS_COLORS]" weights="[COMPLICATION.WEIGHTED_ELEMENTS_WEIGHTS]" interpolate="false" discreteGap="${discreteGap}" cap="ROUND" thickness="${thickness}" />
                    </Arc>
                </PartDraw>
            </Group>"""
    }

    /** How far between the minimum and the maximum the value sits, as 0 to 1. */
    private static String rangedFraction() {
        return 'clamp(([COMPLICATION.RANGED_VALUE_VALUE] - [COMPLICATION.RANGED_VALUE_MIN])' +
                ' / ([COMPLICATION.RANGED_VALUE_MAX] - [COMPLICATION.RANGED_VALUE_MIN]), 0, 1)'
    }

    /** How much of the goal is done, as 0 to 1. Past the goal it stops at the top. */
    private static String goalFraction() {
        return 'clamp([COMPLICATION.GOAL_PROGRESS_VALUE]' +
                ' / [COMPLICATION.GOAL_PROGRESS_TARGET_VALUE], 0, 1)'
    }

    /**
     * A ring drawn inside a slot's box.
     *
     * <p>The oval is pulled in by the stroke's own thickness - half of it at each side - because a
     * stroke straddles the oval it follows. A ring laid on the box itself has half of its width
     * outside the slot, and a slot clips what it draws, so it comes back half as thick as it was
     * asked for and with its outer edge cut flat.
     */
    private static String arc(int w, int h, int thickness, String stroke) {
        return "<Arc startAngle=\"0\" endAngle=\"360\" centerX=\"${w / 2}\" centerY=\"${h / 2}\"" +
                " width=\"${w - thickness}\" height=\"${h - thickness}\">${stroke}</Arc>"
    }

    private Node parse(String xml) {
        return (Node) replaceCustomTags(TemplateProcessor.parse(xml))
    }

    /**
     * A tag's attributes arrive unescaped, having already been through a parser, and go back into
     * XML text here. An expression that names a string - {@code numberFormat("#%", ...)} - carries
     * the quotes that would otherwise close the attribute early.
     */
    private static String escapeAttribute(String text) {
        return text.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')
                .replace('"', '&quot;')
    }

    private static String attribute(Node node, String name) {
        def value = node.attribute(name)
        return value == null ? null : value.toString()
    }

    private static String required(Node node, String name) {
        String value = attribute(node, name)
        if (value == null) {
            throw new IllegalArgumentException(
                    "<${node.name()}> requires ${name}, but was ${node.attributes()}")
        }
        return value
    }

    /**
     * Where in the slot an element goes.
     *
     * <p>A slot is wider than it is tall or it is not, and that decides whether an icon sits beside
     * what it labels or above it. The areas are named for the role rather than the arrangement, so
     * one layout serves both shapes:
     *
     * <ul>
     *   <li>{@code full} - the whole slot.
     *   <li>{@code photo} - the largest square that fits inside a round slot.
     *   <li>{@code icon} - an icon on its own, centered.
     *   <li>{@code glyph} - an icon on its own and larger, for a slot showing nothing else.
     *   <li>{@code icon_beside} - an icon paired with {@code text_beside}.
     *   <li>{@code text} - a line on its own, centered.
     *   <li>{@code text_beside} - a line paired with {@code icon_beside}.
     *   <li>{@code value} and {@code label} - two lines, the value above the label.
     *   <li>{@code header} and {@code body} - a heading over a paragraph.
     * </ul>
     */
    private int[] area(Node node) {
        String name = required(node, 'area')
        int[] rect = geometry.areas()[name]
        if (rect == null) {
            throw new IllegalArgumentException("Unknown ${node.name()} area '${name}';" +
                    " expected one of ${geometry.areas().keySet().join(', ')}")
        }
        return rect
    }

    /**
     * How big to set the type, as a share of the slot's shorter side.
     *
     * <p>The shorter side rather than the area, because two areas of the same height can want very
     * different sizes - a value with a label under it and a value on its own both sit in half the
     * slot - and because it is the diameter of the ring the text has to live inside.
     */
    private int fontSize(Node node, String fallback) {
        String scale = attribute(node, 'scale') ?: fallback
        if (formatVersion >= AUTO_SIZE_FORMAT_VERSION) {
            scale = attribute(node, 'autoSizeScale') ?: scale
        }
        Double factor = ['large': 0.30d, 'medium': 0.23d, 'small': 0.16d, 'tiny': 0.13d][scale]
        if (factor == null) {
            throw new IllegalArgumentException("Unknown ${node.name()} scale '${scale}';" +
                    ' expected large, medium, small or tiny')
        }
        // A band's height is its thickness, not the slot's, and the slot's is the whole face.
        //
        // A line of type takes about a fifth more room than its size, once the parts that reach
        // above and below the letters are counted, so the largest that fits a band is around two
        // thirds of its thickness rather than all of it. Type that wants to be bigger than that
        // needs a thicker band, not a bigger share of this one - there is nowhere else for it to
        // go, and it ends up over the edge.
        double reference = geometry.arc != null
                ? geometry.arc.thickness / 0.30d * 0.62d
                : Math.min(geometry.w, geometry.h)
        return Math.max(MIN_FONT_SIZE, (int) Math.round(reference * factor))
    }

    /**
     * A slot drawn as a band around the bezel, rather than as a ring with things inside it.
     *
     * <p>The oval the arc is scaled to is inset by the stroke's own half-thickness as well as the
     * caller's inset, because a stroke straddles the path it follows: an arc drawn on the very
     * edge of the face would have half of itself off the screen.
     */
    private static class ArcSpan {
        final double centerX, centerY, width, height
        final double startAngle, endAngle
        final int thickness
        final String direction

        ArcSpan(int slotWidth, int slotHeight, double startAngle, double endAngle, int thickness,
                int inset, String direction) {
            centerX = slotWidth / 2d
            centerY = slotHeight / 2d
            width = slotWidth - 2 * inset - thickness
            height = slotHeight - 2 * inset - thickness
            this.startAngle = startAngle
            this.endAngle = endAngle
            this.thickness = thickness
            this.direction = direction
        }

        private ArcSpan(double centerX, double centerY, double width, double height,
                        double startAngle, double endAngle, int thickness, String direction) {
            this.centerX = centerX
            this.centerY = centerY
            this.width = width
            this.height = height
            this.startAngle = startAngle
            this.endAngle = endAngle
            this.thickness = thickness
            this.direction = direction
        }

        /** How far round the band goes, in degrees, whichever way it is travelling. */
        double span() {
            double sweep = endAngle - startAngle
            return sweep < 0 ? sweep + 360 : sweep
        }

        /**
         * The angle the band finishes at, counted on from where it starts rather than wrapped
         * back round to a smaller number.
         *
         * <p>A band across twelve o'clock is written the way it reads - 330 to 30 - but written
         * out that way the end is behind the start, and an Arc given those two draws nothing at
         * all. Counting on says the same thing as 330 to 390, which is unambiguous, and the
         * format's angles are plain floats with no upper bound to bump into.
         */
        double sweepEnd() {
            return startAngle + span()
        }

        /**
         * Where a band is drawn.
         *
         * <p>Always clockwise, whichever way the band was declared, because {@link #sweepEnd}
         * counts the end on from the start and so describes a forward sweep. Labelling that same
         * pair counter-clockwise asks for everything it does not cover - a fortieth of the dial
         * becomes the other three hundred and twenty degrees of it.
         *
         * <p>A band's {@code direction} is about its text, not its stroke: it is what keeps a
         * reading on the lower half of the dial the right way up. {@link #textAttributes} is
         * where it belongs.
         */
        String geometryAttributes() {
            return "centerX=\"${centerX}\" centerY=\"${centerY}\" width=\"${width}\" height=\"${height}\"" +
                    " direction=\"CLOCKWISE\""
        }

        /**
         * Where a line of text on this band runs, and which way along it.
         *
         * <p>A counter-clockwise band reads from the end of its sweep back to the start, which on
         * the lower half of the dial is what puts the letters up the right way, so its two angles
         * are handed over the other way round.
         */
        String textAttributes() {
            boolean back = direction == 'COUNTER_CLOCKWISE'
            return "centerX=\"${centerX}\" centerY=\"${centerY}\" width=\"${width}\" height=\"${height}\"" +
                    " direction=\"${direction}\" startAngle=\"${back ? sweepEnd() : startAngle}\"" +
                    " endAngle=\"${back ? startAngle : sweepEnd()}\""
        }

        String shape(String stroke) {
            return "<Arc startAngle=\"${startAngle}\" endAngle=\"${sweepEnd()}\" ${geometryAttributes()}>${stroke}</Arc>"
        }

        /**
         * The region the watch treats as this complication, and outlines in the editor.
         *
         * <p>It is also a clip: nothing the complication draws survives outside it. A BoundingArc
         * measures its oval from the outside and lays its thickness inwards, where an Arc puts its
         * oval on the center line and straddles it, so the two want ovals half a thickness apart
         * to cover the same band. Handed the same one, the region falls half a thickness short of
         * the band at the rim and cuts the outer half of everything drawn there - the band's own
         * stroke, and the tops of its text - which looks for all the world like the text sitting
         * too low in a band that is thinner than it was asked to be.
         */
        String bounding() {
            return "<BoundingArc centerX=\"${centerX}\" centerY=\"${centerY}\"" +
                    " width=\"${width + thickness}\" height=\"${height + thickness}\"" +
                    " thickness=\"${thickness}\" startAngle=\"${startAngle}\"" +
                    " endAngle=\"${sweepEnd()}\" direction=\"CLOCKWISE\" isRoundEdge=\"TRUE\"" +
                    " outlinePadding=\"2\" />"
        }

        /**
         * Where something sits that should be centred on the band, as x, y, width, height.
         *
         * <p>Zero degrees is twelve o'clock and the angle runs clockwise, so the offsets are sine
         * for x and minus cosine for y rather than the other way round.
         */
        int[] centred(double atAngle, int size) {
            double radians = Math.toRadians(atAngle)
            double x = centerX + (width / 2d) * Math.sin(radians)
            double y = centerY - (height / 2d) * Math.cos(radians)
            return [(int) Math.round(x - size / 2d), (int) Math.round(y - size / 2d), size, size] as int[]
        }
    }

    /**
     * The slot's measurements.
     *
     * <p>The arithmetic uses Groovy's decimal division before truncating, matching the build script
     * these layouts came from so that existing watch faces render identically.
     */
    private static class Geometry {
        final int w
        final int h
        final boolean isHorizontal
        final int padH
        final int padV
        final int iconSize
        final int iconPadding
        final int iconXSingle, iconYSingle
        final int iconXSplit, iconYSplit
        final int textXSingle, textYSingle, textWidthSingle, textHeightSingle
        final int textXSplitIcon, textYSplitIcon, textWidthSplitIcon, textHeightSplitIcon
        final int titleXSplitText, titleYSplitText, titleWidthSplitText, titleHeightSplitText
        final int textXSplitText, textYSplitText, textWidthSplitText, textHeightSplitText
        final int backgroundIconSize
        final int glyphSize
        /** Set when the slot is a band around the bezel rather than a ring in a box. */
        final ArcSpan arc

        Geometry(int w, int h, ArcSpan arc = null) {
            this.w = w
            this.h = h
            this.arc = arc
            isHorizontal = w > h
            // A line of text is a rectangle inside a circle, so its box is narrower than the
            // slot: at a seventh of the height each side, the corners of a line as tall as the
            // largest type clear the ring wherever the layouts put one, and a six-character
            // value still fits at the middle size.
            padH = isHorizontal ? (int) (h / 2) : (int) (h / 7)
            padV = (int) (h / 6)

            iconSize = (int) (h * (isHorizontal ? 0.6f : 0.33f))
            iconPadding = (int) ((h - iconSize) / 2)

            iconXSingle = isHorizontal ? iconPadding : (int) (w / 2 - iconSize / 2)
            iconYSingle = (int) (h / 2 - iconSize / 2)

            iconXSplit = isHorizontal ? iconPadding : (int) (w / 2 - iconSize / 2)
            iconYSplit = isHorizontal ? (int) (h / 2 - iconSize / 2) : (int) (h / 2 - iconSize)

            textXSingle = padH
            textYSingle = padV
            textWidthSingle = w - 2 * padH
            textHeightSingle = h - 2 * padV

            textXSplitIcon = isHorizontal ? (iconSize + 2 * iconPadding) : padH
            textYSplitIcon = isHorizontal ? padV : (iconYSplit + iconSize)
            textWidthSplitIcon = isHorizontal ? (w - padH - textXSplitIcon) : (w - 2 * padH)
            textHeightSplitIcon = isHorizontal ? (h - 2 * padV) : (h - padV - textYSplitIcon)

            // A value with its label under it. Splitting the height in two centres each line in
            // its own half, which leaves a gap between them the width of a line and reads as two
            // things; the label's box is shallower and sits directly under the value's, so the
            // pair reads as one. Both start a little way down, because a label is lighter than
            // its value and the pair looks centred when it sits slightly low.
            int inner = h - 2 * padV
            titleXSplitText = padH
            titleYSplitText = padV + (int) (inner * 0.05f)
            titleWidthSplitText = w - 2 * padH
            titleHeightSplitText = (int) (inner * 0.56f)

            textXSplitText = padH
            textYSplitText = titleYSplitText + titleHeightSplitText
            textWidthSplitText = w - 2 * padH
            textHeightSplitText = (int) (inner * 0.30f)

            backgroundIconSize = (int) (Math.min(w, h) * 0.4f)
            // An image with nothing to share the slot with can be half again as big as one that
            // has, and still clear the ring.
            glyphSize = (int) (Math.min(w, h) * 0.5f)
        }

        Map<String, int[]> areas() {
            if (arc != null) {
                int size = (int) (arc.thickness * 0.8f)
                double middle = arc.startAngle + arc.span() / 2
                return [
                        'full'     : [0, 0, w, h] as int[],
                        // A band has no inside, so what it can hold is one small thing on it.
                        'arc_icon' : arc.centred(middle, size),
                        'arc_start': arc.centred(arc.startAngle, size),
                        'arc_end'  : arc.centred(arc.endAngle, size),
                ]
            }
            return [
                    'full'       : [0, 0, w, h] as int[],
                    // The largest square inside a round slot, for an image meant to fill one.
                    // Watch Face Format cannot clip, so the square is inscribed rather than
                    // cropped: a photo pushed out to the edges would have its corners hanging
                    // outside the ring.
                    'photo'      : [(int) (w * 0.1465f), (int) (h * 0.1465f),
                                    (int) (w * 0.707f), (int) (h * 0.707f)] as int[],
                    'icon'       : [iconXSingle, iconYSingle, iconSize, iconSize] as int[],
                    'glyph'      : [(int) ((w - glyphSize) / 2), (int) ((h - glyphSize) / 2),
                                    glyphSize, glyphSize] as int[],
                    'icon_beside': [iconXSplit, iconYSplit, iconSize, iconSize] as int[],
                    'text'       : [textXSingle, textYSingle, textWidthSingle, textHeightSingle] as int[],
                    'text_beside': [textXSplitIcon, textYSplitIcon, textWidthSplitIcon,
                                    textHeightSplitIcon] as int[],
                    // The value leads and the label follows, which is the order a reader expects
                    // and the order Wear's own complications use.
                    'value'      : [titleXSplitText, titleYSplitText, titleWidthSplitText,
                                    titleHeightSplitText] as int[],
                    'label'      : [textXSplitText, textYSplitText, textWidthSplitText,
                                    textHeightSplitText] as int[],
                    // A long string needs more of the slot than half of it, so the heading it sits
                    // under gets a third and the string keeps the rest.
                    'header'     : [padH, padV, w - 2 * padH, (int) ((h - 2 * padV) / 3)] as int[],
                    'body'       : [padH, padV + (int) ((h - 2 * padV) / 3), w - 2 * padH,
                                    (h - 2 * padV) - (int) ((h - 2 * padV) / 3)] as int[],
            ]
        }
    }
}
