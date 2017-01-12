package ru.mail.kali

import android.os.PowerManager
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification

import java.lang.reflect.Modifier

import static groovy.io.FileType.DIRECTORIES
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

public class TransformerFunctionalTest extends Specification {

    def 'inliner makes specific fields public'() {
        given:
        def testDir = new File('src/inlineTest')

        when:
        def result = GradleRunner.create()
                .withProjectDir(testDir)
                .withArguments('transformClassesWithKaliForDebug')
                .withPluginClasspath()
                .build()

        then:
        result.task(':transformClassesWithKaliForDebug').outcome == SUCCESS

        ClassLoader classLoader = new TestClassLoader(getTransformerOutputDir(testDir.absolutePath));

        def inheritedClass = classLoader.loadClass('com.example.InheritedClass')
        assert Modifier.isPublic(inheritedClass.getDeclaredField('outerClassField').modifiers)
        assert Modifier.isPrivate(inheritedClass.getDeclaredField('untouched').modifiers)

        def baseClass = classLoader.loadClass('com.example.another_package.BaseClass')
        assert Modifier.isPublic(baseClass.getDeclaredField('baseClassField').modifiers)
    }

    def 'method calls replaced'() {
        given:
        def testDir = new File('src/replaceTest')

        when:
        def result = GradleRunner.create()
                .withProjectDir(testDir)
                .withArguments('transformClassesWithKaliForDebug')
                .withPluginClasspath()
                .build()

        then:
        result.task(':transformClassesWithKaliForDebug').outcome == SUCCESS

        ClassLoader classLoader = new TestClassLoader(getTransformerOutputDir(testDir.absolutePath));

        def mockWakeLockClass = classLoader.loadClass('com.example.MockWakeLock')
        def callLog = []
        mockWakeLockClass.setCallLog(callLog)
        def wakeLockUserClass = classLoader.loadClass('com.example.WakeLockUser')
        def wakeLockUser = wakeLockUserClass.newInstance()
        wakeLockUser.useWakeLock(new PowerManager.WakeLock())
        callLog == ['acquire(100)', 'release()']
    }

    def getTransformerOutputDir(String testRootDir) {
        def dir = new File("$testRootDir/build/intermediates/transforms/kali/debug/folders")
        def results = []
        dir.traverse (
                type: DIRECTORIES,
                maxDepth: 2,
                filter: { it =~ /$dir\/[^\/]+\/[^\/]+\/kali/}
        ) { results << it }

        if (results.isEmpty()) {
            throw new FileNotFoundException("$testRootDir/*/*/kali")
        }
        if (results.size() > 1) {
            throw new FileNotFoundException("Ambiguous test output. Canditates: $results.")
        }

        return results.first()
    }

}
