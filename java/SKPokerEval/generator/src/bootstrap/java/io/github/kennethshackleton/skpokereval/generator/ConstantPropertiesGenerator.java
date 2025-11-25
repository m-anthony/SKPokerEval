package io.github.kennethshackleton.skpokereval.generator;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class ConstantPropertiesGenerator implements Closeable {

    private static final int LINE_LENGTH = 120;
    private final PrintStream stream;

    public ConstantPropertiesGenerator(File baseDirectory, String fileName) throws IOException {
        baseDirectory.mkdirs();
        final File classFile = new File(baseDirectory, fileName);
        stream = new PrintStream(classFile, StandardCharsets.UTF_8);
    }

    public void addProperty(String property, String value) {
        stream.println(property + "=" + value);
    }

    public void addArray(String arrayName, byte[] array) {
        addArray(arrayName, IntStream.range(0, array.length).mapToObj(i -> Integer.toString(array[i])));
    }

    public void addArray(String arrayName, short[] array) {
        addArray(arrayName, IntStream.range(0, array.length).mapToObj(i -> Integer.toString(array[i])));
    }

    public void addArray(String arrayName, int[] array) {
        addArray(arrayName, IntStream.of(array).mapToObj(Integer::toString));
    }

    private void addArray(String arrayName, Stream<String> values) {
        String line = arrayName + "=";
        final List<String> valuesString = values.toList();
        final int offset = line.length();
        final String newLine = new String(new char[offset]).replace('\0', ' ');
        for (int i = 0; i < valuesString.size(); i++) {
            if (line.length() + valuesString.get(i).length() + 2 > LINE_LENGTH) {
                stream.println(line.substring(0, line.length() - 1) + '\\');
                line = newLine;
            }
            line += valuesString.get(i);
            if (i != valuesString.size() - 1) line += ", ";
        }
        stream.println(line);
    }

    @Override
    public void close() {
        stream.flush();
        stream.close();
    }

    public static int[] propToIntArray(Properties properties, String arrayName) {
        final String property = properties.getProperty(arrayName);
        return Arrays.stream(property.split(","))
                .mapToInt(v -> Integer.parseInt(v.trim()))
                .toArray();
    }
}
