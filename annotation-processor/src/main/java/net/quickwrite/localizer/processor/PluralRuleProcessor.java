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
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@SupportedAnnotationTypes("net.quickwrite.localizer.processor.PluralRuleGen")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class PluralRuleProcessor extends AbstractProcessor {
    @Override
    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
        final File file = getInputFile("/plural-rule-syntax.bnf");

        final Lexer lexer = BNFParser.createLexer(file.toPath());
        lexer.setRuleByName(CullStrategy.DELETE_ALL, "sep", "samples");


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

        for (int i = 0; i < list.getLength(); i++) {
            final Node node = list.item(i);

            final NodeList children = node.getChildNodes();
            for(int j = 0; j < children.getLength(); j++) {
                if (children.item(j).getNodeName().equals("#text")) {
                    continue;
                }

                if (children.item(j).getAttributes().getNamedItem("count").getTextContent().equals("other")) {
                    continue;
                }

                final Token token = lexer.tokenize(children.item(j).getTextContent());

                // TODO: do something with the token

                unwrapConditions(token.allByName("and_condition"));
            }
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

    private void unwrapConditions(final List<Token> tokens) {
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
    }

    private void unwrapInRelation(final Token token, final StringBuilder builder) {
        unwrapExpr(token.getChildren()[0], builder);
        builder.append(" ");

        final String value = token.getChildren()[1].getValue();
        if (value.equals("=")) {
            builder.append("=");
        }
        builder.append(value);
        builder.append(" ");
        unwrapRangeList(token.getChildren()[2], builder);
    }

    private void unwrapRangeList(final Token token, final StringBuilder builder) {
        assert !token.getType().getName().equals("range_list");

        final Token[] tokens = token.getChildren();
        final Token firstToken = tokens[0].getChildren()[0];
        if (firstToken.getType().getName().equals("value")) {
            builder.append(firstToken.getValue());
        } else {
            unwrapRange(firstToken, builder);
        }

        if (tokens.length == 1) {
            return;
        }

        //builder.append(" ");
        for (Token token1 : token.getChildren()[1].getChildren()) {
            builder.append(",");
            final Token moreToken = token1.getChildren()[1].getChildren()[0];
            if (moreToken.getType().getName().equals("value")) {
                builder.append(moreToken.getValue());
                continue;
            }

            unwrapRange(moreToken, builder);
        }
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

    private void unwrapRange(final Token token, final StringBuilder builder) {
        assert !token.getType().getName().equals("range");
        builder.append(token.getChildren()[0].getValue());
        builder.append(" to ");
        builder.append(token.getChildren()[2].getValue());
    }
}
