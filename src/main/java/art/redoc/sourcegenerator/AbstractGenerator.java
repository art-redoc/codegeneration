package art.redoc.sourcegenerator;

import art.redoc.sourcegenerator.conf.GeneratorConfiguration;
import art.redoc.sourcegenerator.utils.CodeGenerateUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static art.redoc.sourcegenerator.utils.CodeGenerateUtils.addEmptyLine;
import static art.redoc.sourcegenerator.utils.CodeGenerateUtils.addEmptyLineInTheLastLine;
import static art.redoc.sourcegenerator.utils.CodeGenerateUtils.contents2value;
import static art.redoc.sourcegenerator.utils.CodeGenerateUtils.removeEmptyLineAtEndMethod;
import static art.redoc.sourcegenerator.utils.CodeGenerateUtils.removeUnusedImport;
import static art.redoc.sourcegenerator.utils.CodeGenerateUtils.value2contents;

public abstract class AbstractGenerator implements Generator {

    protected Class<?> clazz;

    protected String method;

    protected GeneratorConfiguration config;

    protected AbstractGenerator(final GeneratorConfiguration config, final String method) {
        this.method = method;
        this.config = config;
        this.clazz = this.getClass();
    }

    protected void output(final String value) {
        if (this.config.getOutputType().equals("CONSOLE")) {
            System.out.println("////////////////////////////");
            System.out.println("/// " + this.method + "文件输出");
            System.out.println("////////////////////////////");
            System.out.println(value);
            System.out.println();
            System.out.println();
        } else {
            final String outputPath = this.getFileOutputPath();
            final File outputFile = new File(outputPath);
            if (!this.config.isOverride() && outputFile.exists()) {
                System.out.println(outputPath + " 文件已存在，非强制覆盖模式下，不执行写入操作，生成代码如下：");
                System.out.println("////////////////////////////");
                System.out.println("/// " + this.method + "文件输出");
                System.out.println("////////////////////////////");
                System.out.println(value);
                System.out.println();
                System.out.println();
                return;
            }
            final File parent = outputFile.getParentFile();
            if (!parent.exists()) {
                parent.mkdirs();
            }
            FileOutputStream os = null;
            try {
                os = new FileOutputStream(outputFile);
                os.write(value.getBytes("UTF-8"));
                System.out.println("****** " + this.method + "文件生成 ******");
                System.out.println(outputPath);
            } catch (final IOException e) {
                e.printStackTrace();
            } finally {
                if (os == null) {
                    return;
                }
                try {
                    os.close();
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    protected InputStream getInputStream(final String path) throws IOException {
        InputStream is = this.clazz.getResourceAsStream(path);
        if (is != null) {
            return is;
        }
        final File srcFile = new File(path);
        if (srcFile.exists()) {
            is = new FileInputStream(srcFile);
        }
        if (is == null) {
            throw new FileNotFoundException(path + " cannot be opened because it does not exist");
        }
        return is;
    }

    protected String getFileString(final String path) {
        try {
            final InputStream is = this.getInputStream(path);
            final ByteArrayOutputStream os = new ByteArrayOutputStream();
            CodeGenerateUtils.copy(is, os);
            return os.toString("UTF-8");
        } catch (final IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    protected String getPackage(String method) {
        String endPackage = "";
        if (method.equals("serviceImpl")) {
            method = "service";
            endPackage = ".impl";
        }
        if (this.config.getChildPackage() == null) {
            return this.config.getParentPackage() + "." + method + endPackage;
        } else {
            return this.config.getParentPackage() + "." + method + this.config.getChildPackage() + endPackage;
        }
    }

    protected String getFileOutputPath() {
        final String classPackage = this.getClassPath(this.method);
        return this.config.getSrcRootPath() + classPackage.replace(".", File.separator) + ".java";
    }

    protected String getClassPath(final String method) {
        if ("model".equals(method)) {
            return this.getModelPath();
        }
        final String packagePath = this.getPackage(method);
        return packagePath + "." + this.getClassName(method);
    }

    protected String getClassName(final String method) {
        if ("model".equals(method)) {
            return this.getModelName();
        } else if ("dto".equals(method)) {
            return this.getModelName() + "DTO";
        }
        return this.getModelName() + method.substring(0, 1).toUpperCase() + method.substring(1);
    }

    protected String getModelName() {
        return this.config.getModelClazz().getSimpleName();
    }

    protected String getSeparateModelName() {
        String modelName = getModelName();
        modelName = String.valueOf(modelName.charAt(0)).toUpperCase()
                .concat(modelName.substring(1));
        StringBuffer sb = new StringBuffer();
        Pattern pattern = Pattern.compile("[A-Z]([a-z\\d]+)?");
        Matcher matcher = pattern.matcher(modelName);
        boolean isFirstWord = true;
        while (matcher.find()) {
            String word = matcher.group();
            if (isFirstWord) {
                sb.append(word);
                isFirstWord = false;
            } else {
                sb.append(word.toLowerCase());
            }
            sb.append(matcher.end() == modelName.length() ? "" : " ");
        }
        return sb.toString();
    }

    protected String getModelPath() {
        return this.config.getModelClazz().getName();
    }

    protected String getModelNameWithHeadLow() {
        final String modelName = this.getModelName();
        return modelName.substring(0, 1).toLowerCase() + modelName.substring(1);
    }

    protected String getTemplatePath(String templatePath, String defaultTemplatePath) {
        final InputStream inputStream = AbstractGenerator.class.getResourceAsStream(templatePath);
        return inputStream == null ? defaultTemplatePath : templatePath;
    }

    protected Map<String, String> getFilterMapWithIdType() {
        Map<String, String> filterMap = new HashMap<>();
        filterMap.put("@IDType@", config.getIdType().getType());
        return filterMap;
    }

    protected String optimizeCode(String value) {
        final List<String> contents = value2contents(value);
        removeUnusedImport(contents);
        addEmptyLine(contents);
        removeEmptyLineAtEndMethod(contents);
        addEmptyLineInTheLastLine(contents);
        return contents2value(contents);
    }
}
