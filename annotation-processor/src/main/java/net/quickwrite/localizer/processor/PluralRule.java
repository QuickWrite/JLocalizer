package net.quickwrite.localizer.processor;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import redempt.redlex.data.Token;
import redempt.redlex.processing.Lexer;

import java.util.ArrayList;
import java.util.List;

public class PluralRule {
    private final String[] locales;
    private final List<PluralRuleTuple> pluralRules;

    public PluralRule(final Lexer lexer, final Node node) {
        this.locales = node.getAttributes().getNamedItem("locales").getNodeValue().split(" ");
        this.pluralRules = getRulesForLangs(lexer, node);
    }

    public String[] getLocales() {
        return locales;
    }

    public List<PluralRuleTuple> getPluralRules() {
        return pluralRules;
    }

    public record PluralRuleTuple(String type, String unwrappedConditions) {

    }

    private static List<PluralRuleTuple> getRulesForLangs(final Lexer lexer, final Node node) {
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

    private static String unwrapConditions(final List<Token> tokens) {
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

    private static void unwrapInRelation(final Token token, final StringBuilder builder) {
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

    private static String[] unwrapRange(final Token token) {
        assert !token.getType().getName().equals("range");
        return new String[]{token.getChildren()[0].getValue(), token.getChildren()[2].getValue()};
    }

    private static List<Token> unwrapRangeList(final Token token) {
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

    private static void unwrapExpr(final Token token, final StringBuilder builder) {
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
}
