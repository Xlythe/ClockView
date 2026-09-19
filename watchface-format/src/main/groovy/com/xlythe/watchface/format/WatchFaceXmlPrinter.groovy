package com.xlythe.watchface.format

import groovy.namespace.QName

/**
 * Prints watch face XML with elements indented, but text kept tight against its tags.
 *
 * The Watch Face Format renders whitespace inside text elements, so a pretty-printer that moves
 * {@code TRUE} or {@code %s<Parameter .../>} onto their own indented lines visibly shifts the text.
 * Elements with only text, or with text mixed with elements (like {@code <Template>}), are printed on
 * one line. Elements with only child elements are indented.
 */
final class WatchFaceXmlPrinter {
    private static final String INDENT = '  '

    private WatchFaceXmlPrinter() {}

    static String print(Node root) {
        StringBuilder out = new StringBuilder()
        printBlock(root, 0, out)
        return out.toString()
    }

    private static void printBlock(Node node, int depth, StringBuilder out) {
        indent(out, depth)
        List<Object> children = childrenOf(node)
        if (children.isEmpty()) {
            openTag(node, out, true)
        } else if (children.every { it instanceof Node }) {
            openTag(node, out, false)
            out.append('\n')
            for (Object child : children) {
                printBlock((Node) child, depth + 1, out)
            }
            indent(out, depth)
            closeTag(node, out)
        } else {
            printInline(node, out)
        }
        out.append('\n')
    }

    private static void printInline(Node node, StringBuilder out) {
        List<Object> children = childrenOf(node)
        if (children.isEmpty()) {
            openTag(node, out, true)
            return
        }
        openTag(node, out, false)
        for (Object child : children) {
            if (child instanceof Node) {
                printInline((Node) child, out)
            } else {
                out.append(escapeText(child.toString()))
            }
        }
        closeTag(node, out)
    }

    private static List<Object> childrenOf(Node node) {
        Object value = node.value()
        if (value == null) {
            return Collections.emptyList()
        }
        if (value instanceof List) {
            return ((List<Object>) value).findAll { !(it instanceof CharSequence) || it.toString().length() > 0 }
        }
        String text = value.toString()
        return text.isEmpty() ? Collections.emptyList() : Collections.singletonList((Object) text)
    }

    private static void openTag(Node node, StringBuilder out, boolean selfClosing) {
        out.append('<').append(nameOf(node))
        node.attributes().each { key, value ->
            out.append(' ').append(nameOf(key)).append('="').append(escapeAttribute(value.toString())).append('"')
        }
        out.append(selfClosing ? '/>' : '>')
    }

    private static void closeTag(Node node, StringBuilder out) {
        out.append('</').append(nameOf(node)).append('>')
    }

    private static String nameOf(Object name) {
        if (name instanceof Node) {
            name = ((Node) name).name()
        }
        return name instanceof QName ? ((QName) name).qualifiedName : name.toString()
    }

    private static void indent(StringBuilder out, int depth) {
        for (int i = 0; i < depth; i++) {
            out.append(INDENT)
        }
    }

    private static String escapeText(String text) {
        return text.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')
    }

    private static String escapeAttribute(String text) {
        return escapeText(text)
                .replace('"', '&quot;')
                .replace('\n', '&#10;')
                .replace('\r', '&#13;')
                .replace('\t', '&#9;')
    }
}
