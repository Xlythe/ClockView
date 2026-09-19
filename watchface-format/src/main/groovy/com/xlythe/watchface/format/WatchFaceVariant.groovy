package com.xlythe.watchface.format

import org.gradle.api.Named
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input

import javax.inject.Inject

/**
 * One generated copy of the watch face, e.g. WFF v1 in {@code res/raw} for API 33 and WFF v2 in
 * {@code res/raw-v34} for API 34+. Each variant gets its own replacements, so tokens stubbed out
 * for an older format don't leak into newer ones.
 */
abstract class WatchFaceVariant implements Named {
    private final String name

    @Inject
    WatchFaceVariant(String name) {
        this.name = name
        stubWeather.convention(false)
        // raw-v34 means API 34+. Plain raw means the Watch Face Format minimum, API 33.
        minSdk.convention(resourceQualifier.map { String qualifier ->
            def version = qualifier =~ /-v(\d+)/
            version.find() ? version.group(1).toInteger() : 33
        })
    }

    /**
     * The lowest API level this variant runs on. With {@code bundlePerFormatVersion}, it becomes the
     * minSdk of this variant's bundle. Defaults to the API level in the resource qualifier, or 33.
     */
    @Input
    abstract Property<Integer> getMinSdk()

    @Input
    @Override
    String getName() {
        return name
    }

    /** Resource directory to write {@code watchface.xml} into, e.g. {@code raw} or {@code raw-v34}. */
    @Input
    abstract Property<String> getResourceQualifier()

    /** Watch Face Format version the output targets. Must match the resource qualifier's API level. */
    @Input
    abstract Property<Integer> getFormatVersion()

    /**
     * Literal tokens to replace in this variant's variables and template, e.g. a v2-only attribute.
     * Applied before weather stubbing, so they can also give specific weather values.
     */
    @Input
    abstract MapProperty<String, String> getReplacements()

    /** Whether to replace every remaining {@code [WEATHER.*]} data source with a neutral value. */
    @Input
    abstract Property<Boolean> getStubWeather()

    void replace(String token, String value) {
        replacements.put(token, value)
    }

    /**
     * Replaces the weather data sources that WFF v1 lacks with neutral values: availability and
     * error flags become 0, day flags 1, names empty, and everything else 0. Hide weather UI behind
     * {@code [WEATHER.IS_AVAILABLE]} and it stays hidden in this variant.
     */
    void stubWeatherDataSources() {
        stubWeather.set(true)
    }
}
