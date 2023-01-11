package net.quickwrite.localizer.processor;

import com.google.auto.service.AutoService;
import net.quickwrite.localizer.processor.generator.JEnumFileGenerator;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import redempt.redlex.bnf.BNFParser;
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
    private List<PluralRule> pluralRules;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        final File file = getInputFile("/plural-rule-syntax.bnf");

        final Lexer lexer = BNFParser.createLexer(file.toPath());
        lexer.setRuleByName(CullStrategy.DELETE_ALL, "sep", "samples");
        lexer.setRuleByName(CullStrategy.LIFT_CHILDREN, "digit");

        final NodeList nodePluralRules;
        try {
            final Document document = getXMLDocument("/plurals.xml");

            nodePluralRules = document.getElementsByTagName("pluralRules");
        } catch (final ParserConfigurationException | IOException | SAXException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "An exception occurred while trying to parse the input file for @PluralRuleGen -> \n" +
                            Arrays.toString(e.getStackTrace())
            );

            return;
        }

        pluralRules = new ArrayList<>(nodePluralRules.getLength());

        for (int i = 0; i < nodePluralRules.getLength(); i++) {
            pluralRules.add(new PluralRule(lexer, nodePluralRules.item(i)));
        }
    }

    @Override
    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
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
                            getAnnotationClassValue(element, PluralRuleGen.class, "category")
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
                                     final InternalClass category)
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

        for (final PluralRule rule : pluralRules) {
            final StringBuilder builder = new StringBuilder();

            final String firstLocale = rule.getLocales()[0];
            builder.append(firstLocale);

            builder.append("((operand) -> {\n");

            for (final PluralRule.PluralRuleTuple tuple : rule.getPluralRules()) {
                builder.append("   if(")
                        .append(tuple.unwrappedConditions())
                        .append(") {\n")
                        .append("      return ").append(category.getClassName()).append(".")
                        .append(tuple.type().toUpperCase())
                        .append(";\n   }\n\n");
            }
            builder.append("   return ").append(category.getClassName()).append(".OTHER;\n})");

            generator.addEnumValue(builder.toString());

            for (int j = 1; j < rule.getLocales().length; j++) {
                final String result = rule.getLocales()[j].toUpperCase() +
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
