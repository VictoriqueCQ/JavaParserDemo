import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.FileNotFoundException;

public class VoidVisitorComplete {
    //    private static final String FILE_PATH = "src/main/java/ReversePolishNotation.java";
    static String path1 = "D:\\picasso\\src\\test\\java\\com\\squareup\\picasso3\\AssetRequestHandlerTest.java";

    public static void main(String[] args) throws FileNotFoundException {
//        CompilationUnit cu = StaticJavaParser.parse(new File(path1));
//        System.err.println(cu.getPrimaryTypeName().toString());
//        System.err.println("----------------------------------------");
//        System.err.println(cu.getComments().toString());
//        System.err.println("----------------------------------------");
//        System.err.println(cu.getImports().toString());
//        System.err.println("----------------------------------------");
//        System.err.println(cu.getModule().toString());
//        System.err.println("----------------------------------------");
//        System.err.println(cu.getPackageDeclaration().toString());
//        System.err.println("----------------------------------------");
//        System.err.println(cu.getTypes().toString());
//        System.err.println("----------------------------------------");
//        System.err.println(cu.toString());
//        System.err.println("----------------------------------------");
//        System.err.println(cu.getChildNodes().get(11).toString());
//        System.err.println("----------------------------------------");
//        System.err.println(cu.getChildNodes().get(11).getChildNodes().get(5).toString());
//        System.err.println("----------------------------------------");
//        System.err.println(cu.getChildNodes().get(11).getChildNodes().get(5).getChildNodes().get(5).toString());
//        System.err.println("----------------------------------------");
//        System.err.println(cu.getChildNodes().get(11).getChildNodes().get(5).getChildNodes().get(5).getChildNodes().get(0));
//        System.err.println("----------------------------------------");
//        System.err.println();
//        System.err.println("----------------------------------------");
//        System.err.println();
//        System.err.println("----------------------------------------");


//        VoidVisitor<?> methodNameVisitor = new MethodNamePrinter();
//        methodNameVisitor.visit(cu, null);
//
//        ModifierVisitor<?> numericLiteralVisitor = new IntegerLiteralModifier();
//        numericLiteralVisitor.visit(cu, null);

//        List<CommentReportEntry> comments = cu.getAllContainedComments()
//                .stream()
//                .map(p -> new CommentReportEntry(p.getClass().getSimpleName(),
//                        p.getContent(),
//                        p.getRange().get().begin.line,
//                        !p.getCommentedNode().isPresent()))
//                .collect(Collectors.toList());
//
//        comments.forEach(System.out::println);

    }

    private static class MethodNamePrinter extends VoidVisitorAdapter<Void> {
        @Override
        public void visit(MethodDeclaration md, Void arg) {
            super.visit(md, arg);
            System.out.println("method name: " + md.getName());
            System.out.println("----------------------------------------");
            System.out.println("method body：" + md.getBody().toString());
            System.out.println("----------------------------------------");
            System.out.println("method declarationAsString：" + md.getDeclarationAsString());
            System.out.println("----------------------------------------");
            System.out.println("method type：" + md.getType().toString());
            System.out.println("----------------------------------------");
            System.out.println("method parameters：" + md.getParameters().toString());
            System.out.println("----------------------------------------");
            System.out.println("method modifier：" + md.getModifiers().toString());
            System.out.println("----------------------------------------");
            System.out.println("method receiver parameter：" + md.getReceiverParameter().toString());
            System.out.println("----------------------------------------");
            System.out.println("method signature：" + md.getSignature().toString());
            System.out.println("----------------------------------------");
            System.out.println("method type parameters：" + md.getTypeParameters().toString());
            System.out.println("----------------------------------------");
            System.out.println("method begin line：" + md.getRange().get().begin.line);
            System.out.println("----------------------------------------");
            System.out.println("method annotations" + md.getAnnotations().toString());
            System.out.println("======================================================================");
        }
    }

    private static class IntegerLiteralModifier extends ModifierVisitor<Void> {
        @Override
        public FieldDeclaration visit(FieldDeclaration fd, Void arg) {
            super.visit(fd, arg);
            return fd;
        }

    }
//    @Test
//    public void solveTypeInSamePackage() throws Exception {
//        CompilationUnit cu = StaticJavaParser.parse(new File(FILE_PATH));
//
//        ResolvedReferenceTypeDeclaration otherClass = EasyMock.createMock(ResolvedReferenceTypeDeclaration.class);
//        EasyMock.expect(otherClass.getQualifiedName()).andReturn("org.javaparser.examples.chapter5.Bar");
//
//                /* Start of the relevant part */
//                MemoryTypeSolver memoryTypeSolver = new MemoryTypeSolver();
//        memoryTypeSolver.addDeclaration(
//                "org.javaparser.examples.chapter5.Bar", otherClass);
//        Context context = new CompilationUnitContext(cu, memoryTypeSolver);
//
//        /* End of the relevant part */
//
//        EasyMock.replay(otherClass);
//
//        SymbolReference<ResolvedTypeDeclaration> ref = context.solveType("Bar", memoryTypeSolver);
//        assertEquals(true, ref.isSolved());
//        assertEquals("org.javaparser.examples.chapter5.Bar", ref.getCorrespondingDeclaration().getQualifiedName());
//    }


}
