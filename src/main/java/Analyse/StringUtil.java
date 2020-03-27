package Analyse;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
  * @Author sunweisong
  * @Date 2020/3/6 9:43 PM
  */
public class StringUtil {

    /**
     *
     * @param
     * @return
     * @throws
     * @date 2018/4/25 下午6:12
     * @author sunweisong
     */
    public static String removeContentsInQuotes(String string) {
        Matcher matcher = Pattern.compile("\"(.*?)\"").matcher(string);
        while(matcher.find()){
            string = string.replace(matcher.group(), "");
        }
        return string;
    }

    /**
     *
     * @param
     * @return
     * @throws
     * @date 2018/5/10 上午10:56
     * @author sunweisong
     */
    public static boolean isParenthesesMatchInString(String string) {
        char[] alphaArray = string.toCharArray();
        int count = 0;
        int index = 0;
        for (char alpha : alphaArray) {
            index++;
            if (alpha == '(') {
                count++;
            }
            if (alpha == ')') {
                count--;
            }
            if (count == 0 && index == alphaArray.length) {
                return true;
            }
        }
        return false;
    }
}
