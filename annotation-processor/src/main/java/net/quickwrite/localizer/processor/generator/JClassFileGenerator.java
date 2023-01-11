package net.quickwrite.localizer.processor.generator;

import java.util.ArrayList;
import java.util.List;

public class JClassFileGenerator {
    private final String packageName;
    private final String className;

    private final List<String> imports;

    private final List<String> attributes;
    private final List<String> methods;

    private final List<String> staticConstructor;

    private final List<String> constructor;

    public JClassFileGenerator(final String packageName, final String className) {
        this.packageName = packageName;
        this.className = className;

        this.imports = new ArrayList<>();

        this.attributes = new ArrayList<>();
        this.methods = new ArrayList<>();

        this.staticConstructor = new ArrayList<>();
        this.constructor = new ArrayList<>();
    }

    public JClassFileGenerator addImport(final String importPath) {
        this.imports.add(importPath);

        return this;
    }

    public JClassFileGenerator addAttribute(final String attribute) {
        this.attributes.add(attribute);

        return this;
    }

    public JClassFileGenerator addMethod(final String method) {
        this.methods.add(method);

        return this;
    }

    public JClassFileGenerator addStaticConstructorElement(final String staticConstructorElement) {
        this.staticConstructor.add(staticConstructorElement);

        return this;
    }

    public JClassFileGenerator addConstructor(final String constructor) {
        this.constructor.add(constructor);

        return this;
    }

    public String generate() {
        return String.format("""
                        /**
                        * This file was automatically generated.
                        * Don't change this class as these changes will be lost.
                        */
                        package %s;
                                        
                        %s
                                        
                        public class %s {
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
                this.packageName,
                formatImports(this.imports),
                this.className,
                formatLines(this.attributes, 4),
                formatLines(this.staticConstructor, 8),
                formatLines(this.constructor, 4),
                formatLines(this.methods, 4)
        );
    }

    protected static String formatImports(final List<String> imports) {
        final StringBuilder builder = new StringBuilder();

        for (int i = 0; i < imports.size(); i++) {
            builder.append("import ");
            builder.append(imports.get(i));
            builder.append(";");

            if (i != imports.size() - 1) {
                builder.append("\n");
            }
        }

        return builder.toString();
    }

    protected static String formatLines(final List<String> list, final int indent) {
        final StringBuilder builder = new StringBuilder();

        for (int i = 0; i < list.size(); i++) {
            final String[] stringList = list.get(i).split("\n");

            for (int j = 0; j < stringList.length; j++) {
                if (!(i == 0 && j == 0)) {
                    builder.append(" ".repeat(Math.max(0, indent)));
                }

                builder.append(stringList[j]);

                if (!(i == list.size() - 1 && j == stringList.length - 1)) {
                    builder.append("\n");
                }
            }

            if (i != list.size() - 1) {
                builder.append("\n");
            }
        }

        return builder.toString();
    }

    public String getPackageName() {
        return packageName;
    }

    public String getClassName() {
        return className;
    }

    public List<String> getImports() {
        return imports;
    }

    public List<String> getAttributes() {
        return attributes;
    }

    public List<String> getMethods() {
        return methods;
    }

    public List<String> getStaticConstructor() {
        return staticConstructor;
    }

    public List<String> getConstructor() {
        return constructor;
    }
}
