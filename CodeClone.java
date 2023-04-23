package codeclone;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class CodeClone {

    static class ClassInfo {
        String qualifiedName;
        List<FieldDeclaration> fields;
        List<ConstructorDeclaration> constructors;
        List<MethodDeclaration> methods;
        List<ClassInfo> nestedClasses;

        public ClassInfo() {
            fields = new ArrayList<>();
            constructors = new ArrayList<>();
            methods = new ArrayList<>();
            nestedClasses = new ArrayList<>();
        }
    }

    public static void main(String[] args) throws IOException {
        File file = new File("src/main/resources/jhotdraw/JHotDraw7/src/main/java");
        JavaParserTypeSolver javaParserTypeSolver = new JavaParserTypeSolver(file);

        CombinedTypeSolver combinedSolver = new CombinedTypeSolver();
        combinedSolver.add(new ReflectionTypeSolver());
        combinedSolver.add(javaParserTypeSolver);

        listFilesForFolder(file, combinedSolver);
    }

    public static void listFilesForFolder(File folder, CombinedTypeSolver combinedSolver) throws IOException {
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                listFilesForFolder(fileEntry, combinedSolver);
            } else {
                if (fileEntry.getName().endsWith(".java")) {
                    recursiveFunc(fileEntry, combinedSolver);
                }
            }
        }
    }

    public static void recursiveFunc(File file, CombinedTypeSolver combinedSolver) throws FileNotFoundException {
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedSolver);
        StaticJavaParser.getConfiguration().setSymbolResolver(symbolSolver);

        CompilationUnit cu = StaticJavaParser.parse(file);
        List<ClassOrInterfaceDeclaration> classDeclarations = cu.findAll(ClassOrInterfaceDeclaration.class);

        List<ClassInfo> classInfos = new ArrayList<>();

        for (ClassOrInterfaceDeclaration classDeclaration : classDeclarations) {
            ClassInfo classInfo = new ClassInfo();
            classInfo.qualifiedName = classDeclaration.getFullyQualifiedName().orElse("");
            classInfo.fields = classDeclaration.findAll(FieldDeclaration.class);
            classInfo.constructors = classDeclaration.findAll(ConstructorDeclaration.class);
            classInfo.methods = classDeclaration.findAll(MethodDeclaration.class);

            for (ClassOrInterfaceDeclaration nestedClassDeclaration : classDeclaration.findAll(ClassOrInterfaceDeclaration.class)) {
                ClassInfo nestedClassInfo = new ClassInfo();
                nestedClassInfo.qualifiedName = nestedClassDeclaration.getFullyQualifiedName().orElse("");
                nestedClassInfo.fields = nestedClassDeclaration.findAll(FieldDeclaration.class);
                nestedClassInfo.constructors = nestedClassDeclaration.findAll(ConstructorDeclaration.class);
                nestedClassInfo.methods = nestedClassDeclaration.findAll(MethodDeclaration.class);
                classInfo.nestedClasses.add(nestedClassInfo);
            }

            classInfos.add(classInfo);
        }

        for (int i = 0; i < classInfos.size(); i++) {
            for (int j = i + 1; j < classInfos.size(); j++) {
                ClassInfo classA = classInfos.get(i);
                ClassInfo classB = classInfos.get(j);
                if (areType3Clones(classA, classB)) {
                    System.out.println("Type 3 clones:");
                    System.out.println(classA.qualifiedName);
                    System.out.println(classB.qualifiedName);
                    System.out.println("++++++++++++++++++++++++++++");
                }
            }
        }
    }

    private static boolean compareFields(ClassInfo classA, ClassInfo classB) {
        if (classA.fields.size() != classB.fields.size()) {
            return false;
        }

        List<Type> classAFieldTypes = classA.fields.stream().map(FieldDeclaration::getElementType).collect(Collectors.toList());
        List<Type> classBFieldTypes = classB.fields.stream().map(FieldDeclaration::getElementType).collect(Collectors.toList());

        Collections.sort(classAFieldTypes, Comparator.comparing(Type::toString));
        Collections.sort(classBFieldTypes, Comparator.comparing(Type::toString));

        return classAFieldTypes.equals(classBFieldTypes);
    }

    private static boolean compareConstructors(ClassInfo classA, ClassInfo classB) {
        if (classA.constructors.size() != classB.constructors.size()) {
            return false;
        }

        for (ConstructorDeclaration constructorA : classA.constructors) {
            boolean matchFound = false;
            for (ConstructorDeclaration constructorB : classB.constructors) {
                if (constructorA.getModifiers().equals(constructorB.getModifiers()) &&
                        constructorA.getParameters().equals(constructorB.getParameters())) {
                    matchFound = true;
                    break;
                }
            }
            if (!matchFound) {
                return false;
            }
        }

        return true;
    }

    private static boolean compareMethods(ClassInfo classA, ClassInfo classB) {
        if (classA.methods.size() != classB.methods.size()) {
            return false;
        }

        for (MethodDeclaration methodA : classA.methods) {
            boolean matchFound = false;
            for (MethodDeclaration methodB : classB.methods) {
                if (methodA.getModifiers().equals(methodB.getModifiers()) &&
                        methodA.getType().equals(methodB.getType()) &&
                        methodA.getParameters().equals(methodB.getParameters())) {
                    matchFound = true;
                    break;
                }
            }
            if (!matchFound) {
                return false;
            }
        }

        return true;
    }

    private static boolean areType3Clones(ClassInfo classA, ClassInfo classB) {
        return compareFields(classA, classB) &&
                compareConstructors(classA, classB) &&
                compareMethods(classA, classB);
    }
}
