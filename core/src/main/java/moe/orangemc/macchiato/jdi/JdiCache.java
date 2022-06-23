package macchiato.jdi;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.Field;
import com.sun.jdi.Method;
import com.sun.jdi.ReferenceType;
import macchiato.jar.JarClassCache;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JdiCache {
    private final JarClassCache classCache;
    private final BiMap<ReferenceType, ClassNode> classMap = HashBiMap.create();
    private final BiMap<Method, MethodNode> methodMap = HashBiMap.create();
    private final BiMap<Field, FieldNode> fieldMap = HashBiMap.create();
    
    public JdiCache(JarClassCache jarClassCache) {
        this.classCache = jarClassCache;
    }

    @SuppressWarnings({"CommentedOutCode"})
    public ClassNode findClassNode(ReferenceType referenceType) {
        if (classMap.containsKey(referenceType)) {
            return classMap.get(referenceType);
        }

        List<ClassNode> classNodes = classCache.findClass(referenceType.name().replaceAll("\\.", "/"));
        List<ClassNode> found = new ArrayList<>();

        findClass:
        for (ClassNode classNode : classNodes) {
            if (!referenceType.name().replaceAll("\\.", "/").equals(classNode.name)) {
                continue;
            }
            if (referenceType.modifiers() != classNode.access) {
                continue;
            }
            if (!Objects.equals(referenceType.signature(), classNode.signature)) {
                continue;
            }

            /*
            // Module info from classNode seems to be always null.
            if (vm.canGetModuleInfo()) {
                ModuleReference moduleReference = referenceType.module();
                if (moduleReference == null && classNode.module != null) {
                    continue;
                }
                if (moduleReference != null && classNode.module == null) {
                    continue;
                }
                if (moduleReference != null) {

                }
            }
            */

            if (classNode.fields.size() != referenceType.fields().size()) {
                continue;
            }

            for (int fieldIndex = 0; fieldIndex < classNode.fields.size(); fieldIndex ++) {
                Field fieldInJVM = referenceType.fields().get(fieldIndex);
                FieldNode fieldInClass = classNode.fields.get(fieldIndex);

                if (!Objects.equals(fieldInJVM.name(), fieldInClass.name)) {
                    continue findClass;
                }
                if (fieldInClass.access != fieldInJVM.modifiers()) {
                    continue findClass;
                }
                if (!Objects.equals(Type.getType(fieldInClass.desc).getClassName(), fieldInJVM.typeName())) {
                    continue findClass;
                }
            }

            if (classNode.methods.size() != referenceType.methods().size()) {
                continue;
            }

            for (int methodIndex = 0; methodIndex < classNode.methods.size(); methodIndex ++) {
                Method methodInJVM = referenceType.methods().get(methodIndex);
                MethodNode methodInClass = classNode.methods.get(methodIndex);

                if (!Objects.equals(methodInJVM.name(), methodInClass.name)) {
                    continue findClass;
                }
                if (methodInClass.access != methodInJVM.modifiers()) {
                    continue findClass;
                }

                Type methodType = Type.getMethodType(methodInClass.desc);

                if (!Objects.equals(methodInJVM.returnTypeName(), methodType.getReturnType().getClassName())) {
                    continue findClass;
                }

                try {
                    Type[] argumentsInClass = methodType.getArgumentTypes();
                    List<String> argumentsInJVM = methodInJVM.argumentTypeNames();

                    if (methodInJVM.argumentTypes().size() != argumentsInClass.length) {
                        continue findClass;
                    }
                    for (int argumentIndex = 0; argumentIndex < methodInJVM.argumentTypes().size(); argumentIndex++) {
                        if (!Objects.equals(argumentsInClass[argumentIndex].getClassName(), argumentsInJVM.get(argumentIndex))) {
                            continue findClass;
                        }
                    }
                } catch (ClassNotLoadedException e) {
                    throw new UnsupportedOperationException("Class " + referenceType.name() + " is not loaded yet", e);
                }
            }

            found.add(classNode);
        }

        if (found.size() > 1) {
            throw new IllegalStateException("Multiple class definitions found in class pool.");
        }
        if (found.size() == 0) {
            return null;
        }
        ClassNode result = found.get(0);
        classMap.put(referenceType, result);
        return result;
    }

    public MethodNode findMethodNode(ClassNode classNode, Method method) {
        if (methodMap.containsKey(method)) {
            return methodMap.get(method);
        }

        List<MethodNode> found = new ArrayList<>();

        findMethod:
        for (MethodNode methodNode : classNode.methods) {
            if (!methodNode.name.equals(method.name())) {
                continue;
            }
            if (method.modifiers() != methodNode.access) {
                continue;
            }

            Type methodNodeType = Type.getMethodType(methodNode.desc);

            Type[] argumentTypes = methodNodeType.getArgumentTypes();
            List<String> argumentTypeNamesInJVM = method.argumentTypeNames();

            if (argumentTypeNamesInJVM.size() != argumentTypes.length) {
                continue;
            }

            if (!Objects.equals(methodNodeType.getReturnType().getClassName(), method.returnTypeName())) {
                continue;
            }

            for (int argumentIndex = 0; argumentIndex < argumentTypes.length; argumentIndex ++) {
                if (!Objects.equals(argumentTypes[argumentIndex].getClassName(), argumentTypeNamesInJVM.get(argumentIndex))) {
                    continue findMethod;
                }
            }

            found.add(methodNode);
        }

        if (found.size() > 1) {
            throw new IllegalStateException("Multiple method definitions found in the class.");
        }
        if (found.size() == 0) {
            return null;
        }

        MethodNode result = found.get(0);
        methodMap.put(method, result);
        return result;
    }

    public FieldNode findFieldNode(ClassNode classNode, Field field) {
        if (fieldMap.containsKey(field)) {
            return fieldMap.get(field);
        }

        List<FieldNode> found = new ArrayList<>();

        for (FieldNode fieldNode : classNode.fields) {
            if (!fieldNode.name.equals(field.name())) {
                continue;
            }
            if (fieldNode.access != field.modifiers()) {
                continue;
            }
            if (!Type.getType(fieldNode.desc).getClassName().equals(field.typeName())) {
                continue;
            }
            found.add(fieldNode);
        }

        if (found.size() > 1) {
            throw new IllegalStateException("Multiple field definitions found in the class.");
        }
        if (found.size() == 0) {
            return null;
        }

        FieldNode result = found.get(0);
        fieldMap.put(field, result);
        return result;
    }

    public ReferenceType findJdiClass(ClassNode classNode) {
        return classMap.inverse().get(classNode);
    }

    public Method findJdiMethod(MethodNode methodNode) {
        return methodMap.inverse().get(methodNode);
    }

    public Field findJdiField(FieldNode fieldNode) {
        return fieldMap.inverse().get(fieldNode);
    }
}
