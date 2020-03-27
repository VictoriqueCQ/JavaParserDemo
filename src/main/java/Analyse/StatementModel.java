package Analyse;

import com.github.javaparser.ast.expr.MethodCallExpr;

import java.util.List;
import java.util.Set;

/**
  * @Author sunweisong
  * @Date 2020/3/6 12:43 PM
  */
public class StatementModel {
    private int line;
    private String content;
    private Set<String> declareVariableSet;
    private Set<String> useVariableSet;
    private boolean isAssertStatement;
    private List<StatementModel> contextRelatedStatementModelList;

    /**
     * only for assert statement
     */
    private MethodCallExpr methodCallExpr;
    private String suspiciousTestTarget;
    private String realTestTarget;
    private boolean isCreated;

    public StatementModel(int line, String content) {
        this.line = line;
        this.content = content;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Set<String> getDeclareVariableSet() {
        return declareVariableSet;
    }

    public void setDeclareVariableSet(Set<String> declareVariableSet) {
        this.declareVariableSet = declareVariableSet;
    }

    public Set<String> getUseVariableSet() {
        return useVariableSet;
    }

    public void setUseVariableSet(Set<String> useVariableSet) {
        this.useVariableSet = useVariableSet;
    }

    public boolean isAssertStatement() {
        return isAssertStatement;
    }

    public void setAssertStatement(boolean assertStatement) {
        isAssertStatement = assertStatement;
    }

    public MethodCallExpr getMethodCallExpr() {
        return methodCallExpr;
    }

    public void setMethodCallExpr(MethodCallExpr methodCallExpr) {
        this.methodCallExpr = methodCallExpr;
    }

    public List<StatementModel> getContextRelatedStatementModelList() {
        return contextRelatedStatementModelList;
    }

    public void setContextRelatedStatementModelList(List<StatementModel> contextRelatedStatementModelList) {
        this.contextRelatedStatementModelList = contextRelatedStatementModelList;
    }

    public String getSuspiciousTestTarget() {
        return suspiciousTestTarget;
    }

    public void setSuspiciousTestTarget(String suspiciousTestTarget) {
        this.suspiciousTestTarget = suspiciousTestTarget;
    }

    public String getRealTestTarget() {
        return realTestTarget;
    }

    public void setRealTestTarget(String realTestTarget) {
        this.realTestTarget = realTestTarget;
    }

    public boolean getIsCreated() {
        return isCreated;
    }

    public void setIsCreated(boolean isCreated) {
        this.isCreated = isCreated;
    }

    @Override
    public String toString() {
        return "main.java.Analyse.StatementModel{" +
                "line=" + line +
                ", content='" + content + '\'' +
                ", declareVariableSet=" + declareVariableSet +
                ", useVariableSet=" + useVariableSet +
                ", isAssertStatement=" + isAssertStatement +
                ", methodCallExpr=" + methodCallExpr +
                ", suspiciousTestTarget='" + suspiciousTestTarget + '\'' +
                ", realTestTarget='" + realTestTarget + '\'' +
                '}';
    }
}
