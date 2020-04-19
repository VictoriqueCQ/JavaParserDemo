package Write2Db;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.PackageDeclaration;
import Model.ImportInfoTable;
import Utils.DbUtil;
import Utils.MD5Util;
import Utils.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Import2DB {
    public static final String URL = "jdbc:mysql://localhost:3306/tcdb";
    public static final String USER = "root";
    public static final String PASSWORD = "123456";
    static String ROOT_PATH = "D:/picasso/src/";

    public static void main(String[] args) throws FileNotFoundException, SQLException {
        writeImport2Db();

    }

    public static List<ImportInfoTable> writeImport2Db() throws SQLException, FileNotFoundException {
        Set<String> importDeclarationSet = new HashSet<>();

        List<String> filenames = new ArrayList<>();
        Utils.findFileList(new File(ROOT_PATH), filenames);
        for (String filename : filenames) {
            if (filename.contains(".java")) {
//                System.out.println(filename);
                CompilationUnit cu = Utils.constructCompilationUnit(null, filename, ROOT_PATH);
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
        Connection conn = DbUtil.getConnection();
        List<ImportInfoTable> importInfoTableList = new ArrayList<>();
        for (String importString : importDeclarationSet) {
            Timestamp time = new Timestamp(System.currentTimeMillis());
            //获取连接

            //sql, 每行加空格
            String sql = "INSERT INTO import_info_table (import_id,import_string,storage_time) VALUES (?,?,?)";
            //预编译
            PreparedStatement ptmt = conn.prepareStatement(sql); //预编译SQL，减少sql执行
            ImportInfoTable importInfoTable = new ImportInfoTable();
            importInfoTable.setImportString(importString);
            importInfoTable.setStorageTime(time);
            importInfoTable.setImportId(MD5Util.getMD5(importString));
            importInfoTableList.add(importInfoTable);
            ptmt.setString(1, importInfoTable.getImportId());
            ptmt.setString(2, importInfoTable.getImportString());
            ptmt.setTimestamp(3, importInfoTable.getStorageTime());
            int res = ptmt.executeUpdate();//执行sql语句
            if (res > 0) {
                System.out.println("数据录入成功");
            }
            ptmt.close();//关闭资源

        }
        conn.close();//关闭资源
        return importInfoTableList;
    }
}
