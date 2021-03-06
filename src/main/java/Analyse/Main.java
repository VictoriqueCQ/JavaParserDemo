package Analyse;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import Utils.Utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) throws IOException {
        List<String> filenames = new ArrayList<>();
        Main main = new Main();
        main.findFileList(new File("Test Class"), filenames);
//        filenames.forEach(System.out::println);

        testTargetAnalysis tta = new testTargetAnalysis();
        int numberOfTestCase = 0;
        for (int i = 0; i < filenames.size(); i++) {
//            if (i == 1 || i == 2) {

            try {
                Map<String, Integer> result = tta.getTarget(filenames.get(i));
//                System.out.println(i);
//                System.out.println(result);
                List<String> targetList = new ArrayList<>(result.keySet());
//                System.out.println(targetList.toString());
                if (targetList.size() == 0) {
                    System.err.println("no test target!!!");
                    continue;
                }

                TestGranularityAnalysis tga = new TestGranularityAnalysis();

                if (targetList.size() > 0) {
                    CompilationUnit cu = tga.constructCompilationUnit(null, filenames.get(i));
                    List<ClassOrInterfaceDeclaration> myClassList = tga.reduceTestGranularity(cu, targetList);
                    if (myClassList != null) {
//                    System.out.println(myClassList.size()+"+"+targetList.size());
                        CompilationUnit cu1 = Utils.constructCompilationUnit(null, filenames.get(i), "D:/picasso/src");
                        numberOfTestCase += myClassList.size();
////                    System.out.println("---------------------------------------");
                        String name = filenames.get(i).split("\\\\")[1];
                        String filename = "NewTestClass" + File.separator + name;
                        BufferedWriter bw = new BufferedWriter(new FileWriter(filename));
                        bw.write(cu1.toString());
                        bw.write("===");
//                    System.err.println("+++"+filename);
                        for (ClassOrInterfaceDeclaration myClass : myClassList) {
                            System.out.println(myClass.toString());
                            System.out.println("===");
                            bw.write("\n---\n");
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
}
