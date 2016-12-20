package ru.mail.gradle.plugin.kali

public class StaticWrapperTransform extends BaseTransform {

    Set<String> ignoreClasses
    Map<CallDescription, Replacement> replacements

    StaticWrapperTransform() {
        super('staticWrapper')
    }

    @Override
    void configure(Map params) {
        def ignoreClasses = params['ignoreClasses']
        def replacements = params['replacements']
        if (ignoreClasses == null || replacements == null) {
            return
        }

        this.ignoreClasses = ignoreClasses.collect {
            it.replace('.', '/')
        }
        this.replacements = [:]
        replacements.each { key, value ->
            int descIndex = key.indexOf('(')
            if (descIndex == -1) {
                throw new IllegalArgumentException("Bad replaceable method specification: $key")
            }
            def replaceableDesc = key[descIndex..-1]
            def method = key[0..descIndex - 1]
            int methodNameIndex = method.lastIndexOf('.')
            if (methodNameIndex == -1) {
                throw new IllegalArgumentException("Bad replaceable method specification: $key")
            }
            def replaceableClass = method[0..methodNameIndex - 1].replace('.', '/')
            def replaceableMethod = method[methodNameIndex + 1..-1]
            def replaceable = new CallDescription(owner: replaceableClass, methodName: replaceableMethod, desc: replaceableDesc)

            methodNameIndex = value.lastIndexOf('.')
            if (methodNameIndex == -1) {
                throw new IllegalArgumentException("Bad replacement method specification: $value")
            }
            def replacementClass = value[0..methodNameIndex - 1].replace('.', '/')
            def replacementMethod = value[methodNameIndex + 1..-1]
            def replacement = new Replacement(owner: replacementClass, methodName: replacementMethod)

            this.replacements[replaceable] = replacement

            apply = true
        }
    }

    @Override
    BaseClassProcessor createClassProcessor() {
        new StaticWrapper(ignoreClasses, replacements)
    }
}
