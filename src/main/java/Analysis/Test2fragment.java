package Analysis;

import Utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Test2fragment {
    static List<String> filePathList = new ArrayList<>();
    static List<String> testFilePathList = new ArrayList<>();
    static String SRC_PATH = "D:/avaire_99215275_49/src";
    static int testFramework = 0;
    static int junitVersion = 5;
    static int assertFramework = 0;
    public static void main(String[] args) throws IOException {
        List<String> fileNames = new ArrayList<String>();
        Utils.findFileList(new File(SRC_PATH), fileNames);
        String projectName = SRC_PATH.split("/")[1].split("_")[0];
        String repositoryId = SRC_PATH.split("/")[1].split("_")[1];
        String star = SRC_PATH.split("/")[1].split("_")[2];
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
//        for (int j = 0; j < filePathList.size(); j++) {
//            MethodCollector methodCollector = new MethodCollector(SRC_PATH, filePathList.get(j), projectName, repositoryId, star);
//            List<String> originalMethodClassList = methodCollector.getOriginalMethod();
//            for (int i = 0; i < originalMethodClassList.size(); i++) {
//                methodCollector.methodExtraction(0, originalMethodClassList.get(i));
//            }
//        }
//        for (int j = 0; j < testFilePathList.size(); j++) {
//            MethodCollector methodCollector = new MethodCollector(SRC_PATH, filePathList.get(j), projectName, repositoryId, star);
//            List<String> originalMethodClassList = methodCollector.getOriginalMethod();
//            for (int i = 0; i < originalMethodClassList.size(); i++) {
//                methodCollector.methodExtraction(1, originalMethodClassList.get(i));
//            }
//        }
        for (int j = 0; j < testFilePathList.size(); j++) {
            TestFileAnalysis testFileAnalysis = new TestFileAnalysis(SRC_PATH, testFilePathList.get(j), projectName, repositoryId, star, testFramework,junitVersion,assertFramework);
            List<String> originalTestFragmentClassList = testFileAnalysis.getOriginalTestFragment();
            for (int i = 0; i < originalTestFragmentClassList.size(); i++) {
                testFileAnalysis.dependencyAnalysis(originalTestFragmentClassList.get(i));
            }
        }

//        String testClassPath = projectName + "_" + repositoryId + "_" + star + "_" + "Test Class/";
//        List<String> testClassNames = new ArrayList<>();
//        Utils.findFileList(new File(testClassPath), testClassNames);
//        testTargetAnalysis tta = new testTargetAnalysis(SRC_PATH);
//        int numberOfTestCase = 0;
//        for (int i = 0; i < testClassNames.size(); i++) {
//            try {
//                Map<String, Integer> result = tta.getTarget(testClassNames.get(i));
//                List<String> targetList = new ArrayList<>(result.keySet());
//                if (targetList.size() == 0) {
//                    System.err.println("no test target!!!");
//                    continue;
//                }
//                TestGranularityAnalysis tga = new TestGranularityAnalysis();
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
//
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//        System.out.println("Total Number of Test Cases: " + numberOfTestCase);
    }
}
