package com.xlythe.watchface.format

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider

/**
 * Generates Watch Face Format resources from templates.
 *
 * <pre>
 * apply plugin: 'com.android.application'
 * apply plugin: 'com.xlythe.watchface-format'
 * </pre>
 *
 * The generated {@code res/raw*\/watchface.xml} and {@code @integer/watchface_format_version} are
 * added to every Android variant automatically. See {@link WatchFaceFormatExtension}.
 */
class WatchFaceFormatPlugin implements Plugin<Project> {
    static final String EXTENSION_NAME = 'watchFaceFormat'
    static final String TASK_NAME = 'generateWatchFaceFormatResources'
    static final String SHARED_RESOURCES_TASK_NAME = 'syncWatchFaceSharedResources'
    static final String VALIDATE_TASK_NAME = 'validateWatchFaceFormatResources'
    static final String FLAVOR_DIMENSION = 'watchFaceFormat'

    @Override
    void apply(Project project) {
        WatchFaceFormatExtension extension = project.extensions.create(EXTENSION_NAME, WatchFaceFormatExtension)
        extension.template.convention(project.layout.projectDirectory.file('src/main/template/raw/watchface.xml'))
        extension.standardVariables.convention(false)
        extension.timeZoneCoordinates.convention(false)
        extension.complicationColor.convention('#FFFFFFFF')
        extension.complicationAmbientColor.convention('#FFFFFFFF')
        extension.generateFormatVersionResource.convention(true)
        extension.sharedResourceIncludes.convention(SyncSharedResourcesTask.DEFAULT_INCLUDES)
        extension.bundlePerFormatVersion.convention(false)
        extension.versionCodeMultiplier.convention(10)

        // The defaults can be adjusted (variants { wff1 { replace(...) } }), added to, or removed.
        extension.variants.create('wff1') { WatchFaceVariant variant ->
            variant.resourceQualifier.set('raw')
            variant.formatVersion.set(1)
            variant.stubWeatherDataSources()
        }
        extension.variants.create('wff2') { WatchFaceVariant variant ->
            variant.resourceQualifier.set('raw-v34')
            variant.formatVersion.set(2)
        }

        TaskProvider<SyncSharedResourcesTask> syncShared = project.tasks.register(SHARED_RESOURCES_TASK_NAME, SyncSharedResourcesTask) { SyncSharedResourcesTask task ->
            task.group = 'build'
            task.description = 'Copies shared art and values into the watch face without depending on code.'
            task.resourceDirectories.from(extension.sharedResources)
            task.includes.set(extension.sharedResourceIncludes)
            task.outputDirectory.set(project.layout.buildDirectory.dir('generated/watchface-format/shared-res'))
        }

        // One bundle: every variant goes into its own version-qualified resource directory.
        TaskProvider<GenerateWatchFaceTask> generate = project.tasks.register(TASK_NAME, GenerateWatchFaceTask) { GenerateWatchFaceTask task ->
            task.description = 'Expands Watch Face Format templates into generated resources.'
            configureGenerate(task, extension, project.provider { new ArrayList<>(extension.variants) })
            task.writeToDefaultQualifiers.set(false)
            task.outputDirectory.set(project.layout.buildDirectory.dir('generated/watchface-format/res'))
        }
        TaskProvider<ValidateWatchFaceFormatTask> validate = project.tasks.register(VALIDATE_TASK_NAME, ValidateWatchFaceFormatTask) { ValidateWatchFaceFormatTask task ->
            task.description = "Checks the generated watch faces with Google's Watch Face Format validator."
            configureValidate(task, extension, generate, project.provider {
                extension.variants.collectEntries { [(it.resourceQualifier.get()): it.formatVersion.get()] }
            })
            task.validateAgainstLowestVersion.set(true)
            task.report.set(project.layout.buildDirectory.file('reports/watchface-format/validation.txt'))
        }

        // A bundle per format version: each variant is written to plain res/raw for its own flavor.
        Map<String, TaskProvider<ValidateWatchFaceFormatTask>> variantValidations = [:]
        extension.variants.all { WatchFaceVariant variant ->
            String name = variant.name.capitalize()
            TaskProvider<GenerateWatchFaceTask> variantGenerate = project.tasks.register("generate${name}WatchFaceFormatResources", GenerateWatchFaceTask) { GenerateWatchFaceTask task ->
                task.description = "Expands the ${variant.name} Watch Face Format template for its own bundle."
                configureGenerate(task, extension, project.provider { [variant] })
                task.writeToDefaultQualifiers.set(true)
                task.outputDirectory.set(project.layout.buildDirectory.dir("generated/watchface-format/${variant.name}/res"))
            }
            variantValidations[variant.name] = project.tasks.register("validate${name}WatchFaceFormatResources", ValidateWatchFaceFormatTask) { ValidateWatchFaceFormatTask task ->
                task.description = "Checks the ${variant.name} watch face with Google's Watch Face Format validator."
                configureValidate(task, extension, variantGenerate, project.provider { [raw: variant.formatVersion.get()] })
                task.validateAgainstLowestVersion.set(false)
                task.report.set(project.layout.buildDirectory.file("reports/watchface-format/${variant.name}-validation.txt"))
            }
        }

        // Validate on every build once a validator is configured, for whichever layout is in use.
        Provider<List<Object>> activeValidations = project.provider {
            if (!extension.validator.present) {
                return []
            }
            return extension.bundlePerFormatVersion.get() ? new ArrayList<Object>(variantValidations.values()) : [validate]
        }
        project.tasks.matching { it.name == 'check' || it.name == 'preBuild' }.configureEach { it.dependsOn(activeValidations) }

        project.plugins.withId('com.android.application') {
            def androidComponents = project.extensions.getByName('androidComponents')
            androidComponents.finalizeDsl { android ->
                if (extension.bundlePerFormatVersion.get()) {
                    addFormatVersionFlavors(android, extension)
                }
            }
            androidComponents.onVariants(androidComponents.selector().all()) { variant ->
                if (extension.bundlePerFormatVersion.get()) {
                    String flavor = variant.productFlavors.find { it.first == FLAVOR_DIMENSION }?.second
                    TaskProvider<GenerateWatchFaceTask> variantGenerate = project.tasks.named("generate${flavor.capitalize()}WatchFaceFormatResources", GenerateWatchFaceTask)
                    variant.sources.res.addGeneratedSourceDirectory(variantGenerate) { GenerateWatchFaceTask task ->
                        task.outputDirectory
                    }
                } else {
                    variant.sources.res.addGeneratedSourceDirectory(generate) { GenerateWatchFaceTask task ->
                        task.outputDirectory
                    }
                }
                variant.sources.res.addGeneratedSourceDirectory(syncShared) { SyncSharedResourcesTask task ->
                    task.outputDirectory
                }
                if (variant.buildType != 'debug') {
                    registerBundleValidation(project, variant)
                }
            }
        }
    }

    private static void configureGenerate(GenerateWatchFaceTask task, WatchFaceFormatExtension extension, Provider<List<WatchFaceVariant>> variants) {
        task.group = 'build'
        task.template.set(extension.template)
        task.variableFiles.from(extension.variables)
        task.standardVariables.set(extension.standardVariables)
        task.timeZoneCoordinates.set(extension.timeZoneCoordinates)
        task.timeZoneTable.set(extension.timeZoneTable)
        task.complicationTemplates.set(extension.complicationTemplates)
        task.complicationColor.set(extension.complicationColor)
        task.complicationAmbientColor.set(extension.complicationAmbientColor)
        task.generateFormatVersionResource.set(extension.generateFormatVersionResource)
        task.sharedVariables.set(extension.sharedVariables)
        task.timeZoneCount.set(extension.timeZoneCount)
        task.defaultLatitude.set(extension.defaultLatitude)
        task.defaultLongitude.set(extension.defaultLongitude)
        task.variants.set(variants)
    }

    private static void configureValidate(ValidateWatchFaceFormatTask task, WatchFaceFormatExtension extension,
                                          TaskProvider<GenerateWatchFaceTask> generate, Provider<Map<String, Integer>> formatVersions) {
        task.group = 'verification'
        task.validator.set(extension.validator)
        task.generatedResources.set(generate.flatMap { it.outputDirectory })
        task.formatVersions.set(formatVersions)
        task.onlyIf('watchFaceFormat.validator is set') { extension.validator.present }
    }

    // One flavor per variant, e.g. wff1Release and wff2Release. A higher format version gets a higher
    // version code, so a watch that can run both gets the newer one.
    private static void addFormatVersionFlavors(def android, WatchFaceFormatExtension extension) {
        Integer baseVersionCode = android.defaultConfig.versionCode
        android.flavorDimensions.add(FLAVOR_DIMENSION)
        extension.variants.each { WatchFaceVariant variant ->
            android.productFlavors.create(variant.name) { flavor ->
                flavor.dimension = FLAVOR_DIMENSION
                flavor.minSdk = variant.minSdk.get()
                if (baseVersionCode != null) {
                    flavor.versionCode = baseVersionCode * extension.versionCodeMultiplier.get() + variant.formatVersion.get()
                }
            }
        }
    }

    // Debug builds keep the R class in dex, which is fine for sideloading. Anything else is headed for
    // a store, so check that the bundle is resource-only whenever it's built.
    private static void registerBundleValidation(Project project, def variant) {
        String name = variant.name.capitalize()
        def bundleArtifact = variant.class.classLoader
                .loadClass('com.android.build.api.artifact.SingleArtifact$BUNDLE')
                .getField('INSTANCE')
                .get(null)
        def validate = project.tasks.register("validate${name}WatchFaceBundle", ValidateWatchFaceBundleTask) { ValidateWatchFaceBundleTask task ->
            task.group = 'verification'
            task.description = "Checks that the ${variant.name} bundle contains no code."
            task.bundle.set(variant.artifacts.get(bundleArtifact))
        }
        project.tasks.matching { it.name == "bundle${name}" }.configureEach { it.finalizedBy(validate) }
    }
}
