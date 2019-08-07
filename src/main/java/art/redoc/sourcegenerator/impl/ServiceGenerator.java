package art.redoc.sourcegenerator.impl;

import art.redoc.sourcegenerator.AbstractGenerator;
import art.redoc.sourcegenerator.ContentsFilter;
import art.redoc.sourcegenerator.conf.GeneratorConfiguration;

import java.util.Map;

/**
 * Service generator.
 *
 * @author redoc
 */
public class ServiceGenerator extends AbstractGenerator {

    // User-defined template path
    private static final String customTemplatePath = "/codetemplate/service.template";
    // Default template path
    private static final String defaultTemplatePath = "/codetemplate/service-default.template";

    private ContentsFilter filter;

    private final String templateContents;

    public ServiceGenerator(final GeneratorConfiguration config) {
        super(config, "service");
        this.initFilter();
        this.templateContents = this.getFileString(this.getTemplatePath(customTemplatePath, defaultTemplatePath));
    }

    @Override
    public void generate() {
        final String value = this.filter.filter(this.templateContents);
        this.output(optimizeCode(value));
    }

    private void initFilter() {
        final Map<String, String> filterMap = super.createFilterMapWithIdType();
        filterMap.put("@Package@", this.getPackage("service"));
        filterMap.put("@ModelPath@", this.getModelPath());
        filterMap.put("@Model@", this.getModelName());
        filterMap.put("@SeparateModel@", this.getSeparateModelName());
        this.filter = new ReplaceFilter(filterMap);
    }
}
