import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

/**
  * @Author sunweisong
  * @Date 2020/3/6 12:47 AM
  */
public class TestGranularityAnalysis {

    String ROOT_PATH = "/Users/sunweisong/Desktop/picasso/src/main/java";//"src\\main\\java"
    List<FieldDeclaration> globelVariableList = new ArrayList<>();
    MethodDeclaration beforeMethod = null;
    MethodDeclaration afterMethod = null;
    List<MethodDeclaration> normalMethodList = new ArrayList<>();
    List<String> normalMethodNameList = new ArrayList<>();

    /**
      * 测试粒度还原测试入口
      * @date 2020/3/9 11:26 PM
      * @author sunweisong
      */
    public void test(String[] args) {
        String filePath = "/Users/sunweisong/Desktop/TestClass/com.squareup.picasso3+BitmapHunterTest_+cancelSingleRequest+().txt";
        List<String> testTargetList = new ArrayList<>();
        testTargetList.add("com.squareup.picasso3+BitmapHunter+isCancelled+()");
        testTargetList.add("com.squareup.picasso3+BitmapHunter+cancel+()");
        testTargetList.add("com.squareup.picasso3+BitmapHunter+detach+()");
        try {
            CompilationUnit cu = constructCompilationUnit(null, filePath);
            List<ClassOrInterfaceDeclaration> myClassList = reduceTestGranularity(cu, testTargetList);
            if (myClassList.size() > 0) {
                System.out.println("Total Number of Test Cases: " + myClassList.size());
                System.out.println("---------------------------------------");
                for (ClassOrInterfaceDeclaration myClass : myClassList) {
                    System.out.println(myClass.toString());
                    System.out.println("---------------------------------------");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
      * Create the CompilationUnit.
      * @param code
      * @param filePath
      * @return CompilationUnit
      * @throws FileNotFoundException
      * @date 2020/3/9 11:25 PM
      * @author sunweisong
      */
    public CompilationUnit constructCompilationUnit(String code, String filePath)
            throws FileNotFoundException {
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        TypeSolver javaParserTypeSolver = new JavaParserTypeSolver(new File(ROOT_PATH));
        combinedTypeSolver.add(javaParserTypeSolver);
        TypeSolver reflectionTypeSolver = new ReflectionTypeSolver();
        combinedTypeSolver.add(reflectionTypeSolver);
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
        StaticJavaParser.getConfiguration().setSymbolResolver(symbolSolver);

        return code == null ? StaticJavaParser.parse(new File(filePath)) : StaticJavaParser.parse(code);
    }

    /**
      * Reduce the granularity of the test fragment.
      * @param cu
      * @param testTargetList
      * @return List<ClassOrInterfaceDeclaration>
      * @throws IOException
      * @date 2020/3/9 11:24 PM
      * @author sunweisong
      */
    public List<ClassOrInterfaceDeclaration> reduceTestGranularity(CompilationUnit cu
            , List<String> testTargetList) throws IOException {
        //获取类变量列表（包含可能的内部类的变量）
        List<FieldDeclaration> fieldDeclarationList = cu.findAll(FieldDeclaration.class);
        fieldDeclarationList.forEach(fd -> {
            globelVariableList.add(fd);
        });

        // 获取类中所有函数
        cu.findAll(MethodDeclaration.class).stream().forEach(normalMethodList::add);

        /**
         * 分析函数注解进一步划分：
         * BeforeClass/BeforeAll
         * Before/BeforeEach
         * Test
         * After/AfterEach
         * AfterClass/AfterAll
         */
        MethodDeclaration testMethodDeclaration = null;
        Iterator<MethodDeclaration> iterator = normalMethodList.iterator();
        while (iterator.hasNext()) {
            MethodDeclaration md = iterator.next();
            if (md.getAnnotationByName("Before").isPresent()
                    || md.getAnnotationByName("BeforeEach").isPresent()) {
                beforeMethod = md;
                iterator.remove();
                continue;
            }
            if (md.getAnnotationByName("Test").isPresent()) {
                testMethodDeclaration = md;
                iterator.remove();
                continue;
            }
            if (md.getAnnotationByName("After").isPresent()
                    || md.getAnnotationByName("AfterEach").isPresent()) {
                afterMethod = md;
                iterator.remove();
                continue;
            }
            normalMethodNameList.add(md.getNameAsString());
        }

        /**
         * Analyzing all statements appearing in the test method.
         */
        List<StatementModel> statementModelList = analyzeAllStatementsInTestMethod(testMethodDeclaration);

        /**
         * Analyzing all context-related statements for the assert statement.
         */
        contextRelatedStatementAnalysis(statementModelList);

        /**
         * Reducing the test granularity.
         */
        reduceTestFragmentGranularity(statementModelList, testTargetList);

        /**
         * Producing the Test Class for all test fragments.
         */
        List<ClassOrInterfaceDeclaration> myClassList = produceTestClassForTestFragment(statementModelList);
        if (myClassList.size() == 0) {
            myClassList = null;
        }
        return myClassList;
    }

    /**
      * Produce the Test Class for each Test Fragment.
      * @param statementModelList
      * @return List<ClassOrInterfaceDeclaration>
      * @date 2020/3/9 4:35 PM
      * @author sunweisong
      */
    private List<ClassOrInterfaceDeclaration> produceTestClassForTestFragment(List<StatementModel> statementModelList) {
        List<ClassOrInterfaceDeclaration> myClassList = new ArrayList<>();
        for (int i = 0; i < statementModelList.size(); i++ ) {
            StatementModel statementModel = statementModelList.get(i);
            String realTestTarget = statementModel.getRealTestTarget();
            if (!statementModel.isAssertStatement()) {
                continue;
            }
            if (statementModel.getRealTestTarget() == null) {
                System.err.println(statementModel.toString());
                continue;
            }
            if (statementModel.getIsCreated()) {
                continue;
            }
            List<StatementModel> relatedStatementModelList = statementModel.getContextRelatedStatementModelList();
            if (relatedStatementModelList == null) {
                continue;
            }
            statementModel.setIsCreated(true);
            for (int j = i + 1; j < statementModelList.size(); j++ ) {
                StatementModel tempStatementModel = statementModelList.get(j);
                if (!tempStatementModel.isAssertStatement()) {
                    continue;
                }
                if (tempStatementModel.getRealTestTarget() == null) {
                    continue;
                }
                String tempRealTestTarget = tempStatementModel.getRealTestTarget();
                if (!tempRealTestTarget.equals(realTestTarget)) {
                    continue;
                }
                if (tempStatementModel.getIsCreated()) {
                    continue;
                }
                tempStatementModel.setIsCreated(true);
                relatedStatementModelList.add(tempStatementModel);
                List<StatementModel> tempStatementModelList = tempStatementModel.getContextRelatedStatementModelList();
                if (tempStatementModelList == null) {
                    continue;
                }
                boolean flag = false;
                for (StatementModel temp1 : tempStatementModelList) {
                    for (StatementModel temp : relatedStatementModelList) {
                        if (temp.getLine() == temp1.getLine()) {
                            flag = true;
                            break;
                        }
                    }
                    if (flag) {
                        continue;
                    }
                    relatedStatementModelList.add(temp1);
                    flag = false;
                }
            }

            ClassOrInterfaceDeclaration myClass = assembleClassOrInterfaceDeclaration(statementModel
                    , relatedStatementModelList);
            myClassList.add(myClass);
        }
        return myClassList;
    }

    /**
      * Assemble the ClassOrInterfaceDeclaration.
      * @param statementModel
      * @param relatedStatementModelList
      * @return ClassOrInterfaceDeclaration
      * @date 2020/3/9 11:23 PM
      * @author sunweisong
      */
    private ClassOrInterfaceDeclaration assembleClassOrInterfaceDeclaration(StatementModel statementModel
            , List<StatementModel> relatedStatementModelList) {
        //初始化ClassOrInterfaceDeclaration,用于构造新的测试片段类
        ClassOrInterfaceDeclaration myClass = new ClassOrInterfaceDeclaration();
        myClass.setName("TestFragment");

        for (FieldDeclaration globalFieldDeclaration : globelVariableList) {
            myClass.addMember(globalFieldDeclaration);
        }

        if (beforeMethod != null) {
            myClass.addMember(beforeMethod);
        }

        String testTarget = statementModel.getRealTestTarget();
        String[] elementArray = testTarget.split("\\+");
        String testTargetName = elementArray[2];
        StringBuffer stringBuffer = new StringBuffer(testTargetName);
        char firstChar = stringBuffer.charAt(0);
        if (firstChar >= 'a' && firstChar <= 'z') {
            firstChar -= 32;
            stringBuffer.replace(0, 1, firstChar + "");
        }
        testTargetName = "test" + stringBuffer.toString();
        stringBuffer = null;

        //添加@Test
        MethodDeclaration tempTestMethodDeclaration = new MethodDeclaration();
        tempTestMethodDeclaration.addMarkerAnnotation("Test");
        tempTestMethodDeclaration.setName(testTargetName);
        tempTestMethodDeclaration.setType("void");
        tempTestMethodDeclaration.setPublic(true);

        BlockStmt blockStmt = new BlockStmt();

        int[] lineArray = new int[relatedStatementModelList.size()];
        for (int j = 0; j < relatedStatementModelList.size(); j++) {
            lineArray[j] = relatedStatementModelList.get(j).getLine();
        }
        Arrays.sort(lineArray);
        for (int j = 0; j < lineArray.length; j++) {
            for (StatementModel statementModel1 :relatedStatementModelList) {
                int tempLine = statementModel1.getLine();
                if (tempLine != lineArray[j]) {
                    continue;
                }
                blockStmt.addStatement(statementModel1.getContent());
            }
        }
        blockStmt.addStatement(statementModel.getContent());
        tempTestMethodDeclaration.setBody(blockStmt);
        myClass.addMember(tempTestMethodDeclaration);

        for (MethodDeclaration methodDeclaration : normalMethodList) {
            myClass.addMember(methodDeclaration);
        }
        if (afterMethod != null) {
            myClass.addMember(afterMethod);
        }
        return myClass;
    }

    /**
      * Analyzing all statements in the test method.
      * @param testMethodDeclaration
      * @return List<StatementModel>
      * @date 2020/3/9 9:02 PM
      * @author sunweisong
      */
    private List<StatementModel> analyzeAllStatementsInTestMethod(MethodDeclaration testMethodDeclaration) {
        List<StatementModel> statementModelList = new ArrayList<>();
        NodeList<Statement> statements = testMethodDeclaration.getBody().get().getStatements();
        for (int i = 0; i < statements.size(); i++) {
            Statement statement = statements.get(i);
            String content = statement.toString();
            StatementModel statementModel = new StatementModel(i, content);
            String regex = "^(assert|Assert.assert)[A,D,E,F,I,L,N,S,T][a-zA-Z]*\\(.*?\\);$";
            if (content.matches(regex)) {
                statementModel.setAssertStatement(true);
            }
            // analyze the declared variable existed in the VariableDeclarationExpr.
            List<VariableDeclarationExpr> variableDeclarationExprs = statement.findAll(VariableDeclarationExpr.class);
            if (variableDeclarationExprs.size() > 0) {
                Set<String> declareVariableSet = new HashSet<>();
                for (VariableDeclarationExpr vd :variableDeclarationExprs) {
                    List<VariableDeclarator> variables = vd.getVariables();
                    for (VariableDeclarator variableDeclarator : variables) {
                        declareVariableSet.add(variableDeclarator.getName().asString());
                    }
                }
                statementModel.setDeclareVariableSet(declareVariableSet);
            }

            Set<String> useVariableSet = analyzeUseVariable(statement, statementModel);
            if (useVariableSet.size() > 0) {
                statementModel.setUseVariableSet(useVariableSet);
            }

            if (statementModel.isAssertStatement()) {
                analyzeSuspiciousTestTarget(statementModel);
            }
            statementModelList.add(statementModel);
        }
        return statementModelList;
    }


    /**
      * Analyze the suspicious test target existed in the statements.
      * @param statementModel
      * @return void
      * @date 2020/3/9 9:35 PM
      * @author sunweisong
      */
    private void analyzeSuspiciousTestTarget(StatementModel statementModel) {
        MethodCallExpr mce = statementModel.getMethodCallExpr();
        List<Expression> arguments = mce.getArguments();
        Expression actualExpression;
        String suspiciousTestTarget = null;
        if (arguments.size() == 1) {
            // assertThat(target), target ia an actual value.
            actualExpression = arguments.get(0);
            suspiciousTestTarget = analyzeSuspiciousTestTargetInActual(actualExpression);
        } else {
            //后面根据 JUnit3、4、5 API 判断 actual 在 assert 函数中的位置
            // asssertEquals(expected, actual)
            // suspiciousTestTarget = analyzeSuspiciousTestTargetInActual(actualExpression);
        }
        statementModel.setSuspiciousTestTarget(suspiciousTestTarget);
    }

    /**
      * Analyze the suspicious test target in the actual part of assert methods.
      * @param actualExpression
      * @return String
      * @date 2020/3/9 11:02 PM
      * @author sunweisong
      */
    private String analyzeSuspiciousTestTargetInActual(Expression actualExpression) {
        String suspiciousTestTarget = null;
        if (actualExpression.isMethodCallExpr()) {
            String className = "";
            MethodCallExpr argumentAsMCE = actualExpression.asMethodCallExpr();
            String methodName = argumentAsMCE.getNameAsString();
            if (!argumentAsMCE.getScope().isPresent()) {
                // 2 cases: (1) testing a static method; (2) this method was written by tester.
                // format: assertThat(xxx());
                className = "T"; // 静态函数分析import
                if (!normalMethodNameList.contains(methodName)){
                    methodName = "T";
                }
            } else {
                Expression scope = argumentAsMCE.getScope().get();
                String scopeString = scope.toString();
                if (scope.isMethodCallExpr()) {
                    boolean flag = false;
                    MethodCallExpr targetMethodCallExpr = null;
                    // format: assertThat(xxx().yyy()); scope=xxx(). success
                    if (!normalMethodNameList.contains(methodName)) {
                        // yyy couldn't be found in normalMethodNameList.
                        while (scope.isMethodCallExpr()) {
                            targetMethodCallExpr = scope.asMethodCallExpr();
                            String methodName1 = targetMethodCallExpr.getNameAsString();
                            if (normalMethodNameList.contains(methodName1)) {
                                flag = true;
                                break;
                            }
                            if (targetMethodCallExpr.getScope().isPresent()) {
                                scope = targetMethodCallExpr.getScope().get();
                                continue;
                            }
                            break;
                        }
                    }
                    if (flag) {
                        // xxx may be found in normalMethodNameList.
                        argumentAsMCE = targetMethodCallExpr;
                        if (argumentAsMCE.getScope().isPresent()) {
                            scope = argumentAsMCE.getScope().get();
                            if (!scope.isMethodCallExpr()) {
                                if (scope.toString().contains(".")) {
                                    // format: assertThat(aaa.bbb.xxx().yyy()); scope=aaa.bbb.xxx()
                                    /**
                                     * the class referenced by bbb is too difficult to analyze,
                                     * so we replace the class name with the wildcard 'T'.
                                     */
                                    className = "T";
                                } else {
                                    // format: assertThat(aaa.xxx().yyy()); scope=aaa.xxx(). success
                                    try {
                                        className = scope.calculateResolvedType().toString();
                                    } catch (UnsolvedSymbolException exception) {
                                        className = exception.getName();
                                    }
                                }
                            } else {
                                // format: assertThat(xxx().yyy()); scope=xxx(). success
                                /**
                                 * In this case, we don't know the class referenced by the method xxx(),
                                 * so we replace the class name with the wildcard 'T'.
                                 */
                                className = "T";
                            }
                        } else {
                            // the same as the format: assertThat(xxx());
                            className = "T";
                        }
                        methodName = argumentAsMCE.getNameAsString();
                    } else {
                        /**
                         *  Both yyy and xxx couldn't be found in normalMethodNameList,
                         *  so we replace the class name with the wildcard 'T'.
                         */
                        className = "T";
                        methodName = "T";
                    }
                } else if (scope.isObjectCreationExpr()) {
                    // format: assertThat(new XXX().yyy()); scope=new XXX(). success
                    ObjectCreationExpr objectCreationExpr = scope.asObjectCreationExpr();
                    className= objectCreationExpr.getTypeAsString();
                } else {
                    if (scopeString.contains(".")) {
                        // format: assertThat(xxx.yyy.zzz()); scope=xxx.yyy
                    } else {
                        // format: assertThat(xxx.yyy()); scope=xxx. success
                        try {
                            className = scope.calculateResolvedType().toString();
                        } catch (UnsolvedSymbolException exception) {
                            className = exception.getName();
                        }
                    }
                }
            }
            StringBuffer buffer = new StringBuffer();
            buffer.append(className + "+");
            buffer.append(methodName + "+(");
            NodeList<Expression> argsExpressionList = argumentAsMCE.getArguments();
            if (argsExpressionList.size() > 0) {
                for (Expression argsExpression : argsExpressionList) {
                    String argType;
                    try {
                        argType = argsExpression.calculateResolvedType().toString();
                    } catch (UnsolvedSymbolException exception) {
                        argType = exception.getName();
                    }
                    buffer.append(argType + ",");
                }
                buffer.replace(buffer.length() - 1, buffer.length(), "");
            }
            buffer.append(")");
            suspiciousTestTarget = buffer.toString();
        } else if (actualExpression.isObjectCreationExpr()) {
            // format: assertThat(new X()); testing the constructor. success
            ObjectCreationExpr objectCreationExpr = actualExpression.asObjectCreationExpr();
            String constructorName = objectCreationExpr.getTypeAsString();
            StringBuffer buffer = new StringBuffer();
            buffer.append(constructorName + "+" + constructorName + "+(");
            NodeList<Expression> argsExpressionList = objectCreationExpr.getArguments();
            if (argsExpressionList.size() > 0) {
                for (Expression argsExpression : argsExpressionList) {
                    String argType;
                    try {
                        argType = argsExpression.calculateResolvedType().toString();
                    } catch (UnsolvedSymbolException exception) {
                        argType = exception.getName();
                    }
                    buffer.append(argType + ",");
                }
                buffer.replace(buffer.length() - 1, buffer.length(), "");
            }
            buffer.append(")");
            suspiciousTestTarget = buffer.toString();
        } else if (actualExpression.isLiteralExpr()){
            // format: assertThat(1.25);
            // System.out.println("$" + actualExpression.toString());
        } else {
            // format: assertThat(xxx); xxx is a variable.
            // System.out.println("*" + actualExpression.toString());
        }
        return suspiciousTestTarget;
    }

    /**
      * Analyzing the usage variable flow.
      * @param statement
      * @param statementModel
      * @return Set<String>
      * @date 2020/3/9 9:17 PM
      * @author sunweisong
      */
    private static Set<String> analyzeUseVariable(Statement statement, StatementModel statementModel) {
        Set<String> useVariableSet = new HashSet<>();
        // analyze the used variable existed in the MethodCallExpr.
        List<MethodCallExpr> methodCallExprs = statement.findAll(MethodCallExpr.class);
        if (methodCallExprs.size() > 0) {
            if (methodCallExprs.size() == 1) {
                extractUseVariableFromMethodCallExpr(methodCallExprs.get(0), useVariableSet);
                if (statementModel.isAssertStatement()) {
                    // only assert function
                    statementModel.setMethodCallExpr(methodCallExprs.get(0));
                }
            } else {
                for (MethodCallExpr methodCallExpr : methodCallExprs) {
                    extractUseVariableFromMethodCallExpr(methodCallExpr, useVariableSet);
                    if (statementModel.isAssertStatement()) {
                        boolean flag = false;
                        String methodString = methodCallExpr.toString();
                        String back = methodString;
                        int end = methodString.indexOf(")");
                        StringBuffer buffer = new StringBuffer();
                        while (end != -1) {
                            buffer.append(methodString.substring(0, end + 1));
                            String temp = buffer.toString();
                            if (temp.contains("\"")) {
                                temp = StringUtil.removeContentsInQuotes(temp);
                            }
                            if (!StringUtil.isParenthesesMatchInString(temp)) {
                                methodString = methodString.substring(end + 1);
                                end = methodString.indexOf(")");
                                continue;
                            }
                            if (end + 1 == methodString.length()) {
                                flag = true;
                            }
                            break;
                        }
                        if (back.matches("^(assert|Assert.assert).*?") && flag) {
                            statementModel.setMethodCallExpr(methodCallExpr);
                        }
                    }
                }
            }
        }
        // analyze the used variable existed in the ObjectCreationExpr.
        List<ObjectCreationExpr> objectCreationExprs = statement.findAll(ObjectCreationExpr.class);
        if (objectCreationExprs.size() > 0) {
            for (ObjectCreationExpr objectCreationExpr: objectCreationExprs) {
                extractUseVariableFromObjectCreationExpr(objectCreationExpr, useVariableSet);
            }
        }
        // analyze the used variable existed in the AssignExpr.
        List<AssignExpr> assignExprs = statement.findAll(AssignExpr.class);
        if (assignExprs.size() > 0) {
            for (AssignExpr assignExpr : assignExprs) {
                String assignTarget = assignExpr.getTarget().toString();
                int pointIndex = assignTarget.indexOf(".");
                if (pointIndex != -1) {
                    // format: xxx.xxx
                    useVariableSet.add(assignTarget.substring(0, pointIndex));
                } else {
                    useVariableSet.add(assignTarget);
                }
            }
        }
        return useVariableSet;
    }


    /**
      * Reduce the granularity of the test fragments.
      * @param statementModelList
      * @param testTargetList
      * @return void
      * @date 2020/3/9 10:40 PM
      * @author sunweisong
      */
    private static void reduceTestFragmentGranularity(List<StatementModel> statementModelList
            , List<String> testTargetList) {
        // resolve format: assertThat(xxx.yyy()) or assertThat(new XXX().yyy())
        for (StatementModel statementModel : statementModelList) {
            if (!statementModel.isAssertStatement()) {
                continue;
            }
            String suspiciousTestTarget = statementModel.getSuspiciousTestTarget();
            if (suspiciousTestTarget == null) {
                continue;
            }
            String[] elementArray = suspiciousTestTarget.split("\\+");
            String suspiciousMethodName = elementArray[1];
            if ("T".equals(suspiciousMethodName)) {
                continue;
            }
            List<String> targetMethodNameList = new ArrayList<>();
            for (String realTestTarget : testTargetList) {
                // realTestTarget format such as: com.xxx.xxx+BitmapHunter+attach+();
                String[] realElementArray = realTestTarget.split("\\+");
                String realMethodName = realElementArray[2];
                if (realMethodName.equals(suspiciousMethodName)) {
                    targetMethodNameList.add(realTestTarget);
                }
            }
            if (targetMethodNameList.size() == 0) {
                // format: assertThat(xxx)
                continue;
            }
            if (targetMethodNameList.size() == 1) {
                String temp = targetMethodNameList.get(0);
                statementModel.setRealTestTarget(temp);
                continue;
            }
            if (targetMethodNameList.size() > 1) {
                // the number of methods that have the same method name with suspiciousTestTarget.
                String className = elementArray[0];
                if ("T".equals(className)) {
                    continue;
                }
                Iterator<String> iterator = targetMethodNameList.iterator();
                while (iterator.hasNext()) {
                    String className1 = iterator.next();
                    if (!className.equals(className1)) {
                        iterator.remove();
                    }
                }
                // matching by ClassName + MethodName
                if (targetMethodNameList.size() == 1) {
                    String temp = targetMethodNameList.get(0);
                    statementModel.setRealTestTarget(temp);
                    continue;
                }
                if (targetMethodNameList.size() > 1) {
                    // the number of methods that have the same ClassName + MethodName with suspiciousTestTarget.
                    String parameterType = elementArray[2];
                    for (String targetMethodName: targetMethodNameList) {
                        String parameterType1 = targetMethodName.split("\\+")[3];
                        if (parameterType.equals(parameterType1)) {
                            // please note: exclude PackageName!!!
                            // matching with: ClassName+MethodName+(ParameterType)
                            statementModel.setRealTestTarget(targetMethodName);
                            break;
                        }
                    }
                }
            }
        }

        // resolve format: assertThat(xxx);
        Set<String> realTestTargetSet = new HashSet<>();
        for (StatementModel statementModel : statementModelList) {
            if (statementModel.getRealTestTarget() != null) {
                realTestTargetSet.add(statementModel.getRealTestTarget());
            }
        }
        Iterator<String> iterator = testTargetList.iterator();
        while (iterator.hasNext()) {
            String testTarget = iterator.next();
            if (!realTestTargetSet.contains(testTarget)) {
                continue;
            }
            iterator.remove();
        }
        analyzeTestFragmentForRestTestTarget(statementModelList, testTargetList);
    }

    /**
      * Analyze the test fragment for the rest test target.
      * @param statementModelList
      * @param testTargetList
      * @return void
      * @date 2020/3/9 11:18 PM
      * @author sunweisong
      */
    private static void analyzeTestFragmentForRestTestTarget(List<StatementModel> statementModelList
            , List<String> testTargetList) {
        for (StatementModel statementModel : statementModelList) {
            if (!statementModel.isAssertStatement()) {
                continue;
            }
            if (statementModel.getRealTestTarget() == null) {
                MethodCallExpr mce = statementModel.getMethodCallExpr();
                Expression argumentExpression = mce.getArguments().get(0);
                if (!argumentExpression.isMethodCallExpr() && !argumentExpression.isObjectCreationExpr()) {
                    String argument = argumentExpression.toString();
                    int end=0;
                    if ((end = argument.indexOf(".")) != -1) {
                        argument = argument.substring(0, end);
                    }
                    List<StatementModel> relatedStatementModels = statementModel.getContextRelatedStatementModelList();
                    if (relatedStatementModels != null) {
                        for (StatementModel relatedStatedModel : relatedStatementModels) {
                            String content = relatedStatedModel.getContent();
                            if (!content.contains(argument)) {
                                continue;
                            }
                            boolean flag = false;
                            Iterator<String> iterator1 = testTargetList.iterator();
                            while (iterator1.hasNext()) {
                                String testTarget = iterator1.next();
                                String realTestMethodName = testTarget.split("\\+")[2];
                                if (content.contains(realTestMethodName)) {
                                    statementModel.setRealTestTarget(testTarget);
                                    flag = true;
                                    iterator1.remove();
                                    break;
                                }
                            }
                            if (flag) {
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
      * Extract the usage variable from ObjectCreationExpr
      * @param objectCreationExpr
      * @param useVariableSet
      * @return void
      * @date 2020/3/9 10:41 PM
      * @author sunweisong
      */
    private static void extractUseVariableFromObjectCreationExpr(ObjectCreationExpr objectCreationExpr
            , Set<String> useVariableSet) {
        NodeList<Expression> expressions = objectCreationExpr.getArguments();
        if (expressions.size() > 0) {
            for (Expression expression : expressions) {
                if (expression.isMethodCallExpr()) {
                    extractUseVariableFromMethodCallExpr(expression.asMethodCallExpr(), useVariableSet);
                    continue;
                }
                if (expression.isObjectCreationExpr()) {
                    extractUseVariableFromObjectCreationExpr(expression.asObjectCreationExpr(), useVariableSet);
                    continue;
                }
                String variableStr = expression.toString();
                int pointIndex = variableStr.indexOf(".");
                if (pointIndex != -1) {
                    // format: xxx.xxx
                    useVariableSet.add(variableStr.substring(0, pointIndex));
                } else {
                    useVariableSet.add(variableStr);
                }
            }
        }
    }

    /**
      * Extract the usage variable from MethodCallExpr.
      * @param methodCallExpr
      * @param useVariableSet
      * @return void
      * @date 2020/3/9 10:41 PM
      * @author sunweisong
      */
    private static void extractUseVariableFromMethodCallExpr(MethodCallExpr methodCallExpr
            , Set<String> useVariableSet) {
        NodeList<Expression> expressions = methodCallExpr.getArguments();
        if (expressions.size() == 0) {
            return;
        }
        for (Expression expression : expressions) {
            if (expression.isMethodCallExpr()) {
                MethodCallExpr mce1 = expression.asMethodCallExpr();
                if (mce1.getScope().isPresent()) {
                    Expression mceScope = mce1.getScope().get();
                    extractUseVariableFromMethodScope(mceScope, useVariableSet);
                }
            } else if (expression.isObjectCreationExpr()) {
                extractUseVariableFromObjectCreationExpr(expression.asObjectCreationExpr(), useVariableSet);
            } else {
                useVariableSet.add(expression.toString());
            }
        }
        if (methodCallExpr.getScope().isPresent()) {
            Expression mceScope = methodCallExpr.getScope().get();
            extractUseVariableFromMethodScope(mceScope, useVariableSet);
        }
    }

    /**
      * Extract the usage variable from the method scope.
      * @param mceScope
      * @param useVariableSet
      * @return void
      * @date 2020/3/9 10:42 PM
      * @author sunweisong
      */
    private static void extractUseVariableFromMethodScope(Expression mceScope
            , Set<String> useVariableSet) {
        if (mceScope.isMethodCallExpr()) {
            extractUseVariableFromMethodCallExpr(mceScope.asMethodCallExpr(), useVariableSet);
        } else if (mceScope.isObjectCreationExpr()) {
            // format: new X().xxx();
            extractUseVariableFromObjectCreationExpr(mceScope.asObjectCreationExpr(), useVariableSet);
        } else {
            String obj = mceScope.toString();
            useVariableSet.add(obj);
        }
    }

    /**
      * Analyze the context-related statements.
      * @param statementModelList
      * @return void
      * @date 2020/3/9 10:43 PM
      * @author sunweisong
      */
    private static void contextRelatedStatementAnalysis(List<StatementModel> statementModelList) {
        for (StatementModel statementModel : statementModelList) {
            if (statementModel.isAssertStatement()) {
                Set<String> useVariableSet = statementModel.getUseVariableSet();
                if (useVariableSet == null) {
                    continue;
                }
                int line = statementModel.getLine();
                List<StatementModel> tempStatementModelList = new ArrayList<>();
                Set<String> tempSet = new HashSet<>();
                tempSet.addAll(useVariableSet);
                Set<String> indirectUseVariableSet = new HashSet<>();

                for (int i = statementModelList.size() - 1; i >= 0; i--) {
                    StatementModel statementModel1 = statementModelList.get(i);
                    if (statementModel1.getLine() > line || statementModel1.isAssertStatement()) {
                        continue;
                    }
                    Set<String> declareVariableSet1 = statementModel1.getDeclareVariableSet();
                    Set<String> useVariableSet1 = statementModel1.getUseVariableSet();
                    if (declareVariableSet1 != null) {
                        tempSet.retainAll(declareVariableSet1);
                        if (tempSet.size() > 0) {
                            /*
                              the statement statementModel used variables that
                              declared by the statement statementModel1.
                             */
                            tempStatementModelList.add(statementModel1);
                            if (useVariableSet1 != null) {
                                indirectUseVariableSet.addAll(useVariableSet1);
                            }
                            continue;
                        }
                        if (indirectUseVariableSet.size() > 0) {
                            tempSet.addAll(declareVariableSet1);
                            tempSet.retainAll(indirectUseVariableSet);
                            if (tempSet.size() == 0) {
                                continue;
                            }
                            tempStatementModelList.add(statementModel1);
                            if (useVariableSet1 != null) {
                                indirectUseVariableSet.addAll(useVariableSet1);
                            }
                            continue;
                        }
                    }
                    if (useVariableSet1 == null) {
                        continue;
                    }
                    tempSet.retainAll(useVariableSet1);
                    if (tempSet.size() == 0) {
                        /*
                          the statement statementModel and statementModel1 don't have identical the used variables.
                         */
                        continue;
                    }
                    tempStatementModelList.add(statementModel1);
                    indirectUseVariableSet.addAll(useVariableSet1);
                }
                if (tempStatementModelList.size() > 0) {
                    statementModel.setContextRelatedStatementModelList(tempStatementModelList);
                }
            }
        }
    }

}
