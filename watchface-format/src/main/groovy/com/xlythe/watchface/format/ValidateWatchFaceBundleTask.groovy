package com.xlythe.watchface.format

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * Checks that a watch face bundle is resource-only. Bundletool already refuses dex in bundles that
 * declare the Watch Face Format with minSdk 33+; this also covers bundles it doesn't recognize and
 * warns about Kotlin metadata, which Watch Face Push validation rejects.
 */
abstract class ValidateWatchFaceBundleTask extends DefaultTask {
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    abstract RegularFileProperty getBundle()

    @TaskAction
    void validate() {
        File file = bundle.get().asFile
        List<String> code = []
        List<String> metadata = []
        new ZipFile(file).withCloseable { ZipFile zip ->
            for (ZipEntry entry : Collections.list(zip.entries())) {
                if (entry.name.endsWith('.dex')) {
                    code.add(entry.name)
                } else if (entry.name.endsWith('.kotlin_builtins') || entry.name.endsWith('.kotlin_module')) {
                    metadata.add(entry.name)
                }
            }
        }

        if (!code.isEmpty()) {
            throw new GradleException("${file.name} contains code (${code.take(3).join(', ')}), but Watch Face Format " +
                    'bundles must be resource-only. Remove dependencies on modules with code (use ' +
                    'watchFaceFormat.sharedResources for their art instead) and set minifyEnabled true for this ' +
                    'build type so R8 strips the generated R class.')
        }
        if (!metadata.isEmpty()) {
            logger.warn("{} contains Kotlin metadata ({}). Google Play may accept it, but Watch Face Push validation " +
                    "won't. Set android.builtInKotlin=false in gradle.properties.", file.name, metadata.take(3).join(', '))
        }
    }
}
