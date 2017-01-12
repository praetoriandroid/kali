package ru.mail.kali

public class TestClassLoader extends ClassLoader {

    private final baseDirectory

    TestClassLoader(baseDirectory) {
        this.baseDirectory = baseDirectory
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            return systemClassLoader.loadClass(name)
        } catch (ClassNotFoundException ignored) {
            def classFile = new File("$baseDirectory/${name.replace('.', '/')}.class")
            if (!classFile.exists()) {
                throw new ClassNotFoundException("Missing in classpath and $classFile.path")
            }
            def classBytes = classFile.bytes
            return defineClass(name, classBytes, 0, classBytes.length)
        }
    }
}
