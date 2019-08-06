package art.redoc;

import art.redoc.sourcegenerator.Generator;
import art.redoc.sourcegenerator.conf.GeneratorConfiguration;
import art.redoc.sourcegenerator.impl.ControllerGenerator;
import art.redoc.sourcegenerator.impl.ConvertorGenerator;
import art.redoc.sourcegenerator.impl.DtoGenerator;
import art.redoc.sourcegenerator.impl.RepositoryGenerator;
import art.redoc.sourcegenerator.impl.ServiceGenerator;
import art.redoc.sourcegenerator.impl.ServiceImplGenerator;
import art.redoc.sourcegenerator.utils.CodeGenerateUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * todo 1.抽utils的公共方法 比如 在固定位置添加content等等
 * Please follow the steps below to configure the startup parameters: <br>
 * 1. Model path. e.g. com.leadingsoft.bizfuse.sourcegenerator.model <br>
 * 2. Model class name, separated by ','. e.g. User,Member,Role <br>
 * 3. Output type, default type is 'FILE', 'CONSOLE' type will output to the CONSOLE. e.g. CONSOLE <br>
 * 4. whether to override, default is false. e.g. true <br>
 * *****************e.g.******************* <br>
 * modelBasePackage=com.lyg.model <br>
 * models=User,Member,Role <br>
 * output=FILE <br>
 * override=true <br>
 * *****************e.g.*******************
 */
public class SourceGenerator {

    public enum Output {
        FILE, CONSOLE
    }

    public enum IDType {
        STRING("String"), LONG("Long"), INTEGER("Integer");
        private String type;

        IDType(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }

    public enum OutputFileType {
        DTO(0), REPOSITORY(1), SERVICE(2), SERVICE_IMPL(3), CONVERTOR(4), CONTROLLER(5);
        private int order;

        OutputFileType(int order) {
            this.order = order;
        }

        public int getOrder() {
            return order;
        }
    }

    /**
     * Code generate.
     *
     * @param modelBasePackage  Model path. e.g. com.leadingsoft.bizfuse.sourcegenerator.model.
     * @param models            Model class name, separated by ','. e.g. User,Member,Role.
     * @param entityPackageName Your entities location package name, if your entity reference like this 'com.test.entities.User', this
     *                          param is 'entities', this param may be 'model','models', 'entity', 'entities' but not limited to this,
     *                          depending on you project structure.
     * @throws ClassNotFoundException
     */
    public static void generate(final String modelBasePackage, String[] models, String entityPackageName) throws ClassNotFoundException {
        generate(modelBasePackage, models, entityPackageName, Output.FILE, false, IDType.LONG, null);
    }

    /**
     * Code generate.
     *
     * @param modelBasePackage  Model path. e.g. com.leadingsoft.bizfuse.sourcegenerator.model.
     * @param models            Model class name, separated by ','. e.g. User,Member,Role.
     * @param entityPackageName Your entities location package name, if your entity reference like this 'com.test.entities.User', this
     *                          param is 'entities', this param may be 'model','models', 'entity', 'entities' but not limited to this,
     *                          depending on you project structure.
     * @param outputType        Output type, default type is 'FILE', 'CONSOLE' type will output to the CONSOLE. e.g. CONSOLE.
     * @param override          whether to override, default is false. e.g. true.
     * @param idType            Global type of database primary key, default type 'Long'.
     * @throws ClassNotFoundException
     */
    public static void generate(final String modelBasePackage, String[] models, String entityPackageName, Output outputType,
                                boolean override, IDType idType, List<OutputFileType> fileType) throws ClassNotFoundException {

//        if (args.length < 2) {
//            System.out.println("Please follow the steps below to configure the startup parameters:");
//            System.out.println("1. Model path. e.g. com.leadingsoft.bizfuse.sourcegenerator.model");
//            System.out.println("2. Model class name, separated by ','. e.g. User,Member,Role");
//            System.out.println("3. Output type, default type is 'FILE', 'CONSOLE' type will output to the CONSOLE. e.g. CONSOLE");
//            System.out.println("4. whether to override, default is false. e.g. true");
//            System.out.println("*****************e.g.*******************");
//            System.out.println("java -jar code-generation-0.1.0.jar com.demo.user.model User,Member,Role CONSOLE true");
//            System.out.println("*****************e.g.*******************");
//            return;
//        }

        if (CodeGenerateUtils.isBlank(modelBasePackage) || null == models || models.length == 0) {
            throw new IllegalArgumentException("Illegal args");
        }

        if (outputType != Output.FILE) {
            override = false;
        }

        validateModels(models, modelBasePackage);

        final String outputDir = null;
        for (final String modelClass : models) {
            Class<?> modelClazz = Class.forName(modelBasePackage + "." + modelClass);
            final GeneratorConfiguration config = new GeneratorConfiguration(modelClazz, entityPackageName, outputType.name(), outputDir,
                    override, idType);
            final Generator dtoGenerator = new DtoGenerator(config);
            final Generator repositoryGenerator = new RepositoryGenerator(config);
            final Generator serviceGenerator = new ServiceGenerator(config);
            final Generator serviceImplGenerator = new ServiceImplGenerator(config);
            final Generator convertorGenerator = new ConvertorGenerator(config);
            final Generator controllerGenerator = new ControllerGenerator(config);
            if (fileType == null) {
                dtoGenerator.generate();
                repositoryGenerator.generate();
                serviceGenerator.generate();
                serviceImplGenerator.generate();
                convertorGenerator.generate();
                controllerGenerator.generate();
            } else {
                fileType.sort(Comparator.comparingInt(OutputFileType::getOrder));
                fileType.forEach(x -> {
                    switch (x) {
                        case DTO:
                            dtoGenerator.generate();
                            break;
                        case REPOSITORY:
                            repositoryGenerator.generate();
                            break;
                        case SERVICE:
                            serviceGenerator.generate();
                            break;
                        case SERVICE_IMPL:
                            serviceImplGenerator.generate();
                            break;
                        case CONVERTOR:
                            convertorGenerator.generate();
                            break;
                        case CONTROLLER:
                            controllerGenerator.generate();
                            break;
                    }
                });
            }
        }
    }

    private static void validateModels(String[] models, String modelBasePackage) throws ClassNotFoundException {
        List<String> errors = new ArrayList<>();
        for (String model : models) {
            String className = modelBasePackage + "." + model;
            if (!CodeGenerateUtils.isPresent(className)) {
                errors.add(model);
            }
        }
        if (errors.size() != 0) {
            final String result = String.join(",", errors);
            throw new ClassNotFoundException(String.format("Models array contains a model [%s] that doesn't exist, please check the " +
                    "parameters.", result));
        }
    }
}
