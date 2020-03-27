public class A {
    /**
     * Explicitly opt-out to having a placeholder set when calling {@code into}.
     * <p>
     * By default, Picasso will either set a supplied placeholder or clear the target
     * {@link ImageView} in order to ensure behavior in situations where views are recycled. This
     * method will prevent that behavior and retain any already set image.
     */
    public String foo(Object param){
        //comment1
        //comment2
        System.out.println(1);
        System.out.println("hi");
        System.out.println(param);
        return "hello world;=";
    }
    class B {

        public void run(){
            System.out.println("run");
        }
    }
}
