package ru.mail.gradle.plugin.kali

import com.android.build.api.transform.TransformInvocation

import java.util.zip.ZipEntry
import java.util.zip.ZipFile

abstract class Traverser {

    void traverse(TransformInvocation invocation) {
        invocation.inputs.each { transformInput ->
            transformInput.jarInputs.each { jarInput ->
                ZipFile zip = new ZipFile(jarInput.file)
                zip.entries().findAll { zipEntry ->
                    !zipEntry.directory
                }.each { zipEntry ->
                    handleZipEntry(zip, zipEntry)
                }
            }

            transformInput.directoryInputs.each { directoryInput ->
                directoryInput.file.traverse { file ->
                    handleFile(directoryInput.file, file)
                }
            }
        }
    }

    void handleZipEntry(ZipFile zip, ZipEntry entry) {
        InputStream entryStream = zip.getInputStream(entry);
        if (entry.name.toLowerCase().endsWith('.class')) {
            processZipClass(entry, entryStream)
        } else {
            processZipBytes(entry, entryStream)
        }
        entryStream.close()
    }

    void handleFile(File baseDir, File file) {
        if (file.isDirectory()) {
            processDir(baseDir, file)
        } else {
            InputStream fileStream = file.newInputStream();
            processFile(baseDir, file, fileStream)
            fileStream.close()
        }
    }

    void processZipClass(ZipEntry entry, InputStream stream) {}

    void processZipBytes(ZipEntry entry, InputStream stream) {}

    void processDir(File baseDir, File file) {}

    void processFile(File baseDir, File file, InputStream stream) {}

}
