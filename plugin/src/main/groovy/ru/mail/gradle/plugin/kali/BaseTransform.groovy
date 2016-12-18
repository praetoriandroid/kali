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

    abstract BaseClassProcessor createClassProcessor(PreparedInfo info)

    @Override
    void transform(final TransformInvocation invocation) {
        def outDir = invocation.outputProvider.getContentLocation(name, outputTypes, scopes, Format.DIRECTORY)

        outDir.deleteDir()
        outDir.mkdirs()

        def preparedInfoBuilder = new PreparedInfo.Builder()

        invocation.inputs.each { transformInput ->
            transformInput.jarInputs.each { jarInput ->
                ZipFile zip = new ZipFile(jarInput.file)
                zip.entries().findAll { zipEntry ->
                    !zipEntry.directory
                }.each { zipEntry ->
                    if (zipEntry.name.toLowerCase().endsWith('.class')) {
                        InputStream entryStream = zip.getInputStream(zipEntry);
                        preProcessClass(entryStream, preparedInfoBuilder)
                        entryStream.close()
                    }
                }
            }

            transformInput.directoryInputs.each { directoryInput ->
                directoryInput.file.traverse { file ->
                    if (file.isDirectory()) {
                    } else {
                        InputStream fileStream = new FileInputStream(file)//file.newInputStream();
                        preProcessClass(fileStream, preparedInfoBuilder)
                        fileStream.close()
                    }
                }
            }
        }

        def preparedInfo = preparedInfoBuilder.build()

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
                        processClass(entryStream, outputFile, preparedInfo)
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
                        processClass(fileStream, outputFile, preparedInfo)
                        fileStream.close()
                    }
                }
            }
        }
    }

    void processClass(InputStream classStream, File outputFile, PreparedInfo preparedInfo) {
        ClassReader classReader = new ClassReader(classStream)
        ClassVisitor processor = apply ? createClassProcessor(preparedInfo) : new ClassWriter(COMPUTE_MAXS)
        classReader.accept(processor, 0)
        outputFile.bytes = processor.toByteArray()
    }

    void preProcessClass(InputStream classStream, PreparedInfo.Builder builder) {
        def classReader = new ClassReader(classStream)
        def transformer = new PrepareVisitor()

        classReader.accept(transformer, 0)

        builder.addClass(transformer.build())
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