package ru.mail.gradle.plugin.kali

import com.android.build.api.transform.Format
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInvocation
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor

import java.util.zip.ZipEntry

import static com.android.build.api.transform.QualifiedContent.DefaultContentType.CLASSES

class KaliTransform extends Transform {

    static final LIBRARY_SCOPES = EnumSet.of(
            QualifiedContent.Scope.PROJECT,
            QualifiedContent.Scope.PROJECT_LOCAL_DEPS
    )

    static final APPLICATION_SCOPES = EnumSet.of(
            QualifiedContent.Scope.PROJECT,
            QualifiedContent.Scope.PROJECT_LOCAL_DEPS,
            QualifiedContent.Scope.SUB_PROJECTS,
            QualifiedContent.Scope.SUB_PROJECTS_LOCAL_DEPS,
            QualifiedContent.Scope.EXTERNAL_LIBRARIES
    )

    final boolean applicationProject

    Set<String> ignoreClasses
    List<Replacement> replacements
    List<Replacement> replacementsRegex
    boolean inlineSyntheticFieldAccessors
    Map<String, String> setFieldModifiers

    KaliTransform(boolean applicationProject) {
        this.applicationProject = applicationProject
    }

    void configure(ReplaceCallsExtension replaceCalls, KaliPluginExtension kali) {
        inlineSyntheticFieldAccessors = kali.inlineSyntheticFieldAccessors
        setFieldModifiers = kali.setFieldModifiers
        def ignoreClasses = replaceCalls.ignoreClasses ?: []
        def replacements = replaceCalls.replacements
        def replacementsRegex = replaceCalls.replacementsRegex
        if (!replacements && !replacementsRegex) {
            this.ignoreClasses = []
            this.replacements = []
            this.replacementsRegex = []
            return
        }

        this.ignoreClasses = ignoreClasses.collect {
            it.replace('.', '/')
        }

        this.replacements = []
        replacements.each { key, value ->
            def replaceable = InvokeDescriptor.fromFullDescriptor(key)
            def replacement = InvokeDescriptor.fromFullDescriptor(value)
            this.replacements << new Replacement(from: replaceable, to: replacement)
        }

        this.replacementsRegex = []
        replacementsRegex.each { key, value ->
            def replaceable = InvokeDescriptor.fromRegex(key)
            def replacement = InvokeDescriptor.fromFullDescriptor(value, false)
            this.replacementsRegex << new Replacement(from: replaceable, to: replacement)
        }
    }

    @Override
    void transform(TransformInvocation invocation) {
        def preparedInfo = prepare(invocation)
        def outDir = getOutputDir(invocation)

        invokeTransformation(invocation, preparedInfo, outDir)
    }

    PreparedInfo prepare(TransformInvocation invocation) {
        def preparedInfoBuilder = new PreparedInfo.Builder()

        if (inlineSyntheticFieldAccessors || setFieldModifiers) {
            new Traverser() {
                @Override
                void processZipClass(ZipEntry entry, InputStream stream) {
                    preProcessClass(stream, preparedInfoBuilder)
                }

                @Override
                void processFile(File baseDir, File file, InputStream stream) {
                    preProcessClass(stream, preparedInfoBuilder)
                }
            }.traverse(invocation)
        }

        preparedInfoBuilder.build()
    }

    private File getOutputDir(TransformInvocation invocation) {
        def provider = invocation.outputProvider
        def outDir = provider.getContentLocation(name, outputTypes, scopes, Format.DIRECTORY)
        outDir.deleteDir()
        outDir.mkdirs()

        outDir
    }

    private void invokeTransformation(TransformInvocation invocation, PreparedInfo preparedInfo, File outDir) {
        new Traverser() {
            @Override
            void processZipClass(ZipEntry entry, InputStream stream) {
                processClass(stream, outputForZip(entry), preparedInfo)
            }

            @Override
            void processZipBytes(ZipEntry entry, InputStream stream) {
                outputForZip(entry) << stream
            }

            @Override
            void processDir(File baseDir, File file) {
                outputForFile(baseDir, file).mkdirs()
            }

            @Override
            void processFile(File baseDir, File file, InputStream stream) {
                def outputFile = outputForFile(baseDir, file)
                processClass(stream, outputFile, preparedInfo)
            }

            File outputForZip(ZipEntry entry) {
                def outputFile = new File(outDir, entry.name)
                outputFile.parentFile.mkdirs()

                outputFile
            }

            File outputForFile(File baseDir, File file) {
                int baseDirLength = baseDir.absolutePath.length()
                def path = "${file.absolutePath[baseDirLength..-1]}"

                new File(outDir, path)
            }

        }.traverse(invocation)
    }

    void processClass(InputStream classStream, File outputFile, PreparedInfo preparedInfo) {
        ClassReader classReader = new ClassReader(classStream)
        ClassVisitor processor = new KaliClassVisitor(ignoreClasses, replacements, replacementsRegex, setFieldModifiers, preparedInfo)
        classReader.accept(processor, 0)
        outputFile.bytes = processor.toByteArray()
    }

    void preProcessClass(InputStream classStream, PreparedInfo.Builder builder) {
        def classReader = new ClassReader(classStream)
        def transformer = new PrepareVisitor(setFieldModifiers.keySet())

        classReader.accept(transformer, 0)

        builder.addClass(transformer.build())
    }

    @Override
    String getName() {
        return 'kali'
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return EnumSet.of(CLASSES)
    }

    @Override
    Set<QualifiedContent.Scope> getScopes() {
        return applicationProject ? APPLICATION_SCOPES : LIBRARY_SCOPES
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