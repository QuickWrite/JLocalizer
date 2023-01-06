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
}
