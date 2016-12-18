package ru.mail.gradle.plugin.kali

public class StaticWrapperTransform extends BaseTransform {

    Set<String> ignoreClasses
    List<Replacement> replacements
    List<Replacement> replacementsRegex

    StaticWrapperTransform() {
        super('staticWrapper')
    }

    @Override
    void configure(Map params) {
        def ignoreClasses = params['ignoreClasses']
        def replacements = params['replacements']
        def replacementsRegex = params['replacementsRegex']
        if (!ignoreClasses || (!replacements && !replacementsRegex)) {
            return
        }

        this.ignoreClasses = ignoreClasses.collect {
            it.replace('.', '/')
        }

        this.replacements = []
        replacements.each { key, value ->
            def replaceable = parseDescriptor(key)
            def replacement = parseDescriptor(value)
            this.replacements << new Replacement(from: replaceable, to: replacement)
            apply = true
        }

        this.replacementsRegex = []
        replacementsRegex.each { key, value ->
            def replaceable = parseDescriptorRegex(key)
            def replacement = parseDescriptor(value, false)
            this.replacementsRegex << new Replacement(from: replaceable, to:  replacement)
            apply = true
        }
    }

    static InvokeDescriptor parseDescriptor(String fullInvokeDescriptor, boolean mandatoryDescriptor = false) {
        def descIndex = fullInvokeDescriptor.indexOf('(')
        def methodNameLimit = descIndex == -1 ? fullInvokeDescriptor.length() - 1 : descIndex
        def methodNameIndex = fullInvokeDescriptor.lastIndexOf('.', methodNameLimit) + 1
        if (methodNameIndex < 2) {
            throw new IllegalArgumentException("Bad replacement method specification: $fullInvokeDescriptor")
        }
        def ownerClass = fullInvokeDescriptor[0..methodNameIndex - 2].replace('.', '/')
        def methodNameEnd = descIndex >= 0 ? descIndex - 1 : descIndex
        def methodName = fullInvokeDescriptor[methodNameIndex..methodNameEnd]
        if (descIndex != -1 && fullInvokeDescriptor.indexOf(')', descIndex) == -1) {
            throw new IllegalArgumentException("Replacement with broken arguments descriptor: $fullInvokeDescriptor")
        }
        def descriptor = descIndex == -1 ? null : fullInvokeDescriptor[descIndex..-1]
        if (mandatoryDescriptor && !descriptor) {
            throw new IllegalArgumentException("Missing mandatory argument descriptor: $fullInvokeDescriptor")
        }
        def result = new InvokeDescriptor(owner: ownerClass, methodName: methodName, descriptor: descriptor)
        return result
    }

    static InvokeDescriptor parseDescriptorRegex(String fullInvokeDescriptor) {
        def parts = fullInvokeDescriptor.split(' ')
        if (parts.length != 3) {
            throw new IllegalArgumentException("Bad invoke regex descriptor: $fullInvokeDescriptor")
        }
        def ownerClass = parts[0].replaceAll('(?<!\\\\)\\\\\\.', '/')
        def methodName = parts[1]
        def descriptor = parts[2]
        def result = new InvokeDescriptor(owner: ownerClass, methodName: methodName, descriptor: descriptor)
        return result
    }

    @Override
    BaseClassProcessor createClassProcessor(PreparedInfo info) {
        new StaticWrapper(ignoreClasses, replacements, replacementsRegex, info)
    }
}
