package Analysis;

import Model.ThirdFilterResult;
import Utils.Utils;
import Write2Db.write2DbTest;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) throws IOException, SQLException {
        List<ThirdFilterResult> thirdFilterResultList = new ArrayList<>();
        //1.读取Excel文档对象
        XSSFWorkbook xssfWorkbook = new XSSFWorkbook(new FileInputStream("D:/2017_Aug_third_filter.xlsx"));
        //2.获取要解析的表格（第一个表格）
        XSSFSheet sheet = xssfWorkbook.getSheetAt(0);
        //获得最后一行的行号
        int lastRowNum = sheet.getLastRowNum();
        for (int i = 0; i <= lastRowNum; i++) {//遍历每一行
            //3.获得要解析的行
            XSSFRow row = sheet.getRow(i);
            row.getCell(0).setCellType(Cell.CELL_TYPE_STRING);
            row.getCell(1).setCellType(Cell.CELL_TYPE_STRING);
            row.getCell(2).setCellType(Cell.CELL_TYPE_STRING);
            row.getCell(3).setCellType(Cell.CELL_TYPE_STRING);
            row.getCell(4).setCellType(Cell.CELL_TYPE_STRING);
            //4.获得每个单元格中的内容（String）
            String junitVersion = row.getCell(0).getStringCellValue();
            String projectName = row.getCell(1).getStringCellValue();
            String repositoryId = row.getCell(2).getStringCellValue();
            String repositoryName = row.getCell(3).getStringCellValue();
            String testingFramework = row.getCell(4).getStringCellValue();
            ThirdFilterResult tfr = new ThirdFilterResult();
            tfr.setJunitVersion(junitVersion);
            tfr.setProjectName(projectName);
            tfr.setRepositoryId(repositoryId);
            tfr.setRepositoryName(repositoryName);
            tfr.setTestingFramework(testingFramework);
            thirdFilterResultList.add(tfr);
//            System.out.println(junitVersion+"--"+projectName+"--"+repositoryId+"--"+repositoryName+"--"+testingFramework);
        }
        List<String> directoryNames = new ArrayList<>();
        File file = new File("C:/Users/77627/IdeaProjects/JavaParserDemo");
        File[] tempList = file.listFiles();
//        System.out.println("该目录下对象个数：zd"+tempList.length);
        for (int i = 0; i < tempList.length; i++) {
            if (tempList[i].isDirectory()) {
                directoryNames.add(tempList[i].getName());
//                System.out.println("文件夹："+tempList[i].getName());
            }
        }
        for (int i = 1; i < thirdFilterResultList.size(); i++) {
            System.out.println(thirdFilterResultList.get(i).getProjectName());
            if (i == 169) {
                String junitVersion = thirdFilterResultList.get(i).getJunitVersion();
                String projectName = thirdFilterResultList.get(i).getProjectName();
                String SRC_PATH = projectName + "src";
                String repositoryId = thirdFilterResultList.get(i).getRepositoryId();
                String repositoryName = thirdFilterResultList.get(i).getRepositoryName();
                String testingFramework = thirdFilterResultList.get(i).getTestingFramework();

                int length = repositoryName.split("_").length;
                if (length == 3) {
                    String[] name = projectName.split(repositoryName + "\\\\");
                    if (name.length == 1) {
                        projectName = repositoryName;
                    } else if (name.length == 2) {
                        projectName = name[1].substring(0, name[1].length() - 1).replaceAll("\\\\", ".");
                    }
                    projectName = projectName.split("_")[0];
                    String star = repositoryName.split("_")[2];
                    int testCaseNumber = 0;
                    //写文件到NewTestClass型目录
                    String newTestClassDir = projectName + "_" + repositoryId + "_" + star + "_" + "NewTestClass";
                    if (!directoryNames.contains(newTestClassDir)) {
                        Main main = new Main();
                        testCaseNumber = main.test2fragment(SRC_PATH, projectName, repositoryName, junitVersion);
                        if (testCaseNumber == 0) {
                            String MUTDirectoryName = projectName + "_" + repositoryId + "_" + star + "_" + "MUTClass";
                            String TestDirectoryName = projectName + "_" + repositoryId + "_" + star + "_" + "Test Class";
                            main.deleteDir(MUTDirectoryName);
                            main.deleteDir(TestDirectoryName);
                        }
                    }
                    //写进数据库
                    //1.读取Excel文档对象
                    XSSFWorkbook xssfWorkbook2 = new XSSFWorkbook(new FileInputStream("D:/2017_Aug_first_filter.xlsx"));
                    //2.获取要解析的表格（第一个表格）
                    XSSFSheet sheet2 = xssfWorkbook2.getSheetAt(0);
                    //获得最后一行的行号
                    int lastRowNum2 = sheet2.getLastRowNum();
                    for (int j = 0; j <= lastRowNum2; j++) {//遍历每一行
                        //3.获得要解析的行
                        XSSFRow row = sheet2.getRow(j);
                        row.getCell(1).setCellType(Cell.CELL_TYPE_STRING);
                        row.getCell(6).setCellType(Cell.CELL_TYPE_STRING);
                        //4.获得每个单元格中的内容（String）
                        String id = row.getCell(1).getStringCellValue();
                        if (id.equals(repositoryId)) {
                            String githubUrl = row.getCell(6).getStringCellValue();
                            write2DbTest write2DbTest = new write2DbTest();
                            write2DbTest.write(SRC_PATH, repositoryName, githubUrl);
                        }
                    }
                }
            }
        }
    }

    public void deleteDir(String dirPath) {
        File file = new File(dirPath);
        if (file.isFile()) {
            file.delete();
        } else {
            File[] files = file.listFiles();
            if (files == null) {
                file.delete();
            } else {
                for (int i = 0; i < files.length; i++) {
                    deleteDir(files[i].getAbsolutePath());
                }
                file.delete();
            }
        }
    }

    void write2db() {

    }

    int test2fragment(String SRC_PATH, String projectName, String repositoryName, String junitVersion) throws IOException {
        List<String> filePathList = new ArrayList<>();
        List<String> testFilePathList = new ArrayList<>();
        int testFramework = 0;
        int assertFramework = 0;
        /** 获取当前系统时间*/
        long startTime = System.currentTimeMillis();
        /** 程序运行 processRun();*/


        List<String> fileNames = new ArrayList<String>();
        Utils.findFileList(new File(SRC_PATH), fileNames);
        String repositoryId = repositoryName.split("_")[1];
        String star = repositoryName.split("_")[2];
        System.out.println(projectName + "+" + repositoryId + "+" + star);
        for (String value : fileNames) {
            if (value.contains(".java") && value.contains("src" + File.separator + "main")) {
//                value = value.replaceAll("\\\\", "/");
                filePathList.add(value);
//                System.out.println(value);
            }
            if (value.contains(".java") && value.contains("src" + File.separator + "test")) {
//                value = value.replaceAll("\\\\", "/");
                testFilePathList.add(value);
//                System.err.println(value);
            }
        }
        for (int j = 0; j < filePathList.size(); j++) {
            MethodCollector methodCollector = new MethodCollector(SRC_PATH, filePathList.get(j), projectName, repositoryId, star);
            List<String> originalMethodClassList = methodCollector.getOriginalMethod();
            for (int i = 0; i < originalMethodClassList.size(); i++) {
                methodCollector.methodExtraction(0, originalMethodClassList.get(i));
            }
        }
        for (int j = 0; j < testFilePathList.size(); j++) {
            MethodCollector methodCollector = new MethodCollector(SRC_PATH, testFilePathList.get(j), projectName, repositoryId, star);
            List<String> originalMethodClassList = methodCollector.getOriginalMethod();
            for (int i = 0; i < originalMethodClassList.size(); i++) {
                methodCollector.methodExtraction(1, originalMethodClassList.get(i));
            }

        }
        for (int j = 0; j < testFilePathList.size(); j++) {
            TestFileAnalysis testFileAnalysis = new TestFileAnalysis(SRC_PATH, testFilePathList.get(j), projectName, repositoryId, star, testFramework, Integer.parseInt(junitVersion), assertFramework);
            List<String> originalTestFragmentClassList = testFileAnalysis.getOriginalTestFragment();
            for (int i = 0; i < originalTestFragmentClassList.size(); i++) {
                testFileAnalysis.dependencyAnalysis(originalTestFragmentClassList.get(i));
            }
        }

        String testClassPath = projectName + "_" + repositoryId + "_" + star + "_" + "Test Class/";
        List<String> testClassNames = new ArrayList<>();
        Utils.findFileList(new File(testClassPath), testClassNames);
        int numberOfTestCase = 0;
        for (int i = 0; i < testClassNames.size(); i++) {
//            if (i > 7&&i<10) {
            String[] testClassName = testClassNames.get(i).split("\\\\")[1].split("\\+");
            TestTargetAnalysis tta = new TestTargetAnalysis(SRC_PATH, testFramework, Integer.parseInt(junitVersion), assertFramework);
            try {
                Map<String, Integer> result = tta.getTarget(testClassNames.get(i));
                List<String> targetList = new ArrayList<>(result.keySet());
                if (targetList.size() > 0) {
                    System.out.println("target number:" + targetList.size() + "+" + testClassNames.get(i));
                }
                if (targetList.size() == 0) {
                    continue;
                }

                TestGranularityAnalysis tga = new TestGranularityAnalysis(SRC_PATH);
                CompilationUnit cu = tga.constructCompilationUnit(null, testClassNames.get(i));
                List<ClassOrInterfaceDeclaration> myClassList = tga.reduceTestGranularity(cu, targetList, String.valueOf(junitVersion));
                if (myClassList == null) {
                } else {
                    if (myClassList.size() > 0) {
                        numberOfTestCase += myClassList.size();
                        numberOfTestCase += myClassList.size();
                        CompilationUnit cu1 = Utils.constructCompilationUnit(null, testClassNames.get(i), SRC_PATH);
                        String name = testClassNames.get(i).split("\\\\")[1];
                        String filename = projectName + "_" + repositoryId + "_" + star + "_" + "NewTestClass" + File.separator + name;
                        File file = new File(projectName + "_" + repositoryId + "_" + star + "_" + "NewTestClass" + File.separator);
                        if (!file.exists()) {
                            file.mkdir();
                        }
                        BufferedWriter bw = new BufferedWriter(new FileWriter(filename));
                        bw.write(cu1.toString());
                        bw.write("=&=&=");
                        System.err.println("+++" + filename);
                        System.out.println("---------------------------------------");
                        for (ClassOrInterfaceDeclaration myClass : myClassList) {
                            bw.write("\n-&-&-\n");
                            bw.write(myClass.toString());

                        }
                        bw.close();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

//        }
        System.out.println("Total Number of Test Cases: " + numberOfTestCase);
        /** 获取当前的系统时间，与初始时间相减就是程序运行的毫秒数，除以1000就是秒数*/
        long endTime = System.currentTimeMillis();
        long usedTime = (endTime - startTime) / 1000;
        System.out.println("运行时间：" + usedTime + "s");
        return numberOfTestCase;
    }
}