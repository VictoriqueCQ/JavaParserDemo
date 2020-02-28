import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;

public class TestClass {
    public static int GlobalVariable;

    void testMethod() {
        String localVariable = "abc";
        GlobalVariable = 0;
        CombinedTypeSolver typeSolver = new CombinedTypeSolver();
    }
}
