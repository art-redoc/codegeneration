package art.redoc.sourcegenerator.impl;

import art.redoc.sourcegenerator.AbstractGenerator;
import art.redoc.sourcegenerator.conf.GeneratorConfiguration;
import art.redoc.sourcegenerator.conts.CodeGenerateConstants;
import art.redoc.sourcegenerator.utils.CodeGenerateUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static art.redoc.sourcegenerator.utils.CodeGenerateUtils.contents2value;
import static art.redoc.sourcegenerator.utils.CodeGenerateUtils.removeUnusedContent;
import static art.redoc.sourcegenerator.utils.CodeGenerateUtils.value2contents;

/**
 * DTO generator.
 *
 * @author redoc
 */
public class DtoGenerator extends AbstractGenerator {

    private final String modelSource;

    public DtoGenerator(final GeneratorConfiguration config) {
        super(config, "dto");
        final String modelSource = this.getFileString(this.config.getModelSrcPath());
        final List<String> contentLine = this.getContents();
        this.modelSource = this.initApiDocs(modelSource);
        checkMany2Many();
        handleManyToOne(contentLine);
        handleOneToMany(contentLine);
        handleOneToOne(contentLine);
    }

    private String initApiDocs(final String source) {
//        final Pattern pattern = Pattern.compile("(/\\*\\*\\s+\\*\\s)(.+)(\\s+\\*/)");
        final String result = source;
//        final Matcher matcher = pattern.matcher(source);
        //        while (matcher.find()) {
        //            final String originalStr = matcher.group(0);
        //            final String docStr = matcher.group(2);
        //            result = result.replace(originalStr,
        //                    originalStr + System.lineSeparator() + "    @ApiModelProperty(\"" + docStr + "\")");
        //        }
        return result;
    }

    private void checkMany2Many() {
        if (modelSource.contains("@ManyToMany")) {
            throw new RuntimeException("Forbidden to use @ManyToMany, please use two @ManyToOne instead.");
        }
    }

    @Override
    public void generate() {
        final String value = this.removeAndReplaceUnusedCode4Dto(this.modelSource);
        final List<String> contents = removeJoinObjectsCode(value);
        addJoinObjectsCode(contents);
        generateJavaDoc(contents);
        this.output(this.optimizeCode(contents2value(contents)));
    }

    private void generateJavaDoc(List<String> contents) {
        final Pattern importCompile = Pattern.compile("import\\s.*");
        final Pattern lastCompile = Pattern.compile("@[a-zA-Z].*|public\\sclass\\s.*");
        int importIndex = 0;
        for (int i = 0; i < contents.size(); i++) {
            final String content = contents.get(i);
            if (importCompile.matcher(content).matches()) {
                importIndex = i;
            }
        }

        for (int i = importIndex + 1; i < contents.size(); i++) {
            final String content = contents.get(i);
            if (lastCompile.matcher(content).matches()) {
                break;
            }
            contents.set(i, CodeGenerateConstants.TO_BE_REMOVED);
        }
        removeUnusedContent(contents);
        List<String> comments = new ArrayList<>();
        comments.add("");
        comments.add("/**");
        comments.add(" * " + this.getSeparateModelName() + " DTO.");
        comments.add(" *");
        comments.add(" * @author code generator");
        comments.add(" */");
        final int index = importIndex + 1;
        Collections.reverse(comments);
        comments.forEach(x -> contents.add(index, x));
    }

    private void addJoinObjectsCode(List<String> content) {
        Integer classIndex = null;
        for (int i = 0; i < content.size(); i++) {
            if (content.get(i).contains(" extends AbstractAuditDTO {")) {
                classIndex = i;
                break;
            }
        }
        final List<String> one2OneObjectsName = config.getOne2OneObjectsName();
        final List<String> many2OneObjectsName = config.getMany2OneObjectsName();
        generateJoinObjectsCode(content, classIndex, one2OneObjectsName);
        generateJoinObjectsCode(content, classIndex, many2OneObjectsName);
    }

    private void generateJoinObjectsCode(List<String> content, Integer classIndex, List<String> objectsName) {
        objectsName.forEach(x -> {
            String code = "    private " + config.getIdType().getType() + " " + x + "Id;";
            content.add(classIndex + 1, code);
            content.add(classIndex + 1, "");
        });
    }

    private List<String> removeJoinObjectsCode(String value) {
        final List<String> contents = value2contents(value);
        final List<String> one2OneObjectsName = config.getOne2OneObjectsName();
        final List<String> many2OneObjectsName = config.getMany2OneObjectsName();
        final List<String> one2ManyObjectsName = config.getOne2ManyObjectsName();

        List<List<Integer>> toBeRemoved = new ArrayList<>();

        for (int i = 0; i < contents.size(); i++) {
            String content = contents.get(i);
//            int index = i;
            addToBeRemovedContent(toBeRemoved, one2OneObjectsName, content, contents, i);
            addToBeRemovedContent(toBeRemoved, many2OneObjectsName, content, contents, i);
            addToBeRemovedContent(toBeRemoved, one2ManyObjectsName, content, contents, i);
        }
        toBeRemoved.forEach(x -> x.forEach(y -> contents.set(y, CodeGenerateConstants.TO_BE_REMOVED)));
        removeUnusedContent(contents);
        return contents;
    }

    /**
     * 标记需要被删除的content，针对三种情况的table join，one2one many2one one2many.
     *
     * @param toBeRemoved
     * @param objectsNames
     * @param content
     * @param contents
     * @param index
     */
    private void addToBeRemovedContent(List<List<Integer>> toBeRemoved, List<String> objectsNames, String content,
                                       List<String> contents, int index) {
        objectsNames.forEach(objectsName -> {
            List<Integer> toBeRemovedIndex = new ArrayList<>();
            // todo pattern
            if (content.contains("private") && (content.contains(" " + objectsName + " ") || content.contains(" " + objectsName + ";"))) {
                toBeRemovedIndex.add(index);
                int current = index - 1;
                //需要删除 table join 字段上那些多于的注解，比如 @ManyToOne @NotNull 等等 一口气都删了
                while (!contents.get(current).contains("private") && !contents.get(current).contains(" class ")) {
                    toBeRemovedIndex.add(current);
                    current--;
                }
            }
            if (toBeRemovedIndex.size() > 0) {
                toBeRemoved.add(toBeRemovedIndex);
            }
        });
    }

    private void handleOneToMany(List<String> contents) {
        List<String> one2ManyObject = getJoinObjectsByAnnotation(contents, "@OneToMany");
        config.setOne2ManyObjectsName(one2ManyObject);
    }

    private void handleManyToOne(List<String> contents) {
        List<String> many2OneObject = getJoinObjectsByAnnotation(contents, "@ManyToOne");
        config.setMany2OneObjectsName(many2OneObject);
    }

    private void handleOneToOne(List<String> contents) {
        List<String> one2OneObject = getJoinObjectsByAnnotation(contents, "@OneToOne");
        config.setOne2OneObjectsName(one2OneObject);
    }

    private List<String> getJoinObjectsByAnnotation(List<String> contents, String annotationName) {
        final List<String> result = new ArrayList<>();
        final Iterator<String> iterator = contents.iterator();
        final Pattern compile = Pattern.compile("\\s*private\\s+.*;");
        while (iterator.hasNext()) {
            final String next = iterator.next();
            if (next.contains(annotationName)) {
                String content = iterator.next();
                while (!compile.matcher(content).matches()) {
                    content = iterator.next();
                }
                result.add(content.trim().replaceAll(" +", " ").split(" ")[2].replace(";", ""));
            }
        }
        return result;
    }

    private List<String> getContents() {
        try {
            final InputStream is = this.getInputStream(this.config.getModelSrcPath());
            return CodeGenerateUtils.readLines(is, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    private String removeAndReplaceUnusedCode4Dto(String modelSource) {
        final List<String> contents = value2contents(modelSource);
        removeUnusedCode(contents);
        replaceCode(contents);
        return contents2value(contents);
    }

    private void replaceCode(List<String> contents) {
        Map<Pattern, String> patterns = new HashMap<>();
        patterns.put(Pattern.compile("package.+;"), "package " + this.getPackage("dto") + ";" + System.lineSeparator()
                + "import art.redoc.base.dto.AbstractAuditDTO;");
        patterns.put(Pattern.compile("public class.+\\{"), "public class " + this.config.getModelClazz().getSimpleName() + "DTO extends " +
                "AbstractAuditDTO {");
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
                    contents.set(index, CodeGenerateConstants.TO_BE_REMOVED);
                }
            });
        }
        removeUnusedContent(contents);
    }
}
