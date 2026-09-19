package com.xlythe.watchface.format

import groovy.xml.XmlParser

import javax.xml.XMLConstants
import javax.xml.parsers.SAXParser
import javax.xml.parsers.SAXParserFactory
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Expands Watch Face Format templates.
 *
 * Variables are declared in an {@code <ItemList>} of {@code <Item name="${NAME}">expression</Item>}
 * entries and referenced from templates as {@code ${NAME}}. Every value is wrapped in parentheses so
 * it composes inside larger expressions, except when the placeholder is an entire quoted value
 * (e.g. {@code alpha="${ALPHA}"}), where the outer parentheses are dropped.
 */
final class TemplateProcessor {
    private static final int MAX_RESOLVE_PASSES = 1000
    private static final Pattern PLACEHOLDER = Pattern.compile('\\$\\{[A-Za-z0-9_.]+\\}')
    private static final Pattern QUOTED_OR_BARE_PLACEHOLDER =
            Pattern.compile('"(\\$\\{[A-Za-z0-9_.]+\\})"|(\\$\\{[A-Za-z0-9_.]+\\})')

    private TemplateProcessor() {}

    /**
     * Returns a parser configured like {@link XmlParser#XmlParser()}, but without JDK 24+'s default
     * 100,000 character entity limit. Expanded watch faces are routinely far larger than that.
     */
    static XmlParser newParser() {
        SAXParserFactory factory = SAXParserFactory.newInstance()
        factory.namespaceAware = true
        factory.validating = false
        setFeatureQuietly(factory, XMLConstants.FEATURE_SECURE_PROCESSING, true)
        setFeatureQuietly(factory, 'http://apache.org/xml/features/disallow-doctype-decl', true)
        SAXParser saxParser = factory.newSAXParser()
        for (String limit : [
                'jdk.xml.maxGeneralEntitySizeLimit',
                'jdk.xml.totalEntitySizeLimit',
                'http://www.oracle.com/xml/jaxp/properties/maxGeneralEntitySizeLimit',
                'http://www.oracle.com/xml/jaxp/properties/totalEntitySizeLimit']) {
            try {
                saxParser.setProperty(limit, '0')
            } catch (Exception ignored) {
                // Unknown to this JDK, which means it doesn't enforce the limit either.
            }
        }
        return new XmlParser(saxParser)
    }

    private static void setFeatureQuietly(SAXParserFactory factory, String feature, boolean value) {
        try {
            factory.setFeature(feature, value)
        } catch (Exception ignored) {
            // Not supported by this parser implementation.
        }
    }

    /** Parses a variable list into an ordered map of placeholder to parenthesized, XML-escaped value. */
    static Map<String, String> parseVariables(String xml, String sourceName) {
        Map<String, String> variables = new LinkedHashMap<>()
        for (Object child : newParser().parseText(xml).children()) {
            if (!(child instanceof Node)) {
                continue
            }
            Node item = (Node) child
            String name = item.attribute('name')
            if (name == null || !PLACEHOLDER.matcher(name).matches()) {
                throw new IllegalArgumentException(
                        "Variable in ${sourceName} must be named like \${NAME}, but was '${name}'")
            }
            String text = item.text()
            int open = text.count('(')
            int close = text.count(')')
            if (open != close) {
                throw new IllegalArgumentException(
                        "Mismatched parentheses for var ${name} in ${sourceName}: ( = ${open}, ) = ${close}")
            }
            variables.put(name, '(' + escape(normalizeWhitespace(text)) + ')')
        }
        return variables
    }

    /**
     * Builds {@code ${LATITUDE}} and {@code ${LONGITUDE}} from a table of
     * {@code <Location name="America/New_York" latitude="40.71" longitude="-74.00" />} entries,
     * selected at runtime by the watch's {@code [TIMEZONE_ID]}. Unknown zones fall back to 0.
     */
    static Map<String, String> parseTimeZoneCoordinates(String xml) {
        StringBuilder latitude = new StringBuilder()
        StringBuilder longitude = new StringBuilder()
        for (Object child : newParser().parseText(xml).children()) {
            if (!(child instanceof Node)) {
                continue
            }
            Node location = (Node) child
            String zone = location.attribute('name')
            latitude.append('([TIMEZONE_ID] == &quot;').append(zone).append('&quot;) ? ')
                    .append(location.attribute('latitude')).append(' : ')
            longitude.append('([TIMEZONE_ID] == &quot;').append(zone).append('&quot;) ? ')
                    .append(location.attribute('longitude')).append(' : ')
        }
        latitude.append('0')
        longitude.append('0')
        Map<String, String> variables = new LinkedHashMap<>()
        variables.put('${LATITUDE}', '(' + latitude + ')')
        variables.put('${LONGITUDE}', '(' + longitude + ')')
        return variables
    }

    /** Substitutes variables into each other until no known placeholder remains. */
    static Map<String, String> resolve(Map<String, String> variables) {
        Map<String, String> resolved = new LinkedHashMap<>(variables)
        for (String key : new ArrayList<>(resolved.keySet())) {
            String value = resolved.get(key)
            String previous = null
            int passes = 0
            while (previous != value) {
                if (++passes > MAX_RESOLVE_PASSES || value.contains(key)) {
                    throw new IllegalArgumentException("Variable ${key} refers to itself")
                }
                previous = value
                for (Map.Entry<String, String> entry : resolved.entrySet()) {
                    value = value.replace(entry.key, entry.value)
                }
            }
            resolved.put(key, value)
        }
        return resolved
    }

    /** Returns a copy of {@code variables} with each token replaced, e.g. unsupported data sources. */
    static Map<String, String> withReplacements(Map<String, String> variables, Map<String, String> replacements) {
        Map<String, String> copy = new LinkedHashMap<>()
        variables.each { String key, String value -> copy.put(key, replaceTokens(value, replacements)) }
        return copy
    }

    static String replaceTokens(String text, Map<String, String> replacements) {
        String result = text
        replacements.each { String token, String value -> result = result.replace(token, value) }
        return result
    }

    /** Inserts variable values into a template. Unknown placeholders are left in place. */
    static String expand(String template, Map<String, String> variables) {
        Matcher matcher = QUOTED_OR_BARE_PLACEHOLDER.matcher(template)
        StringBuilder out = new StringBuilder(template.length())
        int last = 0
        while (matcher.find()) {
            out.append(template, last, matcher.start())
            String quoted = matcher.group(1)
            if (quoted != null) {
                String value = variables.get(quoted)
                out.append(value == null ? matcher.group() : '"' + value.substring(1, value.length() - 1) + '"')
            } else {
                String value = variables.get(matcher.group(2))
                out.append(value == null ? matcher.group() : value)
            }
            last = matcher.end()
        }
        out.append(template, last, template.length())
        return out.toString()
    }

    /** Returns the distinct {@code ${NAME}} placeholders left in {@code text}. */
    static Set<String> findPlaceholders(String text) {
        Set<String> found = new TreeSet<>()
        Matcher matcher = PLACEHOLDER.matcher(text)
        while (matcher.find()) {
            found.add(matcher.group())
        }
        return found
    }

    /** Returns the distinct {@code [PREFIX...]} data sources referenced in {@code text}. */
    static Set<String> findDataSources(String text, String prefix) {
        Set<String> found = new TreeSet<>()
        Matcher matcher = Pattern.compile('\\[' + Pattern.quote(prefix) + '[A-Za-z0-9_.]*\\]').matcher(text)
        while (matcher.find()) {
            found.add(matcher.group())
        }
        return found
    }

    static Node parse(String xml) {
        return newParser().parseText(xml)
    }

    static String print(Node root) {
        return WatchFaceXmlPrinter.print(root)
    }

    /**
     * Replaces the {@code [WEATHER.*]} data sources left in {@code text} with neutral values, for
     * Watch Face Format versions that have no weather. Availability and error flags become 0, day
     * flags become 1, names become empty strings and everything else becomes 0.
     */
    static String stubWeatherDataSources(String text) {
        String result = text
        for (String token : findDataSources(text, 'WEATHER.')) {
            result = result.replace(token, weatherStub(token))
        }
        return result
    }

    static String weatherStub(String token) {
        String name = token.substring(1, token.length() - 1)
        if (name.endsWith('.IS_DAY')) {
            return '1'
        }
        if (name.endsWith('_NAME')) {
            return '&quot;&quot;'
        }
        return '0'
    }

    static String normalizeWhitespace(String text) {
        return text.replace('\n', '').replaceAll('\\s+', ' ').trim()
    }

    // Values are inserted into the raw template before it's parsed, so they must already be escaped.
    // CDATA can't be used because it isn't allowed inside attribute values like expression="${FOO}".
    static String escape(String text) {
        return text
                .replace('&', '&amp;')
                .replace('<', '&lt;')
                .replace('>', '&gt;')
                .replace("'", '&apos;')
                .replace('"', '&quot;')
    }
}
