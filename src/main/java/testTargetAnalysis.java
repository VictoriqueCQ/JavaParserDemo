import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class testTargetAnalysis {
    String ROOT_PATH = "D:/picasso/src/main/java/";
    List<FieldDeclaration> globelVariableList = null;

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

    public void findFileList(File dir, List<String> fileNames) {
        if (!dir.exists() || !dir.isDirectory()) {// 判断是否存在目录
            return;
        }
        String[] files = dir.list();// 读取目录下的所有目录文件信息
        if (files != null) {
            for (String s : files) {// 循环，添加文件名或回调自身
                File file = new File(dir, s);
                if (file.isFile()) {// 如果文件
                    fileNames.add(dir + File.separator + file.getName());// 添加文件全路径名
                } else {// 如果是目录
                    findFileList(file, fileNames);// 回调自身继续查询
                }
            }
        }
    }

    public Map<String, Integer> getTarget(String filePath) throws FileNotFoundException {
        //测试目标列表最后放在这里
        Map<String, Integer> targetMap = new HashMap<>();

        //抽象语法树
        CompilationUnit cu = constructCompilationUnit(null, filePath);

        //类中所有方法
        List<MethodDeclaration> methodDeclarationList = cu.findAll(MethodDeclaration.class);

        //内部类
        List<ClassOrInterfaceDeclaration> innerClassList = new ArrayList<>();
        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(coid -> {
//            if (!coid.getNameAsString().equals("TestFragment")) {
            innerClassList.add(coid);
//            }
        });
//        System.err.println(innerClassList.size());

        List<FieldDeclaration> fieldList = cu.findAll(FieldDeclaration.class);
        if (fieldList != null) {
            globelVariableList = fieldList;
        }
//        globelVariableList.addAll(cu.findAll(FieldDeclaration.class));

        //测试方法列表（实际上只有一个测试方法）
        List<MethodDeclaration> testMethodList = new ArrayList<>();
        cu.findAll(MethodDeclaration.class).stream().filter(md -> md.getAnnotationByName("Test").isPresent()).forEach(testMethodList::add);

        //测试方法的行范围
        int[] testMethodRange = new int[2];

        //测试方法
        MethodDeclaration testMethod = testMethodList.get(0);
        testMethodRange[0] = testMethod.getRange().get().begin.line;
        testMethodRange[1] = testMethod.getRange().get().end.line;

        //类中所有函数调用
        List<MethodCallExpr> oldMethodCallExprList = cu.findAll(MethodCallExpr.class);
        List<MethodCallExpr> methodCallExprList = new ArrayList<>();
        for (MethodCallExpr mce : oldMethodCallExprList) {
            if (mce.getRange().get().begin.line > testMethodRange[0] || mce.getRange().get().begin.line < testMethodRange[1]) {
                methodCallExprList.add(mce);
            }
        }
        List<VariableDeclarator> oldVariableDeclaratorList = cu.findAll(VariableDeclarator.class);
        List<VariableDeclarator> variableDeclaratorList = new ArrayList<>();
        for (VariableDeclarator vd : oldVariableDeclaratorList) {
            if (vd.getRange().get().begin.line > testMethodRange[0] || vd.getRange().get().begin.line < testMethodRange[1]) {
                variableDeclaratorList.add(vd);
            }
        }
        int firstAssertLine = 0;
        List<Expression> arguments;
        for (MethodCallExpr mce : methodCallExprList) {
            if (mce.getNameAsString().contains("assert")) {
                if (firstAssertLine == 0) {
                    firstAssertLine = mce.getRange().get().begin.line;
                } else {
                    if (mce.getRange().get().begin.line < firstAssertLine) {
                        firstAssertLine = mce.getRange().get().begin.line;
                    }
                }
//                System.out.println(mce.toString());
                arguments = mce.getArguments();
                if (arguments != null) {
                    Expression expression = arguments.get(0);
                    if (expression.isMethodCallExpr()) {
                        //如果是方法调用，那么就是目标那个方法
                        MethodCallExpr methodCallExpr = expression.asMethodCallExpr();
                        for (MethodDeclaration md : methodDeclarationList) {
                            int methodCallLine = 0;
                            String name = methodCallExpr.getNameAsString();
                            List<Expression> param = methodCallExpr.getArguments();
                            List<String> typeList = getVariableTypeList(param, variableDeclaratorList);
                            if (matchMethod(name, typeList, md)) {
                                methodCallLine = methodCallExpr.getRange().get().begin.line;
                                String target = fromMethodCommentToTarget(md);
                                if (!targetMap.containsKey(target)) {
//                                    System.out.println(mce+"+" + methodCallLine);
//                                    if (i > methodCallLine) {
                                    targetMap.put(target, methodCallLine);
//                                    }
                                }
                                break;
                            }
                        }
                        if (expression.asMethodCallExpr().getScope().isPresent()) {
                            Expression expression1 = expression.asMethodCallExpr().getScope().get();
                            //获取变量初始化的地方
                            VariableDeclarator variableDeclarator = getVariableInitialize(expression1, globelVariableList, variableDeclaratorList);
                            if (variableDeclarator != null && variableDeclarator.getInitializer().isPresent()) {
                                int variableInitializeLine = 0;
                                if (variableDeclarator.getInitializer().get().isMethodCallExpr()) {
//                                System.out.println(variableDeclarator);
                                    MethodCallExpr methodCallExpr1 = variableDeclarator.getInitializer().get().asMethodCallExpr();
                                    Map<String, Integer> map1 = getCallObjectMethod(methodDeclarationList, methodCallExpr1, variableDeclaratorList, firstAssertLine);
                                    if (!map1.isEmpty()) {
                                        targetMap.putAll(map1);
                                    } else {
//                                    System.out.println(expression);
//                                    System.out.println(variableDeclarator);
                                        if (methodCallExpr1.getArguments().size() != 0) {
                                            Expression expression2 = methodCallExpr1.getArgument(0);
                                            VariableDeclarator variableDeclarator1 = getVariableInitialize(expression2, globelVariableList, variableDeclaratorList);
                                            if (variableDeclarator1 != null && variableDeclarator1.getInitializer().isPresent()) {
                                                if (variableDeclarator1.getInitializer().get().isMethodCallExpr()) {
                                                    MethodCallExpr methodCallExpr2 = variableDeclarator1.getInitializer().get().asMethodCallExpr();
                                                    Map<String, Integer> map2 = getCallObjectMethod(methodDeclarationList, methodCallExpr2, variableDeclaratorList, firstAssertLine);
//                                            System.out.println(map2);
                                                    if (!map2.isEmpty()) {
//                                                System.out.println(map1.toString());
                                                        targetMap.putAll(map2);
                                                    }
                                                }
                                            }
                                        }
//                                    System.out.println(variableDeclarator1);
                                    }
                                }
                            }
                            //找到变量函数调用
                            for (MethodCallExpr methodCall : methodCallExprList) {
                                int variableLastCallLine = 0;
                                if (methodCall.getScope().isPresent()) {
                                    if (methodCall.getScope().get().toString().equals(expression.asMethodCallExpr().getScope().get().toString()) && !methodCall.equals(expression)) {
//                                    System.out.println(methodCall);
                                        Map<String, Integer> map = getVariableCallMethod(methodDeclarationList, methodCall, variableDeclaratorList, firstAssertLine);

                                        targetMap.putAll(map);
                                    }
                                }
                            }
                        }
                    } else if (expression.isNameExpr()) {
                        //如果是变量
                        int variableInitializeLine = 0;
                        int variableLastCallLine = 0;
                        //获取变量初始化的地方
                        VariableDeclarator variableDeclarator = getVariableInitialize(expression, globelVariableList, variableDeclaratorList);
                        if (variableDeclarator != null && variableDeclarator.getInitializer().isPresent()) {
                            if (variableDeclarator.getInitializer().get().isMethodCallExpr()) {
                                MethodCallExpr methodCallExpr = variableDeclarator.getInitializer().get().asMethodCallExpr();
                                Map<String, Integer> map = getCallObjectMethod(methodDeclarationList, methodCallExpr, variableDeclaratorList, firstAssertLine);
                                if (map != null) {
                                    targetMap.putAll(map);
                                }
                            } else if (variableDeclarator.getInitializer().get().isObjectCreationExpr()) {
                                ObjectCreationExpr objectCreationExpr = variableDeclarator.getInitializer().get().asObjectCreationExpr();
                                Map<String, Integer> map = getNewObjectMethod(innerClassList, objectCreationExpr, variableDeclaratorList, firstAssertLine);
                                targetMap.putAll(map);
                            }
                        }
                        //找到最后一次变量函数调用
                        for (MethodCallExpr methodCall : methodCallExprList) {
                            if (methodCall.getScope().isPresent()) {
                                if (methodCall.getScope().get().toString().equals(expression.toString())) {
                                    Map<String, Integer> map = getVariableCallMethod(methodDeclarationList, methodCall, variableDeclaratorList, firstAssertLine);
                                    targetMap.putAll(map);
                                }
                            }
                        }
                    } else if (expression.isFieldAccessExpr()) {
                        //如果是xx.xx
                        int variableInitializeLine = 0;
                        int variableLastCallLine = 0;
                        Expression var = expression.asFieldAccessExpr().getScope();
                        if (var.isMethodCallExpr()) {
                            MethodCallExpr methodCallExpr = var.asMethodCallExpr();
                            Map<String, Integer> map = getCallObjectMethod(methodDeclarationList, methodCallExpr, variableDeclaratorList, firstAssertLine);
                            if (map != null) {
                                targetMap.putAll(map);
                            }
                        } else if (var.isNameExpr()) {
                            VariableDeclarator variableDeclarator = getVariableInitialize(var, globelVariableList, variableDeclaratorList);
                            if (variableDeclarator != null && variableDeclarator.getInitializer().isPresent()) {
                                if (variableDeclarator.getInitializer().get().isMethodCallExpr()) {
                                    MethodCallExpr methodCallExpr = variableDeclarator.getInitializer().get().asMethodCallExpr();
                                    Map<String, Integer> map = getCallObjectMethod(methodDeclarationList, methodCallExpr, variableDeclaratorList, firstAssertLine);
                                    if (map != null) {
                                        targetMap.putAll(map);
                                    }
                                } else if (variableDeclarator.getInitializer().get().isObjectCreationExpr()) {
                                    ObjectCreationExpr objectCreationExpr = variableDeclarator.getInitializer().get().asObjectCreationExpr();
                                    Map<String, Integer> map = getNewObjectMethod(innerClassList, objectCreationExpr, variableDeclaratorList, firstAssertLine);
                                    targetMap.putAll(map);
                                }
                            }
                            //找到最后一次变量函数调用
                            for (MethodCallExpr methodCall : methodCallExprList) {
                                if (methodCall.getScope().isPresent()) {
                                    if (methodCall.getScope().get().toString().equals(expression.asFieldAccessExpr().getScope().toString())) {
                                        Map<String, Integer> map = getVariableCallMethod(methodDeclarationList, methodCall, variableDeclaratorList, firstAssertLine);
                                        targetMap.putAll(map);
                                    }
                                }
                            }
                        }

                    }
                }
            }
        }

        return targetMap;
    }

    public String fromMethodCommentToTarget(MethodDeclaration md) {
        String target = "";
//        System.out.println(md.getJavadoc().toString());

        String[] comment = md.getJavadoc().get().toText().split(System.lineSeparator());
        for (int i = 0; i < comment.length; i++) {
            if (comment[i].contains("packagename") || comment[i].contains("classname") || comment[i].contains("methodname") || comment[i].contains("parametertype") || comment[i].contains("fromTest")) {
                target += comment[i].split(":")[1] + "+";
            }
        }
        if (target.split("\\+")[target.split("\\+").length - 1].equals("false")) {
            return target.substring(0, target.length() - 7);
        }
        return null;
    }

    public String fromConstructorCommentToTarget(ConstructorDeclaration cd) {
        String target = "";
//        System.out.println(cd.getJavadoc().toString());

        String[] comment = cd.getJavadoc().get().toText().split(System.lineSeparator());
        for (int i = 0; i < comment.length; i++) {
            if (comment[i].contains("packagename") || comment[i].contains("classname") || comment[i].contains("methodname") || comment[i].contains("parametertype") || comment[i].contains("fromTest")) {
                target += comment[i].split(":")[1] + "+";
            }
        }
        if (target.split("\\+")[target.split("\\+").length - 1].equals("false")) {
            return target.substring(0, target.length() - 7);
        }
        return null;
    }

    public Map<String, Integer> getVariableCallMethod(List<MethodDeclaration> methodDeclarationList, MethodCallExpr methodCall, List<VariableDeclarator> variableDeclaratorList, int firstAssertLine) {
        Map<String, Integer> targetMap = new HashMap<>();

        for (MethodDeclaration md : methodDeclarationList) {
            String name = methodCall.getNameAsString();
            List<Expression> param = methodCall.getArguments();
            List<String> typeList = getVariableTypeList(param, variableDeclaratorList);
            if (matchMethod(name, typeList, md)) {
                int variableLastCallLine = methodCall.getRange().get().begin.line;
                String target = fromMethodCommentToTarget(md);
                if (!targetMap.containsKey(target)) {
                    targetMap.put(target, variableLastCallLine);

                }
                break;
            }
        }
        return targetMap;
    }

    public Map<String, Integer> getCallObjectMethod(List<MethodDeclaration> methodDeclarationList, MethodCallExpr methodCallExpr, List<VariableDeclarator> variableDeclaratorList, int firstAssertLine) {
        Map<String, Integer> targetMap = new HashMap<>();

        for (MethodDeclaration md : methodDeclarationList) {
            String name = methodCallExpr.getNameAsString();
            List<Expression> param = methodCallExpr.getArguments();
            List<String> typeList = getVariableTypeList(param, variableDeclaratorList);
            if (matchMethod(name, typeList, md)) {
                int variableInitializeLine = methodCallExpr.getRange().get().begin.line;
                String target = fromMethodCommentToTarget(md);
                if (!targetMap.containsKey(target)) {
                    targetMap.put(target, variableInitializeLine);
                }
                break;
            }
        }
        return targetMap;
    }

    public Map<String, Integer> getNewObjectMethod(List<ClassOrInterfaceDeclaration> innerClassList, ObjectCreationExpr objectCreationExpr, List<VariableDeclarator> variableDeclaratorList, int firstAssertLine) {
        Map<String, Integer> targetMap = new HashMap<>();
        for (ClassOrInterfaceDeclaration coid : innerClassList) {
            if (objectCreationExpr.getTypeAsString().equals(coid.getNameAsString())) {
                List<Expression> param = objectCreationExpr.getArguments();
                List<String> typeList = getVariableTypeList(param, variableDeclaratorList);
                List<ConstructorDeclaration> constructorDeclarationList = coid.getConstructors();
                for (ConstructorDeclaration cd : constructorDeclarationList) {
                    List<Parameter> parameters = cd.getParameters();
                    List<String> parametersType = new ArrayList<>();
                    for (Parameter p : parameters) {
                        parametersType.add(p.getTypeAsString());
                    }
                    boolean flag = true;
                    if (typeList.size() == parametersType.size()) {
                        for (int i = 0; i < typeList.size(); i++) {
                            if (typeList.get(i) != null) {
                                if (!typeList.get(i).equals(parametersType.get(i))) {
                                    flag = false;
                                    break;
                                }
                            }
                        }
                    }
                    if (flag) {
                        int variableInitializeLine = objectCreationExpr.getRange().get().begin.line;
                        String target = fromConstructorCommentToTarget(cd);
                        if (!targetMap.containsKey(target)) {
                            targetMap.put(target, variableInitializeLine);
                        }
                        break;
                    }
                }
            }
        }
        return targetMap;
    }

    public boolean matchMethod(String methodName, List<String> typeList, MethodDeclaration methodDeclaration) {
        boolean flag = true;
        if (!methodDeclaration.getAnnotationByName("Test").isPresent()) {
            String name = methodDeclaration.getNameAsString();
            List<Parameter> parameterList = methodDeclaration.getParameters();
            List<String> parameterString = new ArrayList<>();
            for (Parameter p : parameterList) {
                parameterString.add(p.getTypeAsString());
            }
//        System.out.println(methodName);
            if (!methodName.equals(name)) {
                flag = false;
            } else if (typeList.size() != parameterString.size()) {
                flag = false;
            } else {
                for (int i = 0; i < typeList.size(); i++) {
                    if (typeList.get(i) != null) {
                        if (!typeList.get(i).equals(parameterString.get(i))) {
                            flag = false;
                            break;
                        }
                    }
                }
            }
        } else {
            flag = false;
        }
        return flag;
    }

    public List<String> getVariableTypeList(List<Expression> arguments, List<VariableDeclarator> variableDeclarators) {
        List<String> variableTypeList = new ArrayList<>();
        for (Expression expression1 : arguments) {
            VariableDeclarator vd2 = getVariableInitialize(expression1, globelVariableList, variableDeclarators);
            if (vd2 != null) {
                variableTypeList.add(vd2.getTypeAsString());
            } else {
                variableTypeList.add(getBasicDataType(expression1));
            }
        }
        return variableTypeList;
    }

    public VariableDeclarator getVariableInitialize(Expression expression, List<FieldDeclaration> fieldDeclarations, List<VariableDeclarator> variableDeclarators) {
        VariableDeclarator result = null;
        for (VariableDeclarator vd : variableDeclarators) {
            if (vd.getNameAsExpression().equals(expression)) {
                result = vd;
                break;
            }
        }
        for (FieldDeclaration fd : fieldDeclarations) {
            if (fd.getVariable(0).getNameAsExpression().equals(expression)) {
                result = fd.getVariable(0);
                break;
            }
        }
        return result;
    }

    public String getBasicDataType(Expression expression) {
        String type = null;
        if (expression.isIntegerLiteralExpr()) {
            type = "int";
        } else if (expression.isLongLiteralExpr()) {
            type = "long";
        } else if (expression.isCharLiteralExpr()) {
            type = "char";
        } else if (expression.isDoubleLiteralExpr()) {
            type = "double";
        } else if (expression.isStringLiteralExpr()) {
            type = "String";
        } else if (expression.isBooleanLiteralExpr()) {
            type = "boolean";
        }
        return type;
    }

    public static Map<Integer, MethodCallExpr> sortMapByKey(Map<Integer, MethodCallExpr> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
//        Map<String, String> sortMap = new TreeMap<String, String>(new MapKeyComparator());
        //降序排序
        Map<Integer, MethodCallExpr> sortMap = new TreeMap<>(Comparator.reverseOrder());
        sortMap.putAll(map);
        return sortMap;
    }

    public boolean isInTest(int[] range, int line) {
        boolean flag = false;
        if (line >= range[0] && line <= range[1]) {
            flag = true;
        }
        return flag;
    }

    public static void main(String[] args) throws FileNotFoundException {
        testTargetAnalysis tta = new testTargetAnalysis();
        List<String> fileNames = new ArrayList<String>();
        tta.findFileList(new File("C:/Users/77627/IdeaProjects/JavaParserDemo/Test Class"), fileNames);
        int count = 0;
        for (int i = 0; i < fileNames.size(); i++) {
//            if (i == 217) {
            Map<String, Integer> result = tta.getTarget(fileNames.get(i));
            System.out.println(i);
            System.out.println(fileNames.get(i));
            if (!result.isEmpty()) {
                count += 1;
//            if (fileNames.get(i).contains("rotation90WithPivotSizing")) {
//                System.out.println("+++" + i + "+++" + fileNames.get(i));
//            }
                for (Map.Entry<String, Integer> entry : result.entrySet()) {
                    System.out.println(entry.getKey() + "line:" + entry.getValue());
                }

//            }

//            }
            }
            System.out.println("count:" + count);
            System.out.println("===================================");
        }
    }
}
