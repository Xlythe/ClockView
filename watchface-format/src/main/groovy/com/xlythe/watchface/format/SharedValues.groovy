package com.xlythe.watchface.format

/**
 * Publishes a variable once and lets the rest of the face read it, on Watch Face Format 4 and up.
 *
 * <p>Everywhere else in this plugin a variable is inlined at each use, because the format has no
 * variables. That costs the watch real work: an expression is re-evaluated whenever a data source
 * inside it changes, so a sunrise calculation written into twenty places is worked out twenty
 * times. Format 4 added {@code <Reference>}, which publishes a transformable attribute of an
 * element under a name that other expressions read as {@code [REFERENCE.NAME]}.
 *
 * <p>The published value rides on {@code scaleX}, which is a float. The obvious choice, alpha, is
 * an integer from 0 to 255 and would round every value it carried.
 *
 * <p>Each published expression is still complete in itself: a shared variable that uses another
 * shared variable inlines it rather than reading its reference. Chaining would be smaller again,
 * but the format's own documentation warns that a reference reached through another reference
 * "is updated only once", and a sun that stops moving is worse than a sun that costs more.
 */
class SharedValues {
    /** A group that draws nothing, sized so it is laid out rather than skipped. */
    private static final String PUBLISHER_SIZE = '1'

    private SharedValues() {}

    /** {@code SUNRISE} and {@code ${SUNRISE}} both name the same variable. */
    static String placeholder(String name) {
        return name.startsWith('${') ? name : "\${${name}}"
    }

    static String referenceName(String name) {
        return name.replaceAll(/^\$\{|\}$/, '')
    }

    /**
     * What to rewrite the template's uses into. Applied to the template only: the published
     * expressions keep the real thing.
     */
    static Map<String, String> asReferences(Collection<String> names) {
        Map<String, String> references = new LinkedHashMap<>()
        for (String name : names) {
            references.put(placeholder(name), "[REFERENCE.${referenceName(name)}]".toString())
        }
        return references
    }

    /**
     * The group that works each shared variable out once. Belongs inside {@code <Scene>}, before
     * anything that reads it.
     */
    static String publisherXml(Map<String, String> resolved, Collection<String> names) {
        StringBuilder xml = new StringBuilder()
        xml.append('<Group name="SharedValues" x="0" y="0" width="')
                .append(PUBLISHER_SIZE).append('" height="').append(PUBLISHER_SIZE).append('">\n')
        for (String name : names) {
            String placeholder = placeholder(name)
            String expression = resolved.get(placeholder)
            if (expression == null) {
                throw new IllegalArgumentException("sharedVariables names ${placeholder}, which is not defined")
            }
            String reference = referenceName(name)
            xml.append('    <Group name="Shared_').append(reference)
                    .append('" x="0" y="0" width="').append(PUBLISHER_SIZE)
                    .append('" height="').append(PUBLISHER_SIZE).append('" scaleX="1">\n')
                    .append('        <Transform target="scaleX" value="').append(expression).append('" />\n')
                    .append('        <Reference name="').append(reference)
                    .append('" source="scaleX" defaultValue="0" />\n')
                    .append('    </Group>\n')
        }
        xml.append('</Group>\n')
        return xml.toString()
    }

    /**
     * Puts the publisher group at the top of the scene. Text rather than tree surgery, because
     * this runs before the template is parsed - the template still holds the plugin's own
     * complication tags at that point.
     */
    static String insertIntoScene(String template, String publisher) {
        def scene = template =~ /<Scene\b[^>]*>/
        if (!scene.find()) {
            throw new IllegalArgumentException('The template has no <Scene> to publish shared values into')
        }
        int at = scene.end()
        return template.substring(0, at) + '\n' + publisher + template.substring(at)
    }
}
