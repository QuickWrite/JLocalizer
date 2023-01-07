package net.quickwrite.localizer.processor;

import com.google.auto.service.AutoService;
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
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.*;

@SupportedAnnotationTypes("net.quickwrite.localizer.processor.PluralRuleGen")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class PluralRuleProcessor extends AbstractProcessor {
    private static boolean test = false;

    @Override
    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
        if (test) {
            return true;
        }
        test = true;

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
            writePluralRuleFile(lexer, list);
        } catch (final IOException exception) {
            exception.printStackTrace();
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

    private void writePluralRuleFile(final Lexer lexer, final NodeList pluralRules) throws IOException {
        final StringBuilder out = new StringBuilder();
            out.append("package net.quickwrite.localizer;\n\n");
            out.append("""
                    import java.util.HashMap;
                    import java.util.Map;
                    import java.util.function.Function;
                    
                    """);

            out.append("public class PluralRuleChecker {\n");
            out.append("   private static Map<String, Function<PluralOperand, PluralCategory>> PLURALIZATION_MAP;\n\n");
            out.append("   static {\n" +
                    "      PLURALIZATION_MAP = new HashMap<>(" + pluralRules.getLength() + ");\n");
            for (int i = 0; i < pluralRules.getLength(); i++) {
                final Node node = pluralRules.item(i);

                out.append("addRule(new String[] { ");

                final String[] locales = node.getAttributes().getNamedItem("locales").getNodeValue().split(" ");
                for(int j = 0; j < locales.length; j++) {
                    if (j != 0) {
                        out.append(", ");
                    }

                    out.append("\"");
                    out.append(locales[j]);
                    out.append("\"");
                }

                out.append(" }, operand -> {");

                for(final PluralRuleTuple tuple : getRulesForLangs(lexer, node)) {
                    out.append("   if(");
                    out.append(tuple.unwrappedConditions());
                    out.append(") {\n");
                    out.append("      return PluralCategory.");
                    out.append(tuple.type().toUpperCase());
                    out.append(";\n   }");
                }
                out.append("   return PluralCategory.OTHER;\n}\n");
                out.append(");\n\n");
            }

            out.append("   }\n");

            out.append("""
                       public static Function<PluralOperand, PluralCategory> getPluralizer(final String key) {
                            return PLURALIZATION_MAP.get(key);
                       }
                       """);

            out.append("""
                        private static void addRule(final String[] cultures, final Function<PluralOperand, PluralCategory> rule) {
                            for (final String culture : cultures) {
                                PLURALIZATION_MAP.put(culture, rule);
                            }
                        }
                        """);

            out.append("""
                       private static boolean isInRange(int value, int min, int max) {
                          return min <= value && value <= max;
                       }
                       """);
            out.append("""
                       private static boolean isInRange(double value, int min, int max) {
                          return min <= value && value <= max;
                       }
                       """);

            out.append("}");

            generateClass(out.toString());
    }

    private List<PluralRuleTuple> getRulesForLangs(final Lexer lexer, final Node node) {
        final List<PluralRuleTuple> list = new ArrayList<>();

        final NodeList children = node.getChildNodes();
        for(int j = 0; j < children.getLength(); j++) {
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
        for(int i = 0; i < tokens.size(); i++) {
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
        return new String[] {token.getChildren()[0].getValue(), token.getChildren()[2].getValue()};
    }

    private void generateClass(final String file) throws IOException {
        final JavaFileObject sourceFile = processingEnv.getFiler().createSourceFile("net.quickwrite.localizer.PluralRuleChecker");
        final Writer writer = sourceFile.openWriter();
        writer.write(file);
        writer.close();
    }

    private static record PluralRuleTuple(String type, String unwrappedConditions) {

    }
}
