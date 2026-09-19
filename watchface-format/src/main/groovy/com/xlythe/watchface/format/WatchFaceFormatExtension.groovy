package com.xlythe.watchface.format

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

import javax.inject.Inject

/**
 * Configures Watch Face Format generation.
 *
 * <pre>
 * watchFaceFormat {
 *     template = file('src/main/template/raw/watchface.xml')   // the default
 *     variables.from('src/main/template-variables/values/vars.xml')
 *     standardVariables = true   // ${IS_DAY}, ${IS_SUNSET}, ${IS_WEATHER_RAINY}, ...
 * }
 * </pre>
 *
 * Two variants are defined by default: {@code wff1} writes WFF v1 to {@code res/raw} (API 33, weather
 * stubbed out) and {@code wff2} writes WFF v2 to {@code res/raw-v34}. Adjust them in a
 * {@code variants} block, add more, or remove them.
 */
abstract class WatchFaceFormatExtension {
    final NamedDomainObjectContainer<WatchFaceVariant> variants

    @Inject
    WatchFaceFormatExtension(ObjectFactory objects) {
        variants = objects.domainObjectContainer(WatchFaceVariant) { String name ->
            objects.newInstance(WatchFaceVariant, name)
        }
    }

    /** The watch face template. Defaults to {@code src/main/template/raw/watchface.xml}. */
    abstract RegularFileProperty getTemplate()

    /** Variable lists ({@code <ItemList><Item name="${NAME}">...</Item></ItemList>}). Later files win. */
    abstract ConfigurableFileCollection getVariables()

    /**
     * Includes the bundled helper variables: time of day, sunrise/sunset, moon phase and weather
     * categories. Implies {@link #getTimeZoneCoordinates()}. Defaults to false.
     */
    abstract Property<Boolean> getStandardVariables()

    /** Defines {@code ${LATITUDE}} and {@code ${LONGITUDE}} from the watch's time zone. Defaults to false. */
    abstract Property<Boolean> getTimeZoneCoordinates()

    /** Replaces the bundled time zone coordinate table. */
    abstract RegularFileProperty getTimeZoneTable()

    /**
     * How many time zones to keep, counting from the top of the table. Defaults to all of them.
     *
     * <p>Watch Face Format has no location source, so the zone is the only clue to where the
     * watch is, and every zone in the table costs one string comparison at each place the face
     * asks. Inlined that adds up; published once with {@code <Reference>} it does not. The
     * bundled table leads with the zones the apps already offered, so trimming keeps those.
     */
    abstract Property<Integer> getTimeZoneCount()

    /** Where a zone outside the table is assumed to be. Defaults to 0, off the coast of Africa. */
    abstract Property<String> getDefaultLatitude()

    /** @see #getDefaultLatitude() */
    abstract Property<String> getDefaultLongitude()

    /** Directory with {@code complication_<type>.xml} files overriding the bundled slot layouts. */
    abstract DirectoryProperty getComplicationTemplates()

    /** Default complication color when a slot doesn't set {@code color}. Defaults to white. */
    abstract Property<String> getComplicationColor()

    /** Default ambient complication color when a slot doesn't set {@code ambientColor}. Defaults to white. */
    abstract Property<String> getComplicationAmbientColor()

    /**
     * Generates {@code @integer/watchface_format_version} for each variant, for use in the manifest's
     * {@code com.google.wear.watchface.format.version} property. Defaults to true.
     */
    abstract Property<Boolean> getGenerateFormatVersionResource()

    /**
     * Resource roots to copy art and simple values from, e.g. {@code '../ClockLibrary/src/main/res'}.
     * Use this instead of a project dependency: Watch Face Format bundles can't contain code.
     */
    abstract ConfigurableFileCollection getSharedResources()

    /**
     * Which files to copy from {@link #getSharedResources()}. Defaults to drawables, mipmaps, fonts,
     * raw files, and strings/integers/bools/colors/dimens values (no layouts, styles or attrs).
     */
    abstract ListProperty<String> getSharedResourceIncludes()

    /**
     * Variables to work out once and publish with {@code <Reference>}, rather than inlining at
     * every use. Watch Face Format 4 and up only; variants targeting an older version inline them
     * as usual, so the same template builds for both.
     *
     * <p>Worth doing for anything expensive that several elements read - sunrise, the position of
     * the sun or moon - because the watch re-evaluates an inlined expression once per use, every
     * time a data source in it changes.
     *
     * <p>Unverified on a device. The format says a reference falls back to its default value while
     * its element "is not available", and whether a group that draws nothing counts as available
     * is not written down anywhere. Check a published value arrives before relying on it.
     */
    abstract ListProperty<String> getSharedVariables()

    /**
     * Google's wff-validator.jar (https://github.com/google/watchface/releases). When set, every build
     * validates each generated variant against the format version it targets.
     */
    abstract RegularFileProperty getValidator()

    /**
     * Builds a separate bundle for each variant instead of one bundle with version-qualified
     * resources. Defaults to false.
     *
     * <p>Google's memory footprint check, which Google Play also runs, validates every watchface.xml
     * in a bundle against a single format version. In one bundle, every variant must therefore stay
     * valid at the lowest version. With separate bundles, each variant gets its own product flavor
     * ({@code wff1}, {@code wff2}, ...) with its own minSdk, format version and a version code of
     * {@code versionCode * versionCodeMultiplier + formatVersion}, so newer watches get the newest
     * format. Upload all of the bundles in the same release.
     */
    abstract Property<Boolean> getBundlePerFormatVersion()

    /** Multiplies the base versionCode when {@link #getBundlePerFormatVersion()} is enabled. Defaults to 10. */
    abstract Property<Integer> getVersionCodeMultiplier()

    void variants(Action<? super NamedDomainObjectContainer<WatchFaceVariant>> action) {
        action.execute(variants)
    }
}
