package ru.mail.gradle.plugin.kali

import org.gradle.api.Plugin
import org.gradle.api.Project

class KaliPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        if (!project.pluginManager.findPlugin('com.android.application') &&
                !project.pluginManager.findPlugin('com.android.library')) {
            throw new IllegalStateException('Either com.android.application or com.android.library plugin is required')
        }

        project.extensions.create('kali', KaliPluginExtension)
        project.kali.extensions.create('replaceCalls', ReplaceCallsExtension)

        def transform = new KaliTransform()
        project.android.registerTransform(transform)

        project.afterEvaluate {
            transform.configure(project.kali.replaceCalls, project.kali.inlineSyntheticFieldAccessors)

            //TODO it is debug only
            project.tasks.findAll {
                it.name.startsWith('transformClassesWithKaliFor')
            }.each {
                it.outputs.upToDateWhen {false}
            }
        }
    }
}