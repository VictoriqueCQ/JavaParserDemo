package Write2Db;

import Analysis.MUTAnalysis;
import Analysis.Main;
import Model.ImportInfoTable;
import Model.MethodInfoTable;
import Model.ProjectInfoTable;
import Model.TestInfoTable;
import Utils.MD5Util;
import Utils.Utils;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

import java.io.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

public class write2DbTest {
    public static final String URL = "jdbc:mysql://localhost:3306/tcdb";
    public static final String USER = "root";
    public static final String PASSWORD = "123456";
//    private static Connection conn = null;

    //    String SRC_PATH = "G:/agile-wroking-backend_98998940_57/src";
//    String repository = "agile-wroking-backend_98998940_57";
//    String repositoryUrl = "https://github.com/7upcat/agile-wroking-backend";
    String SRC_PATH;
    String repository;
    String repositoryUrl;
    String repositoryName;
    String projectName;
    String repositoryId;
    String star;


    public void write(String path, String repoName, String githubUrl) throws IOException, SQLException {
        /** 获取当前系统时间*/
        SRC_PATH = path;
        repository = repoName;
        repositoryUrl = githubUrl;
        long startTime = System.currentTimeMillis();
        /** 程序运行 processRun();*/
//        write2DbTest write2DbTest = new write2DbTest();
//        write2DbTest.getProjectNameAndRepositoryId();
        if (!SRC_PATH.contains("picasso")) {
            String[] name = SRC_PATH.split(repository+"\\\\");
            if(name[1].equals("src")){
                projectName = repository.split("_")[0];
            }else{
                projectName = name[1].split("\\\\")[0].split("_")[0];
            }
            this.repositoryName = repository.split("_")[0];
            repositoryId = repository.split("_")[1];
            star = repository.split("_")[2];
        } else {
            projectName = "picasso";
            repositoryId = "000000";
            repositoryName = "picasso";
        }
//        conn = DbUtil.getConnection();

        List<ImportInfoTable> importInfoTableList = writeImport2Db();
        System.out.println("import ok");
        List<TestInfoTable> testInfoTableList = writeTest2Db();
        System.out.println("test ok");
        Map<String, MethodInfoTable> methodInfoTableList = writeMethod2Db();
        System.out.println("method ok");
        List<String> idt = new ArrayList<>();
        List<String> mdt = new ArrayList<>();
        for (TestInfoTable testInfoTable : testInfoTableList) {
            idt.add(testInfoTable.getImportDependencies());
            mdt.add(testInfoTable.getMethodDependencies());
        }
        List<String> importList = new ArrayList<>();
        for (ImportInfoTable importInfoTable : importInfoTableList) {
            importList.add(importInfoTable.getImportId());
        }
        List<String> methodList = new ArrayList<>();
        for (Map.Entry<String, MethodInfoTable> entry : methodInfoTableList.entrySet()) {
            methodList.add(entry.getValue().getMethodId());
//            methodList.add(entry.getValue().getProjectId() + "+" + entry.getValue().getMethodSignature());
        }
        System.out.println("add ok");
//        importList.forEach(System.out::println);
//        methodList.forEach(System.err::println);
        System.out.println("import check");
        List<Boolean> importTrueList = new ArrayList<>();
        for (String s : idt) {
//            System.err.println(s);
            String[] arr = s.split(",");
            for (String value : arr) {
                if (value.length() > 0) {
                    if (importList.contains(value)) {
                        System.out.println(true);
                        importTrueList.add(true);
                    } else {
                        System.out.println(value);
                        System.out.println(false);
                        importTrueList.add(true);
                    }
                }
            }
        }
        System.out.println("method check");
        List<Boolean> methodTrueList = new ArrayList<>();
        for (String s : mdt) {
//            String[] arr = s.split("&&");
            String[] arr = s.split(",");
            for (String value : arr) {
//                System.out.println(value);
                if (value.length() > 0) {
//                    if (mpList.contains(value)) {
                    if (methodList.contains(value)) {
                        System.out.println(true);
                        methodTrueList.add(true);
                    } else {
                        System.out.println(value);
                        System.out.println(false);
                        methodTrueList.add(false);
                    }
                }
            }
        }
        if (!importTrueList.contains(false) && !methodTrueList.contains(false)) {
            ProjectInfoTable projectInfoTable = writeProject2Db();
            System.out.println("project write ok");
            for (ImportInfoTable importInfoTable : importInfoTableList) {
                writeImport2Db(importInfoTable);
            }
            System.out.println("import write ok");
            for (TestInfoTable testInfoTable : testInfoTableList) {
                writeTest2Db(testInfoTable);
            }
            System.out.println("test write ok");
            for (Map.Entry<String, MethodInfoTable> entry : methodInfoTableList.entrySet()) {
                writeMethod2Db(entry.getValue());
            }
            System.out.println("method write ok");
        } else {
            System.out.println("write false");
        }
//        conn.close();
        /** 获取当前的系统时间，与初始时间相减就是程序运行的毫秒数，除以1000就是秒数*/
        long endTime = System.currentTimeMillis();
        long usedTime = (endTime - startTime) / 1000;
        System.out.println("运行时间：" + usedTime + "s");
    }

    public void getProjectNameAndRepositoryId() {


//        System.out.println(projectName + "+" + repositoryName + "+" + repositoryId + "+" + star);
    }

    public static boolean isInDb(List<String> list, String s) {
        boolean flag = false;
        for (String str : list) {
            if (str.equals(s)) {
                flag = true;
//                System.err.println(s);
                break;
            }
        }
        return flag;
    }

    public List<ImportInfoTable> writeImport2Db() throws FileNotFoundException, SQLException {
        Set<String> importDeclarationSet = new HashSet<>();

        List<String> filenames = new ArrayList<>();
        Utils.findFileList(new File(SRC_PATH), filenames);
        for (String filename : filenames) {
            if (filename.contains(".java")) {
//                System.out.println(filename);
                CompilationUnit cu = Utils.constructCompilationUnit(null, filename, SRC_PATH);
                PackageDeclaration packageDeclaration = cu.findFirst(PackageDeclaration.class).get();
                String packageName = packageDeclaration.getNameAsString();
                List<ImportDeclaration> importDeclarationList = cu.findAll(ImportDeclaration.class);
                for (ImportDeclaration id : importDeclarationList) {
                    if (!id.getNameAsString().contains(packageName) && !id.getNameAsString().contains("java.")) {
                        String importName = id.getNameAsString();
                        importDeclarationSet.add(importName);
                    }
                }
            }
        }


        List<ImportInfoTable> importInfoTableList = new ArrayList<>();

        for (String importString : importDeclarationSet) {
            String importId = MD5Util.getMD5(importString);
//            if (!isInDb(importIdList, importId)) {
//                System.out.println("okk");
            Timestamp time = new Timestamp(System.currentTimeMillis());
            //sql, 每行加空格
//                String sql = "INSERT INTO import_info_table (import_id,import_string,storage_time) VALUES (?,?,?)";
//                //预编译
//                PreparedStatement ptmt = conn.prepareStatement(sql); //预编译SQL，减少sql执行
            ImportInfoTable importInfoTable = new ImportInfoTable();
            importInfoTable.setImportString(importString);
            importInfoTable.setStorageTime(time);
            importInfoTable.setImportId(importId);
            importInfoTableList.add(importInfoTable);
//                ptmt.setString(1, importInfoTable.getImportId());
//                ptmt.setString(2, importInfoTable.getImportString());
//                ptmt.setTimestamp(3, importInfoTable.getStorageTime());
//                int res = ptmt.executeUpdate();//执行sql语句
//                if (res > 0) {
//                    System.out.println("数据录入成功");
//                }
//                ptmt.close();//关闭资源
        }
//        }


        return importInfoTableList;
    }

    public void writeImport2Db(ImportInfoTable importInfoTable) throws SQLException {
        String s = "select import_id from import_info_table";
        List<String> importIdList = new ArrayList<>();
        PreparedStatement p = Main.conn.prepareStatement(s);
        ResultSet rs = p.executeQuery();
        while (rs.next()) {
            String id1 = rs.getString(1);
            importIdList.add(id1);
//            System.err.println(id1);
        }
        String importId = importInfoTable.getImportId();
        if (!isInDb(importIdList, importId)) {
//            System.out.println(importId);
//            //sql, 每行加空格
            String sql = "INSERT INTO import_info_table (import_id,import_string,storage_time) VALUES (?,?,?)";
            //预编译
            PreparedStatement ptmt = Main.conn.prepareStatement(sql); //预编译SQL，减少sql执行
            ptmt.setString(1, importInfoTable.getImportId());
            ptmt.setString(2, importInfoTable.getImportString());
            ptmt.setTimestamp(3, importInfoTable.getStorageTime());
            int res = ptmt.executeUpdate();//执行sql语句
            if (res > 0) {
                System.out.println("import数据录入成功");
            }
            ptmt.close();//关闭资源
        }
    }


    public Map<String, MethodInfoTable> writeMethod2Db() throws IOException, SQLException {
//        MUT2DB mut2DB = new MUT2DB();
        List<String> filenames = new ArrayList<>();
//        String[] project = SRC_PATH.split(":/")[1].split("_");
//        String projectName = project[0];
//        String repositoryId = project[1];
//        String star = project[2].split("/")[0];
        String filename;
        if (projectName.equals("picasso")) {
            filename = "NewTestClass";
        } else {
            filename = projectName + "_" + repositoryId + "_" + star + "_" + "NewTestClass";
        }

        Utils.findFileList(new File(filename), filenames);
        Set<String> pathSet = new HashSet<>();
        for (int i = 0; i < filenames.size(); i++) {
//            if (i == 3) {
//                System.out.println(filenames.get(i));
            File file = new File(filenames.get(i));//定义一个file对象，用来初始化FileReader
            FileReader reader = new FileReader(file);//定义一个fileReader对象，用来初始化BufferedReader
            BufferedReader bReader = new BufferedReader(reader);//new一个BufferedReader对象，将文件内容读取到缓存
            StringBuilder sb = new StringBuilder();//定义一个字符串缓存，将字符串存放缓存中
            String s = "";
            while ((s = bReader.readLine()) != null) {//逐行读取文件内容，不读取换行符和末尾的空格
                sb.append(s + "\n");//将读取的字符串添加换行符后累加存放在缓存中
            }
            bReader.close();
            String str = sb.toString();
            String[] strArray = str.split("=&=&=\n");
            String originalCase = strArray[0];
            CompilationUnit cu = Utils.constructCompilationUnit(originalCase, null, SRC_PATH);
            if (cu != null) {
                List<MethodDeclaration> methodDeclarationList = cu.findAll(MethodDeclaration.class);
                for (MethodDeclaration methodDeclaration : methodDeclarationList) {
                    String packageName = "";
                    String className = "";
                    String path = "";
                    String isTest = "";
                    if (!methodDeclaration.getAnnotationByName("Test").isPresent()) {
                        if (methodDeclaration.getJavadoc().isPresent()) {
                            String[] dependencyComment = methodDeclaration.getJavadoc().get().toText().split(System.lineSeparator());
                            for (String value : dependencyComment) {
                                if (value.contains("packagename")) {
                                    packageName = value.split(":")[1];
                                    packageName = packageName.replaceAll("\\.", "/");
                                } else if (value.contains("classname")) {
                                    className = value.split(":")[1];
                                } else if (value.contains("fromTest")) {
                                    isTest = value.split(":")[1];
                                }
                                if (isTest.equals("true")) {
                                    path = SRC_PATH + "/" + "test/java" + "/" + packageName + "/" + className + ".java";
                                } else {
                                    path = SRC_PATH + "/" + "main/java" + "/" + packageName + "/" + className + ".java";
                                }
                            }
                            pathSet.add(path);
                        }
                    }
                }
            }
//            }

        }
//
        Map<String, MethodInfoTable> methodInfoTableMap = new HashMap<>();
        pathSet.forEach(s -> {
//            System.out.println(s);
            MUTAnalysis mutAnalysis = new MUTAnalysis(SRC_PATH, s, projectName, repositoryId, star);
            List<String> originalMethodClassList = null;
            try {
                originalMethodClassList = mutAnalysis.getOriginalMethod();
                for (int i = 0; i < originalMethodClassList.size(); i++) {
                    MethodInfoTable methodInfoTable = mutAnalysis.methodExtraction(projectName, repositoryId, 0, originalMethodClassList.get(i));
//                    System.err.println(methodInfoTable.getMethodSignature());
//                    System.out.println("signature:"+methodInfoTable.getMethodSignature());
                    methodInfoTableMap.put(methodInfoTable.getMethodSignature(), methodInfoTable);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        });
        return methodInfoTableMap;
    }

    public void writeMethod2Db(MethodInfoTable methodInfoTable) throws SQLException {
        String s = "select method_id from method_info_table";
        List<String> methodIdList = new ArrayList<>();
        PreparedStatement p = Main.conn.prepareStatement(s);
        ResultSet rs = p.executeQuery();
        while (rs.next()) {
            String id1 = rs.getString(1);
            methodIdList.add(id1);
//            System.err.println(id1);
        }
        String methodId = methodInfoTable.getMethodId();
        if (!isInDb(methodIdList, methodId)) {
            String sql = "INSERT INTO method_info_table (method_id,method_signature,method_name,parameter_types,class_name,package_name,method_comment,method_comment_keywords,method_code,is_mut,import_dependencies,method_dependencies,project_id,storage_time,return_type) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
            PreparedStatement ptmt = Main.conn.prepareStatement(sql);
            ptmt.setString(1, methodInfoTable.getMethodId());
            ptmt.setString(2, methodInfoTable.getMethodSignature());
            ptmt.setString(3, methodInfoTable.getMethodName());
            ptmt.setString(4, methodInfoTable.getParameterTypes());
            ptmt.setString(5, methodInfoTable.getClassName());
            ptmt.setString(6, methodInfoTable.getPackageName());
            ptmt.setString(7, methodInfoTable.getMethodComment());
            ptmt.setString(8, methodInfoTable.getMethodCommentKeywords());
            ptmt.setString(9, methodInfoTable.getMethodCode());
            ptmt.setLong(10, methodInfoTable.getIsMut());
            ptmt.setString(11, methodInfoTable.getImportDependencies());
            ptmt.setString(12, methodInfoTable.getMethodDependencies());
            ptmt.setString(13, methodInfoTable.getProjectId());
            ptmt.setTimestamp(14, methodInfoTable.getStorageTime());
            ptmt.setString(15, methodInfoTable.getReturnType());
            int res = ptmt.executeUpdate();//执行sql语句
            if (res > 0) {
                System.out.println("method数据录入成功");
            }
            ptmt.close();//关闭资源
        }

    }

    public ProjectInfoTable writeProject2Db() throws SQLException {
        String s = "select project_id from project_info_table";
        List<String> projectIdList = new ArrayList<>();
        PreparedStatement p = Main.conn.prepareStatement(s);
        ResultSet rs = p.executeQuery();
        while (rs.next()) {
            String id1 = rs.getString(1);
            projectIdList.add(id1);
        }
        String projectId = MD5Util.getMD5(repositoryId + projectName);
        ProjectInfoTable projectInfoTable = new ProjectInfoTable();

        if (!isInDb(projectIdList, projectId)) {
//        String[] project = SRC_PATH.split(":/")[1].split("_");
//        String projectName = project[0];
//        String repositoryId = project[1];
            projectInfoTable.setProjectId(projectId);
            projectInfoTable.setProjectName(projectName);
            projectInfoTable.setProjectType(0);
            projectInfoTable.setRepositoryUrl(repositoryUrl);
            projectInfoTable.setRepositoryId(repositoryId);
            projectInfoTable.setRepositoryName(repositoryName);
            projectInfoTable.setStorageTime(new Timestamp(System.currentTimeMillis()));
            String sql = "INSERT INTO project_info_table (project_id,project_name,project_type,repository_id,repository_url,repository_name,storage_time) VALUES (?,?,?,?,?,?,?)";
            PreparedStatement ptmt = Main.conn.prepareStatement(sql); //预编译SQL，减少sql执行
            ptmt.setString(1, projectInfoTable.getProjectId());
            ptmt.setString(2, projectInfoTable.getProjectName());
            ptmt.setLong(3, projectInfoTable.getProjectType());
            ptmt.setString(4, projectInfoTable.getRepositoryId());
            ptmt.setString(5, projectInfoTable.getRepositoryUrl());
            ptmt.setString(6, projectInfoTable.getRepositoryName());
            ptmt.setTimestamp(7, projectInfoTable.getStorageTime());
            int res = ptmt.executeUpdate();//执行sql语句
            if (res > 0) {
                System.out.println("数据录入成功");
            }
            ptmt.close();//关闭资源
        }
        return projectInfoTable;
    }

    public List<TestInfoTable> writeTest2Db() throws IOException, SQLException {
        Test2DB test2DB = new Test2DB();
//        String[] project = SRC_PATH.split(":/")[1].split("_");
//        String projectName = project[0];
//        String repositoryId = project[1];
//        String star = project[2].split("/")[0];
        List<TestInfoTable> testInfoTableList = new ArrayList<>();
        List<String> filenames = new ArrayList<>();
        if(projectName.equals("picasso")){
            Utils.findFileList(new File("NewTestClass"),filenames);
        }else{
            Utils.findFileList(new File(projectName + "_" + repositoryId + "_" + star + "_" + "NewTestClass"), filenames);
        }

        for (int i = 0; i < filenames.size(); i++) {
//            if(i==0){
                File file = new File(filenames.get(i));//定义一个file对象，用来初始化FileReader
                FileReader reader = new FileReader(file);//定义一个fileReader对象，用来初始化BufferedReader
                BufferedReader bReader = new BufferedReader(reader);//new一个BufferedReader对象，将文件内容读取到缓存
                StringBuilder sb = new StringBuilder();//定义一个字符串缓存，将字符串存放缓存中
                String s = "";
                while ((s = bReader.readLine()) != null) {//逐行读取文件内容，不读取换行符和末尾的空格
                    sb.append(s + "\n");//将读取的字符串添加换行符后累加存放在缓存中
                }
                bReader.close();
                String str = sb.toString();
                String[] strArray = str.split("=&=&=\n");
                String testCaseStr = strArray[1].trim();
                String[] testCase = testCaseStr.split("-&-&-\n");
                String originalCase = strArray[0];
//            System.out.println(originalCase);
                for (int j = 1; j < testCase.length; j++) {
//                System.out.println(testCase[j]);
                    TestInfoTable testInfoTable = test2DB.getTestInfoTable(SRC_PATH, originalCase, testCase[j], filenames.get(i), projectName, repositoryId);
//                System.out.println("testinfo:"+testInfoTable.toString());
////                test2DB.write2DB(testInfoTable);
//                String sql = "INSERT INTO test_info_table (test_case_name,test_case_code,test_target_id,test_target_signature,class_name,package_name,import_dependencies,method_dependencies,test_framework,junit_version,assert_framework,storage_time) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
//                PreparedStatement ptmt = conn.prepareStatement(sql);
//                ptmt.setString(1, testInfoTable.getTestCaseName());
//                ptmt.setString(2, testInfoTable.getTestCaseCode());
//                ptmt.setString(3, testInfoTable.getTestTargetId());
//                ptmt.setString(4, testInfoTable.getTestTargetSignature());
//                ptmt.setString(5, testInfoTable.getClassName());
//                ptmt.setString(6, testInfoTable.getPackageName());
//                ptmt.setString(7, testInfoTable.getImportDependencies());
//                ptmt.setString(8, testInfoTable.getMethodDependencies());
//                ptmt.setLong(9, testInfoTable.getTestFramework());
//                ptmt.setLong(10, testInfoTable.getJunitVersion());
//                ptmt.setLong(11, testInfoTable.getAssertFramework());
//                ptmt.setTimestamp(12, testInfoTable.getStorageTime());
//
//                int res = ptmt.executeUpdate();//执行sql语句
//                if (res > 0) {
//                    System.out.println("数据录入成功");
//                }
//                ptmt.close();//关闭资源
                    testInfoTableList.add(testInfoTable);
                }
//            }
        }
        System.out.println(testInfoTableList.size());
        return testInfoTableList;
    }

    public void writeTest2Db(TestInfoTable testInfoTable) throws SQLException {
        String s = "select test_target_signature from test_info_table";
        List<String> testTargetSignatureList = new ArrayList<>();
        PreparedStatement p = Main.conn.prepareStatement(s);
        ResultSet rs = p.executeQuery();
        while (rs.next()) {
            String id1 = rs.getString(1);
            testTargetSignatureList.add(id1);
//            System.err.println(id1);
        }
        String testTargetSignature = testInfoTable.getTestTargetSignature();
        if (!isInDb(testTargetSignatureList, testTargetSignature)) {
//            System.out.println(testTargetSignature);
            String sql = "INSERT INTO test_info_table (test_case_name,test_case_code,test_target_id,test_target_signature,class_name,package_name,import_dependencies,method_dependencies,test_framework,junit_version,assert_framework,storage_time) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
            PreparedStatement ptmt = Main.conn.prepareStatement(sql);
//            System.out.println(testInfoTable.getTestCaseCode());
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
                System.out.println("test数据录入成功");
            }
            ptmt.close();//关闭资源
        }
    }
}
