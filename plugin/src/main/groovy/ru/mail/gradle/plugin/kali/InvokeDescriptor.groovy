package ru.mail.gradle.plugin.kali

class InvokeDescriptor {
    String owner
    String methodName
    String descriptor

    public String toString() {
        "${owner.replace('/', '.')}.$methodName${descriptor == null ? '(...)' : descriptor}"
    }

    boolean equals(String owner, String methodName, String descriptor) {
        return owner == this.owner && methodName == this.methodName && descriptor == this.descriptor
    }

    boolean matches(String owner, String methodName, String descriptor) {
        return owner =~ this.owner && methodName =~ this.methodName && descriptor =~ this.descriptor
    }

    static InvokeDescriptor fromFullDescriptor(String fullInvokeDescriptor,
                                               boolean mandatoryDescriptor = false) {
        def descIndex = fullInvokeDescriptor.indexOf('(')
        int methodNameIndex = methodNameIndex(descIndex, fullInvokeDescriptor)

        def ownerClass = fullInvokeDescriptor[0..methodNameIndex - 2].replace('.', '/')
        String methodName = methodName(descIndex, fullInvokeDescriptor, methodNameIndex)

        if (descIndex != -1 && fullInvokeDescriptor.indexOf(')', descIndex) == -1) {
            throw new IllegalArgumentException("Replacement with broken arguments descriptor: $fullInvokeDescriptor")
        }

        def descriptor = descIndex == -1 ? null : fullInvokeDescriptor[descIndex..-1]
        if (mandatoryDescriptor && !descriptor) {
            throw new IllegalArgumentException("Missing mandatory argument descriptor: $fullInvokeDescriptor")
        }

        new InvokeDescriptor(owner: ownerClass, methodName: methodName, descriptor: descriptor)
    }

    static InvokeDescriptor fromRegex(String fullInvokeDescriptor) {
        def parts = fullInvokeDescriptor.split(' ')
        if (parts.length != 3) {
            throw new IllegalArgumentException("Bad invoke regex descriptor: $fullInvokeDescriptor")
        }
        def ownerClass = parts[0].replaceAll('(?<!\\\\)\\\\\\.', '/')
        def methodName = parts[1]
        def descriptor = parts[2]

        new InvokeDescriptor(owner: ownerClass, methodName: methodName, descriptor: descriptor)
    }

    private static String methodName(int descIndex,
                                     String fullInvokeDescriptor,
                                     int methodNameIndex) {
        def methodNameEnd = descIndex >= 0 ? descIndex - 1 : descIndex

        fullInvokeDescriptor[methodNameIndex..methodNameEnd]
    }

    private static int methodNameIndex(int descIndex, String fullInvokeDescriptor) {
        def methodNameLimit = descIndex == -1 ? fullInvokeDescriptor.length() - 1 : descIndex
        def methodNameIndex = fullInvokeDescriptor.lastIndexOf('.', methodNameLimit) + 1
        if (methodNameIndex < 2) {
            throw new IllegalArgumentException("Bad replacement method specification: $fullInvokeDescriptor")
        }

        methodNameIndex
    }

}
