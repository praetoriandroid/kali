package ru.mail.gradle.plugin.kali

public class FieldExposerTransform extends BaseTransform {

    FieldExposerTransform() {
        super('fieldExposer')
    }

    @Override
    void configure(Map params) {
        apply = params['makeAllFieldsPublic'] == true
    }

    @Override
    BaseClassProcessor createClassProcessor() {
        new FieldExposer()
    }
}
