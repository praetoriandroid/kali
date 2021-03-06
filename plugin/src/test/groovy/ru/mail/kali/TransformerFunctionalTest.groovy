package ru.mail.kali

import android.os.PowerManager
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification

import java.lang.reflect.Field
import java.lang.reflect.Modifier

import static groovy.io.FileType.DIRECTORIES
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class TransformerFunctionalTest extends Specification {

    def 'inliner makes specific fields public'() {
        given:
        def testDir = 'src/inlineTest'

        when:
        def result = build(testDir)
        def classLoader = getBuildClassLoader(testDir)
        def inheritedClass = classLoader.loadClass('com.example.InheritedClass')
        def baseClass = classLoader.loadClass('com.example.another_package.BaseClass')

        then:
        isSuccess(result)
        Modifier.isPublic(inheritedClass.getDeclaredField('outerClassField').modifiers)
        Modifier.isPrivate(inheritedClass.getDeclaredField('untouched').modifiers)
        Modifier.isPublic(baseClass.getDeclaredField('baseClassField').modifiers)
    }

    def 'method calls replaced'() {
        given:
        def testDir = 'src/replaceTest'

        when:
        def result = build(testDir)
        ClassLoader classLoader = getBuildClassLoader(testDir)

        def mockWakeLockClass = classLoader.loadClass('com.example.MockWakeLock')
        def callLog = []
        mockWakeLockClass.setCallLog(callLog)
        def wakeLockUserClass = classLoader.loadClass('com.example.WakeLockUser')
        def wakeLockUser = wakeLockUserClass.newInstance()
        wakeLockUser.useWakeLock(new PowerManager.WakeLock())

        then:
        isSuccess(result)
        callLog == ['acquire(100)', 'release()']
    }

    def 'field access modified'() {
        given:
        def testDir = 'src/fieldAccessTest'

        when:
        def result = build(testDir)
        ClassLoader classLoader = getBuildClassLoader(testDir)
        def outerClass = classLoader.loadClass('com.example.OuterClass')
        def innerClass = classLoader.loadClass('com.example.OuterClass$InnerClass')

        then:
        isSuccess(result)
        (outerClass.declaredFields + innerClass.declaredFields).each { field ->
            nonStaticBecomeStatic(field) || staticBecomeNonStatic(field)
            Modifier.isPublic(field.modifiers)
        }
    }

    def 'class with modified fields could be instantiated'() {
        given:
        def testDir = 'src/fieldAccessTest'

        when:
        def result = build(testDir)
        ClassLoader classLoader = getBuildClassLoader(testDir)
        def outerClass = classLoader.loadClass('com.example.OuterClass')
        def innerClass = classLoader.loadClass('com.example.OuterClass$InnerClass')

        then:
        isSuccess(result)
        outerClass.newInstance()
        innerClass.newInstance()
    }

    def 'modified fields are properly initialized'() {
        given:
        def testDir = 'src/fieldAccessTest'

        when:
        def result = build(testDir)
        ClassLoader classLoader = getBuildClassLoader(testDir)
        def outerClass = classLoader.loadClass('com.example.OuterClass')
        def innerClass = classLoader.loadClass('com.example.OuterClass$InnerClass')
        def outerInstance = outerClass.newInstance()
        def innerInstance1 = innerClass.newInstance()
        def innerInstance2 = innerClass.newInstance(0)

        then:
        isSuccess(result)
        outerClass.field == 'outer non-static'
        innerClass.field == 'inner non-static'
        outerClass.timestamp != 0
        outerInstance.staticField == 'outer static'
        innerInstance1.staticField == 'inner static'
        innerInstance2.staticField == 'inner static'
    }

    def 'missing static initializer is generated'() {
        given:
        def testDir = 'src/fieldAccessTest'

        when:
        def result = build(testDir)
        ClassLoader classLoader = getBuildClassLoader(testDir)
        def classWithoutStaticInitializer = classLoader.loadClass('com.example.ClassWithoutStaticInitializer')

        then:
        isSuccess(result)
        classWithoutStaticInitializer.timestamp != 0
    }

    private static build(String testDir) {
        GradleRunner.create()
                .withProjectDir(new File(testDir))
                .withArguments('transformClassesWithKaliForDebug')
                .withPluginClasspath()
                .build()
    }

    private static isSuccess(BuildResult result) {
        result.task(':transformClassesWithKaliForDebug').outcome == SUCCESS
    }

    private static getBuildClassLoader(String testDir) {
        new TestClassLoader(getTransformerOutputDir(testDir))
    }

    def static getTransformerOutputDir(String testRootDir) {
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

    private static boolean nonStaticBecomeStatic(Field field) {
        !field.name.startsWith('static') && Modifier.isStatic(field.modifiers)
    }

    private static boolean staticBecomeNonStatic(Field field) {
        field.name.startsWith('static') && !Modifier.isStatic(field.modifiers)
    }

}
