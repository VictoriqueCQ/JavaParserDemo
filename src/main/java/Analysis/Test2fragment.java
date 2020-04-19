package Analysis;

import Utils.Utils;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Test2fragment {
    static List<String> filePathList = new ArrayList<>();
    static List<String> testFilePathList = new ArrayList<>();
    static String SRC_PATH = "G:/agile-wroking-backend_98998940_57/src";
    static String repositoryName = "agile-wroking-backend_98998940_57";
    static int testFramework = 0;
    static int junitVersion = 4;
    static int assertFramework = 0;

    public static void main(String[] args) throws IOException {
        /** 获取当前系统时间*/
        long startTime = System.currentTimeMillis();
        /** 程序运行 processRun();*/


        List<String> fileNames = new ArrayList<String>();
        Utils.findFileList(new File(SRC_PATH), fileNames);
        String projectName = SRC_PATH.split("/")[1].split("_")[0];
//        String repositoryId = SRC_PATH.split("/")[1].split("_")[1];
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
            TestFileAnalysis testFileAnalysis = new TestFileAnalysis(SRC_PATH, testFilePathList.get(j), projectName, repositoryId, star, testFramework, junitVersion, assertFramework);
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
            int testFramework = Integer.parseInt(testClassName[0]);
            int junitVersion = Integer.parseInt(testClassName[1]);
            int assertFramework = Integer.parseInt(testClassName[2]);
            TestTargetAnalysis tta = new TestTargetAnalysis(SRC_PATH, testFramework, junitVersion, assertFramework);

            try {
                Map<String, Integer> result = tta.getTarget(testClassNames.get(i));
                List<String> targetList = new ArrayList<>(result.keySet());
//                numberOfTestCase += targetList.size();
                if (targetList.size() > 0) {
                    System.out.println("target number:" + targetList.size() + "+" + testClassNames.get(i));
                }
//                    for(String s:targetList){
//                        System.out.println("target:"+s);
//                    }
                if (targetList.size() == 0) {
//                    System.err.println("no test target!!!");
                    continue;
                }

                TestGranularityAnalysis tga = new TestGranularityAnalysis(SRC_PATH);
                CompilationUnit cu = tga.constructCompilationUnit(null, testClassNames.get(i));
                List<ClassOrInterfaceDeclaration> myClassList = tga.reduceTestGranularity(cu, targetList, String.valueOf(junitVersion));
                if (myClassList == null) {
//                    System.out.println("null:"+testClassNames.get(i));
//                    System.out.println("+++++++++++++++++++++++++++++++++++++++++");
//                    System.err.println("Failed to reduce the test granularity!");
                } else {
                    if (myClassList.size() > 0) {
//                        System.out.println("Total Number of Test Cases: " + myClassList.size());
                        numberOfTestCase += myClassList.size();
                        numberOfTestCase += myClassList.size();
//                        System.out.println("---------------------------------------");
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
//                            System.out.println(myClass.toString());
//                            System.out.println("===");
                            bw.write("\n-&-&-\n");
                            bw.write(myClass.toString());

                        }
                        bw.close();

                        //                                for (ClassOrInterfaceDeclaration myClass : myClassList) {
//                                    System.out.println(myClass.toString());
//                                    System.out.println("---------------------------------------");
//                                }
                    }
                }

//                if (targetList.size() > 0) {
//                    CompilationUnit cu = tga.constructCompilationUnit(null, testClassNames.get(i));
//                    List<ClassOrInterfaceDeclaration> myClassList = tga.reduceTestGranularity(cu, targetList);
//                    if (myClassList != null) {
//                        System.out.println(myClassList.size() + "+" + targetList.size());
//                        CompilationUnit cu1 = Utils.constructCompilationUnit(null, testClassNames.get(i), SRC_PATH);
//                        numberOfTestCase += myClassList.size();
//////                    System.out.println("---------------------------------------");
//                        String name = testClassNames.get(i).split("\\\\")[1];
//                        String filename = projectName + "_" + repositoryId + "_" + star + "_" + "NewTestClass" + File.separator + name;
////                        BufferedWriter bw = new BufferedWriter(new FileWriter(filename));
////                        bw.write(cu1.toString());
////                        bw.write("===");
////                    System.err.println("+++"+filename);
////                        for (ClassOrInterfaceDeclaration myClass : myClassList) {
////                            System.out.println(myClass.toString());
////                            System.out.println("===");
////                            bw.write("\n---\n");
////                            bw.write(myClass.toString());
////
////                        }
////                        bw.close();
//                    }
//                }

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
    }
}
