package Analysis;

import Utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Fragment2target {
    static String SRC_PATH = "D:/avaire_99215275_49/src";

    public static void main(String[] args) {
        List<String> filenames = new ArrayList<>();
        Utils.findFileList(new File(SRC_PATH), filenames);
        String projectName = SRC_PATH.split("/")[1].split("_")[0];
        String repositoryId = SRC_PATH.split("/")[1].split("_")[1];
        String star = SRC_PATH.split("/")[1].split("_")[2];
        String dir = projectName+"_"+repositoryId+"_"+star+"_"+"Test Class";
        Utils.findFileList(new File(dir),filenames);
    }
}
