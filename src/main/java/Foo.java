public class Foo {
    int foo = 100;
    int i = 0;
    void fooMethod(int i,String b,Foo f){
        i = i+1;
        System.out.println(i);
        System.out.println("it is foo class fooMethod method");
    }
    static class AAA{
        static int i;
        AAA(int i){
            this.i = i;
        }
        static void print(){
            System.out.println(AAA.i);
        }

    }
    void aMethod(){
        i = i+1;
    }
}
