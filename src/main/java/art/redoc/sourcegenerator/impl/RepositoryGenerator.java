package art.redoc.sourcegenerator.impl;

import art.redoc.sourcegenerator.AbstractGenerator;
import art.redoc.sourcegenerator.ContentsFilter;
import art.redoc.sourcegenerator.conf.GeneratorConfiguration;
import art.redoc.sourcegenerator.utils.GeneratorUtils;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static art.redoc.sourcegenerator.utils.GeneratorUtils.contents2value;
import static art.redoc.sourcegenerator.utils.GeneratorUtils.value2contents;

/**
 * Repository generator.
 *
 * @author redoc
 */
public class RepositoryGenerator extends AbstractGenerator {

    // User-defined template path
    private static final String customTemplatePath = "/codetemplate/repository.template";
    // Default template path
    private static final String defaultTemplatePath = "/codetemplate/repository-default.template";

    private ContentsFilter filter;

    private final String templateContents;

    public RepositoryGenerator(final GeneratorConfiguration config) {
        super(config, "repository");
        this.initFilter();
        this.templateContents = this.getFileString(this.getTemplatePath(customTemplatePath, defaultTemplatePath));
    }

    @Override
    public void generate() {
        String value = this.filter.filter(this.templateContents);
        final List<String> contents = value2contents(value);
        generateJoinMethod(contents);
        this.output(this.optimizeCode(contents2value(contents)));
    }

    /**
     * Generate join method for repository.
     *
     * @param contents Contents.
     * @return Contents.
     */
    private void generateJoinMethod(List<String> contents) {
        int interfaceIndex = 0;
        int lastImportIndex = 0;
        final Pattern interfaceCompile = Pattern.compile("\\s*public\\s+interface\\s+.*\\{\\s*");
        final Pattern lastImportCompile = Pattern.compile("import\\s.*");
        for (int i = 0; i < contents.size(); i++) {
            final String content = contents.get(i);
            // Find the index of the 'interface' location.
            if (interfaceCompile.matcher(content).matches()) {
                interfaceIndex = i;
                continue;
            }
            // Find the index of last 'import'
            if (lastImportCompile.matcher(content).matches()) {
                lastImportIndex = i;
            }
        }
        generateMany2OneMethodCode(contents, interfaceIndex, lastImportIndex);
        generateOne2OneMethodCode(contents, interfaceIndex);
    }

    /**
     * Generate many to one method code.
     * <pre>
     * e.g.
     * {@code @ManyToOne}
     * {@code private User user;}
     *
     * Required to generate the code as follows:
     * {@code List<User> findByUserId(Long userId);}
     * </pre>
     *
     * @param contents        Contents.
     * @param interfaceIndex  Interface index.
     * @param lastImportIndex Last import index.
     */
    private void generateMany2OneMethodCode(List<String> contents, int interfaceIndex, int lastImportIndex) {
        if (config.getMany2OneObjectsName().size() > 0) {
            final List<String> many2OneObjectsName = config.getMany2OneObjectsName();
            many2OneObjectsName.forEach(x -> {
                String code =
                        "    List<" + this.getModelName() + "> findBy" + GeneratorUtils.capitalize(x) + "Id("
                                + this.config.getIdType().getType() + " " + x + "Id);";
                contents.add(interfaceIndex + 1, code);
                contents.add(interfaceIndex + 1, "");
            });

            // Add additional import.
            contents.add(lastImportIndex + 1, "import java.util.List;");
            contents.add(lastImportIndex + 1, "");
        }
    }

    /**
     * Generate one to one method code.
     * <pre>
     * e.g.
     * {@code @OneToOne}
     * {@code private User user;}
     *
     * Required to generate the code as follows:
     * {@code User findByUserId(Long userId);}
     * </pre>
     *
     * @param contents       Contents.
     * @param interfaceIndex Interface index.
     */
    private void generateOne2OneMethodCode(List<String> contents, int interfaceIndex) {
        if (config.getOne2OneObjectsName().size() > 0) {
            final List<String> one2OneObjectsName = config.getOne2OneObjectsName();
            one2OneObjectsName.forEach(x -> {
                String code =
                        "    " + this.getModelName() + " findBy" + GeneratorUtils.capitalize(x) + "Id("
                                + this.config.getIdType().getType() + " " + x + "Id);";
                contents.add(interfaceIndex + 1, code);
                contents.add(interfaceIndex + 1, "");
            });
        }
    }

    private void initFilter() {
        final Map<String, String> filterMap = super.createFilterMapWithIdType();
        filterMap.put("@Package@", this.getPackage("repository"));
        filterMap.put("@ModelPath@", this.getModelPath());
        filterMap.put("@Model@", this.getModelName());
        filterMap.put("@SeparateModel@", this.getSeparateModelName());
        this.filter = new ReplaceFilter(filterMap);
    }
}
