package net.quickwrite.localizer.processor.generator;

import java.util.ArrayList;
import java.util.List;

public class JEnumFileGenerator extends JClassFileGenerator {
    private final List<String> enumValues;

    public JEnumFileGenerator(final String packageName, final String className) {
        super(packageName, className);

        this.enumValues = new ArrayList<>();
    }

    @Override
    public String generate() {
        return String.format("""
                        /**
                        * This file was automatically generated.
                        * Don't change this class as these changes will be lost.
                        */
                        package %s;
                                        
                        %s
                                        
                        public enum %s {
                            // enum values
                            %s
                        
                            // attributes
                            %s
                            
                            // static constructor
                            static {
                                %s
                            }
                            
                            // constructor
                            %s
                            
                            // methods
                            %s
                        }
                        """,
                getPackageName(),
                formatImports(getImports()),
                getClassName(),
                formatLines(formatEnumValues(this.enumValues), 4),
                formatLines(getAttributes(), 4),
                formatLines(getStaticConstructor(), 8),
                formatLines(getConstructor(), 4),
                formatLines(getMethods(), 4)
        );
    }

    protected List<String> formatEnumValues(final List<String> list) {
        final List<String> result = new ArrayList<>(list.size());

        for (int i = 0; i < list.size(); i++) {
            result.add(list.get(i) + (i != list.size() - 1 ? ',' : ';'));
        }

        return result;
    }

    public JEnumFileGenerator addEnumValue(final String enumValue) {
        this.enumValues.add(enumValue);

        return this;
    }

    public List<String> getEnumValues() {
        return enumValues;
    }
}
