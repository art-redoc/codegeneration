package art.redoc.sourcegenerator.impl;

import art.redoc.sourcegenerator.AbstractGenerator;
import art.redoc.sourcegenerator.conf.GeneratorConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static art.redoc.sourcegenerator.conts.GeneratorConstants.TO_BE_REMOVED;
import static art.redoc.sourcegenerator.utils.GeneratorUtils.contents2value;
import static art.redoc.sourcegenerator.utils.GeneratorUtils.removeUnusedContent;
import static art.redoc.sourcegenerator.utils.GeneratorUtils.value2contents;

/**
 * DTO generator.
 *
 * @author redoc
 */
public class DtoGenerator extends AbstractGenerator {

    private final static Pattern variablesPattern = Pattern.compile("\\s*private\\s+\\S+\\s+\\S+.*;");
    private final Pattern classPattern = Pattern.compile(".+\\s+class\\s+" + this.getModelName() + ".+\\s+extends\\s+AbstractAuditDTO.*\\{.*");
    private final String separateModelName = this.getSeparateModelName();
    private final List<String> comments = new ArrayList<String>() {
        {
            add("");
            add("/**");
            add(" * " + separateModelName + " DTO.");
            add(" *");
            add(" * @author code generator");
            add(" */");
        }
    };


    private final String modelSource;

    /**
     * Initial and validate resource.
     *
     * @param config Configuration.
     */
    public DtoGenerator(final GeneratorConfiguration config) {
        super(config, "dto");
        this.modelSource = this.getFileString(this.config.getModelSrcPath());
        final List<String> contentLine = value2contents(modelSource);
        checkMany2Many();
        handleManyToOne(contentLine);
        handleOneToMany(contentLine);
        handleOneToOne(contentLine);
    }

    /**
     * Forbidden to use {@code @ManyToMany}.
     */
    private void checkMany2Many() {
        if (modelSource.contains("@ManyToMany")) {
            throw new RuntimeException("Forbidden to use @ManyToMany, please use two @ManyToOne instead.");
        }
    }

    @Override
    public void generate() {
        final List<String> contents = this.removeAndReplaceUnusedCode4Dto(this.modelSource);
        removeJoinRelatedCode(contents);
        addJoinRelatedCode(contents);
        generateJavaDoc(contents);
        this.output(this.optimizeCode(contents2value(contents)));
    }

    /**
     * Generate the java doc of DTO.
     *
     * @param contents Contents.
     */
    private void generateJavaDoc(List<String> contents) {
        final Pattern importCompile = Pattern.compile("import\\s.*");
        final Pattern lastCompile = Pattern.compile("@[a-zA-Z].*|.+\\s+class\\s+" + this.getModelName() + ".+\\s+extends\\s" +
                "+AbstractAuditDTO.*\\{.*");
        int importIndex = 0;
        // Find the index of last 'import'
        for (int i = 0; i < contents.size(); i++) {
            final String content = contents.get(i);
            if (importCompile.matcher(content).matches()) {
                importIndex = i;
            }
        }
        // Mark these code as deleted,
        // these code start with the index of last 'import' and end with the line where class or annotation is located.
        for (int i = importIndex + 1; i < contents.size(); i++) {
            final String content = contents.get(i);
            if (lastCompile.matcher(content).matches()) {
                break;
            }
            contents.set(i, TO_BE_REMOVED);
        }
        removeUnusedContent(contents);
        // Generate the java doc.
        final int index = importIndex + 1;
        Collections.reverse(comments);
        comments.forEach(x -> contents.add(index, x));
    }

    /**
     * Generate related code.
     * <pre>
     * e.g.     {@code private User user;}
     * generate {@code private Long userId;}
     * </pre>
     *
     * @param contents Contents.
     */
    private void addJoinRelatedCode(List<String> contents) {
        Integer classIndex = null;
        for (int i = 0; i < contents.size(); i++) {
            if (classPattern.matcher(contents.get(i)).matches()) {
                classIndex = i;
                break;
            }
        }
        final List<String> one2OneObjectsName = config.getOne2OneObjectsName();
        final List<String> many2OneObjectsName = config.getMany2OneObjectsName();
        generateJoinRelatedCode(contents, classIndex, one2OneObjectsName);
        generateJoinRelatedCode(contents, classIndex, many2OneObjectsName);
    }

    /**
     * Generate related code.
     * <pre>
     * e.g.     {@code private User user;}
     * generate {@code private Long userId;}
     * </pre>
     *
     * @param contents Contents.
     */
    private void generateJoinRelatedCode(List<String> contents, Integer classIndex, List<String> objectsName) {
        objectsName.forEach(x -> {
            String code = "    private " + config.getIdType().getType() + " " + x + "Id;";
            contents.add(classIndex + 1, code);
            contents.add(classIndex + 1, "");
        });
    }

    /**
     * Remove about join code, the join code include {@code @OneToMany},{@code @ManyToOne} and {@code @OneToOne}.
     *
     * @param contents Contents.
     */
    private void removeJoinRelatedCode(List<String> contents) {
        markJoinRelatedCode("@ManyToOne", contents);
        markJoinRelatedCode("@OneToMany", contents);
        markJoinRelatedCode("@OneToOne", contents);
        removeUnusedContent(contents);
    }

    /**
     * Mark the join related code as {@code TO_BE_REMOVED}.
     *
     * @param annotationName Annotation name.
     * @param contents       Contents.
     */
    private void markJoinRelatedCode(String annotationName, List<String> contents) {
        final List<Integer> indexes = new ArrayList<>();
        // The first character after the annotation cannot be a number or a letter.
        final Pattern compile = Pattern.compile("\\s*" + annotationName + "[^a-zA-Z\\d].*");
        for (int i = 0; i < contents.size(); i++) {
            String content = contents.get(i) + " ";
            if (compile.matcher(content).matches()) {
                contents.set(i, TO_BE_REMOVED);
                indexes.add(i);
            }
        }
        indexes.forEach(x -> {
            // Mark forwards.
            int beforeIndex = x - 1;
            String beforeContent = contents.get(beforeIndex);
            while (!variablesPattern.matcher(beforeContent).matches()) {
                contents.set(beforeIndex, TO_BE_REMOVED);
                beforeIndex--;
                beforeContent = contents.get(beforeIndex);
            }
            // Mark backwards.
            int afterIndex = x + 1;
            String afterContent = contents.get(afterIndex);
            while (!variablesPattern.matcher(afterContent).matches()) {
                contents.set(afterIndex, TO_BE_REMOVED);
                afterIndex++;
                afterContent = contents.get(afterIndex);
            }
            // Mark the annotated variables.
            contents.set(afterIndex, TO_BE_REMOVED);
        });
    }

    /**
     * Handle {@code @OneToMany} annotation in model.
     *
     * @param contents Contents.
     */
    private void handleOneToMany(List<String> contents) {
        List<String> one2ManyObject = getJoinVariableNameByAnnotation(contents, "@OneToMany");
        config.setOne2ManyObjectsName(one2ManyObject);
    }

    /**
     * Handle {@code @ManyToOne} annotation in model.
     *
     * @param contents Contents.
     */
    private void handleManyToOne(List<String> contents) {
        List<String> many2OneObject = getJoinVariableNameByAnnotation(contents, "@ManyToOne");
        config.setMany2OneObjectsName(many2OneObject);
    }

    /**
     * Handle {@code @OneToOne} annotation in model.
     *
     * @param contents Contents.
     */
    private void handleOneToOne(List<String> contents) {
        List<String> one2OneObject = getJoinVariableNameByAnnotation(contents, "@OneToOne");
        config.setOne2OneObjectsName(one2OneObject);
    }

    /**
     * Get the variable name list, the variable name include {@code @OneToMany},{@code @ManyToOne} and {@code @OneToOne}.
     *
     * <pre>
     * e.g.          {@code @ManyToOne
     *                      private User user;}
     * The result is {@code Arrays.asList("user")}.
     * </pre>
     *
     * @param contents       Contents.
     * @param annotationName Annotation name.
     * @return Variable name list.
     */
    private List<String> getJoinVariableNameByAnnotation(List<String> contents, String annotationName) {
        final List<String> result = new ArrayList<>();
        final Iterator<String> iterator = contents.iterator();
        while (iterator.hasNext()) {
            final String next = iterator.next();
            if (next.contains(annotationName)) {
                String content = iterator.next();
                // If the current line is not the same line as the variable, the pointer continues to move down.
                while (!variablesPattern.matcher(content).matches()) {
                    content = iterator.next();
                }
                // The line of the variable has been found, split by " " and take the third element.
                result.add(content.trim().replaceAll(" +", " ").split(" ")[2].replace(";", ""));
            }
        }
        return result;
    }

    /**
     * Remove and replace unused code.
     *
     * @param modelSource Model source.
     * @return Contents.
     */
    private List<String> removeAndReplaceUnusedCode4Dto(String modelSource) {
        final List<String> contents = value2contents(modelSource);
        removeUnusedCode(contents);
        replaceCode(contents);
        return contents;
    }

    /**
     * Replace code.
     *
     * @param contents Contents.
     */
    private void replaceCode(List<String> contents) {
        Map<Pattern, String> patterns = new HashMap<>();
        // Replace about package.
        patterns.put(Pattern.compile("package.+;"), "package " + this.getPackage("dto") + ";" + System.lineSeparator()
                + "import art.redoc.base.dto.AbstractAuditDTO;");
        // Replace about 'public class extents xxx'.
        patterns.put(Pattern.compile("public class.+\\{"), "public class " + this.config.getModelClazz().getSimpleName() + "DTO extends " +
                "AbstractAuditDTO {");
        // Replace ID type by configuration.
        patterns.put(Pattern.compile("@IDType@"), config.getIdType().getType());
        for (int i = 0; i < contents.size(); i++) {
            String content = contents.get(i);
            final int index = i;
            patterns.forEach((k, v) -> {
                if (k.matcher(content).matches()) {
                    contents.set(index, v);
                }
            });
        }
    }

    /**
     * Remove unused code by pattern.
     *
     * @param contents Contents.
     */
    private void removeUnusedCode(List<String> contents) {
        List<Pattern> patterns = new ArrayList<>();
        patterns.add(Pattern.compile("import javax\\.persistence+.+;"));
        patterns.add(Pattern.compile("\\s*@Entity+.*"));
        patterns.add(Pattern.compile("\\s*@Table+.*"));
        patterns.add(Pattern.compile("\\s*@Index+.*"));
        patterns.add(Pattern.compile("\\s*.+serialVersionUID.+;"));
        patterns.add(Pattern.compile("\\s*@Lob+.*"));
        patterns.add(Pattern.compile("\\s*@Column.*"));
        patterns.add(Pattern.compile("\\s*@Enumerated.*"));
        patterns.add(Pattern.compile("\\s*\\(type = \"yes_no\"\\).*"));
        patterns.add(Pattern.compile("\\s*@Temporal.*"));
        for (int i = 0; i < contents.size(); i++) {
            String content = contents.get(i);
            final int index = i;
            patterns.forEach(x -> {
                if (x.matcher(content).matches()) {
                    contents.set(index, TO_BE_REMOVED);
                }
            });
        }
        removeUnusedContent(contents);
    }
}
