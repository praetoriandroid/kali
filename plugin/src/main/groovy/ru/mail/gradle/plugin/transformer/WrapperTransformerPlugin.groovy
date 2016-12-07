package ru.mail.gradle.plugin.transformer

import org.gradle.api.Plugin
import org.gradle.api.Project

public class WrapperTransformerPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        if (!project.pluginManager.findPlugin('com.android.application') &&
                !project.pluginManager.findPlugin('com.android.application')) {
            throw new IllegalStateException('Either com.android.application or com.android.library plugin is required')
        }
        project.android.registerTransform(new WrapperTransformer('com.example.gte.FakeLock', [
                'android.os.PowerManager$WakeLock.acquire(J)V': 'com.example.gte.FakeLock.acquire',
                'android.os.PowerManager$WakeLock.acquire()V': 'com.example.gte.FakeLock.acquire',
                'android.os.PowerManager$WakeLock.release()V': 'com.example.gte.FakeLock.release'
        ]))

        println('hello!')
        def task = project.tasks.create('foo') << {
            def adb = project.android.adbExe as String
            println "$adb devices".execute().text
        }
        task.group = 'wrapperplugin'
        task.description = 'An example task'
    }
}