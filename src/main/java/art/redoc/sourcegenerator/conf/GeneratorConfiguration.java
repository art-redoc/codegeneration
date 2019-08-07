package art.redoc.sourcegenerator.conf;

import art.redoc.SourceGenerator;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Generator configuration.
 *
 * @author redoc
 */
public class GeneratorConfiguration {

    /**
     * Parent package.
     */
    private final String parentPackage;
    /**
     * Children package.
     */
    private final String childPackage;
    /**
     * File output dir.
     */
    private final String outputDir;
    /**
     * Model class.
     */
    private final Class<?> modelClazz;
    /**
     * Model source path.
     */
    private final String modelSrcPath;
    /**
     * Model properties.
     */
    private final GeneratorModelProperties modelProperties;
    /**
     * Source root path.
     */
    private final String srcRootPath;
    /**
     * Output type.
     */
    private final String outputType;
    /**
     * Whether to override.
     */
    private final boolean override;
    /**
     * ID type.
     */
    private final SourceGenerator.IDType idType;

    /**
     * The model name list that the model relationship is {@code @ManyToOne}.
     */
    private final List<String> many2OneObjectsName = new ArrayList<>();

    /**
     * The model name list that the model relationship is {@code @OneToMany}.
     */
    private final List<String> one2ManyObjectsName = new ArrayList<>();

    /**
     * The model name list that the model relationship is {@code @OneToOne}.
     */
    private final List<String> one2OneObjectsName = new ArrayList<>();

    /**
     * Initial all necessary configuration.
     *
     * @param modelClazz       Model class.
     * @param modelPackageName Model package name.
     * @param outputType       Output type.
     * @param outputDir        Output dir.
     * @param override         Override.
     * @param idType           ID type.
     */
    public GeneratorConfiguration(final Class<?> modelClazz, String modelPackageName, final String outputType, final String outputDir,
                                  final boolean override, SourceGenerator.IDType idType) {
        this.modelClazz = modelClazz;
        this.modelProperties = new GeneratorModelProperties(modelClazz);
        this.outputDir = outputDir;
        this.outputType = outputType;
        this.override = override;

        final URL url = ClassLoader.getSystemClassLoader().getResource("./");
        File root;
        try {
            root = new File(URLDecoder.decode(url.getPath(), "utf-8"));
        } catch (final UnsupportedEncodingException e) {
            throw new RuntimeException("Get model source path error.", e);
        }
        final String classPath = root.getAbsolutePath();
        this.srcRootPath =
                classPath.substring(0, classPath.indexOf("target" + File.separator + "classes"))
                        + "src.main.java.".replace(".", File.separator);
        this.modelSrcPath = this.srcRootPath + this.modelClazz.getName().replace(".", File.separator) + ".java";
        final String modelFullname = modelClazz.getName();
        final int indexOfModelEntity = modelFullname.indexOf("." + modelPackageName + ".");
        final int indexOfModelModel = modelFullname.indexOf("." + modelPackageName + ".");
        int indexOfModel = indexOfModelEntity;
        final int lengthModel = ("." + modelPackageName + ".").length() - 1;
        if (indexOfModelEntity == -1) {
            indexOfModel = indexOfModelModel;
            if (indexOfModelModel == -1) {
                throw new RuntimeException("Given modelName is invalid.");
            }
        }
        final String modelName = modelClazz.getSimpleName();
        this.parentPackage = modelFullname.substring(0, indexOfModel);
        if ((indexOfModel + lengthModel + modelName.length()) < modelFullname.length()) {
            this.childPackage =
                    modelFullname.substring(indexOfModel + lengthModel, modelFullname.length() - modelName.length() - 1);
        } else {
            this.childPackage = null;
        }
        this.idType = idType;
    }

    public String getParentPackage() {
        return this.parentPackage;
    }

    public String getOutputDir() {
        return this.outputDir;
    }

    public Class<?> getModelClazz() {
        return this.modelClazz;
    }

    public String getChildPackage() {
        return this.childPackage;
    }

    public GeneratorModelProperties getModelProperties() {
        return this.modelProperties;
    }

    public String getModelSrcPath() {
        return this.modelSrcPath;
    }

    public String getSrcRootPath() {
        return this.srcRootPath;
    }

    public String getOutputType() {
        return this.outputType;
    }

    public boolean isOverride() {
        return this.override;
    }

    public SourceGenerator.IDType getIdType() {
        return idType;
    }

    public List<String> getMany2OneObjectsName() {
        return many2OneObjectsName;
    }

    public void setMany2OneObjectsName(List<String> many2OneObjectsName) {
        this.many2OneObjectsName.addAll(many2OneObjectsName);
    }

    public List<String> getOne2ManyObjectsName() {
        return one2ManyObjectsName;
    }

    public void setOne2ManyObjectsName(List<String> one2ManyObjectsName) {
        this.one2ManyObjectsName.addAll(one2ManyObjectsName);
    }

    public List<String> getOne2OneObjectsName() {
        return one2OneObjectsName;
    }

    public void setOne2OneObjectsName(List<String> one2OneObjectsName) {
        this.one2OneObjectsName.addAll(one2OneObjectsName);
    }

}
