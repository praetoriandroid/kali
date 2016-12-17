package ru.mail.gradle.plugin.kali

import com.android.build.api.transform.Format
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInvocation
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor

import static com.android.build.api.transform.QualifiedContent.DefaultContentType.CLASSES

import java.util.zip.ZipFile

class WrapperTransformer extends Transform {

    String ignoreClass
    Map<CallDescription, Replacement> replacements

    void configure(String ignoreClass, Map<String, String> replacements) {
        this.ignoreClass = ignoreClass
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
        }
    }

    @Override
    void transform(final TransformInvocation invocation) {
        def outDir = invocation.outputProvider.getContentLocation(name, outputTypes, scopes, Format.DIRECTORY)

        outDir.deleteDir()
        outDir.mkdirs()

        invocation.inputs.each { transformInput ->
            transformInput.jarInputs.each { jarInput ->
                ZipFile zip = new ZipFile(jarInput.file)
                zip.entries().findAll { zipEntry ->
                    !zipEntry.directory
                }.each { zipEntry ->
                    def outputFile = new File(outDir, zipEntry.name)
                    outputFile.parentFile.mkdirs()
                    if (zipEntry.name.toLowerCase().endsWith('.class')) {
                        InputStream entryStream = zip.getInputStream(zipEntry);
                        processClass(entryStream, outputFile)
                        entryStream.close()
                    } else {
                        outputFile.bytes = zip.getInputStream(zipEntry).bytes
                    }
                }
            }

            transformInput.directoryInputs.each { directoryInput ->
                int baseDirLength = directoryInput.file.absolutePath.length()
                directoryInput.file.traverse { file ->
                    def path = "${file.absolutePath[baseDirLength..-1]}"
                    def outputFile = new File(outDir, path)

                    if (file.isDirectory()) {
                        outputFile.mkdirs()
                    } else {
                        InputStream fileStream = new FileInputStream(file)//file.newInputStream();
                        processClass(fileStream, outputFile)
                        fileStream.close()
                    }
                }
            }
        }
    }

    @SuppressWarnings("GrMethodMayBeStatic")
    private void processClass(InputStream classStream, File outputFile) {
        ClassReader classReader = new ClassReader(classStream)
        ClassVisitor transformer = new StaticWrapper(ignoreClass, replacements)

        classReader.accept(transformer, 0)
        outputFile.bytes = transformer.toByteArray()
    }

    @Override
    String getName() {
        return StaticWrapper.name
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return EnumSet.of(CLASSES)
    }

    @Override
    Set<QualifiedContent.Scope> getScopes() {
        return EnumSet.of(
                QualifiedContent.Scope.PROJECT,
                QualifiedContent.Scope.PROJECT_LOCAL_DEPS,
                QualifiedContent.Scope.SUB_PROJECTS,
                QualifiedContent.Scope.SUB_PROJECTS_LOCAL_DEPS,
                QualifiedContent.Scope.EXTERNAL_LIBRARIES
        )
    }

    @Override
    Set<QualifiedContent.Scope> getReferencedScopes() {
        return EnumSet.of(
                QualifiedContent.Scope.PROJECT_LOCAL_DEPS,
                QualifiedContent.Scope.SUB_PROJECTS,
                QualifiedContent.Scope.SUB_PROJECTS_LOCAL_DEPS,
                QualifiedContent.Scope.EXTERNAL_LIBRARIES,
                QualifiedContent.Scope.TESTED_CODE,
                QualifiedContent.Scope.PROVIDED_ONLY
        )
    }

    @Override
    boolean isIncremental() {
        return false
    }
}