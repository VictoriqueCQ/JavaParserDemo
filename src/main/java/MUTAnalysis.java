import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.util.*;

public class MUTAnalysis {

    final String SRC_PATH = "D:/picasso/src";
    String FILE_PATH = "";
    static List<ClassOrInterfaceDeclaration> innerClassList = new ArrayList<>();
    static List<FieldDeclaration> globelVariableList = new ArrayList<>();
    static List<PackageDeclaration> packageList = new ArrayList<>();
    static List<ImportDeclaration> importPackageList = new ArrayList<>();

    public MUTAnalysis(String filePath) {
        this.FILE_PATH = filePath;
    }
//    public static void main(String[] args) throws Exception {
//        MUTAnalysis mutAnalysis = new MUTAnalysis();
//        mutAnalysis.getOriginalMethod();
//        for (int i = 0; i < originalMethodClassList.size(); i++) {
//            if (i == 1) {
//                mutAnalysis.methodExtraction(i, originalMethodClassList.get(i));
//            }
//        }
//    }

    public CompilationUnit constructCompilationUnit(String code, String filePath) throws FileNotFoundException {
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        TypeSolver javaParserTypeSolver = new JavaParserTypeSolver(new File(SRC_PATH));
        combinedTypeSolver.add(javaParserTypeSolver);
        TypeSolver reflectionTypeSolver = new ReflectionTypeSolver();
        combinedTypeSolver.add(reflectionTypeSolver);
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
        StaticJavaParser.getConfiguration().setSymbolResolver(symbolSolver);
        return code == null ? StaticJavaParser.parse(new File(filePath)) : StaticJavaParser.parse(code);
    }

    public List<String> getOriginalMethod() throws FileNotFoundException {
        List<String> originalMethodClassList = new ArrayList<>();
        CompilationUnit cu = constructCompilationUnit(null, FILE_PATH);
        //获取package列表
        packageList = cu.findAll(PackageDeclaration.class);
        //获取所有import列表
        importPackageList = cu.findAll(ImportDeclaration.class);
        //获取所有内部类
        cu.findAll(ClassOrInterfaceDeclaration.class).stream().filter(coid -> !coid.getName().toString().equals(FILE_PATH)).forEach(innerClassList::add);
        //获取类变量列表（包含可能的内部类的变量）
        List<FieldDeclaration> fieldDeclarationList = cu.findAll(FieldDeclaration.class);
//        fieldDeclarationList.forEach(System.out::println);
        fieldDeclarationList.forEach(fd -> {
//            System.out.println(fd.toString());
//            int indexOfPublic = fd.getParentNode().get().toString().indexOf("public");
//            int indexOfBraces = fd.getParentNode().get().toString().indexOf("{");
//            if (indexOfPublic < indexOfBraces) {
            globelVariableList.add(fd);
//            }
        });
        //获取方法并转成类
        cu.findAll(MethodDeclaration.class).forEach(md -> originalMethodClassList.add("class TestFragment{\n" + md.toString() + "}"));
        return originalMethodClassList;
    }


    public void methodExtraction(int index, String methodClass) throws FileNotFoundException {
        CompilationUnit cu2 = constructCompilationUnit(methodClass, null);
        List<VariableTuple> variableList = new ArrayList<>();
        for (FieldDeclaration fd : globelVariableList) {
//            System.out.println(fd);
            variableList.add(new VariableTuple(true, fd.getVariable(0).getTypeAsString(), fd.getVariable(0).getNameAsString(), fd));
        }
        //获取所有调用方法中的变量.若第一项为true，则表示该变量是全局变量
        Map<Expression, Boolean> normalArgumentMap = new HashMap<>();
        Map<Expression, Boolean> abnormalArgumentMap = new HashMap<>();
        cu2.findAll(MethodCallExpr.class).forEach(mce -> {
            NodeList<Expression> argumentList = mce.getArguments();
            for (Expression expression : argumentList) {
                try {
                    expression.calculateResolvedType().describe();
                    normalArgumentMap.put(expression, true);
                } catch (Exception e) {
                    if (e.getMessage().contains(" in ")) {
                        abnormalArgumentMap.put(expression, true);
                    } else {
                        abnormalArgumentMap.put(expression, false);
                    }
                }
            }
        });
        //获取所有构造方法中的变量
        cu2.findAll(ObjectCreationExpr.class).forEach(oce -> {
            NodeList<Expression> argumentList = oce.getArguments();
            for (Expression expression : argumentList) {
                try {
                    expression.calculateResolvedType().describe();
                    normalArgumentMap.put(expression, true);
                } catch (Exception e) {
                    if (e.getMessage().contains(" in ")) {
                        abnormalArgumentMap.put(expression, true);
                    } else {
                        abnormalArgumentMap.put(expression, false);
                    }
                }
            }
        });
//        globelVariableList.forEach(gv-> System.out.println(gv.getVariable(0)));
        //获取if，switch中的变量
        cu2.findAll(IfStmt.class).forEach(i -> {
            try {
                for (FieldDeclaration fd : globelVariableList) {
//                    System.out.println(fd.getVariables().toString());;
//                    System.out.println(i.getCondition().toString().contains(fd.getVariable(0).toString()));
                }

            } catch (Exception e) {
//                System.err.println(e.getMessage());
            }
//            if(i.getCondition().isAssignExpr()){
//                System.out.println();
//            }
        });

        //初始化ClassOrInterfaceDeclaration,用于构造新的测试片段类
        ClassOrInterfaceDeclaration myClass = new ClassOrInterfaceDeclaration();
        //获取MethodDeclanation，用于后续添加语句
        MethodDeclaration myMethod = cu2.findAll(MethodDeclaration.class).get(0);
        myClass.setName("MUT");
        Set<String> importTypeSet = new HashSet<>();
        cu2.findAll(VariableDeclarator.class).forEach(v -> importTypeSet.add(v.getTypeAsString()));

        Map<Expression, Boolean> tempAbnormalArgumentMap = new HashMap<>(abnormalArgumentMap);
        for (Map.Entry<Expression, Boolean> entry : abnormalArgumentMap.entrySet()) {
            Expression var = entry.getKey();
            boolean isGlobal = entry.getValue();
            for (VariableTuple vt : variableList) {
                if (var.toString().equals(vt.name) && (isGlobal == vt.isGlobal)) {
                    if (vt.original.getClass().equals(FieldDeclaration.class)) {
                        FieldDeclaration fd = (FieldDeclaration) vt.original;
                        myClass.addMember(fd);
                        importTypeSet.add(fd.getElementType().toString());
                        tempAbnormalArgumentMap.remove(var);
                    }
                }
            }
        }

        for (Map.Entry<Expression, Boolean> entry : tempAbnormalArgumentMap.entrySet()) {
            if (entry.getValue() == true) {
                importTypeSet.add(entry.getKey().toString());
            }
        }

        //处理import
        List<ImportDeclaration> importDeclarationList = new ArrayList<>();
        List<ImportDeclaration> importList = new ArrayList<>();
        for (String s : importTypeSet) {
            for (ImportDeclaration id : importPackageList) {
                String[] importNameArray = id.getNameAsString().split("\\.");
                String importName = importNameArray[importNameArray.length - 1];
                if (importName.equals(s)) {
                    importDeclarationList.add(id);
                    importList.add(id);
                }
            }
        }
        List<String> writeFileImportList = new ArrayList<>();
        Map<String, String> importMap = new HashMap<>();
        for (ImportDeclaration i : importList) {
            String path = "D:/picasso/src/main/java/" + i.getNameAsString().replaceAll("\\.", "/");
            String newPath = findFile(path);
            if (newPath.length() == 0) {
                writeFileImportList.add(i.toString());
            }
            if (newPath.length() > 0) {
                String tempPath = newPath.replaceAll("\\.java", "");
                String variable = path.replaceAll(tempPath + "/", "");
                importMap.put(variable, newPath);
            }
        }
        for (Map.Entry<String, String> entry : importMap.entrySet()) {
//            System.err.println(entry.getKey()+" "+entry.getValue());
            //暂时没在测试片段提取中加判断是不是枚举类型的代码
            if (entry.getKey().contains("/")) {
                EnumDeclaration enumDeclaration = findEnum(entry.getKey(), entry.getValue());
                if (enumDeclaration != null) {
//                    System.out.println(enumDeclaration);
                    myClass.addMember(enumDeclaration);
                }
            } else {
                Map<FieldDeclaration, ImportDeclaration> declarationMap = findVariable(entry.getKey(), entry.getValue());
                FieldDeclaration f = new FieldDeclaration();
                ImportDeclaration i = null;
                for (Map.Entry<FieldDeclaration, ImportDeclaration> entry1 : declarationMap.entrySet()) {
                    f = entry1.getKey();
                    i = entry1.getValue();
                }
                if (i != null) {
                    writeFileImportList.add(i.toString());
                }
                if (f != null) {
//                System.err.println(f);
                    myClass.addMember(f);
                }
            }
        }
        String packageName = packageList.get(0).getNameAsString().replaceAll("\\.", "_");
        myClass.addMember(myMethod);
        String methodName = myMethod.getNameAsString();
//        System.out.println(methodName);
//        System.out.println(myClass);
        try {
            String[] filenameArray = FILE_PATH.split("/");
            String filename = filenameArray[filenameArray.length - 1].split("\\.")[0];
            String outputFileName = "MUT/" + packageName + "_" + filename + "_" + methodName + myMethod.getParameters().toString() + ".txt";
            BufferedWriter bw = new BufferedWriter(new FileWriter(outputFileName));
//            System.err.println(writeFileImportList.size());
            for (String s : writeFileImportList) {
//                System.out.println(s);
                System.out.println(outputFileName);
                bw.write(s);
            }
            bw.write(myClass.toString());
            bw.close();
            System.err.println("文件写入成功");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public EnumDeclaration findEnum(String variable, String filePath) throws FileNotFoundException {
        CompilationUnit cu = constructCompilationUnit(null, filePath);
        List<EnumDeclaration> enumDeclarationList = cu.findAll(EnumDeclaration.class);
        for (EnumDeclaration ed : enumDeclarationList) {
            if (variable.contains(ed.getName().toString())) {
                return ed;
            }
        }
        return null;
    }

    public Map<FieldDeclaration, ImportDeclaration> findVariable(String variable, String filePath) throws FileNotFoundException {
//        System.out.println(filePath);
        CompilationUnit cu = constructCompilationUnit(null, filePath);
        List<FieldDeclaration> newfieldDeclarationList = cu.findAll(FieldDeclaration.class);
        List<ImportDeclaration> newImportDeclarationList = cu.findAll(ImportDeclaration.class);
        Set<String> newImportTypeSet = new HashSet<>();
        ImportDeclaration resultImportDeclaration = null;
        FieldDeclaration resultFieldDeclaration = new FieldDeclaration();
        for (FieldDeclaration fd : newfieldDeclarationList) {
            List<VariableDeclarator> variableDeclaratorList = fd.getVariables();
            for (VariableDeclarator vd : variableDeclaratorList) {
                if (vd.getNameAsString().equals(variable)) {
                    newImportTypeSet.add(fd.getElementType().toString());
                    resultFieldDeclaration = fd;
                }
            }
        }

        for (String s : newImportTypeSet) {
            for (ImportDeclaration id : newImportDeclarationList) {
                String[] importNameArray = id.getNameAsString().split("\\.");
                String importName = importNameArray[importNameArray.length - 1];
                if (importName.equals(s)) {
                    resultImportDeclaration = id;
                }
            }
        }
        Map<FieldDeclaration, ImportDeclaration> resultMap = new HashMap<>();
        resultMap.put(resultFieldDeclaration, resultImportDeclaration);
        return resultMap;
    }

    public String findFile(String sourcePath) {
        String filePath = sourcePath + ".java";
        File file = new File(filePath);
        String[] pathArray = sourcePath.split("/");
        if (file.exists()) return filePath;
        if (pathArray.length == 1) return "";
        String newPathString = "";
        if (!file.exists() && pathArray.length > 1) {
            StringBuilder newPath = new StringBuilder();
            for (int i = 0; i < pathArray.length - 1; i++) {
                newPath.append(pathArray[i]).append("/");
            }
            newPathString = newPath.toString().substring(0, newPath.length() - 1);
        }
        return findFile(newPathString);
    }


}
