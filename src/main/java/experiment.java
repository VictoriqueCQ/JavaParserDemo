import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.io.File;
import java.io.FileNotFoundException;

public class experiment {
    static String ROOT_PATH = "D:/picasso/src/main/java/";

    public static void main(String[] args) throws FileNotFoundException {
        CompilationUnit cu = constructCompilationUnit(null, "src/main/java/A.java");
        ClassOrInterfaceDeclaration myClass = cu.findAll(ClassOrInterfaceDeclaration.class).get(0);

        MethodDeclaration md = cu.findAll(MethodDeclaration.class).get(0);
        JavadocComment javadocComment = new JavadocComment();

//        myClass.setBlockComment("packageName:pn\nclassName:cn\nmethodName:mn");
//        System.out.println(md.getComment().get().getContent()+"packageName:pn\nclassName:cn\nmethodName:mn");
//        md.addOrphanComment(javadocComment);
//        md.setLineComment("packageName:pn\nclassName:cn\nmethodName:mn");
//        md.getAllContainedComments().add(javadocComment);
//        List<Comment> comments = md.getAllContainedComments();
//        Comment comment = md.getJavadocComment().isPresent()?md.getJavadocComment().get():null;
//
//        System.out.println(comments.toString());
//        comments.add(javadocComment);
//        System.out.println(comments.toString());
//        md.setJavadocComment1(javadocComment);
//        for(Comment c:comments){
//            md.setComment(c);
//        }
        Comment c = md.getJavadocComment().get();
        String ss = md.hasJavaDocComment() ? md.getJavadocComment().get().getContent() + "\n" : "";
//        String s = c.getContent()+"\n"+"packageName:pn\nclassName:cn\nmethodName:mn";
        ss += "packageName:pn\nclassName:cn\nmethodName:mn";
        javadocComment.setContent(ss);
        md.setJavadocComment(javadocComment);
        System.out.println(md);
//        System.out.println(md.getSignature());
//        System.out.println(md.getParentNode().toString());

//        MethodDeclaration md1 = cu.findAll(MethodDeclaration.class).get(1);
//        System.out.println(md1.getParentNode().get().findAll(ClassOrInterfaceDeclaration.class).get(0).getNameAsString());
//        System.out.println(myClass.toString());
    }

    public static CompilationUnit constructCompilationUnit(String code, String FILE_PATH) throws FileNotFoundException {
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        TypeSolver javaParserTypeSolver = new JavaParserTypeSolver(new File(ROOT_PATH));
        combinedTypeSolver.add(javaParserTypeSolver);
        TypeSolver reflectionTypeSolver = new ReflectionTypeSolver();
        combinedTypeSolver.add(reflectionTypeSolver);
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
        StaticJavaParser.getConfiguration().setSymbolResolver(symbolSolver);

        return code == null ? StaticJavaParser.parse(new File(FILE_PATH)) : StaticJavaParser.parse(code);
    }
}
