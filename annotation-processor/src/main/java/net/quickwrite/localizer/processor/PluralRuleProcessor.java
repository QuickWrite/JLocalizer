package net.quickwrite.localizer.processor;

import com.google.auto.service.AutoService;
import net.quickwrite.localizer.processor.generator.JEnumFileGenerator;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import redempt.redlex.bnf.BNFParser;
import redempt.redlex.data.Token;
import redempt.redlex.processing.CullStrategy;
import redempt.redlex.processing.Lexer;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.util.*;

@SupportedAnnotationTypes("net.quickwrite.localizer.processor.PluralRuleGen")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class PluralRuleProcessor extends AbstractProcessor {
    @Override
    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
        final File file = getInputFile("/plural-rule-syntax.bnf");

        final Lexer lexer = BNFParser.createLexer(file.toPath());
        lexer.setRuleByName(CullStrategy.DELETE_ALL, "sep", "samples");
        lexer.setRuleByName(CullStrategy.LIFT_CHILDREN, "digit");

        final Document document;
        try {
            document = getXMLDocument("/plurals.xml");
        } catch (final ParserConfigurationException | IOException | SAXException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "An exception occurred while trying to parse the input file for @PluralRuleGen -> \n" +
                            Arrays.toString(e.getStackTrace())
            );

            return false;
        }

        final NodeList list = document.getElementsByTagName("pluralRules");

        try {
            for (final Element element : roundEnv.getRootElements()) {

                final PluralRuleGen ruleGen = element.getAnnotation(PluralRuleGen.class);

                if (ruleGen == null) {
                    continue;
                }

                try {
                    writePluralRuleFile(
                            ruleGen.packageName(),
                            getAnnotationClassValue(element, PluralRuleGen.class, "operand"),
                            getAnnotationClassValue(element, PluralRuleGen.class, "category"),
                            lexer,
                            list
                    );
                } catch (final IOException exception) {
                    exception.printStackTrace();
                }
            }
        } catch (NoSuchElementException e) {
            e.printStackTrace();
        }

        return true;
    }

    private File getInputFile(final String path) throws NullPointerException {
        return new File(Objects.requireNonNull(this.getClass().getResource(path)).getFile());
    }

    private Document getXMLDocument(final String path)
            throws ParserConfigurationException, IOException, SAXException, NullPointerException {
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newDefaultInstance();
        return dbf.newDocumentBuilder().parse(getInputFile(path));
    }

    private void writePluralRuleFile(final String packageName,
                                     final InternalClass operand,
                                     final InternalClass category,
                                     final Lexer lexer,
                                     final NodeList pluralRules)
            throws IOException {
        final JEnumFileGenerator generator = new JEnumFileGenerator(
                packageName,
                "PluralRuleChecker"
        );

        generator.addImport("java.util.function.Function");

        if (!operand.getPackageName().equals(packageName)) {
            generator.addImport(operand.getFullName());
        }

        if (!category.getPackageName().equals(packageName)) {
            generator.addImport(category.getFullName());
        }

        generator.addAttribute("private final Function<" +
                operand.getClassName() +
                ", " +
                category.getClassName() +
                "> localizationFunction;");

        for (int i = 0; i < pluralRules.getLength(); i++) {
            final StringBuilder builder = new StringBuilder();
            final Node node = pluralRules.item(i);

            final String[] locales = node.getAttributes().getNamedItem("locales").getNodeValue().split(" ");
            final String firstLocale = locales[0].toUpperCase();
            builder.append(firstLocale);

            builder.append("((operand) -> {\n");

            for (final PluralRuleTuple tuple : getRulesForLangs(lexer, node)) {
                builder.append("   if(")
                        .append(tuple.unwrappedConditions())
                        .append(") {\n")
                        .append("      return ").append(category.getClassName()).append(".")
                        .append(tuple.type().toUpperCase())
                        .append(";\n   }\n\n");
            }
            builder.append("   return ").append(category.getClassName()).append(".OTHER;\n})");

            generator.addEnumValue(builder.toString());

            for (int j = 1; j < locales.length; j++) {
                final String result = locales[j].toUpperCase() +
                        "(" + firstLocale + ".localizationFunction)";

                generator.addEnumValue(result);
            }
        }

        generator.addConstructor(
                String.format("""
                PluralRuleChecker(final Function<%s, %s> localizationFunction) {
                    this.localizationFunction = localizationFunction;
                }
                """,
                operand.getClassName(),
                category.getClassName()
            )
        );

        generator.addMethod(
                String.format("""
                        public %s getCategory(final %s operand) {
                            return this.localizationFunction.apply(operand);
                        }
                        """,
                        category.getClassName(),
                        operand.getClassName()
                        )
                )
                .addMethod("""
                        private static boolean isInRange(int value, int min, int max) {
                            return min <= value && value <= max;
                        }
                        """)
                .addMethod("""
                        private static boolean isInRange(long value, int min, int max) {
                            return min <= value && value <= max;
                        }
                        """)
                .addMethod("""
                        private static boolean isInRange(double value, int min, int max) {
                            return min <= value && value <= max;
                        }
                        """);

        generateClass(generator.generate());
    }

    private List<PluralRuleTuple> getRulesForLangs(final Lexer lexer, final Node node) {
        final List<PluralRuleTuple> list = new ArrayList<>();

        final NodeList children = node.getChildNodes();
        for (int j = 0; j < children.getLength(); j++) {
            if (children.item(j).getNodeName().equals("#text")) {
                continue;
            }

            final String type = children.item(j).getAttributes().getNamedItem("count").getTextContent();
            if (type.equals("other")) {
                continue;
            }

            final Token token = lexer.tokenize(children.item(j).getTextContent());

            final String condition = unwrapConditions(token.allByName("and_condition"));

            list.add(new PluralRuleTuple(type, condition));
        }

        return list;
    }

    private String unwrapConditions(final List<Token> tokens) {
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < tokens.size(); i++) {
            if (i != 0) {
                builder.append(" || ");
            }

            List<Token> inRelationTokens = tokens.get(i).allByName("in_relation");
            for (int j = 0; j < inRelationTokens.size(); j++) {
                if (j != 0) {
                    builder.append(" && ");
                }

                unwrapInRelation(inRelationTokens.get(j), builder);
            }
        }

        return builder.toString();
    }

    private void unwrapInRelation(final Token token, final StringBuilder builder) {
        final List<Token> rangeToken = unwrapRangeList(token.getChildren()[2]);
        if (rangeToken.size() != 1) {
            builder.append("(");
        }

        for (int i = 0; i < rangeToken.size(); i++) {
            if (i != 0) {
                builder.append(" || ");
            }

            boolean isRange = rangeToken.get(i).getType().getName().equals("range");

            builder.append("(");
            final String operator = token.getChildren()[1].getValue();

            if (isRange) {
                if (operator.equals("!=")) {
                    builder.append("!");
                }
                builder.append("isInRange(");
            }
            unwrapExpr(token.getChildren()[0], builder);

            if (isRange) {
                builder.append(", ");
                final String[] rangeValues = unwrapRange(rangeToken.get(i));
                builder.append(rangeValues[0]);
                builder.append(", ");
                builder.append(rangeValues[1]);
                builder.append(")");
            } else {
                builder.append(" ");
                if (operator.equals("=")) {
                    builder.append("=");
                }
                builder.append(operator);
                builder.append(" ");
                builder.append(rangeToken.get(i).getValue());
            }
            builder.append(")");
        }

        if (rangeToken.size() != 1) {
            builder.append(")");
        }
    }

    private List<Token> unwrapRangeList(final Token token) {
        assert !token.getType().getName().equals("range_list");

        List<Token> list = new ArrayList<>();

        final Token[] tokens = token.getChildren();
        list.add(tokens[0].getChildren()[0]);

        if (tokens.length == 1) {
            return list;
        }

        for (final Token nextToken : token.getChildren()[1].getChildren()) {
            list.add(nextToken.getChildren()[1].getChildren()[0]);
        }

        return list;
    }

    private void unwrapExpr(final Token token, final StringBuilder builder) {
        assert !token.getType().getName().equals("expr");

        final Token[] children = token.getChildren();

        builder.append("operand.");
        builder.append(children[0].getValue());
        builder.append("()");

        if (!(children.length > 1)) {
            return;
        }

        builder.append(" % ");
        builder.append(children[1].allByName("value").get(0).getValue());
    }

    private String[] unwrapRange(final Token token) {
        assert !token.getType().getName().equals("range");
        return new String[]{token.getChildren()[0].getValue(), token.getChildren()[2].getValue()};
    }

    private void generateClass(final String file) throws IOException {
        final JavaFileObject sourceFile = processingEnv.getFiler().createSourceFile("net.quickwrite.localizer.PluralRuleChecker");
        final Writer writer = sourceFile.openWriter();
        writer.write(file);
        writer.close();
    }

    private Optional<? extends AnnotationMirror> getAnnotationMirror(final Element element, final Class<? extends Annotation> annotationClass) {
        final var annotationClassName = annotationClass.getName();

        return element.getAnnotationMirrors().stream()
                .filter(m -> m.getAnnotationType().toString().equals(annotationClassName))
                .findFirst();
    }

    private Optional<? extends AnnotationValue> getAnnotationValue(final AnnotationMirror annotationMirror, final String name) {
        final Elements elementUtils = this.processingEnv.getElementUtils();
        final Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues = elementUtils.getElementValuesWithDefaults(annotationMirror);

        return elementValues.keySet().stream()
                .filter(k -> k.getSimpleName().toString().equals(name))
                .map(elementValues::get)
                .findAny();
    }

    private InternalClass getAnnotationClassValue(final Element element, final Class<? extends Annotation> annotationClass, final String name) {
        final AnnotationMirror mirror = getAnnotationMirror(element, annotationClass).orElseThrow();

        final String className = getAnnotationValue(mirror, name).orElseThrow().getValue().toString();

        return new InternalClass(className);
    }

    private static record PluralRuleTuple(String type, String unwrappedConditions) {

    }

    private static class InternalClass {
        private final String packageName;
        private final String className;
        private final String fullName;

        public InternalClass(final String classIdentifier) {
            final String[] values = classIdentifier.split("\\.");

            this.className = values[values.length - 1];

            final StringBuilder builder = new StringBuilder();

            for (int i = 0; i < values.length - 1; i++) {
                if (i != 0) {
                    builder.append('.');
                }

                builder.append(values[i]);
            }

            this.packageName = builder.toString();

            this.fullName = classIdentifier;
        }

        public String getPackageName() {
            return this.packageName;
        }

        public String getClassName() {
            return this.className;
        }

        public String getFullName() {
            return this.fullName;
        }
    }
}
