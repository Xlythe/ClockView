package com.xlythe.watchface.format

import java.util.regex.Matcher
import java.util.regex.Pattern

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
 * <p>Chained references updated on a Wear OS 6 emulator when the source changed every second.
 * Shared inputs can therefore be evaluated once and read by downstream publishers. Publishers
 * are emitted in dependency order so each source exists before its consumers.
 */
class SharedValues {
    /** A group that draws nothing, sized so it is laid out rather than skipped. */
    private static final String PUBLISHER_SIZE = '1'
    private static final Pattern PLACEHOLDER = Pattern.compile('\\$\\{[A-Za-z0-9_.]+\\}')
    private static final Pattern REFERENCE = Pattern.compile('\\[REFERENCE\\.([A-Za-z0-9_.]+)\\]')

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

    static final class Plan {
        final Map<String, String> templateVariables
        final Map<String, String> publisherExpressions
        final List<String> publisherOrder

        Plan(Map<String, String> templateVariables, Map<String, String> publisherExpressions,
             List<String> publisherOrder) {
            this.templateVariables = templateVariables
            this.publisherExpressions = publisherExpressions
            this.publisherOrder = publisherOrder
        }
    }

    /** Expands ordinary variables, stopping at each published value so it can be read by name. */
    static Plan plan(Map<String, String> declared, Collection<String> names) {
        Set<String> shared = new LinkedHashSet<>(names.collect { placeholder(it) })
        for (String name : shared) {
            if (!declared.containsKey(name)) {
                throw new IllegalArgumentException("sharedVariables names ${name}, which is not defined")
            }
        }

        Map<String, String> publishers = new LinkedHashMap<>()
        for (String name : shared) {
            publishers.put(name, expand(declared.get(name), declared, shared,
                    new LinkedHashSet<String>([name])))
        }

        Map<String, String> template = new LinkedHashMap<>()
        for (Map.Entry<String, String> entry : declared.entrySet()) {
            template.put(entry.key, shared.contains(entry.key)
                    ? "[REFERENCE.${referenceName(entry.key)}]".toString()
                    : expand(entry.value, declared, shared,
                            new LinkedHashSet<String>([entry.key])))
        }
        return new Plan(template, publishers, dependencyOrder(publishers))
    }

    private static String expand(String expression, Map<String, String> declared,
                                 Set<String> shared, Set<String> active) {
        Matcher matcher = PLACEHOLDER.matcher(expression)
        StringBuffer result = new StringBuffer()
        while (matcher.find()) {
            String name = matcher.group()
            String replacement
            if (!declared.containsKey(name)) {
                replacement = name
            } else if (active.contains(name)) {
                throw new IllegalArgumentException("Variable ${name} refers to itself")
            } else if (shared.contains(name)) {
                replacement = "[REFERENCE.${referenceName(name)}]"
            } else {
                active.add(name)
                replacement = expand(declared.get(name), declared, shared, active)
                active.remove(name)
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement))
        }
        matcher.appendTail(result)
        return result.toString()
    }

    private static List<String> dependencyOrder(Map<String, String> publishers) {
        Map<String, Integer> states = [:]
        List<String> ordered = []
        Closure visit
        visit = { String name ->
            if (states.get(name) == 1) {
                throw new IllegalArgumentException("Shared variable ${name} depends on itself")
            }
            if (states.get(name) == 2) return
            states.put(name, 1)
            Matcher matcher = REFERENCE.matcher(publishers.get(name))
            while (matcher.find()) {
                String dependency = placeholder(matcher.group(1))
                if (publishers.containsKey(dependency)) visit(dependency)
            }
            states.put(name, 2)
            ordered.add(referenceName(name))
        }
        for (String name : publishers.keySet()) visit(name)
        return ordered
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
