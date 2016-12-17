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


        WrapperTransformer transformer = new WrapperTransformer()
        project.android.registerTransform(transformer)

        project.afterEvaluate {
            def ignoreClass = project.extensions.kali.ignoreClass
            def replacements = project.extensions.kali.replacements
            transformer.configure(ignoreClass, replacements)
        }
    }
}