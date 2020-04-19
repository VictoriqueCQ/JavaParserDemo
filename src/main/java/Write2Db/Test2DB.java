package Write2Db;

import Model.TestInfoTable;
import Utils.DbUtil;
import Utils.MD5Util;
import Utils.Utils;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;

import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Test2DB {
    private static Connection conn = null;
//    static List<FieldDeclaration> globelVariableList = null;

    public static void main(String[] args) throws IOException, SQLException {
        Test2DB test2DB = new Test2DB();
        List<String> filenames = new ArrayList<>();
        Utils.findFileList(new File("NewTestClass"), filenames);
        conn = DbUtil.getConnection();
        for (int i = 0; i < filenames.size(); i++) {
//            if (i == 1) {
//            if(filenames.get(i).contains("getResourceById")){
            File file = new File(filenames.get(i));//定义一个file对象，用来初始化FileReader
            FileReader reader = new FileReader(file);//定义一个fileReader对象，用来初始化BufferedReader
            BufferedReader bReader = new BufferedReader(reader);//new一个BufferedReader对象，将文件内容读取到缓存
            StringBuilder sb = new StringBuilder();//定义一个字符串缓存，将字符串存放缓存中
            String s = "";
            while ((s = bReader.readLine()) != null) {//逐行读取文件内容，不读取换行符和末尾的空格
                sb.append(s + "\n");//将读取的字符串添加换行符后累加存放在缓存中
//                    System.out.println(s);
            }
            bReader.close();
            String str = sb.toString();
            String[] strArray = str.split("=&=&=");
            String testCaseStr = strArray[1].trim();
            String[] testCase = testCaseStr.split("-&-&-");
            String originalCase = strArray[0];
//            System.out.println(testCase.length);
            for (int j = 1; j < testCase.length; j++) {
                TestInfoTable testInfoTable = test2DB.getTestInfoTable("D:/picasso/src", originalCase, testCase[j], filenames.get(i), "picasso", "000000");
//                System.out.println(testInfoTable.toString());
                test2DB.write2DB(testInfoTable);
            }
//            }

//            }
        }
        conn.close();
    }

    public void write2DB(TestInfoTable testInfoTable) throws SQLException {
        String sql = "INSERT INTO test_info_table (test_case_name,test_case_code,test_target_id,test_target_signature,class_name,package_name,import_dependencies,method_dependencies,test_framework,junit_version,assert_framework,storage_time) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
        PreparedStatement ptmt = conn.prepareStatement(sql);
        ptmt.setString(1, testInfoTable.getTestCaseName());
        ptmt.setString(2, testInfoTable.getTestCaseCode());
        ptmt.setString(3, testInfoTable.getTestTargetId());
        ptmt.setString(4, testInfoTable.getTestTargetSignature());
        ptmt.setString(5, testInfoTable.getClassName());
        ptmt.setString(6, testInfoTable.getPackageName());
        ptmt.setString(7, testInfoTable.getImportDependencies());
        ptmt.setString(8, testInfoTable.getMethodDependencies());
        ptmt.setLong(9, testInfoTable.getTestFramework());
        ptmt.setLong(10, testInfoTable.getJunitVersion());
        ptmt.setLong(11, testInfoTable.getAssertFramework());
        ptmt.setTimestamp(12, testInfoTable.getStorageTime());

        int res = ptmt.executeUpdate();//执行sql语句
        if (res > 0) {
            System.out.println("数据录入成功");
        }
        ptmt.close();//关闭资源
    }

    public TestInfoTable getTestInfoTable(String SRC_PATH, String originalTestCase, String testCase, String filename, String projectName, String repositoryId) throws FileNotFoundException {
        TestInfoTable testInfoTable = new TestInfoTable();
        CompilationUnit cu1 = Utils.constructCompilationUnit(originalTestCase, null, SRC_PATH);
        CompilationUnit cu2 = Utils.constructCompilationUnit(testCase, null, SRC_PATH);
        List<FieldDeclaration> globalVariableList = new ArrayList<>();
        List<FieldDeclaration> fdList = cu2.findAll(FieldDeclaration.class);
        if (fdList != null) {
            globalVariableList = fdList;
        }
        //测试方法列表。实际上只有一个
        List<MethodDeclaration> testMethod = new ArrayList<>();
        cu2.findAll(MethodDeclaration.class).stream().filter(md -> md.getAnnotationByName("Test").isPresent()).forEach(testMethod::add);
        String testCaseName = testMethod.get(0).getNameAsString();
        String targetMethodName = testCaseName.replaceAll("test", "");
        targetMethodName = String.valueOf(targetMethodName.charAt(0)).toLowerCase() + targetMethodName.substring(1);
        String[] filenameArray = filename.split("\\+");
        String packageName;
        String className;
        if (SRC_PATH.equals("D:/picasso/src")) {
            packageName = filenameArray[0].split("\\\\")[1];
            className = filenameArray[1];
        } else {
            packageName = filenameArray[3];
            className = filenameArray[4];
        }

//        String typeList = filenameArray[3];
        String testTargetSignature = "";
        List<MethodDeclaration> methodDeclarationList = cu2.findAll(MethodDeclaration.class);
        List<ConstructorDeclaration> constructorDeclarationList = cu2.findAll(ConstructorDeclaration.class);
        for (MethodDeclaration md : methodDeclarationList) {
            if (md.getNameAsString().equals(targetMethodName)) {
                testTargetSignature = fromMethodCommentToTarget(md);
//                System.out.println(testTargetSignature);
            }
        }
        if (testTargetSignature.length() == 0) {
            for (ConstructorDeclaration cd : constructorDeclarationList) {
                if (cd.getNameAsString().equals(targetMethodName)) {
                    testTargetSignature = fromConstructorCommentToTarget(cd);
                }
            }
        }
        String testTargetId = MD5Util.getMD5(repositoryId + projectName + testTargetSignature);
        String testCaseCode = "";
        for (FieldDeclaration fd : globalVariableList) {
            testCaseCode += fd.toString();
        }
        testCaseCode += testMethod.get(0).toString();
        int testFramework = 0;
        int junitVersion = 4;
        int assertFramework = 2;
        Timestamp storageTime = new Timestamp(System.currentTimeMillis());

        List<ImportDeclaration> importDeclarationList = cu1.findAll(ImportDeclaration.class);
        String importDependencyString = "";
        if (importDeclarationList.size() > 0) {
            for (ImportDeclaration id : importDeclarationList) {
                if (!id.getNameAsString().contains("java.") && !id.getNameAsString().contains(packageName)) {
                    importDependencyString += MD5Util.getMD5(id.getNameAsString()) + ",";
//                    importDependencyString += id.getNameAsString() + ",";
                }
            }
        }
        String methodDependencyString = "";
        Set<String> methodDependencySet = new HashSet<>();
        //测试方法的行范围
        int[] testMethodRange = new int[2];
        testMethodRange[0] = testMethod.get(0).getRange().get().begin.line;
        testMethodRange[1] = testMethod.get(0).getRange().get().end.line;

        //类中所有函数调用
        List<MethodCallExpr> methodCallExprList = new ArrayList<>();
        List<MethodCallExpr> oldMethodCallExprList = cu2.findAll(MethodCallExpr.class);

        for (MethodCallExpr mce : oldMethodCallExprList) {
            if (mce.getRange().get().begin.line > testMethodRange[0] || mce.getRange().get().begin.line < testMethodRange[1]) {
                methodCallExprList.add(mce);
            }
        }

        //类中所有构造方法
        List<ObjectCreationExpr> objectCreationExprList = new ArrayList<>();
        List<ObjectCreationExpr> oldObjectCreationExprList = cu2.findAll(ObjectCreationExpr.class);

        for (ObjectCreationExpr oce : oldObjectCreationExprList) {
            if (oce.getRange().get().begin.line > testMethodRange[0] || oce.getRange().get().begin.line < testMethodRange[1]) {
                objectCreationExprList.add(oce);
            }
        }


        //类中所有变量
        List<VariableDeclarator> oldVariableDeclaratorList = cu2.findAll(VariableDeclarator.class);
        List<VariableDeclarator> variableDeclaratorList = new ArrayList<>();
        for (VariableDeclarator vd : oldVariableDeclaratorList) {
            if (vd.getRange().get().begin.line > testMethodRange[0] || vd.getRange().get().begin.line < testMethodRange[1]) {
                variableDeclaratorList.add(vd);
            }
        }

        for (MethodCallExpr mce : methodCallExprList) {
            for (MethodDeclaration md : methodDeclarationList) {
                String name = mce.getNameAsString();
                List<Expression> param = mce.getArguments();
                List<String> mceTypeList = getVariableTypeList(param, variableDeclaratorList, globalVariableList);
                if (matchMethod(name, mceTypeList, md)) {
                    String target = fromMethodCommentToTarget(md);
                    methodDependencySet.add(target);
                    break;
                }
            }
        }
        for (ObjectCreationExpr oce : objectCreationExprList) {
            for (ConstructorDeclaration cd : constructorDeclarationList) {
                String name = oce.getTypeAsString();
                List<Expression> param = oce.getArguments();
                List<String> oceTypeList = getVariableTypeList(param, variableDeclaratorList, globalVariableList);
                if (matchMethod(name, oceTypeList, cd)) {
                    String target = fromConstructorCommentToTarget(cd);
                    methodDependencySet.add(target);
                }
            }
        }
        if (methodDependencySet.size() > 0) {
            for (String s : methodDependencySet) {
//                System.out.println(s);
                if (s.length() > 0) {
                    methodDependencyString += MD5Util.getMD5(repositoryId + projectName + s) + ",";
//                    methodDependencyString += repositoryId + projectName + "+" + s + "&&";
                }
            }
        }
//        System.out.println(methodDependencyString);

        testInfoTable.setTestCaseName(testCaseName);
        testInfoTable.setTestCaseCode(testCaseCode);
        testInfoTable.setTestTargetId(testTargetId);
        testInfoTable.setTestTargetSignature(testTargetSignature);
        testInfoTable.setClassName(className);
        testInfoTable.setPackageName(packageName);
        testInfoTable.setImportDependencies(importDependencyString);
        testInfoTable.setMethodDependencies(methodDependencyString);
        testInfoTable.setTestFramework(testFramework);
        testInfoTable.setJunitVersion(junitVersion);
        testInfoTable.setAssertFramework(assertFramework);
        testInfoTable.setStorageTime(storageTime);

        return testInfoTable;
    }

    public static String fromConstructorCommentToTarget(ConstructorDeclaration cd) {
        String target = "";
//        System.out.println(cd.getJavadoc().toString());

        String[] comment = cd.getJavadoc().get().toText().split(System.lineSeparator());
        for (int i = 0; i < comment.length; i++) {
            if (target.contains("packagename") && target.contains("classname") && target.contains("methodname") && target.contains("parametertype")) {
                break;
            }
            if (comment[i].contains("packagename") || comment[i].contains("classname") || comment[i].contains("methodname") || comment[i].contains("parametertype")) {
                target += comment[i].split(":")[1] + "+";
            }
        }
        target = target.substring(0, target.length() - 1);
        return target;
    }

    public static String fromMethodCommentToTarget(MethodDeclaration md) {
        String target = "";
//        System.out.println(md);
        if (md.getJavadoc().isPresent()) {
            String[] comment = md.getJavadoc().get().toText().split(System.lineSeparator());
            for (int i = 0; i < comment.length; i++) {
                if (comment[i].contains("packagename") || comment[i].contains("classname") || comment[i].contains("methodname") || comment[i].contains("parametertype")) {
                    target += comment[i].split(":")[1] + "+";
                }
            }
            target = target.substring(0, target.length() - 1);
        }
        return target;
    }

    public static boolean matchMethod(String methodName, List<String> typeList, ConstructorDeclaration constructorDeclaration) {
        boolean flag = true;
        String name = constructorDeclaration.getNameAsString();
        List<Parameter> parameterList = constructorDeclaration.getParameters();
        List<String> parameterString = new ArrayList<>();
        for (Parameter p : parameterList) {
            parameterString.add(p.getTypeAsString());
        }
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

        return flag;
    }

    public static boolean matchMethod(String methodName, List<String> typeList, MethodDeclaration methodDeclaration) {
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

    public static List<String> getVariableTypeList(List<Expression> arguments, List<VariableDeclarator> variableDeclarators, List<FieldDeclaration> globelVariableList) {
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

    public static VariableDeclarator getVariableInitialize(Expression expression, List<FieldDeclaration> fieldDeclarations, List<VariableDeclarator> variableDeclarators) {
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

    public static String getBasicDataType(Expression expression) {
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
}
