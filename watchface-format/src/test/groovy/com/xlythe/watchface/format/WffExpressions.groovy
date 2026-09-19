package com.xlythe.watchface.format

/**
 * Works out what a bundled variable comes to for a given place and moment.
 *
 * <p>standard.xml is read off the classpath and resolved the way GenerateWatchFaceTask resolves
 * it, so what gets evaluated is the string that ships. The platform's own sources are filled in
 * here, since a test has no watch to ask.
 */
class WffExpressions {

    private final Map<String, String> mResolved

    WffExpressions() {
        String xml = WffExpressions.class
                .getResourceAsStream('/com/xlythe/watchface/format/variables/standard.xml')
                .getText('UTF-8')
        mResolved = TemplateProcessor.resolve(TemplateProcessor.parseVariables(xml, 'standard.xml'))
    }

    /** @param hour the hour of the day, UTC, since the test places the watch in UTC. */
    double evaluate(String variable, double latitude, double longitude,
                    int year, int dayOfYear, double hour = 12d) {
        String expression = mResolved[variable]
        assert expression != null: "$variable is not defined in standard.xml"
        // The watch sits in UTC for these, so the epoch timestamp and the local clock agree.
        long millis = java.time.LocalDate.ofYearDay(year, dayOfYear).toEpochDay() * 86400000L +
                Math.round(hour * 3600000d)
        expression = expression
                .replace('${LATITUDE}', "($latitude)")
                .replace('${LONGITUDE}', "($longitude)")
                // The place is given directly rather than looked up from a zone, so stand the
                // zone table down and put the watch in UTC, which is what these tests assume.
                .replace('${TIMEZONE_STANDARD_OFFSET_MINUTES}', '(0)')
                .replace('${TIMEZONE_DAYLIGHT_MINUTES}', '(60)')
                .replace('[IS_DAYLIGHT_SAVING_TIME]', '(0)')
                .replace('[YEAR]', "($year)")
                .replace('[DAY_OF_YEAR]', "($dayOfYear)")
                .replace('[HOUR_0_23]', "(${(int) hour})")
                .replace('[MINUTE]', "(${(int) ((hour - (int) hour) * 60)})")
                .replace('[SECOND]', '(0)')
                .replace('[MILLISECOND]', '(0)')
                .replace('[UTC_TIMESTAMP]', "($millis)")
                .replace('&lt;', '<').replace('&gt;', '>').replace('&amp;', '&')
                .replace('&quot;', '"').replace('&apos;', "'")
        return new Evaluator(expression).evaluate()
    }

    /** Just enough of the Watch Face Format's arithmetic to work out a number. */
    private static class Evaluator {
        private final String mText
        private int mAt = 0

        Evaluator(String text) {
            mText = text
        }

        double evaluate() {
            double value = readTernary()
            skipSpace()
            if (mAt < mText.length()) {
                throw new IllegalStateException("Unparsed from ${mAt}: ${peek()}")
            }
            return value
        }

        private double readTernary() {
            double condition = readOr()
            skipSpace()
            if (take('?')) {
                double whenTrue = readTernary()
                skipSpace()
                expect(':')
                double whenFalse = readTernary()
                return condition != 0d ? whenTrue : whenFalse
            }
            return condition
        }

        // Both sides are always read, short-circuiting or not: the parser has to get past them.
        private double readOr() {
            double value = readAnd()
            while (true) {
                skipSpace()
                if (takeAll('||')) {
                    boolean left = value != 0d
                    boolean right = readAnd() != 0d
                    value = (left || right) ? 1d : 0d
                } else {
                    return value
                }
            }
        }

        private double readAnd() {
            double value = readComparison()
            while (true) {
                skipSpace()
                if (takeAll('&&')) {
                    boolean left = value != 0d
                    boolean right = readComparison() != 0d
                    value = (left && right) ? 1d : 0d
                } else {
                    return value
                }
            }
        }

        private double readComparison() {
            double value = readSum()
            skipSpace()
            if (takeAll('>=')) {
                return (value >= readSum()) ? 1d : 0d
            } else if (takeAll('<=')) {
                return (value <= readSum()) ? 1d : 0d
            } else if (takeAll('==')) {
                return (value == readSum()) ? 1d : 0d
            } else if (takeAll('!=')) {
                return (value != readSum()) ? 1d : 0d
            } else if (peekIs('>')) {
                mAt++
                return (value > readSum()) ? 1d : 0d
            } else if (peekIs('<')) {
                mAt++
                return (value < readSum()) ? 1d : 0d
            }
            return value
        }

        private double readSum() {
            double value = readProduct()
            while (true) {
                skipSpace()
                // Not the start of >=, <=, ==, != or a comparison: those belong to readComparison.
                if (peekIs('+')) {
                    mAt++
                    value += readProduct()
                } else if (peekIs('-')) {
                    mAt++
                    value -= readProduct()
                } else {
                    return value
                }
            }
        }

        private double readProduct() {
            double value = readTerm()
            while (true) {
                skipSpace()
                if (peekIs('*')) {
                    mAt++
                    value *= readTerm()
                } else if (peekIs('/')) {
                    mAt++
                    value /= readTerm()
                } else if (peekIs('%')) {
                    mAt++
                    value %= readTerm()
                } else {
                    return value
                }
            }
        }

        private double readTerm() {
            skipSpace()
            if (take('-')) {
                return -readTerm()
            }
            if (take('+')) {
                return readTerm()
            }
            if (take('(')) {
                double value = readTernary()
                skipSpace()
                expect(')')
                return value
            }
            int start = mAt
            while (mAt < mText.length() && Character.isLetter(mText.charAt(mAt))) {
                mAt++
            }
            if (mAt > start) {
                return readCall(mText.substring(start, mAt))
            }
            while (mAt < mText.length()
                    && (Character.isDigit(mText.charAt(mAt)) || mText.charAt(mAt) == ('.' as char))) {
                mAt++
            }
            if (mAt == start) {
                throw new IllegalStateException("Expected a number at $mAt: ${peek()}")
            }
            return Double.parseDouble(mText.substring(start, mAt))
        }

        private double readCall(String name) {
            skipSpace()
            expect('(')
            List<Double> arguments = [readTernary()]
            skipSpace()
            while (take(',')) {
                arguments.add(readTernary())
                skipSpace()
            }
            expect(')')
            switch (name) {
                case 'sin': return Math.sin(arguments[0])
                case 'cos': return Math.cos(arguments[0])
                case 'tan': return Math.tan(arguments[0])
                case 'asin': return Math.asin(arguments[0])
                case 'acos': return Math.acos(arguments[0])
                case 'atan': return Math.atan(arguments[0])
                case 'rad': return Math.toRadians(arguments[0])
                case 'deg': return Math.toDegrees(arguments[0])
                case 'abs': return Math.abs(arguments[0])
                case 'floor': return Math.floor(arguments[0])
                case 'ceil': return Math.ceil(arguments[0])
                case 'round': return Math.round(arguments[0]) as double
                case 'sqrt': return Math.sqrt(arguments[0])
                case 'exp': return Math.exp(arguments[0])
                case 'log': return Math.log(arguments[0])
                case 'pow': return Math.pow(arguments[0], arguments[1])
                case 'clamp': return Math.max(arguments[1], Math.min(arguments[2], arguments[0]))
                default: throw new IllegalStateException("No such function: $name")
            }
        }

        private String peek() {
            return mText.substring(mAt, Math.min(mAt + 40, mText.length()))
        }

        private boolean peekIs(String expected) {
            return mAt < mText.length() && mText.charAt(mAt) == expected.charAt(0)
        }

        private void skipSpace() {
            while (mAt < mText.length() && Character.isWhitespace(mText.charAt(mAt))) {
                mAt++
            }
        }

        private boolean take(String expected) {
            skipSpace()
            if (mAt < mText.length() && mText.charAt(mAt) == expected.charAt(0)) {
                mAt++
                return true
            }
            return false
        }

        private boolean takeAll(String expected) {
            skipSpace()
            if (mText.startsWith(expected, mAt)) {
                mAt += expected.length()
                return true
            }
            return false
        }

        private void expect(String expected) {
            if (!take(expected)) {
                throw new IllegalStateException("Expected '$expected' at $mAt: ${peek()}")
            }
        }
    }
}
