package art.redoc.sourcegenerator.utils;

import art.redoc.sourcegenerator.conts.CodeGenerateConts;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Code generate utils.
 *
 * @author redoc
 */
public class CodeGenerateUtils {

    /**
     * This is a utility class.
     */
    private CodeGenerateUtils() {
        throw new IllegalStateException("Utility class");
    }

    private static final String EMPTY_STRING = "";

    /**
     * Reference {@code org.apache.commons.lang3.StringUtils#isNotBlank}.
     *
     * @param cs Char sequence.
     * @return Reference {@code org.apache.commons.lang3.StringUtils#isNotBlank}.
     */
    public static boolean isNotBlank(CharSequence cs) {
        return !isBlank(cs);
    }

    /**
     * Reference {@code org.apache.commons.lang3.StringUtils#isBlank}.
     *
     * @param cs Char sequence.
     * @return Reference {@code org.apache.commons.lang3.StringUtils#isBlank}.
     */
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

    /**
     * Reference {@code org.apache.commons.lang3.StringUtils#isEmpty}.
     *
     * @param cs Char sequence.
     * @return Reference {@code org.apache.commons.lang3.StringUtils#isEmpty}.
     */
    public static boolean isEmpty(CharSequence cs) {
        return cs == null || cs.length() == 0;
    }

    /**
     * Reference {@code org.apache.commons.lang3.StringUtils#isNotEmpty}.
     *
     * @param cs Char sequence.
     * @return Reference {@code org.apache.commons.lang3.StringUtils#isNotEmpty}.
     */
    public static boolean isNotEmpty(CharSequence cs) {
        return !isEmpty(cs);
    }

    /**
     * Reference {@code org.apache.commons.lang3.StringUtils#capitalize}.
     *
     * @param str String.
     * @return Reference {@code org.apache.commons.lang3.StringUtils#capitalize}.
     */
    public static String capitalize(String str) {
        if (str == null || str.length() == 0) {
            return str;
        }
        return Character.toTitleCase(str.charAt(0)) + str.substring(1);
    }

    /**
     * Reference {@code org.apache.commons.io.IOUtils#readLines}.
     *
     * @param input Input stream.
     * @return Reference {@code org.apache.commons.io.IOUtils#readLines}.
     * @throws IOException IO exception.
     */
    public static List<String> readLines(InputStream input) throws IOException {
        return readLines(input, Charset.defaultCharset());
    }

    /**
     * Reference {@code org.apache.commons.io.IOUtils#readLines}.
     *
     * @param input    Input stream.
     * @param encoding Charset.
     * @return Reference {@code org.apache.commons.io.IOUtils#readLines}.
     * @throws IOException IO exception.
     */
    public static List<String> readLines(InputStream input, Charset encoding) throws IOException {
        InputStreamReader reader = new InputStreamReader(input, encoding);
        return readLines(reader);
    }

    /**
     * Reference {@code org.apache.commons.io.IOUtils#readLines}.
     *
     * @param input    Input stream.
     * @param encoding Charset.
     * @return Reference {@code org.apache.commons.io.IOUtils#readLines}.
     * @throws IOException IO exception.
     */
    public static List<String> readLines(InputStream input, String encoding) throws IOException {
        return readLines(input, Charset.forName(encoding));
    }

    /**
     * Reference {@code org.apache.commons.io.IOUtils#readLines}.
     *
     * @param input Reader.
     * @return Reference {@code org.apache.commons.io.IOUtils#readLines}.
     * @throws IOException IO exception.
     */
    public static List<String> readLines(Reader input) throws IOException {
        BufferedReader reader = input instanceof BufferedReader ? (BufferedReader) input : new BufferedReader(input);
        List<String> list = new ArrayList();
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            list.add(line);
        }
        return list;
    }

    /**
     * Copy input stream to out put stream.
     *
     * @param input  Input stream.
     * @param output Out put stream.
     * @throws IOException IO exception.
     */
    public static void copy(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[4096];
        int len;
        while ((len = input.read(buffer)) > -1) {
            output.write(buffer, 0, len);
        }
        output.flush();
    }

    /**
     * Determine if a class exists in JVM.
     *
     * @param name Class reference.
     * @return Whether the class exists.
     */
    public static boolean isPresent(String name) {
        try {
            Thread.currentThread().getContextClassLoader().loadClass(name);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Convert value to contents.
     *
     * @param value Resource value.
     * @return Contents.
     */
    public static List<String> value2contents(String value) {
        return new ArrayList<>(Arrays.asList(value.replaceAll("\\r", EMPTY_STRING).split("\\n")));
    }

    /**
     * Convert contents to value.
     *
     * @param contents Contents.
     * @return Resource value.
     */
    public static String contents2value(List<String> contents) {
        return String.join("\n", contents);
    }

    /**
     * Remove unused content by the mark.
     *
     * @param contents Contents.
     */
    public static void removeUnusedContent(List<String> contents) {
        contents.removeAll(Arrays.asList(CodeGenerateConts.TO_BE_REMOVED));
    }

    /**
     * Add a empty line in the last line.
     *
     * @param contents Contents.
     */
    public static void addEmptyLineInTheLastLine(List<String> contents) {
        if (isNotBlank(contents.get(contents.size() - 1))) {
            contents.add(EMPTY_STRING);
        }
    }

    /**
     * Remove empty line at the end method.
     *
     * @param contents Contents.
     */
    public static void removeEmptyLineAtEndMethod(List<String> contents) {
        final String lastLine = contents.get(contents.size() - 1);
        if (isNotBlank(lastLine) && lastLine.trim().equals("}")) {
            int currentIndex = contents.size() - 2;
            String currentContent = contents.get(currentIndex);
            while (isBlank(currentContent)) {
                contents.set(currentIndex, CodeGenerateConts.TO_BE_REMOVED);
                currentIndex--;
                currentContent = contents.get(currentIndex);
            }
            removeUnusedContent(contents);
        }
    }

    /**
     * Remove unused import.
     *
     * @param contents Contents.
     */
    public static void removeUnusedImport(List<String> contents) {
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
        contents.forEach(content -> {
            imports.forEach(x -> {
                if (Pattern.matches("(?!import)(?!package).*[^a-zA-Z\\d]" + x.getClassName() + "[^a-zA-Z\\d].*", content + " ")) {
                    x.setRemoved(false);
                    return;
                }
            });
        });
        imports.stream().filter(x -> x.getRemoved()).forEach(x -> contents.set(x.getIndex(), CodeGenerateConts.TO_BE_REMOVED));
        removeUnusedContent(contents);
    }

    private static class Import {
        private Integer index;
        private String className;
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
        }

        public Boolean getRemoved() {
            return removed;
        }

        public void setRemoved(Boolean removed) {
            this.removed = removed;
        }
    }

    /**
     * Add empty line for contents.
     *
     * @param contents Contents.
     */
    public static void addEmptyLine(List<String> contents) {
        // if there is no empty line after "private XX xx;", must add a empty line.
        final Pattern privateCompile = Pattern.compile("\\s+private+.+;");
        addEmptyLine(contents, x -> privateCompile.matcher(contents.get(x)).matches() && isNotBlank(contents.get(x + 1)));
        // if there is no empty line after "package aa.bb.cc;", must add a empty line.
        final Pattern packageCompile = Pattern.compile("package +.+;");
        addEmptyLine(contents, x -> packageCompile.matcher(contents.get(x)).matches() && isNotBlank(contents.get(x + 1)));
        removeTwoMoreConsecutiveBlankLine(contents);
    }

    /**
     * Add empty line for contents.
     *
     * @param contents  Contents.
     * @param predicate Condition.
     */
    private static void addEmptyLine(List<String> contents, Predicate<Integer> predicate) {
        final List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < contents.size(); i++) {
            if (predicate.test(i)) {
                indexes.add(i);
            }
        }
        Collections.sort(indexes, Comparator.reverseOrder());
        indexes.forEach(x -> contents.add(x + 1, EMPTY_STRING));
    }

    /**
     * Remove two or more consecutive blank lines.
     *
     * @param contents Contents.
     */
    private static void removeTwoMoreConsecutiveBlankLine(List<String> contents) {
        for (int i = 0; i < contents.size(); i++) {
            final String content = contents.get(i);
            if (i > 0 && isBlank(contents.get(i - 1)) && isBlank(content)) {
                contents.set(i - 1, CodeGenerateConts.TO_BE_REMOVED);
            }
        }
        removeUnusedContent(contents);
    }
}
