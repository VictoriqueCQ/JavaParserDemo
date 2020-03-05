import com.github.javaparser.ast.body.MethodDeclaration;

public class MethodModel {
    private String packageName;
    private String className;
    private String methodName;
    private MethodDeclaration methodBody;

    public MethodModel(String packageName, String className, String methodName, MethodDeclaration methodBody) {
        this.packageName = packageName;
        this.className = className;
        this.methodName = methodName;
        this.methodBody = methodBody;
    }

}
