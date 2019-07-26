package art.redoc.sourcegenerator.impl;

import art.redoc.sourcegenerator.AbstractGenerator;
import art.redoc.sourcegenerator.ContentsFilter;
import art.redoc.sourcegenerator.conf.GeneratorConfiguration;
import art.redoc.sourcegenerator.utils.CodeGenerateUtils;

import java.util.List;
import java.util.Map;

import static art.redoc.sourcegenerator.utils.CodeGenerateUtils.contents2value;
import static art.redoc.sourcegenerator.utils.CodeGenerateUtils.removeUnusedImport;
import static art.redoc.sourcegenerator.utils.CodeGenerateUtils.value2contents;

public class RepositoryGenerator extends AbstractGenerator {

    // 模板路径
    private static final String templatePath = "/codetemplate/repository.template";
    private static final String defaultTemplatePath = "/codetemplate/repository-default.template";

    private ContentsFilter filter;

    private final String templateContents;

    public RepositoryGenerator(final GeneratorConfiguration config) {
        super(config, "repository");
        this.initFilter();
        this.templateContents = this.getFileString(this.getTemplatePath(templatePath, defaultTemplatePath));
    }

    @Override
    public void generate() {
        String value = this.filter.filter(this.templateContents);
        final List<String> content = value2contents(value);
        removeUnusedImport(content);
        generateJoinMethod(content);
        this.output(contents2value(content));
    }

    private List<String> generateJoinMethod(List<String> contents) {

        int mainIndex = 0;
        int listIndex = 0;
        for (int i = 0; i < contents.size(); i++) {
            if (CodeGenerateUtils.isNotBlank(contents.get(i)) && contents.get(i).contains(" interface ")) {
                mainIndex = i;
                continue;
            }
            if (CodeGenerateUtils.isNotBlank(contents.get(i)) && contents.get(i).contains("import ")) {
                listIndex = i;
                continue;
            }
        }
        return generateJoinMethodCode(contents, mainIndex, listIndex);
    }

    private List<String> generateJoinMethodCode(List<String> contents, int mainIndex, int listIndex) {
        if (config.getMany2OneObjectsName().size() > 0) {
            final List<String> many2OneObjectsName = config.getMany2OneObjectsName();
            many2OneObjectsName.forEach(x -> {
                String code = "    List<" + this.getModelName() + "> findBy" + CodeGenerateUtils.capitalize(x) + "Id(String id);";
                contents.add(mainIndex + 1, code);
                contents.add(mainIndex + 1, "");
            });
        }
        if (config.getOne2OneObjectsName().size() > 0) {
            final List<String> one2OneObjectsName = config.getOne2OneObjectsName();
            one2OneObjectsName.forEach(x -> {
                String code = "    " + this.getModelName() + " findBy" + CodeGenerateUtils.capitalize(x) + "Id(String id);";
                contents.add(mainIndex + 1, code);
                contents.add(mainIndex + 1, "");
            });
        }
        contents.add(listIndex + 1, "import java.util.List;");
        contents.add(listIndex + 1, "");
        return contents;
    }

    private void initFilter() {
        final Map<String, String> filterMap = super.getFilterMapWithIdType();
        filterMap.put("@Package@", this.getPackage("repository"));
        filterMap.put("@ModelPath@", this.getModelPath());
        filterMap.put("@Model@", this.getModelName());
        this.filter = new ReplaceFilter(filterMap);
    }
}
