import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;

import static java.awt.color.ICC_ProfileGray.CLASS_ABSTRACT;
public class Bar {

    int aMethod(int a) {
        a = a + 1;
        return a;
    }

    void bMethod() {
        int a = 0;
        int b = 0;
//        aMethod(a);

        Foo f = new Foo();
        int fooVariable = f.foo;
        f.fooMethod(a,"aaa",f);

        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        aMethod(CLASS_ABSTRACT);
//        Uri.parse("file:///android_asset/foo/bar.png");
        new Foo.AAA(b).print();
    }
}
