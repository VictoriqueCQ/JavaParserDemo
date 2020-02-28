import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.javaparser.Navigator;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class TestFunction {

    public static void main(String[] args) throws IOException {
//        resolveTypeInContext();
//        GetTypeOfReference();
        getLocalVariable();
    }

    public static void getLocalVariable() throws IOException {
//        String FILE_PATH = "src/main/java/TestClass.java";
        String FILE_PATH = "D:\\picasso\\src\\test\\java\\com\\squareup\\picasso3\\BitmapHunterTest.java";
        CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        typeSolver.add(new ReflectionTypeSolver());
//        typeSolver.add(new JarTypeSolver("C:\\Users\\77627\\.m2\\repository\\com\\github\\javaparser\\javaparser-symbol-solver-core\\3.15.7\\javaparser-symbol-solver-core-3.15.7.jar"));
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);
        StaticJavaParser.getConfiguration().setSymbolResolver(symbolSolver);
        CompilationUnit cu = StaticJavaParser.parse(new File(FILE_PATH));
//        cu.findAll(AssignExpr.class).forEach(ae -> {
//            ResolvedType resolvedType = ae.calculateResolvedType();
//            System.out.println(ae.toString() + " is a: " + resolvedType);
//        });
//        cu.findAll(VariableDeclarationExpr.class).forEach(vde -> {
//            ResolvedType resolvedType = vde.calculateResolvedType();
//            System.out.println(vde.toString() + " is a: " + resolvedType);
//        });
//        FieldDeclaration fieldDeclaration = Navigator.findNodeOfGivenClass(cu,FieldDeclaration.class);
//        System.out.println("Field type: "+fieldDeclaration.getVariables().get(0).getType().resolve().asReferenceType().getQualifiedName());
        VariableDeclarator variableDeclaration = Navigator.findNodeOfGivenClass(cu,VariableDeclarator.class);
//        List<VariableDeclarator> variableDeclaratorList = cu.findAll(VariableDeclarator.class);
//        for(VariableDeclarator vd:variableDeclaratorList){
//            System.out.println(vd.getType().toString());
//        }
//        System.out.println("Field type: "+variableDeclaration.getType());
        cu.findAll(MethodCallExpr.class).forEach(mce -> {
            System.out.println(mce.toString());
            System.out.println(mce.calculateResolvedType());
        });
    }

    public static void resolveTypeInContext() throws FileNotFoundException {
        final String FILE_PATH = "src/main/java/Foo.java";
        final String SRC_PATH = "src/main/java";
        TypeSolver reflectionTypeSolver = new ReflectionTypeSolver();
        TypeSolver javaParserTypeSolver = new JavaParserTypeSolver(new File(SRC_PATH));
        reflectionTypeSolver.setParent(reflectionTypeSolver);
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(reflectionTypeSolver);
        combinedTypeSolver.add(javaParserTypeSolver);
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
        StaticJavaParser.getConfiguration().setSymbolResolver(symbolSolver);
        CompilationUnit cu = StaticJavaParser.parse(new File(FILE_PATH));
        FieldDeclaration fieldDeclaration = Navigator.findNodeOfGivenClass(cu,FieldDeclaration.class);
        System.out.println("Field type: "+fieldDeclaration.getVariables().get(0).getType().resolve().asReferenceType().getQualifiedName());
    }

    public static void GetTypeOfReference() throws FileNotFoundException {
        String FILE_PATH = "src/main/java/Bar.java";
        TypeSolver typeSolver = new CombinedTypeSolver();
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);
        StaticJavaParser.getConfiguration().setSymbolResolver(symbolSolver);
        CompilationUnit cu = StaticJavaParser.parse(new File(FILE_PATH));
        cu.findAll(AssignExpr.class).forEach(ae -> {
            ResolvedType resolvedType = ae.calculateResolvedType();
            System.out.println(ae.toString() + " is a: " + resolvedType);
        });
    }

//    public static void resolveMethodCalls() throws FileNotFoundException{
//        final String FILE_PATH = "A.java";
//        TypeSolver typeSolver = new ReflectionTypeSolver();
//        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);
//        StaticJavaParser.getConfiguration().setSymbolResolver(symbolSolver);
//        CompilationUnit cu = StaticJavaParser.parse(new File(FILE_PATH));
//        cu.findAll(MethodCallExpr.class).forEach(mce->{
//            System.out.println(mce.resolveInvokedMethod().getQualifiedSignature());
//        });
//    }
}
