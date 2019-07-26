package art.redoc.sourcegenerator.utils;

import art.redoc.sourcegenerator.conts.CodeGenerateConts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class CodeGenerateUtils {
    public static boolean isNotBlank(CharSequence cs) {
        return !isBlank(cs);
    }

    public static boolean isBlank(CharSequence cs) {
        int strLen;
        if (cs != null && (strLen = cs.length()) != 0) {
            for (int i = 0; i < strLen; ++i) {
                if (!Character.isWhitespace(cs.charAt(i))) {
                    return false;
                }
            }
            return true;
        } else {
            return true;
        }
    }

    public static boolean isEmpty(CharSequence cs) {
        return cs == null || cs.length() == 0;
    }

    public static boolean isNotEmpty(CharSequence cs) {
        return !isEmpty(cs);
    }


    public static String capitalize(String str) {
        int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return str;
        }
        return new StringBuilder(strLen)
                .append(Character.toTitleCase(str.charAt(0)))
                .append(str.substring(1))
                .toString();
    }

    public static void removeUnusedImport(List<String> contents) {
        final String value = contents2value(contents);
        final List<Import> imports = new ArrayList<>();
        for (int i = 0; i < contents.size(); i++) {
            final String content = contents.get(i);
            if (content.startsWith("import") && !content.contains("*")) {
                final String importedClassName = content.substring(content.lastIndexOf(".") + 1, content.indexOf(";"));
                Import clazz = new Import();
                clazz.setClassName(importedClassName);
                clazz.setIndex(i);
                imports.add(clazz);
            }
        }
        imports.forEach(x -> x.getRule().forEach(y -> {
            if (value.contains(y)) {
                x.setRemoved(false);
                return;
            }
        }));
        imports.stream().filter(x -> x.getRemoved()).forEach(x -> contents.set(x.getIndex(), CodeGenerateConts.TO_BE_REMOVED));
        contents.removeAll(Arrays.asList(CodeGenerateConts.TO_BE_REMOVED));
    }

    private static void formatCode(List<String> contents, Predicate<Integer> predicate) {
        final List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < contents.size(); i++) {
            if (predicate.test(i)) {
                indexes.add(i);
            }
        }
        Collections.sort(indexes, Comparator.reverseOrder());
        indexes.forEach(x -> contents.add(x + 1, ""));
    }

    public static void formatCode(List<String> contents) {
        // private 属性 后面如果没有空行必须接空行
        formatCode(contents, x -> Pattern.matches("\\s+private+.+;", contents.get(x)) && isNotBlank(contents.get(x + 1)));
        // package 后面 如果没有空行必须接空行
        formatCode(contents, x -> Pattern.matches("package +.+;", contents.get(x)) && isNotBlank(contents.get(x + 1)));
    }

    private static class Import {
        private Integer index;
        private String className;
        private List<String> rule = new ArrayList<>();
        private Boolean removed = true;

        public Integer getIndex() {
            return index;
        }

        public void setIndex(Integer index) {
            this.index = index;
        }

        public String getClassName() {
            return className;
        }

        public void setClassName(String className) {
            this.className = className;
            this.rule.add(" " + className);
            this.rule.add("(" + className);
            this.rule.add(")" + className);
            this.rule.add("@" + className);
            this.rule.add("<" + className);
            this.rule.add(">" + className);
            this.rule.add("=" + className);
        }

        public List<String> getRule() {
            return rule;
        }

        public Boolean getRemoved() {
            return removed;
        }

        public void setRemoved(Boolean removed) {
            this.removed = removed;
        }
    }

    public static List<String> value2contents(String value) {
        return new ArrayList<>(Arrays.asList(value.replaceAll("\\r", "").split("\\n")));
    }

    public static String contents2value(List<String> contents) {
        return String.join("\n", contents);
    }
}
