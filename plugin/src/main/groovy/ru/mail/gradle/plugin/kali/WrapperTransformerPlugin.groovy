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

        BaseTransform staticWrapperTransform = new StaticWrapperTransform()
        project.android.registerTransform(staticWrapperTransform)

        BaseTransform fieldExposerTransform = new FieldExposerTransform()
        // FIXME Both transforms fail to apply
//        project.android.registerTransform(fieldExposerTransform)

        project.afterEvaluate {
            def ignoreClasses = project.kali.replaceCalls.ignoreClasses
            def replacements = project.kali.replaceCalls.replacements
            def replacementsRegex = project.kali.replaceCalls.replacementsRegex
            staticWrapperTransform.configure(ignoreClasses: ignoreClasses,
                    replacements: replacements, replacementsRegex: replacementsRegex)

            def makeAllFieldsPublic = project.kali.makeAllFieldsPublic
            fieldExposerTransform.configure(makeAllFieldsPublic: makeAllFieldsPublic)
        }
    }
}