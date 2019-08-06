package art.redoc.sourcegenerator.impl;

import art.redoc.sourcegenerator.AbstractGenerator;
import art.redoc.sourcegenerator.ContentsFilter;
import art.redoc.sourcegenerator.conf.GeneratorConfiguration;

import java.util.Map;

/**
 * Service implement generator.
 *
 * @author redoc
 */
public class ServiceImplGenerator extends AbstractGenerator {

    // User-defined template path
    private static final String customTemplatePath = "/codetemplate/service-impl.template";
    // Default template path
    private static final String defaultTemplatePath = "/codetemplate/service-impl-default.template";

    private ContentsFilter filter;

    private final String templateContents;

    public ServiceImplGenerator(final GeneratorConfiguration config) {
        super(config, "serviceImpl");
        this.initFilter();
        this.templateContents = this.getFileString(this.getTemplatePath(customTemplatePath, defaultTemplatePath));
    }

    @Override
    public void generate() {
        final String value = this.filter.filter(this.templateContents);
        this.output(optimizeCode(value));
    }

    private void initFilter() {
        final String servicePackage = this.getPackage("service") + ".impl";

        final Map<String, String> filterMap = super.createFilterMapWithIdType();
        filterMap.put("@Package@", servicePackage);
        filterMap.put("@ServicePath@", this.getClassPath("service"));
        filterMap.put("@ModelPath@", this.getModelPath());
        filterMap.put("@Model@", this.getModelName());
        filterMap.put("@SeparateModel@", this.getSeparateModelName());
        filterMap.put("@model@", this.getModelNameWithFirstLetterLowercase());
        filterMap.put("@RepositoryPath@", this.getClassPath("repository"));
        this.filter = new ReplaceFilter(filterMap);
    }
}
