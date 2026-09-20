package com.xlythe.watchface.format

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

class SyncSharedResourcesTaskTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder()

    private File shared
    private File face
    private File output

    @Before
    void setUp() {
        shared = folder.newFolder('shared-res')
        face = folder.newFolder('generated')
        output = folder.newFolder('out')

        // Art the face names outright.
        write('drawable/mountains_day.png', 'png')
        write('drawable-xhdpi/mountains_day.png', 'png')
        // A selector that names art of its own: reached only through the selector.
        write('drawable/animated_rain.xml', '''
            <animation-list>
                <item android:drawable="@drawable/rain_00001" />
                <item android:drawable="@drawable/rain_00002" />
            </animation-list>
        ''')
        write('drawable/rain_00001.png', 'png')
        write('drawable/rain_00002.png', 'png')
        // Art nothing names: a configuration screen's button state, and a paid-for scene.
        write('drawable/style_a_selected.png', 'png')
        write('drawable/beach_day.png', 'png')
        // Not addressed by file name, so not the pruner's business.
        write('values/strings.xml', '<resources><string name="app_name">Scenery</string></resources>')
        write('font/display.ttf', 'ttf')
    }

    private void write(String path, String contents) {
        File file = new File(shared, path)
        file.parentFile.mkdirs()
        file.text = contents
    }

    private List<String> sync(boolean prune, String watchFace) {
        new File(face, 'raw').mkdirs()
        new File(face, 'raw/watchface.xml').text = watchFace
        Project project = ProjectBuilder.builder().withProjectDir(folder.root).build()
        SyncSharedResourcesTask task = project.tasks.create('sync', SyncSharedResourcesTask)
        task.resourceDirectories.from(shared)
        task.referenceFiles.from(face)
        task.includes.set(SyncSharedResourcesTask.DEFAULT_INCLUDES)
        task.pruneUnreferenced.set(prune)
        task.outputDirectory.set(output)
        task.sync()
        List<String> copied = []
        output.eachFileRecurse(groovy.io.FileType.FILES) {
            copied << output.toPath().relativize(it.toPath()).toString().replace('\\', '/')
        }
        return copied.sort()
    }

    @Test
    void sync_keepsWhatTheFaceNames() {
        List<String> copied = sync(true, '<WatchFace><Image resource="mountains_day" /></WatchFace>')

        assertTrue(copied.toString(), copied.contains('drawable/mountains_day.png'))
        // Every density of it, not just the one: which density is used is the watch's decision.
        assertTrue(copied.toString(), copied.contains('drawable-xhdpi/mountains_day.png'))
    }

    @Test
    void sync_dropsWhatNothingNames() {
        List<String> copied = sync(true, '<WatchFace><Image resource="mountains_day" /></WatchFace>')

        assertFalse(copied.toString(), copied.contains('drawable/style_a_selected.png'))
        assertFalse(copied.toString(), copied.contains('drawable/beach_day.png'))
    }

    @Test
    void sync_followsOneResourceIntoAnother() {
        List<String> copied = sync(true, '<WatchFace><Image resource="animated_rain" /></WatchFace>')

        assertTrue(copied.toString(), copied.contains('drawable/animated_rain.xml'))
        assertTrue(copied.toString(), copied.contains('drawable/rain_00001.png'))
        assertTrue(copied.toString(), copied.contains('drawable/rain_00002.png'))
    }

    @Test
    void sync_dropsTheFramesWhenNothingNamesTheAnimation() {
        List<String> copied = sync(true, '<WatchFace><Image resource="mountains_day" /></WatchFace>')

        assertFalse(copied.toString(), copied.contains('drawable/animated_rain.xml'))
        assertFalse(copied.toString(), copied.contains('drawable/rain_00001.png'))
    }

    @Test
    void sync_keepsWhatIsNotAddressedByFileName() {
        List<String> copied = sync(true, '<WatchFace />')

        assertTrue(copied.toString(), copied.contains('values/strings.xml'))
        assertTrue(copied.toString(), copied.contains('font/display.ttf'))
    }

    @Test
    void sync_keepsEverythingWhenPruningIsOff() {
        List<String> copied = sync(false, '<WatchFace />')

        assertEquals(9, copied.size())
    }
}
