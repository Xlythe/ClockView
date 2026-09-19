package com.xlythe.watchface.format

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

import javax.inject.Inject

/**
 * Copies art and simple values out of other resource directories (typically a shared library
 * module that also contains code) so a resource-only watch face can use them without depending on
 * that module. Watch Face Format bundles can't contain code, so a project dependency won't do.
 */
@CacheableTask
abstract class SyncSharedResourcesTask extends DefaultTask {
    static final List<String> DEFAULT_INCLUDES = Collections.unmodifiableList([
            'drawable*/**',
            'mipmap*/**',
            'font*/**',
            'raw*/**',
            'values*/strings.xml',
            'values*/integers.xml',
            'values*/bools.xml',
            'values*/colors.xml',
            'values*/dimens.xml',
    ])

    /** Resource roots (directories containing drawable/, values/, ...). Earlier roots win on conflicts. */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract ConfigurableFileCollection getResourceDirectories()

    @Input
    abstract ListProperty<String> getIncludes()

    @OutputDirectory
    abstract DirectoryProperty getOutputDirectory()

    @Inject
    abstract FileSystemOperations getFileSystemOperations()

    @TaskAction
    void sync() {
        List<String> patterns = includes.get()
        fileSystemOperations.sync { spec ->
            spec.into(outputDirectory)
            spec.duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            for (File directory : resourceDirectories.files) {
                spec.from(directory) { child ->
                    child.include(patterns)
                    // The generated watch face owns this name.
                    child.exclude('raw*/watchface.xml')
                }
            }
        }
    }
}
