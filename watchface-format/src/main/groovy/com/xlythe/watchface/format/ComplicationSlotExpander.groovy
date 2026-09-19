package com.xlythe.watchface.format

/**
 * Replaces {@code <com.xlythe.ComplicationSlot>} tags with full Watch Face Format
 * {@code <ComplicationSlot>} layouts, mirroring ClockView's {@code ComplicationView}.
 *
 * <pre>
 * &lt;com.xlythe.ComplicationSlot slotId="1" x="200" y="200" width="160" height="160"
 *     type="chip" complicationDrawableStyle="line" color="#FFFFFFFF" ambientColor="#FFFFFFFF" /&gt;
 * </pre>
 *
 * {@code type} is {@code chip} or {@code background}; {@code complicationDrawableStyle} is
 * {@code fill}, {@code line}, {@code dot} or {@code empty}. {@code color} and {@code ambientColor}
 * are optional and accept any WFF color, including configuration references such as
 * {@code [CONFIGURATION.themeColor.1]}.
 */
final class ComplicationSlotExpander {
    static final String TAG = 'com.xlythe.ComplicationSlot'

    /** GOAL_PROGRESS and WEIGHTED_ELEMENTS arrived in Watch Face Format 2. */
    private static final int GOAL_PROGRESS_FORMAT_VERSION = 2

    private int formatVersion = 1

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

    /** @param formatVersion gates the complication types the output may name. */
    Node expand(Node root, int formatVersion) {
        this.formatVersion = formatVersion
        return (Node) replaceCustomTags(root)
    }

    private Object replaceCustomTags(Object node) {
        if (!(node instanceof Node)) {
            return node
        }
        Node element = (Node) node
        if (element.name() == TAG) {
            return expandSlot(element)
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
        String color = node.attribute('color') ?: defaultColor
        String ambientColor = node.attribute('ambientColor') ?: defaultAmbientColor
        if (slotId == null || x == null || y == null || width == null || height == null) {
            throw new IllegalArgumentException(
                    "${TAG} requires slotId, x, y, width and height, but was ${node.attributes()}")
        }

        int w = width.toString().toInteger()
        int h = height.toString().toInteger()
        boolean isHorizontal = w > h

        // Layout math intentionally uses Groovy's decimal division before truncating, matching the
        // original build script so existing watch faces render identically.
        int padH = isHorizontal ? (int) (h / 2) : (int) (h / 6)
        int padV = (int) (h / 6)

        int iconSize = (int) (h * (isHorizontal ? 0.6f : 0.33f))
        int iconPadding = (int) ((h - iconSize) / 2)

        // Single pane icon
        int iconXSingle = isHorizontal ? iconPadding : (int) (w / 2 - iconSize / 2)
        int iconYSingle = isHorizontal ? (int) (h / 2 - iconSize / 2) : (int) (h / 2 - iconSize / 2)

        // Split pane icon (icon_text, icon_title)
        int iconXSplit = isHorizontal ? iconPadding : (int) (w / 2 - iconSize / 2)
        int iconYSplit = isHorizontal ? (int) (h / 2 - iconSize / 2) : (int) (h / 2 - iconSize)

        // Single pane text / title
        int textXSingle = padH
        int textYSingle = padV
        int textWidthSingle = w - 2 * padH
        int textHeightSingle = h - 2 * padV

        // Split pane text / title with icon (icon_text, icon_title)
        int textXSplitIcon = isHorizontal ? (iconSize + 2 * iconPadding) : padH
        int textYSplitIcon = isHorizontal ? padV : (iconYSplit + iconSize)
        int textWidthSplitIcon = isHorizontal ? (w - padH - textXSplitIcon) : (w - 2 * padH)
        int textHeightSplitIcon = isHorizontal ? (h - 2 * padV) : (h - padV - textYSplitIcon)

        // Split pane text_title (no icon)
        int titleXSplitText = padH
        int titleYSplitText = padV
        int titleWidthSplitText = w - 2 * padH
        int titleHeightSplitText = (int) ((h - 2 * padV) / 2)

        int textXSplitText = padH
        int textYSplitText = padV + titleHeightSplitText
        int textWidthSplitText = w - 2 * padH
        int textHeightSplitText = (h - 2 * padV) - titleHeightSplitText

        // Arcs only accept a Stroke, so the filled style draws an Ellipse instead.
        String bgShapeAmbient = ''
        String bgShapeFullres = ''
        if (drawableStyle == 'fill') {
            bgShapeAmbient = "<Ellipse x=\"0\" y=\"0\" width=\"${width}\" height=\"${height}\"><Fill color=\"${ambientColor}\" /></Ellipse>"
            bgShapeFullres = "<Ellipse x=\"0\" y=\"0\" width=\"${width}\" height=\"${height}\"><Fill color=\"${color}\" /></Ellipse>"
        } else if (drawableStyle == 'line' || drawableStyle == null) {
            bgShapeAmbient = arc(w, h, width, height, "<Stroke color=\"${ambientColor}\" cap=\"ROUND\" thickness=\"4\" />")
            bgShapeFullres = arc(w, h, width, height, "<Stroke color=\"${color}\" cap=\"ROUND\" thickness=\"4\" />")
        } else if (drawableStyle == 'dot') {
            bgShapeAmbient = arc(w, h, width, height, "<Stroke color=\"${ambientColor}\" cap=\"ROUND\" thickness=\"4\" dashIntervals=\"6 3\" />")
            bgShapeFullres = arc(w, h, width, height, "<Stroke color=\"${color}\" cap=\"ROUND\" thickness=\"4\" dashIntervals=\"6 3\" />")
        } else if (drawableStyle != 'empty') {
            throw new IllegalArgumentException(
                    "Unknown complicationDrawableStyle '${drawableStyle}' for ${TAG} ${slotId}; expected fill, line, dot or empty")
        }

        String bgGroup = ''
        if (drawableStyle != 'empty') {
            bgGroup = """
            <!-- Draw the background -->
            <Group name="Background" x="0" y="0" width="${width}" height="${height}">
                <!-- Ambient background -->
                <PartDraw x="0" y="0" width="${width}" height="${height}" alpha="0">
                    <Variant mode="AMBIENT" target="alpha" value="255" />
                    ${bgShapeAmbient}
                </PartDraw>
                <!-- Full-res background -->
                <PartDraw x="0" y="0" width="${width}" height="${height}" alpha="255">
                    <Variant mode="AMBIENT" target="alpha" value="0" />
                    ${bgShapeFullres}
                </PartDraw>
            </Group>
"""
        }

        // Background slots: a ring inset by its stroke, with a centered icon.
        int backgroundIconSize = (int) (Math.min(w, h) * 0.4f)

        Map<String, Object> replacements = [
                '${COMPLICATION_ID}'        : slotId,
                '${POS_X}'                  : x,
                '${POS_Y}'                  : y,
                '${WIDTH}'                  : width,
                '${HEIGHT}'                 : height,
                '${BACKGROUND_GROUP}'       : bgGroup,
                // The sweep that makes a ranged value look like one.
                '${RANGED_VALUE_ARC}'       : progressArc(w, h, width, height, color, ambientColor, rangedFraction()),
                '${GOAL_PROGRESS_ARC}'      : progressArc(w, h, width, height, color, ambientColor, goalFraction()),
                // Single pane icon E.g. <Compare expression="icon">
                '${IMG_POS_X_SINGLE}'       : "${iconXSingle}",
                '${IMG_POS_Y_SINGLE}'       : "${iconYSingle}",
                '${IMG_WIDTH_SINGLE}'       : "${iconSize}",
                '${IMG_HEIGHT_SINGLE}'      : "${iconSize}",
                // Split pane icon E.g. <Compare expression="icon_text">, <Compare expression="icon_title">
                '${IMG_POS_X_SPLIT}'        : "${iconXSplit}",
                '${IMG_POS_Y_SPLIT}'        : "${iconYSplit}",
                '${IMG_WIDTH_SPLIT}'        : "${iconSize}",
                '${IMG_HEIGHT_SPLIT}'       : "${iconSize}",
                // Single pane text / title E.g. <Compare expression="text">, <Compare expression="title">
                '${TEXT_POS_X_SINGLE}'      : "${textXSingle}",
                '${TEXT_POS_Y_SINGLE}'      : "${textYSingle}",
                '${TEXT_WIDTH_SINGLE}'      : "${textWidthSingle}",
                '${TEXT_HEIGHT_SINGLE}'     : "${textHeightSingle}",
                // Split pane text / title with icon E.g. <Compare expression="icon_text">, <Compare expression="icon_title">
                '${TEXT_POS_X_SPLIT_ICON}'  : "${textXSplitIcon}",
                '${TEXT_POS_Y_SPLIT_ICON}'  : "${textYSplitIcon}",
                '${TEXT_WIDTH_SPLIT_ICON}'  : "${textWidthSplitIcon}",
                '${TEXT_HEIGHT_SPLIT_ICON}' : "${textHeightSplitIcon}",
                // Split pane text_title (no icon) E.g. <Compare expression="text_title">
                '${TITLE_POS_X_SPLIT_TEXT}' : "${titleXSplitText}",
                '${TITLE_POS_Y_SPLIT_TEXT}' : "${titleYSplitText}",
                '${TITLE_WIDTH_SPLIT_TEXT}' : "${titleWidthSplitText}",
                '${TITLE_HEIGHT_SPLIT_TEXT}': "${titleHeightSplitText}",
                '${TEXT_POS_X_SPLIT_TEXT}'  : "${textXSplitText}",
                '${TEXT_POS_Y_SPLIT_TEXT}'  : "${textYSplitText}",
                '${TEXT_WIDTH_SPLIT_TEXT}'  : "${textWidthSplitText}",
                '${TEXT_HEIGHT_SPLIT_TEXT}' : "${textHeightSplitText}",
                // Background slot E.g. complication_background.xml
                '${CENTER_X}'               : "${w / 2}",
                '${CENTER_Y}'               : "${h / 2}",
                '${RING_WIDTH}'             : "${w - 4}",
                '${RING_HEIGHT}'            : "${h - 4}",
                '${ICON_SIZE}'              : "${backgroundIconSize}",
                '${ICON_X}'                 : "${(int) ((w - backgroundIconSize) / 2)}",
                '${ICON_Y}'                 : "${(int) ((h - backgroundIconSize) / 2)}",
        ]

        def type = node.attribute('type')
        String template = withGoalProgress(templates.get(type))
        if (formatVersion < GOAL_PROGRESS_FORMAT_VERSION && template != null) {
            template = template.replace(' GOAL_PROGRESS', '')
        }
        if (template == null) {
            throw new IllegalArgumentException("Unknown ComplicationSlot type ${type}")
        }

        String content = template
        replacements.each { String key, Object value -> content = content.replace(key, value.toString()) }
        // Colors last: they may appear inside ${BACKGROUND_GROUP} and the template itself.
        content = content.replace('${AMBIENT_COLOR}', ambientColor).replace('${COLOR}', color)

        // Tidy the layout's text the same way as the rest of the document. WFF renders whitespace
        // inside text, e.g. around the %s in <Template>.
        return (Node) replaceCustomTags(TemplateProcessor.parse(content))
    }

    /**
     * Gives a slot a GOAL_PROGRESS layout by copying its RANGED_VALUE one.
     *
     * <p>The two show the same thing - a number against a bound, an optional icon, text and title
     * - and differ only in where the bound comes from and in that a goal can be passed. Copying
     * the block keeps one layout to maintain rather than three hundred lines said twice.
     *
     * <p>Format 1 has no such complication type, so it gets neither the layout nor the mention of
     * it in supportedTypes.
     */
    private String withGoalProgress(String template) {
        if (template == null || !template.contains('<Complication type="RANGED_VALUE">')
                || formatVersion < GOAL_PROGRESS_FORMAT_VERSION) {
            return template
        }
        int start = template.indexOf('<Complication type="RANGED_VALUE">')
        int end = template.indexOf('</Complication>', start)
        if (end < 0) {
            throw new IllegalArgumentException('A RANGED_VALUE complication was opened and never closed')
        }
        end += '</Complication>'.length()
        String goal = template.substring(start, end)
                .replace('<Complication type="RANGED_VALUE">', '<Complication type="GOAL_PROGRESS">')
                .replace('${RANGED_VALUE_ARC}', '${GOAL_PROGRESS_ARC}')
        return template.substring(0, end) + System.lineSeparator() + goal + template.substring(end)
    }

    private static String arc(int w, int h, Object width, Object height, String stroke) {
        return "<Arc startAngle=\"0\" endAngle=\"360\" centerX=\"${w / 2}\" centerY=\"${h / 2}\" width=\"${width}\" height=\"${height}\">${stroke}</Arc>"
    }

    /**
     * The sweep that shows how far along a ranged value or a goal is.
     *
     * <p>Drawn over the slot's own ring, so the ring reads as the track and this as what has been
     * filled. It starts at twelve o'clock, which is where Watch Face Format puts zero degrees,
     * and goes clockwise.
     *
     * <p>endAngle takes a number rather than an expression, so the sweep is applied with a
     * Transform - which is the only reason an Arc accepts one.
     *
     * @param fraction an expression between 0 and 1, already clamped.
     */
    private static String progressArc(int w, int h, Object width, Object height, String color,
                                      String ambientColor, String fraction) {
        String geometry = "centerX=\"${w / 2}\" centerY=\"${h / 2}\" width=\"${width}\" height=\"${height}\""
        String sweep = "<Transform target=\"endAngle\" value=\"360 * (${fraction})\" />"
        return """<Group name="Progress" x="0" y="0" width="${width}" height="${height}">
                <PartDraw x="0" y="0" width="${width}" height="${height}" alpha="0">
                    <Variant mode="AMBIENT" target="alpha" value="255" />
                    <Arc startAngle="0" endAngle="0" ${geometry}>
                        ${sweep}
                        <Stroke color="${ambientColor}" cap="ROUND" thickness="8" />
                    </Arc>
                </PartDraw>
                <PartDraw x="0" y="0" width="${width}" height="${height}" alpha="255">
                    <Variant mode="AMBIENT" target="alpha" value="0" />
                    <Arc startAngle="0" endAngle="0" ${geometry}>
                        ${sweep}
                        <Stroke color="${color}" cap="ROUND" thickness="8" />
                    </Arc>
                </PartDraw>
            </Group>"""
    }

    /** How far between the minimum and the maximum the value sits, as 0 to 1. */
    private static String rangedFraction() {
        return 'clamp(([COMPLICATION.RANGED_VALUE_VALUE] - [COMPLICATION.RANGED_VALUE_MIN])' +
                ' / ([COMPLICATION.RANGED_VALUE_MAX] - [COMPLICATION.RANGED_VALUE_MIN]), 0, 1)'
    }

    /**
     * How much of the goal is done, as 0 to 1. A goal can be passed - that is rather the point of
     * one - and the ring has nowhere to put more than a full turn, so it stops at the top.
     */
    private static String goalFraction() {
        return 'clamp([COMPLICATION.GOAL_PROGRESS_VALUE]' +
                ' / [COMPLICATION.GOAL_PROGRESS_TARGET_VALUE], 0, 1)'
    }
}
