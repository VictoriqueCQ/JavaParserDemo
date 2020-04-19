package Write2Db;

import Analysis.MUTAnalysis;
import Model.MethodInfoTable;
import Utils.DbUtil;
import Utils.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MUT2DB {
    private static Connection conn = null;
    String FILE_PATH = "";
    static String projectName;
    static  String repositoryId;
    static String star;
    static String SRC_PATH;
    public static void main(String[] args) throws FileNotFoundException, SQLException {
//        FirstWrite2Db();
        MUT2DB mut2DB = new MUT2DB();
        Map<String, MethodInfoTable> methodInfoTableMap = new HashMap<>();
        List<String> filePathList = new ArrayList<>();
        List<String> testFilePathList = new ArrayList<>();
        String ROOT_PATH = "D:/picasso/src/";
        List<String> filenames = new ArrayList<>();
        Utils.findFileList(new File(ROOT_PATH), filenames);
        for (String value : filenames) {
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
        conn = DbUtil.getConnection();
        for (int j = 0; j < filePathList.size(); j++) {
            MUTAnalysis mutAnalysis = new MUTAnalysis(SRC_PATH, filePathList.get(j), projectName, repositoryId, star);
            List<String> originalMethodClassList = mutAnalysis.getOriginalMethod();
            for (int i = 0; i < originalMethodClassList.size(); i++) {
                MethodInfoTable methodInfoTable = mutAnalysis.methodExtraction("picasso","000000",0, originalMethodClassList.get(i));
                methodInfoTableMap.put(methodInfoTable.getMethodSignature(), methodInfoTable);
            }
        }
        for (int j = 0; j < testFilePathList.size(); j++) {
            MUTAnalysis mutAnalysis = new MUTAnalysis(SRC_PATH, testFilePathList.get(j), projectName, repositoryId, star);
            List<String> originalMethodClassList = mutAnalysis.getOriginalMethod();
            for (int i = 0; i < originalMethodClassList.size(); i++) {
                MethodInfoTable methodInfoTable = mutAnalysis.methodExtraction("picasso","000000",1, originalMethodClassList.get(i));
                methodInfoTableMap.put(methodInfoTable.getMethodSignature(),methodInfoTable);
            }
        }
        for (Map.Entry<String, MethodInfoTable> entry : methodInfoTableMap.entrySet()) {
            mut2DB.write2DB(entry.getValue());
//            System.out.println(entry.getValue());
        }
        conn.close();//关闭资源
    }

    public static void updateDependency() throws SQLException {
        conn = DbUtil.getConnection();
        String sql = "select * from method_info_table";
        PreparedStatement ptmt = conn.prepareStatement(sql);
        ResultSet rs = ptmt.executeQuery();
        while(rs.next()){
            int id = rs.getInt(1);
            String[] methodDependency = rs.getString(13).split("$$");
            String[] importDependency = rs.getString(13).split("$$");
        }

        conn.close();
    }
    public static void FirstWrite2Db() throws FileNotFoundException, SQLException {

    }

    public void write2DB(MethodInfoTable methodInfoTable) throws SQLException {
        String sql = "INSERT INTO method_info_table (method_id,method_signature,method_name,parameter_types,class_name,package_name,method_comment,method_comment_keywords,method_code,is_mut,import_dependencies,method_dependencies,project_id,storage_time,return_type) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        PreparedStatement ptmt = conn.prepareStatement(sql);
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
            System.out.println("数据录入成功");
        }
        ptmt.close();//关闭资源
    }
}

