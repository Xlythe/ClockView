package com.xlythe.watchface.format

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

import javax.inject.Inject
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Copies art and simple values out of other resource directories (typically a shared library
 * module that also contains code) so a resource-only watch face can use them without depending on
 * that module. Watch Face Format bundles can't contain code, so a project dependency won't do.
 *
 * <p>A shared library's resources are shared with an app that has screens, and a watch face has
 * none: the button states, the selector drawables and the animation frames behind an
 * {@code animation-list} all come along with the art the face wants. So does anything the app
 * charges for but the face doesn't draw. None of it is reachable from a watch face, and Watch Face
 * Format bundles can't be shrunk the usual way - the shrinker reads code, and there is none, while
 * every reference the face makes is a name in an XML attribute it doesn't understand. So the names
 * are followed here instead, and what nothing names is left behind. See {@link #unreferencedPaths}.
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

    /**
     * Resource directories whose files are addressed by file name, and so can be matched against
     * the names the watch face uses. The rest - {@code values}, and fonts, which a watch face
     * asks for by family rather than by file - are copied whole.
     */
    private static final List<String> NAMED_BY_FILE = ['drawable', 'mipmap', 'raw', 'anim', 'color']

    /** {@code @drawable/foo}, as one resource file refers to another. */
    private static final Pattern QUALIFIED = ~/@(?:drawable|mipmap|raw|anim|color|font)\/([A-Za-z0-9_]+)/

    /** {@code resource="foo"}, as a watch face refers to art: a bare name, with the type implied. */
    private static final Pattern BARE = ~/(?:resource|thumbnail|icon)\s*=\s*"([A-Za-z0-9_]+)"/

    /** Resource roots (directories containing drawable/, values/, ...). Earlier roots win on conflicts. */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract ConfigurableFileCollection getResourceDirectories()

    /**
     * Everything that might name a shared resource: the generated watch faces, and the module's
     * own manifests and resources. Whatever none of these reach is not copied.
     */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract ConfigurableFileCollection getReferenceFiles()

    @Input
    abstract ListProperty<String> getIncludes()

    /** Off copies the shared directories whole, for a project that reaches art some other way. */
    @Input
    abstract Property<Boolean> getPruneUnreferenced()

    @OutputDirectory
    abstract DirectoryProperty getOutputDirectory()

    @Inject
    abstract FileSystemOperations getFileSystemOperations()

    @Inject
    abstract ObjectFactory getObjects()

    @TaskAction
    void sync() {
        List<String> patterns = includes.get()
        // Worked out up front, and as paths rather than names, so that the exclude below is a set
        // lookup and nothing else: a closure on a Gradle task cannot see this class's own methods.
        Set<String> unwanted = pruneUnreferenced.get() ? unreferencedPaths(patterns) : Collections.emptySet()
        fileSystemOperations.sync { spec ->
            spec.into(outputDirectory)
            spec.duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            for (File directory : resourceDirectories.files) {
                spec.from(directory) { child ->
                    child.include(patterns)
                    // The generated watch face owns this name.
                    child.exclude('raw*/watchface.xml')
                    child.exclude { element -> unwanted.contains(element.relativePath.pathString) }
                }
            }
        }
    }

    /**
     * The relative paths of the shared resources the watch face can't reach.
     *
     * <p>The names it does reach start with the files that name resources outright, and grow by
     * following each name found into whatever that resource names in turn - a selector naming its
     * states, an {@code animation-list} naming twenty-four frames. Only XML can refer onwards, so
     * the walk ends at the art.
     */
    private Set<String> unreferencedPaths(List<String> patterns) {
        Map<String, List<File>> byName = [:].withDefault { [] }
        Map<String, String> pathByFile = [:]
        for (File directory : resourceDirectories.files) {
            for (File file : objects.fileTree().from(directory).matching { it.include(patterns) }.files) {
                String path = directory.toPath().relativize(file.toPath()).toString().replace('\\', '/')
                String name = addressableName(path)
                if (name != null) {
                    byName[name] << file
                    pathByFile[file.absolutePath] = path
                }
            }
        }

        Set<String> wanted = [] as Set
        Deque<String> pending = new ArrayDeque<>()
        // A generated resource directory arrives as the directory, not as what is in it, so the
        // trees are walked rather than read.
        for (File root : referenceFiles.files) {
            if (root.directory) {
                for (File file : objects.fileTree().from(root).files) {
                    push(names(file), wanted, pending)
                }
            } else if (root.file) {
                push(names(root), wanted, pending)
            }
        }
        while (!pending.isEmpty()) {
            for (File file : byName[pending.pop()]) {
                if (file.name.endsWith('.xml')) {
                    push(names(file), wanted, pending)
                }
            }
        }

        Set<String> unwanted = [] as Set
        for (Map.Entry<String, List<File>> entry : byName.entrySet()) {
            if (!wanted.contains(entry.key)) {
                for (File file : entry.value) {
                    unwanted.add(pathByFile[file.absolutePath])
                }
            }
        }
        if (!unwanted.isEmpty()) {
            logger.info("Leaving behind ${unwanted.size()} shared resources the watch face never names.")
        }
        return unwanted
    }

    private static void push(Set<String> found, Set<String> wanted, Deque<String> pending) {
        for (String name : found) {
            if (wanted.add(name)) {
                pending.push(name)
            }
        }
    }

    private static Set<String> names(File file) {
        String text
        try {
            text = file.getText('UTF-8')
        } catch (Exception ignored) {
            // Art, not a reference to it. Nothing here can name anything.
            return Collections.emptySet()
        }
        Set<String> found = [] as Set
        for (Pattern pattern : [QUALIFIED, BARE]) {
            Matcher matcher = pattern.matcher(text)
            while (matcher.find()) {
                found.add(matcher.group(1))
            }
        }
        return found
    }

    /**
     * The resource name a file is addressed by, or null if it isn't addressed by file name at all
     * and so has to be copied whichever way the decision goes. The qualifiers after the first dash
     * name a configuration rather than a resource, and the extension is never part of the name
     * (nor is the second half of {@code foo.9.png}), so both are dropped.
     */
    private static String addressableName(String path) {
        List<String> segments = path.split('/') as List<String>
        if (segments.size() != 2) {
            return null
        }
        String type = segments[0].split('-', 2)[0]
        if (!NAMED_BY_FILE.contains(type)) {
            return null
        }
        return segments[1].split('\\.', 2)[0]
    }
}
