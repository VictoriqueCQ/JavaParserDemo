import com.github.javaparser.ast.Node;

public class VariableTuple<T extends Node> {
    boolean isGlobal;
    String type;
    String name;
    T original;

    VariableTuple(boolean isGlobal, String type, String name, T original) {
        this.isGlobal = isGlobal;
        this.type = type;
        this.name = name;
        this.original = original;
    }

    void printString() {
        System.out.println(
                "isGlobal: " + isGlobal +
                        "\ntype: " + type +
                        "\nname: " + name +
                        "\n================================================="
        );
    }
}