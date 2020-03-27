package Comment;


//import com.tbooster.models.SynonymPair;
//import com.tbooster.models.TokenModel;
//import com.tbooster.utils.WordNet;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

import java.util.*;

/**
  * @Author sunweisong
  * @Date 2020/3/2 4:51 PM
  */
public class CommentAnalysis {

    final static String REGEX_SINGLE_LINE_COMMENT = "\\s*(//)[\\d\\D]*";

    final static String REGEX_MULTI_LINE_COMMENT = "\\s*(/\\*)([\\d\\D]*)(\\*/)";

    final static String REGEX_DOC_COMMENT = "\\s*(/\\*\\*)([\\d\\D]*)(\\*/)";

    final static List<String> posAbbrList = Arrays.asList("NN", "NNS", "NNP", "NNPS"
            , "VB", "VBD", "VBG", "VBN", "VBP", "VBZ");

    /**
      * Measure the distance between two comments.
      * @param commentStr1
      * @param commentStr2
      * @return double
      * @date 2020/3/3 9:22 PM
      * @author sunweisong
      */
//    public static double measureCommentDistance(String commentStr1, String commentStr2) {
//        String commentDescription1 = extractCommentDescription(commentStr1);
//        System.out.println("comment1: " + commentDescription1);
//        String commentDescription2 = extractCommentDescription(commentStr2);
//        System.out.println("comment2: " + commentDescription2);
//        List<TokenModel> tokenModelList1  = commentNLPProcessing(commentDescription1);
//        List<TokenModel> tokenModelList2  = commentNLPProcessing(commentDescription2);
//        Map<String, String> keywordMap1 = getNormalizedToken(tokenModelList1);
//        Map<String, String> keywordMap2 = getNormalizedToken(tokenModelList2);
//        Set<String> keywordSet1 = keywordMap1.keySet();
//        Set<String> keywordSet2 = keywordMap2.keySet();
//
//        Set<String> tempSet = new HashSet<>();
//        // intersection of keywordSet1 and keywordSet2
//        Set<String> intersection = new HashSet<>();
//        tempSet.addAll(keywordSet1);
//        tempSet.retainAll(keywordSet2);
//        intersection.addAll(tempSet);
//        // union of keywordSet1 and keywordSet2
//        Set<String> union = new HashSet<>();
//        tempSet.addAll(keywordSet1);
//        tempSet.addAll(keywordSet2);
//        union.addAll(tempSet);
//        // difference of keywordSet1 and intersection
//        Set<String> differenceOfKeywordSet1AndIntersection = new HashSet<>();
//        keywordSet1.removeAll(intersection);
//        differenceOfKeywordSet1AndIntersection.addAll(keywordSet1);
//        // difference of keywordSet2 and intersection
//        Set<String> differenceOfKeywordSet2AndIntersection = new HashSet<>();
//        keywordSet2.removeAll(intersection);
//        differenceOfKeywordSet2AndIntersection.addAll(keywordSet2);
//        // Get synonyms from WordNet
//        int differenceOfKeywordSet1AndIntersectionSize = differenceOfKeywordSet1AndIntersection.size();
//        int differenceOfKeywordSet2AndIntersectionSize = differenceOfKeywordSet2AndIntersection.size();
//        Map<String, String> wordMap;
//        Map<String, Set<String>> synonymMap;
//        List<SynonymPair> synonymPairList;
//        if (differenceOfKeywordSet1AndIntersectionSize <= differenceOfKeywordSet2AndIntersectionSize) {
//            wordMap = new HashMap<>(differenceOfKeywordSet1AndIntersectionSize);
//            for (String word : differenceOfKeywordSet1AndIntersection) {
//                wordMap.put(word, keywordMap1.get(word));
//            }
//            synonymMap = WordNet.getSynonymsForWords(wordMap);
//            synonymPairList = getSynonymPairs(synonymMap, differenceOfKeywordSet2AndIntersection);
//            if (synonymPairList != null) {
//                // analyze the pos of synonyms. some word may have pos of VERB or NOUN.
//                Iterator<SynonymPair> iterator = synonymPairList.iterator();
//                while(iterator.hasNext()){
//                    SynonymPair synonymPair = iterator.next();
//                    String word1Pos = keywordMap1.get(synonymPair.getWord1());
//                    String word2Pos = keywordMap2.get(synonymPair.getWord2());
//                    if (word1Pos.equals(word2Pos)) {
//                        synonymPair.setPos(word1Pos);
//                    } else {
//                        iterator.remove();
//                    }
//                }
//            }
//        } else {
//            wordMap = new HashMap<>(differenceOfKeywordSet2AndIntersectionSize);
//            for (String word : differenceOfKeywordSet2AndIntersection) {
//                wordMap.put(word, keywordMap2.get(word));
//            }
//            synonymMap = WordNet.getSynonymsForWords(wordMap);
//            synonymPairList = getSynonymPairs(synonymMap, differenceOfKeywordSet1AndIntersection);
//            if (synonymPairList != null) {
//                Iterator<SynonymPair> iterator = synonymPairList.iterator();
//                while(iterator.hasNext()){
//                    SynonymPair synonymPair = iterator.next();
//                    String word1Pos = keywordMap2.get(synonymPair.getWord1());
//                    String word2Pos = keywordMap1.get(synonymPair.getWord2());
//                    if (word1Pos.equals(word2Pos)) {
//                        synonymPair.setPos(word1Pos);
//                    } else {
//                        iterator.remove();
//                    }
//                }
//            }
//        }
//        // calculate the comment distance.
//        int intersectionSize = intersection.size();
//        int unionSize = union.size();
//        int synonymPairNumber = 0;
//        if (synonymPairList != null) {
//            synonymPairNumber = synonymPairList.size();
//        }
//        System.out.println("intersectionSize: " + intersectionSize);
//        System.out.println("unionSize: " + unionSize);
//        System.out.println("synonymPairNumber:" + synonymPairNumber);
//        double distance = 1 - (double) (intersectionSize + synonymPairNumber) / unionSize;
//        return distance;
//    }

    /**
      * Get the synonym pairs.
      * @param  synonymMap
      * @param  differenceOfKeywordSetAndIntersection
      * @return List<SynonymPair>
      * @date 2020/3/5 3:22 PM
      * @author sunweisong
      */
    private static List<SynonymPair> getSynonymPairs(Map<String,Set<String>> synonymMap
            , Set<String> differenceOfKeywordSetAndIntersection) {
        List<SynonymPair> synonymPairs = new ArrayList<>();
        Iterator<Map.Entry<String, Set<String>>> entries = synonymMap.entrySet().iterator();
        while(entries.hasNext()){
            Map.Entry<String, Set<String>> entry = entries.next();
            String word = entry.getKey();
            Set<String> synonymSet = entry.getValue();
            if (synonymSet == null) {
                continue;
            }
            for (String keyword : differenceOfKeywordSetAndIntersection) {
                if (!synonymSet.contains(keyword)) {
                    continue;
                }
                synonymPairs.add(new SynonymPair(word, keyword));
                differenceOfKeywordSetAndIntersection.remove(keyword);
                break;
            }
        }
        if (synonymPairs.size() == 0) {
            synonymPairs = null;
        }
        return synonymPairs;
    }

    /**
      * Normalize the tokens.
      * @param tokenModelList
      * @return Map<String, String>:<token, pos>
      * @throws
      * @date 2020/3/5 2:18 PM
      * @author sunweisong
      */
    private static Map<String, String> getNormalizedToken(List<TokenModel> tokenModelList) {
        Map<String, String> keywordMap = new HashMap<>();
        for (TokenModel tokenModel : tokenModelList) {
            String lemma = tokenModel.getLemma();
            String pos = tokenModel.getPos();
            if (pos.startsWith("N")) {
                keywordMap.put(lemma, "NOUN");
            } else {
                keywordMap.put(lemma, "VERB");
            }
        }
        return keywordMap;
    }

    /**
      * Preprocessing the comments including tokenize, pos and lemma
      * @param commentStr
      * @return List<TokenModel>
      * @date 2020/3/3 7:48 PM
      * @author sunweisong
      */
    public static List<TokenModel> commentNLPProcessing(String commentStr) {
        List<TokenModel> tokenModelList = new ArrayList<>();
        Properties props = new Properties();
        // 设置相应的properties
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma");
        props.setProperty("tokenize.options", "ptb3Escaping=false");
        // 获得StanfordCoreNLP 对象
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        Annotation document = new Annotation(commentStr);
        pipeline.annotate(document);
        if(document.get(CoreAnnotations.SentencesAnnotation.class).size()>0){
            CoreMap sentence = document.get(CoreAnnotations.SentencesAnnotation.class).get(0);
            for (CoreLabel tempToken : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                // this is the text of the token
                String token = tempToken.get(CoreAnnotations.TextAnnotation.class);
                // this is the POS tag of the token
                String pos = tempToken.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                // this is the lemma of the token
                String lemma = tempToken.get(CoreAnnotations.LemmaAnnotation.class);
                if (posAbbrList.contains(pos)) {
                    tokenModelList.add(new TokenModel(token, pos, lemma));
                }
            }
        }
        return tokenModelList;
    }


    /**
      * Extract the comment description from the comment string.
      * @param commentStr
      * @return String
      * @date 2020/3/3 9:12 PM
      * @author sunweisong
      */
    public static String extractCommentDescription(String commentStr) {
        String commentDescription = "";
        if (commentStr.matches(REGEX_DOC_COMMENT)) {
            System.out.println("DOC_COMMENT");
            String[] lineArray = commentStr.split("\n");
            for (String line : lineArray) {
                line = line.trim();
                if (line.contains("/**")) {
                    line = line.substring(3).trim();
                } else {
                    line = line.substring(1).trim();
                }
                if ("".equals(line)) {
                    continue;
                }
                commentDescription = line;
                break;
            }
        } else if (commentStr.matches(REGEX_MULTI_LINE_COMMENT)) {
            System.out.println("MULTI_LINE_COMMENT");
            int start = commentStr.indexOf("/*");
            int end = commentStr.lastIndexOf("*/");
            commentStr = commentStr.substring(start + 2, end).trim();
            commentDescription = commentStr.replaceAll("\n", " ");
        } else if (commentStr.matches(REGEX_SINGLE_LINE_COMMENT)) {
            System.out.println("SINGLE_LINE_COMMENT");
            int start = commentStr.indexOf("//");
            commentDescription = commentStr.substring(start + 2).trim();
        } else {
            System.err.println("未识别的注释！");
        }
        return commentDescription;
    }
}
