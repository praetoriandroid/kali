package ru.mail.gradle.plugin.kali

import com.android.build.api.transform.Format
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInvocation
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter

import static com.android.build.api.transform.QualifiedContent.DefaultContentType.CLASSES

import java.util.zip.ZipFile

import static org.objectweb.asm.ClassWriter.COMPUTE_MAXS

abstract class BaseTransform extends Transform {

    final String name
    boolean apply

    BaseTransform(String name) {
        this.name = name
    }

    abstract void configure(Map params)

    abstract BaseClassProcessor createClassProcessor()

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

    void processClass(InputStream classStream, File outputFile) {
        ClassReader classReader = new ClassReader(classStream)
        ClassVisitor processor = apply ? createClassProcessor() : new ClassWriter(COMPUTE_MAXS)
        classReader.accept(processor, 0)
        outputFile.bytes = processor.toByteArray()
    }

    @Override
    String getName() {
        return name
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