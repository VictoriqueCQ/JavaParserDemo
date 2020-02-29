import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.io.*;
import java.util.*;

public class TestFileAnalysis {
    //    String FILE_NAME = "BitmapHunterTest";
//    String FILE_PATH = "D:/picasso/src/test/java/com/squareup/picasso3/BitmapHunterTest.java";
    String FILE_PATH = "";
    String ROOT_PATH = "D:/picasso/src/main/java/";//"src\\main\\java"
    static List<PackageDeclaration> packageList;
    static List<ImportDeclaration> importPackageList;
    static List<ClassOrInterfaceDeclaration> innerClassList = new ArrayList<>();
    static List<FieldDeclaration> globelVariableList = new ArrayList<>();
    //    static List<VariableDeclarator> globel_Variable_List;
    MethodDeclaration beforeMethod = new MethodDeclaration();
    static List<MethodDeclaration> normalMethodList = new ArrayList<>();

    //    static List<MethodDeclaration> testMethodList = new ArrayList<>();
    public TestFileAnalysis(String filePath) {
        this.FILE_PATH = filePath;
    }

    public CompilationUnit constructCompilationUnit(String code, String FILE_PATH) throws FileNotFoundException {
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        TypeSolver javaParserTypeSolver = new JavaParserTypeSolver(new File(ROOT_PATH));
        combinedTypeSolver.add(javaParserTypeSolver);
        TypeSolver reflectionTypeSolver = new ReflectionTypeSolver();
        combinedTypeSolver.add(reflectionTypeSolver);
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
        StaticJavaParser.getConfiguration().setSymbolResolver(symbolSolver);

        return code == null ? StaticJavaParser.parse(new File(FILE_PATH)) : StaticJavaParser.parse(code);
    }

    public List<String> getOriginalTestFragment() throws IOException {
        List<String> originalTestFragmentClassList = new ArrayList<>();
        CompilationUnit cu = constructCompilationUnit(null, FILE_PATH);
        //获取package列表
        packageList = cu.findAll(PackageDeclaration.class);
        //获取所有import列表
        importPackageList = cu.findAll(ImportDeclaration.class);
        //获取所有内部类
        cu.findAll(ClassOrInterfaceDeclaration.class).stream().filter(coid -> !FILE_PATH.contains(coid.getName().toString())).forEach(innerClassList::add);
        //获取类变量列表（包含可能的内部类的变量）
        List<FieldDeclaration> fieldDeclarationList = cu.findAll(FieldDeclaration.class);
        fieldDeclarationList.forEach(fd -> {
            int indexOfPublic = fd.getParentNode().get().toString().indexOf("public");
            int indexOfBraces = fd.getParentNode().get().toString().indexOf("{");
            if (indexOfPublic < indexOfBraces) {
                globelVariableList.add(fd);
            }
        });

        //获取非测试方法
        cu.findAll(MethodDeclaration.class).stream().filter(md -> !md.getAnnotationByName("Test").isPresent()).forEach(normalMethodList::add);
        //获取before方法
        for (MethodDeclaration md : normalMethodList) {
            if (md.getAnnotationByName("Before").isPresent()) {
                beforeMethod = md;
            }
        }
//        normalMethodList.forEach(nm -> System.out.println(nm.toString()));
        //获取测试方法。先获取所有测试方法，然后转成类
        cu.findAll(MethodDeclaration.class).stream().filter(md -> md.getAnnotationByName("Test").isPresent()).forEach(md -> originalTestFragmentClassList.add("class TestFragment{\n" + md.toString() + "}"));
        return originalTestFragmentClassList;
    }

    public void dependencyAnalysis(int index, String testFragmentString) throws IOException {
        List<String> importDependency = new ArrayList<>();
        List<String> externalVariableDependency = new ArrayList<>();
        List<String> fragmentContent = new ArrayList<>();
        List<String> methodDependency = new ArrayList<>();
        MethodDeclaration targetMethodDeclaration = null;

        //获得compilationUnit，用于后续分析
        CompilationUnit cu = constructCompilationUnit(testFragmentString, null);
        //把全局变量扔进变量列表，以后还会根据方法中新的参数列表新增.第一个是变量名，第二个是变量类型。
        List<VariableTuple> variableList = new ArrayList<>();
        for (FieldDeclaration fd : globelVariableList) {
            variableList.add(new VariableTuple(true, fd.getVariable(0).getTypeAsString(), fd.getVariable(0).getNameAsString(), fd));
        }
        //获取所有调用方法中的变量.若第一项为true，则表示该变量是全局变量
        Map<Expression, Boolean> normalArgumentMap = new HashMap<>();
        Map<Expression, Boolean> abnormalArgumentMap = new HashMap<>();
        cu.findAll(MethodCallExpr.class).forEach(mce -> {
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
        cu.findAll(ObjectCreationExpr.class).forEach(oce -> {
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

//        System.out.println("=============================");
//        List<MethodCallExpr> methodCallList = cu.findAll(MethodCallExpr.class);
//        for (MethodCallExpr mce : methodCallList) {
//                try {
//                    System.err.println("======" + mce.resolve().getReturnType().describe());
//                    System.err.println("===="+mce.toString());
//                } catch (Exception e) {
//                    System.out.println(e.getMessage());
//                }
//        }
//        cu.findAll(MethodCallExpr.class).forEach(mc -> System.out.println(mc.toString()));
//        cu.findAll(MethodReferenceExpr.class).forEach(mr-> System.out.println(mr.toString()));

        //初始化ClassOrInterfaceDeclaration,用于构造新的测试片段类
        ClassOrInterfaceDeclaration myClass = new ClassOrInterfaceDeclaration();
        //获取MethodDeclanation，用于后续添加语句
        MethodDeclaration myMethod = cu.findAll(MethodDeclaration.class).get(0);
//        System.out.println(myMethod.toString());


//        List<MethodCallExpr> methodCallExprList = cu.findAll(MethodCallExpr.class);
//        List<VariableDeclarator> variableDeclarators = cu.findAll(VariableDeclarator.class);
//        List<Expression> expressions = new ArrayList<>();
//
//        for (MethodCallExpr methodCallExpr : methodCallExprList) {
//            String methodCallName = methodCallExpr.getNameAsString();
////            System.err.println(methodCallExpr.toString());
////            System.err.println(methodCallName);
//            if (methodCallName.contains("assert")) {
//                //拆出可能是测试目标的参数变量
//                Expression expression = methodCallExpr.getArgument(0);
//                expressions.add(expression);
//                //寻找测试目标参数变量初始化的地方
//                for (VariableDeclarator vd : variableDeclarators) {
//                    if (vd.getNameAsExpression().equals(expression)) {
//                        String targetMethodName = vd.getInitializer().get().asMethodCallExpr().getNameAsString();
////                        System.out.println(targetMethodName);
//                        String targetMethodCall = vd.getInitializer().get().toString().split("\\.")[0];
////                        System.out.println(targetMethodCall);
//
//
//                        String packageName = packageList.get(0).getNameAsString().replaceAll("\\.", "_");
//                        List<String> typeList = new ArrayList<>();
//                        NodeList<Expression> arguments = vd.getInitializer().get().asMethodCallExpr().getArguments();
//                        for(Expression e1:arguments){
//                            for(VariableDeclarator vd2:variableDeclarators){
//                                if(vd2.getNameAsExpression().equals(e1)){
//                                    typeList.add(vd2.getType().toString());
//                                }
//                            }
//                        }
//                        List<String> MUTList = new ArrayList<>();
//                        MUTList = findFileList(new File("MUT/"),MUTList);
////                        MUTList.forEach(System.err::println);
////                        System.err.println(packageName);
//                        for(String s:MUTList){
//                            if(s.contains(targetMethodName)&&s.contains(packageName)&&judgeArguments(s,typeList)){
//                                CompilationUnit cu2 = constructCompilationUnit(null,s);
////                                System.out.println(cu2.findAll(MethodDeclaration.class).get(0).toString());
//                                targetMethodDeclaration = cu2.findAll(MethodDeclaration.class).get(0);
//                            }
//                        }
//                    }
//                }
//            }
////            System.out.println(methodCallExpr.getNameAsString());
//        }


        /**
         * 获取第三方变量，处理import
         */
        myClass.setName("TestFragment");
        Set<String> importTypeSet = new HashSet<>();
        cu.findAll(VariableDeclarator.class).forEach(v -> importTypeSet.add(v.getTypeAsString()));


        Map<Expression, Boolean> tempAbnormalArgumentMap = new HashMap<>(abnormalArgumentMap);
        for (Map.Entry<Expression, Boolean> entry : abnormalArgumentMap.entrySet()) {
            Expression var = entry.getKey();
            boolean isGlobal = entry.getValue();
            for (VariableTuple vt : variableList) {
                if (var.toString().equals(vt.name) && (isGlobal == vt.isGlobal)) {
                    if (vt.original.getClass().equals(FieldDeclaration.class)) {
                        FieldDeclaration fd = (FieldDeclaration) vt.original;
                        myClass.addMember(fd);
                        fragmentContent.add(fd.toString());
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
            String path = "D:/picasso/src/test/java/" + i.getNameAsString().replaceAll("\\.", "/");
            String newPath = findFile(path);
            //暂时不考虑非变量import，直接提供完整的import列表
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
            myClass.addMember(f);
            externalVariableDependency.add(f.toString());
        }
        //添加@Before
        myClass.addMember(beforeMethod);
        //添加@Test
        myClass.addMember(myMethod);


        String methodName = myMethod.getNameAsString();
        //给新的写入文件添加测试方法语句
        List<String> methodContent = new ArrayList<>();
        myMethod.getBody().get().getStatements().forEach(s -> methodContent.add(s.toString()));
        methodContent.forEach(m -> fragmentContent.add(m));
//        System.out.println(myClass.toString());
        String packageName = packageList.get(0).getNameAsString().replaceAll("\\.", "_");
//        System.err.println(packageName);

        //写文件
        writeTestClass(packageName, methodName, myMethod, writeFileImportList, myClass);
        writeTestFragment(packageName,methodName,myMethod,writeFileImportList,externalVariableDependency,fragmentContent);

    }


    //获取目录下所有文件
    public List<String> findFileList(File dir, List<String> fileNames) {

        String[] files = dir.list();// 读取目录下的所有目录文件信息
        if (files != null) {
            for (String s : files) {// 循环，添加文件名或回调自身
                File file = new File(dir, s);
                if (file.isFile()) {// 如果文件
                    fileNames.add(dir + "\\" + file.getName());// 添加文件全路径名
                } else {// 如果是目录
                    findFileList(file, fileNames);// 回调自身继续查询
                }
            }
        }
        return fileNames;
    }

    public boolean judgeArguments(String filepath, List<String> arguments){
        boolean flag = true;
        for(String s:arguments){
            if(!filepath.contains(s)){
                flag = false;
                break;
            }
        }
        return flag;
    }

    public void writeTestClass(String packageName, String methodName, MethodDeclaration myMethod, List<String> writeFileImportList, ClassOrInterfaceDeclaration myClass) {
        try {
            String[] filenameArray = FILE_PATH.split("/");
            String filename = filenameArray[filenameArray.length - 1].split("\\.")[0];
            String parameters = myMethod.getParameters().toString();
            parameters = "("+parameters.substring(1,parameters.length()-1)+")";
            String outputFileName = "Test Class/" + packageName + "_" + filename + "_" + methodName + parameters + ".txt";
//            System.out.println(outputFileName);
            BufferedWriter bw = new BufferedWriter(new FileWriter(outputFileName));
            for (String s : writeFileImportList) {
                bw.write(s);
            }
            bw.write(myClass.toString());
            bw.close();
            System.err.println("文件写入成功");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void writeTestFragment(String packageName, String methodName, MethodDeclaration myMethod, List<String> writeFileImportList, List<String> externalVariableDependency, List<String> fragmentContent) {
        try {
            String[] filenameArray = FILE_PATH.split("/");
            String filename = filenameArray[filenameArray.length - 1].split("\\.")[0];
            String parameters = myMethod.getParameters().toString();
            parameters = "("+parameters.substring(1,parameters.length()-1)+")";
            String outputFileName = "Test Fragment/" + packageName + "_" + filename + "_" + methodName + parameters + ".txt";
            BufferedWriter bw = new BufferedWriter(new FileWriter(outputFileName));
            bw.write("test fragment:{\n");
            bw.write("\timport dependency:{\n");
            for (String s : writeFileImportList) {
                bw.write("\t\t" + s);
            }
//            for(ImportDeclaration i: importPackageList){
//                bw.write("\t\t"+i.toString());
//            }
            bw.write("\t}\n}");
            bw.write("\texternal variable dependency:{\n");
            for (String s : externalVariableDependency) {
                bw.write("\t\t" + s + "\n");
            }
            bw.write("\t},\n");
            bw.write("\tfragment content:{\n");
            for (String s : fragmentContent) {
                bw.write("\t\t" + s + "\n");
            }
            bw.write("\t},\n");

            bw.close();
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
//                    System.out.println(fd.getElementType().toString());
                    newImportTypeSet.add(fd.getElementType().toString());
                    resultFieldDeclaration = fd;
//                    return fd;
                }
            }
        }

        for (String s : newImportTypeSet) {
            for (ImportDeclaration id : newImportDeclarationList) {
                String[] importNameArray = id.getNameAsString().split("\\.");
                String importName = importNameArray[importNameArray.length - 1];
                if (importName.equals(s)) {
                    resultImportDeclaration = id;
//                    System.out.println(resultImportDeclaration);
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

//    public static void main(String[] args) throws IOException {
//        TestFileAnalysis testFileAnalysis = new TestFileAnalysis();
//        testFileAnalysis.getOriginalTestFragment();
//        for (int i = 0; i < originalTestFragmentClassList.size(); i++) {
////            System.out.println("+++" + i);
////            if (i == 1) {
//            testFileAnalysis.dependencyAnalysis(i, originalTestFragmentClassList.get(i));
////            }
//        }
//    }
}

