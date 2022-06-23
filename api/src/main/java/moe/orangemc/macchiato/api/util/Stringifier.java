package moe.orangemc.macchiato.util;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class Stringifier {
    public Stringifier() {
        throw new UnsupportedOperationException("Macchiato think you do not need an instance of Stringifier.");
    }

    public static String stringifyClass(ClassNode classNode) {
        StringBuilder classStringBuilder = new StringBuilder();

        if ((classNode.access & Opcodes.ACC_PUBLIC) != 0) {
            classStringBuilder.append("public ");
        }
        if ((classNode.access & Opcodes.ACC_FINAL) != 0) {
            classStringBuilder.append("final ");
        }
        if ((classNode.access & Opcodes.ACC_STATIC) != 0) {
            classStringBuilder.append("static ");
        }
        if ((classNode.access & Opcodes.ACC_ABSTRACT) != 0) {
            classStringBuilder.append("abstract ");
        }

        if ((classNode.access & Opcodes.ACC_INTERFACE) != 0) {
            classStringBuilder.append("interface ");
        } else if ((classNode.access & Opcodes.ACC_RECORD) != 0) {
            classStringBuilder.append("record ");
        } else if ((classNode.access & Opcodes.ACC_ENUM) != 0) {
            classStringBuilder.append("enum ");
        } else {
            classStringBuilder.append("class ");
        }

        classStringBuilder.append(classNode.name).append(" ");
        if (classNode.signature != null) {
            classStringBuilder.append(classNode.signature).append(" ");
        }
        if (classNode.superName != null) {
            classStringBuilder.append("extends ").append(classNode.superName).append(" ");
        }
        if (classNode.interfaces.size() != 0) {
            classStringBuilder.append("implements ").append(String.join(",", classNode.interfaces.toArray(new String[0])));
        }

        return classStringBuilder.toString();
    }

    public static String stringifyMethod(MethodNode methodNode) {
        StringBuilder methodStringBuilder = new StringBuilder();

        if ((methodNode.access & Opcodes.ACC_PUBLIC) != 0) {
            methodStringBuilder.append("public ");
        } else if ((methodNode.access & Opcodes.ACC_PROTECTED) != 0) {
            methodStringBuilder.append("protected ");
        } else if ((methodNode.access & Opcodes.ACC_PRIVATE) != 0) {
            methodStringBuilder.append("private ");
        }

        if ((methodNode.access & Opcodes.ACC_SYNTHETIC) != 0) {
            methodStringBuilder.append("synthetic ");
        }
        if ((methodNode.access & Opcodes.ACC_STRICT) != 0) {
            methodStringBuilder.append("strictfp ");
        }
        if ((methodNode.access & Opcodes.ACC_STATIC) != 0) {
            methodStringBuilder.append("static ");
        }
        if ((methodNode.access & Opcodes.ACC_BRIDGE) != 0) {
            methodStringBuilder.append("bridge ");
        }
        if ((methodNode.access & Opcodes.ACC_NATIVE) != 0) {
            methodStringBuilder.append("native ");
        }
        if ((methodNode.access & Opcodes.ACC_SYNCHRONIZED) != 0) {
            methodStringBuilder.append("synchronized ");
        }
        if ((methodNode.access & Opcodes.ACC_FINAL) != 0) {
            methodStringBuilder.append("final ");
        }

        Type descriptorType = Type.getMethodType(methodNode.desc);
        methodStringBuilder.append(descriptorType.getReturnType().getClassName()).append(" ")
                .append(methodNode.name).append("(");

        String[] argumentTypeString = new String[descriptorType.getArgumentTypes().length];
        Type[] argumentTypes = descriptorType.getArgumentTypes();
        for (int i = 0; i < argumentTypes.length; i++) {
            Type argumentType = argumentTypes[i];
            argumentTypeString[i] = argumentType.getClassName();
        }
        methodStringBuilder.append(String.join(",", argumentTypeString)).append(")");

        return methodStringBuilder.toString();
    }
}
