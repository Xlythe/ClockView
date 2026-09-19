package com.xlythe.watchface.format

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/** Expands the watch face template into {@code res/<qualifier>/watchface.xml} for each variant. */
@CacheableTask
abstract class GenerateWatchFaceTask extends DefaultTask {
    static final String FORMAT_VERSION_RESOURCE = 'watchface_format_version'

    /** &lt;Reference&gt; arrived in Watch Face Format 4. */
    private static final int REFERENCE_FORMAT_VERSION = 4

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract RegularFileProperty getTemplate()

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract ConfigurableFileCollection getVariableFiles()

    @Input
    abstract Property<Boolean> getStandardVariables()

    @Input
    abstract Property<Boolean> getTimeZoneCoordinates()

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract RegularFileProperty getTimeZoneTable()

    @InputDirectory
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract DirectoryProperty getComplicationTemplates()

    @Input
    abstract Property<String> getComplicationColor()

    @Input
    abstract Property<String> getComplicationAmbientColor()

    @Input
    abstract Property<Boolean> getGenerateFormatVersionResource()

    @Input
    abstract ListProperty<String> getSharedVariables()

    @Nested
    abstract ListProperty<WatchFaceVariant> getVariants()

    /**
     * Writes the (single) variant to {@code res/raw} and {@code res/values} regardless of its
     * resource qualifier, for bundles that each hold one format version.
     */
    @Input
    abstract Property<Boolean> getWriteToDefaultQualifiers()

    @OutputDirectory
    abstract DirectoryProperty getOutputDirectory()

    @TaskAction
    void generate() {
        File outputRoot = outputDirectory.get().asFile
        outputRoot.deleteDir()
        outputRoot.mkdirs()

        File templateFile = template.get().asFile
        Map<Integer, Map<String, String>> variablesByVersion = new LinkedHashMap<>()
        ComplicationSlotExpander expander
        try {
            for (WatchFaceVariant variant : variants.get()) {
                int version = variant.formatVersion.get()
                variablesByVersion.computeIfAbsent(version) { TemplateProcessor.resolve(declaredVariables(it)) }
            }
            expander = new ComplicationSlotExpander(
                    complicationLayouts(), complicationColor.get(), complicationAmbientColor.get())
        } catch (IllegalArgumentException e) {
            throw new GradleException(e.message, e)
        }
        String templateText = templateFile.getText('UTF-8')
        Set<String> qualifiers = new HashSet<>()
        boolean defaultQualifiers = writeToDefaultQualifiers.getOrElse(false)
        if (defaultQualifiers && variants.get().size() != 1) {
            throw new GradleException("Expected one watch face variant per bundle, but got ${variants.get()*.name}")
        }

        for (WatchFaceVariant variant : variants.get()) {
            String qualifier = defaultQualifiers ? 'raw' : variant.resourceQualifier.get()
            if (!qualifier.startsWith('raw')) {
                throw new GradleException("Watch face variant '${variant.name}' must write to a raw resource directory, but was '${qualifier}'")
            }
            if (!qualifiers.add(qualifier)) {
                throw new GradleException("More than one watch face variant writes to res/${qualifier}")
            }

            Map<String, String> replacements = variant.replacements.get()
            Map<String, String> variantVariables = TemplateProcessor.withReplacements(
                    variablesByVersion.get(variant.formatVersion.get()), replacements)

            // Reference arrived in format 4. Older variants inline as usual, so one template
            // serves both and the shared list costs nothing where it can't be honoured.
            String variantTemplate = templateText
            List<String> shared = sharedVariables.getOrElse([])
            if (!shared.isEmpty() && variant.formatVersion.get() >= REFERENCE_FORMAT_VERSION) {
                try {
                    variantTemplate = SharedValues.insertIntoScene(variantTemplate,
                            SharedValues.publisherXml(variantVariables, shared))
                } catch (IllegalArgumentException e) {
                    throw new GradleException("${templateFile.name} (${variant.name}): ${e.message}", e)
                }
                variantVariables = new LinkedHashMap<>(variantVariables)
                variantVariables.putAll(SharedValues.asReferences(shared))
            }

            String expanded = TemplateProcessor.replaceTokens(
                    TemplateProcessor.expand(variantTemplate, variantVariables),
                    replacements)
            if (variant.stubWeather.get()) {
                expanded = TemplateProcessor.stubWeatherDataSources(expanded)
            }

            if (expanded.contains('resource="@drawable/')) {
                logger.warn("{} references resources as @drawable/NAME. The Wear OS runtime accepts that, but Google's " +
                        "memory footprint check (also run by Google Play) can't find them. Use resource=\"NAME\".", templateFile.name)
            }

            Set<String> undefined = TemplateProcessor.findPlaceholders(expanded)
            if (!undefined.isEmpty()) {
                throw new GradleException("${templateFile.name} uses undefined variables: ${undefined.join(', ')}")
            }
            if (variant.formatVersion.get() < 2) {
                Set<String> unsupported = TemplateProcessor.findDataSources(expanded, 'WEATHER.')
                if (!unsupported.isEmpty()) {
                    throw new GradleException("Watch face variant '${variant.name}' targets WFF ${variant.formatVersion.get()}, " +
                            "which has no weather data, but uses ${unsupported.join(', ')}. Call stubWeatherDataSources() " +
                            "in that variant, or replace them.")
                }
            }

            Node root
            try {
                root = expander.expand(TemplateProcessor.parse(expanded))
            } catch (IllegalArgumentException e) {
                throw new GradleException("${templateFile.name} (${variant.name}): ${e.message}", e)
            }
            writeUtf8(new File(outputRoot, "${qualifier}/watchface.xml"), TemplateProcessor.print(root))

            if (generateFormatVersionResource.get()) {
                String valuesQualifier = qualifier.replaceFirst('^raw', 'values')
                writeUtf8(new File(outputRoot, "${valuesQualifier}/${FORMAT_VERSION_RESOURCE}.xml"),
                        """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <integer name="${FORMAT_VERSION_RESOURCE}">${variant.formatVersion.get()}</integer>
</resources>
""")
            }
            logger.info("Generated {} (WFF {})", qualifier, variant.formatVersion.get())
        }
    }

    /**
     * @param formatVersion decides how the date and the UTC offset are worked out. See
     *     {@link StandardDates}.
     */
    private Map<String, String> declaredVariables(int formatVersion) {
        Map<String, String> declared = new LinkedHashMap<>()
        if (standardVariables.get()) {
            declared.putAll(TemplateProcessor.parseVariables(bundledResource('variables/standard.xml'), 'standard variables'))
            declared.putAll(StandardDates.forFormatVersion(formatVersion))
        }
        for (File file : variableFiles.files) {
            declared.putAll(TemplateProcessor.parseVariables(file.getText('UTF-8'), file.name))
        }
        if (standardVariables.get() || timeZoneCoordinates.get()) {
            String table = timeZoneTable.present
                    ? timeZoneTable.get().asFile.getText('UTF-8')
                    : bundledResource('geo/timezones.xml')
            declared.putAll(TemplateProcessor.parseTimeZoneCoordinates(table))
        }
        return declared
    }

    private Map<String, String> complicationLayouts() {
        Map<String, String> layouts = new LinkedHashMap<>()
        for (String type : ['chip', 'background']) {
            layouts.put(type, bundledResource("templates/complication_${type}.xml"))
        }
        if (complicationTemplates.present) {
            complicationTemplates.get().asFile.eachFileMatch(~/complication_(.+)\.xml/) { File file ->
                String type = (file.name =~ /complication_(.+)\.xml/)[0][1]
                layouts.put(type, file.getText('UTF-8'))
            }
        }
        return layouts
    }

    private static String bundledResource(String path) {
        InputStream stream = GenerateWatchFaceTask.getResourceAsStream("/com/xlythe/watchface/format/${path}")
        if (stream == null) {
            throw new IllegalStateException("Missing bundled resource ${path}")
        }
        return stream.withStream { it.getText('UTF-8') }
    }

    private static void writeUtf8(File file, String text) {
        file.parentFile.mkdirs()
        file.setText(text, 'UTF-8')
    }
}
