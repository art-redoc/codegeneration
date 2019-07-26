package art.redoc.sourcegenerator.impl;

import art.redoc.sourcegenerator.AbstractGenerator;
import art.redoc.sourcegenerator.ContentsFilter;
import art.redoc.sourcegenerator.conf.GeneratorConfiguration;
import art.redoc.sourcegenerator.conts.CodeGenerateConts;
import art.redoc.sourcegenerator.utils.CodeGenerateUtils;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static art.redoc.sourcegenerator.utils.CodeGenerateUtils.*;

public class DtoGenerator extends AbstractGenerator {

    private ContentsFilter filter;

    private final String modelSource;

    public DtoGenerator(final GeneratorConfiguration config) {
        super(config, "dto");
        final String modelSource = this.getFileString(this.config.getModelSrcPath());
        final List<String> contentLine = this.getContentLine();
        this.modelSource = this.initApiDocs(modelSource);
        checkMany2Many();
        this.initFilter();
        handleManyToOne(contentLine);
        handleOneToMany(contentLine);
        handleOneToOne(contentLine);
    }

    // 初始化API文档信息
    private String initApiDocs(final String source) {
        final Pattern pattern = Pattern.compile("(/\\*\\*\\s+\\*\\s)(.+)(\\s+\\*/)");
        final String result = source;
        final Matcher matcher = pattern.matcher(source);
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
        final String value = this.filter.filter(this.modelSource);
        final List<String> content = removeJoinObjectsCode(value);
        removeUnusedImport(content);
        formatCode(content);
        addJoinObjectsCode(content);
        this.output(contents2value(content));
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
        final List<String> one2ManyObjectsName = config.getOne2ManyObjectsName();
        generateJoinObjectsCode(content, classIndex, one2OneObjectsName);
        generateJoinObjectsCode(content, classIndex, many2OneObjectsName);
        generateJoinObjectsCode(content, classIndex, one2ManyObjectsName);
    }

    private void generateJoinObjectsCode(List<String> content, Integer classIndex, List<String> objectsName) {
        objectsName.forEach(x -> {
            String code = "    private " + config.getIdType().getType() + " " + x + "Id;";
            content.add(classIndex + 1, code);
            content.add(classIndex + 1, "");
        });
    }

    private List<String> removeJoinObjectsCode(String value) {
        final List<String> contents = CodeGenerateUtils.value2contents(value);
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
        toBeRemoved.forEach(x -> x.forEach(y -> contents.set(y, CodeGenerateConts.TO_BE_REMOVED)));
        contents.removeAll(Arrays.asList(CodeGenerateConts.TO_BE_REMOVED));
        return contents;
    }

    private void addToBeRemovedContent(List<List<Integer>> toBeRemoved, List<String> one2OneObjectsName, String content,
                                       List<String> contents, int index) {
        one2OneObjectsName.forEach(one2Many -> {
            List<Integer> toBeRemovedIndex = new ArrayList<>();
            // todo pattern
            if (content.contains("private") && (content.contains(" " + one2Many + " ") || content.contains(" " + one2Many + ";"))) {
                toBeRemovedIndex.add(index);
                int current = index - 1;
                while (!contents.get(current).contains("private")) {
                    toBeRemovedIndex.add(current);
                    current--;
                }
            }
            if (toBeRemovedIndex.size() > 0) {
                toBeRemoved.add(toBeRemovedIndex);
            }
        });
    }

    private void handleOneToMany(List<String> contentLine) {
        final Iterator<String> iterator = contentLine.iterator();
        List<String> one2ManyObject = new ArrayList<>();
        while (iterator.hasNext()) {
            final String next = iterator.next();
            if (next.contains("@OneToMany")) {
                String content = iterator.next();
                while (CodeGenerateUtils.isBlank(content) || !content.contains("private")) {
                    content = iterator.next();
                }
                one2ManyObject.add(content.trim().replaceAll(" +", " ").split(" ")[2].replace(";", ""));
            }
        }
        config.setOne2ManyObjectsName(one2ManyObject);
    }

    private void handleManyToOne(List<String> contentLine) {
        final Iterator<String> iterator = contentLine.iterator();
        List<String> many2OneObject = new ArrayList<>();
        while (iterator.hasNext()) {
            final String next = iterator.next();
            if (next.contains("@ManyToOne")) {
                String content = iterator.next();
                while (CodeGenerateUtils.isBlank(content) || !content.contains("private")) {
                    content = iterator.next();
                }
                many2OneObject.add(content.trim().replaceAll(" +", " ").split(" ")[2].replace(";", ""));
            }
        }
        config.setMany2OneObjectsName(many2OneObject);
    }

    private void handleOneToOne(List<String> contentLine) {
        final Iterator<String> iterator = contentLine.iterator();
        List<String> one2OneObject = new ArrayList<>();
        while (iterator.hasNext()) {
            final String next = iterator.next();
            if (next.contains("@OneToOne")) {
                String content = iterator.next();
                while (CodeGenerateUtils.isBlank(content) || !content.contains("private")) {
                    content = iterator.next();
                }
                one2OneObject.add(content.trim().replaceAll(" +", " ").split(" ")[2].replace(";", ""));
            }
        }
        config.setOne2OneObjectsName(one2OneObject);
    }

    private List<String> getContentLine() {
        try {
            final InputStream is = this.getInputStream(this.config.getModelSrcPath());
            return IOUtils.readLines(is, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void initFilter() {
        final Map<String, String> filterMap = super.getFilterMapWithIdType();
        final String packageStr = "package " + this.getPackage("dto") + ";" + System.lineSeparator()
                + "import art.redoc.base.dto.AbstractAuditDTO;";
        filterMap.put("package.+;", packageStr);
        filterMap.put("import javax\\.persistence.+\\s+", "");
        filterMap.put("@Entity\\s+", "");
        filterMap.put("@Table.+\\s+", "");
        filterMap.put("@Lob\\s+", "");
        filterMap.put("\\r\\n\\s+.+serialVersionUID+.+\\n?", "");
        filterMap.put("\\n\\s+@Column.+", "");
        filterMap.put("\\n\\s+@Enumerated.+", "");
//        filterMap.put("\\n\\s+@OneToOne.+", "");
//        filterMap.put("\\n\\s+@OneToMany.+", "");
//        filterMap.put("\\n\\s+@ManyToMany.+", "");
//        filterMap.put("\\n\\s+@ManyToOne.+", "");
        filterMap.put("\\n\\s+@org.hibernate.annotations.Type.+", "");
        filterMap.put("\\n\\s+@Table.+", "");
        filterMap.put("\\n\\s+@Temporal.+", "");
        final String dtoClass = "public class @Model@DTO extends AbstractAuditDTO {".replace("@Model@",
                this.config.getModelClazz().getSimpleName());
        filterMap.put("public class.+\\{", dtoClass);
        this.filter = new ReplaceFilter(filterMap);
    }
}
