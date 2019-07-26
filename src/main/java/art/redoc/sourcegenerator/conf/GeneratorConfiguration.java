package art.redoc.sourcegenerator.conf;

import art.redoc.SourceGenerator;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

public class GeneratorConfiguration {

    /**
     * 父包
     */
    private final String parentPackage;
    /**
     * 子包
     */
    private final String childPackage;
    /**
     * 代码生成目标目录
     */
    private final String outputDir;
    /**
     * Model类
     */
    private final Class<?> modelClazz;
    /**
     * Model源码路径
     */
    private final String modelSrcPath;
    /**
     * Model属性
     */
    private final ModelProperties modelProperties;
    /**
     * 源码根目录
     */
    private final String srcRootPath;
    /**
     * 输出类型
     */
    private final String outputType;
    /**
     * 是否覆盖原文件
     */
    private final boolean override;
    /**
     * 主键类型
     */
    private final SourceGenerator.IDType idType;

    private final List<String> many2OneObjectsName = new ArrayList<>();

    private final List<String> one2ManyObjectsName = new ArrayList<>();

    private final List<String> one2OneObjectsName = new ArrayList<>();

    public GeneratorConfiguration(final Class<?> modelClazz, String entityPackageName, final String outputType, final String outputDir,
                                  final boolean override, SourceGenerator.IDType idType) {
        this.modelClazz = modelClazz;
        this.modelProperties = new ModelProperties(modelClazz);
        this.outputDir = outputDir;
        this.outputType = outputType;
        this.override = override;

        final URL url = ClassLoader.getSystemClassLoader().getResource("./");
        File root;
        try {
            root = new File(URLDecoder.decode(url.getPath(), "utf-8"));
        } catch (final UnsupportedEncodingException e) {
            throw new RuntimeException("获取Model源码路径失败.", e);
        }
        final String classPath = root.getAbsolutePath();
        this.srcRootPath =
                classPath.substring(0, classPath.indexOf("target" + File.separator + "classes"))
                        + "src.main.java.".replace(".", File.separator);
        this.modelSrcPath = this.srcRootPath + this.modelClazz.getName().replace(".", File.separator) + ".java";
        final String modelFullname = modelClazz.getName();
        final int indexOfModelEntity = modelFullname.indexOf("." + entityPackageName + ".");
        final int indexOfModelModel = modelFullname.indexOf("." + entityPackageName + ".");
        int indexOfModel = indexOfModelEntity;
        final int lengthModel = ("." + entityPackageName + ".").length() - 1;
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

    public ModelProperties getModelProperties() {
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

    /**
     * Get the value of idType.
     *
     * @return The value of idType.
     */
    public SourceGenerator.IDType getIdType() {
        return idType;
    }

    /**
     * Get the value of many2OneObjectsName.
     *
     * @return The value of many2OneObjectsName.
     */
    public List<String> getMany2OneObjectsName() {
        return many2OneObjectsName;
    }

    /**
     * Set the many2OneObjectsName.
     *
     * @param many2OneObjectsName Many2OneObjectsName.
     */
    public void setMany2OneObjectsName(List<String> many2OneObjectsName) {
        this.many2OneObjectsName.addAll(many2OneObjectsName);
    }

    /**
     * Get the value of one2ManyObjectsName.
     *
     * @return The value of one2ManyObjectsName.
     */
    public List<String> getOne2ManyObjectsName() {
        return one2ManyObjectsName;
    }

    /**
     * Set the one2ManyObjectsName.
     *
     * @param one2ManyObjectsName One2ManyObjectsName.
     */
    public void setOne2ManyObjectsName(List<String> one2ManyObjectsName) {
        this.one2ManyObjectsName.addAll(one2ManyObjectsName);
    }

    /**
     * Get the value of one2OneObjectsName.
     *
     * @return The value of one2OneObjectsName.
     */
    public List<String> getOne2OneObjectsName() {
        return one2OneObjectsName;
    }

    /**
     * Set the one2OneObjectsName.
     *
     * @param one2OneObjectsName One2OneObjectsName.
     */
    public void setOne2OneObjectsName(List<String> one2OneObjectsName) {
        this.one2OneObjectsName.addAll(one2OneObjectsName);
    }

}
