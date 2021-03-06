package Analyse;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainAnalysis {
    static List<String> filePathList = new ArrayList<>();
    static List<String> testFilePathList = new ArrayList<>();
    static String SRC_PATH = "D:/picasso/src";

    public static void main(String[] args) throws IOException {
        List<String> fileNames = new ArrayList<String>();
        MainAnalysis mainAnalysis = new MainAnalysis();
        mainAnalysis.findFileList(new File(SRC_PATH), fileNames);
        for (String value : fileNames) {
            if (value.contains(".java") && value.contains("src\\main")) {
                value = value.replaceAll("\\\\", "/");
                filePathList.add(value);
//                System.out.println(value);
            }
            if (value.contains(".java") && value.contains("src\\test")) {
                value = value.replaceAll("\\\\", "/");
                testFilePathList.add(value);
//                System.err.println(value);
            }
        }
        for (int j = 0; j < testFilePathList.size(); j++) {
//            if (j == 9) {
            TestFileAnalysis testFileAnalysis = new TestFileAnalysis(testFilePathList.get(j));
//                System.out.println(testFilePathList.get(j));
//            if (testFilePathList.get(j).contains("UtilsTest.java") || testFilePathList.get(j).contains("DispatcherTest.java")) {
            List<String> originalTestFragmentClassList = testFileAnalysis.getOriginalTestFragment();
            for (int i = 0; i < originalTestFragmentClassList.size(); i++) {
//                if (originalTestFragmentClassList.get(i).contains("getResourceByTypeAndName")) {
//                        System.out.println(i);
//                    }
//                    if (i == 2) {
//                    if(originalTestFragmentClassList.get(i).contains("shouldRetryTwiceWithAirplaneModeOffAndNoNetworkInfo")){
//                        System.out.println(j+"+++"+i+"+++");
//                    }
//                    if(originalTestFragmentClassList.get(i).contains("createwithBitmapcacheHit")){
//                        System.out.println(j+"+++"+i+"+++");
//                    }
                    testFileAnalysis.dependencyAnalysis(originalTestFragmentClassList.get(i));
//                }
            }
//            }

        }
//        for (int j = 0; j < filePathList.size(); j++) {
//            MethodCollector methodCollector = new MethodCollector(filePathList.get(j));
//            List<String> originalMethodClassList = methodCollector.getOriginalMethod();
//            for (int i = 0; i < originalMethodClassList.size(); i++) {
//                methodCollector.methodExtraction(0, originalMethodClassList.get(i));
//            }
//        }
//        for (int j = 0; j < testFilePathList.size(); j++) {
//            MethodCollector methodCollector = new MethodCollector(testFilePathList.get(j));
//            List<String> originalMethodClassList = methodCollector.getOriginalMethod();
//            for (int i = 0; i < originalMethodClassList.size(); i++) {
//                methodCollector.methodExtraction(1, originalMethodClassList.get(i));
//            }
//        }

//        File file = new File("Test Fragment/");
//        if(!file.exists()){
//            file.mkdir();
//        }
//        File file1 = new File("Test Class/");
//        if(!file1.exists()){
//            file1.mkdir();
//        }
//        File file2 = new File("MUT/");
//        if(!file2.exists()){
//            file2.mkdir();
//        }

    }

    //获取目录下所有文件
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
}
