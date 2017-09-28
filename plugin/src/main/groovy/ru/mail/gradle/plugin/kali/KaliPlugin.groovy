package ru.mail.gradle.plugin.kali

import org.gradle.api.Plugin
import org.gradle.api.Project

class KaliPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        def applicationPluginApplied = project.pluginManager.findPlugin('com.android.application') as boolean
        def libraryPluginApplied = project.pluginManager.findPlugin('com.android.library') as boolean
        if (!applicationPluginApplied && !libraryPluginApplied) {
            throw new IllegalStateException('Either com.android.application or com.android.library plugin is required')
        }

        project.extensions.create('kali', KaliPluginExtension)
        project.kali.extensions.create('replaceCalls', ReplaceCallsExtension)

        def transform = new KaliTransform(applicationPluginApplied)
        project.android.registerTransform(transform)

        project.afterEvaluate {
            transform.configure(project.kali.replaceCalls, project.kali)

            //TODO it is debug only
            project.tasks.findAll {
                it.name.startsWith('transformClassesWithKaliFor')
            }.each {
                it.outputs.upToDateWhen {false}
            }
        }
    }
}