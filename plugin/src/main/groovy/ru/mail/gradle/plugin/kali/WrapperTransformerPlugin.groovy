package ru.mail.gradle.plugin.kali

import org.gradle.api.Plugin
import org.gradle.api.Project

class WrapperTransformerPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        if (!project.pluginManager.findPlugin('com.android.application') &&
                !project.pluginManager.findPlugin('com.android.application')) {
            throw new IllegalStateException('Either com.android.application or com.android.library plugin is required')
        }

        project.extensions.create('kali', WrapperTransformerPluginExtension)
        project.kali.extensions.create('replaceCalls', StaticWrapperExtension)

        def staticWrapperTransform = new StaticWrapperTransform()
        project.android.registerTransform(staticWrapperTransform)

        project.afterEvaluate {
            def ignoreClasses = project.kali.replaceCalls.ignoreClasses
            def replacements = project.kali.replaceCalls.replacements
            def replacementsRegex = project.kali.replaceCalls.replacementsRegex
            staticWrapperTransform.configure(ignoreClasses: ignoreClasses,
                    replacements: replacements, replacementsRegex: replacementsRegex)

            project.tasks.findAll {
                it.name.startsWith('transformClassesWithStaticWrapperFor')
            }.each {
                //TODO it is debug only
                it.outputs.upToDateWhen {false}
            }
        }
    }
}