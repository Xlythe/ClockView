package com.xlythe.watchface.format

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations

import javax.inject.Inject

/**
 * Runs Google's Watch Face Format validator (wff-validator.jar from
 * https://github.com/google/watchface/releases) against each generated variant, using the format
 * version that variant targets.
 */
@CacheableTask
abstract class ValidateWatchFaceFormatTask extends DefaultTask {
    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.NONE)
    abstract RegularFileProperty getValidator()

    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract DirectoryProperty getGeneratedResources()

    /** Resource qualifier (e.g. raw-v34) to the Watch Face Format version it targets. */
    @Input
    abstract MapProperty<String, Integer> getFormatVersions()

    /** Also validates every document against the lowest version, as Google's check does for one bundle. */
    @Input
    abstract Property<Boolean> getValidateAgainstLowestVersion()

    @OutputFile
    abstract RegularFileProperty getReport()

    @Inject
    abstract ExecOperations getExecOperations()

    @TaskAction
    void validate() {
        String java = new File(System.getProperty('java.home'), 'bin/java').absolutePath
        StringBuilder report = new StringBuilder()
        List<String> failures = []

        // Google's memory footprint tool, which Play also runs, validates every watchface.xml against
        // the manifest's format version, i.e. the lowest one. So each variant must pass its own version
        // and the lowest; only data sources (like weather) can differ between them.
        int lowestVersion = formatVersions.get().values().min() ?: 1
        formatVersions.get().each { String qualifier, Integer version ->
            File watchFace = new File(generatedResources.get().asFile, "${qualifier}/watchface.xml")
            List<Integer> schemaVersions = validateAgainstLowestVersion.getOrElse(true) ? [version, lowestVersion] : [version]
            for (int schemaVersion : (schemaVersions as LinkedHashSet<Integer>)) {
                ByteArrayOutputStream output = new ByteArrayOutputStream()
                def result = execOperations.exec { spec ->
                    // Expanded watch faces nest deeply, which overflows the default thread stack.
                    spec.commandLine(java, '-Xss16m', '-jar', validator.get().asFile.absolutePath, schemaVersion.toString(), watchFace.absolutePath)
                    spec.standardOutput = output
                    spec.errorOutput = output
                    spec.ignoreExitValue = true
                }
                String text = output.toString('UTF-8')
                report.append("== ${qualifier} against WFF ${schemaVersion} ==\n").append(text).append('\n')
                if (result.exitValue != 0 || text.contains('FAILED') || !text.contains('PASSED')) {
                    String problem = text.readLines().find { it.contains('SEVERE') } ?: text.readLines().findAll { it.trim() }.takeRight(1).join('')
                    String context = schemaVersion == version ? '' : " (Google Play's memory footprint check validates every variant against the lowest format version)"
                    failures.add("res/${qualifier}/watchface.xml is not valid WFF ${schemaVersion}${context}: ${problem.trim()}")
                }
            }
        }

        File reportFile = report.get().asFile
        reportFile.parentFile.mkdirs()
        reportFile.setText(report.toString(), 'UTF-8')
        if (!failures.isEmpty()) {
            throw new GradleException(failures.join('\n') + "\nFull output: ${reportFile}")
        }
    }
}
