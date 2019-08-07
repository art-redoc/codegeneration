package art.redoc.sourcegenerator.impl;

import art.redoc.sourcegenerator.AbstractGenerator;
import art.redoc.sourcegenerator.ContentsFilter;
import art.redoc.sourcegenerator.conf.GeneratorConfiguration;

import java.util.List;
import java.util.Map;

/**
 * Convertor generator.
 *
 * @author redoc
 */
public class ConvertorGenerator extends AbstractGenerator {

    // User-defined template path
    private static final String customTemplatePath = "/codetemplate/convertor.template";
    // Default template path
    private static final String defaultTemplatePath = "/codetemplate/convertor-default.template";

    private ContentsFilter filter;

    private final String templateContents;
    private String setModelCode;
    private String setDTOCode;

    public ConvertorGenerator(final GeneratorConfiguration config) {
        super(config, "convertor");
        this.initConvertingCode();
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
        filterMap.put("@Package@", this.getPackage("convertor"));
        filterMap.put("@DTOPath@", this.getClassPath("dto"));
        filterMap.put("@ModelPath@", this.getModelPath());
        filterMap.put("@ServicePath@", this.getClassPath("service"));
        filterMap.put("@Model@", this.getModelName());
        filterMap.put("@SeparateModel@", this.getSeparateModelName());
        filterMap.put("@model@", this.getModelNameWithFirstLetterLowercase());
        filterMap.put(" +@setModelCode@", this.setModelCode);
        filterMap.put(" +@setDTOCode@", this.setDTOCode);
        this.filter = new ReplaceFilter(filterMap);
    }

    /**
     * Generate getter and setter of properties in convertor.
     */
    private void initConvertingCode() {
        final StringBuilder setModelCode = new StringBuilder();
        final StringBuilder setDTOCode = new StringBuilder();
        final List<String> one2OneObjectsName = config.getOne2OneObjectsName();
        final List<String> many2OneObjectsName = config.getMany2OneObjectsName();
        final List<String> one2ManyObjectsName = config.getOne2ManyObjectsName();
        // Filter out other join model like '@ManyToOne', '@OneToMany' or '@OneToOne' and generate the remaining properties.
        this.config.getModelProperties().getProperties().stream().filter(x ->
                !one2OneObjectsName.contains(x.getName())
                        && !many2OneObjectsName.contains(x.getName())
                        && !one2ManyObjectsName.contains(x.getName())
        ).forEach(property -> {
            setModelCode.append("        model.").append(property.getSetter()).append("(dto.")
                    .append(property.getGetter())
                    .append("());").append(System.lineSeparator());
            setDTOCode.append("        dto.").append(property.getSetter()).append("(model.")
                    .append(property.getGetter())
                    .append("());").append(System.lineSeparator());
        });
        this.setModelCode = setModelCode.toString();
        this.setDTOCode = setDTOCode.toString();
    }
}
