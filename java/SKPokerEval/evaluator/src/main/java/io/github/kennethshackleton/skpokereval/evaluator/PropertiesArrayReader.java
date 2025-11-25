package io.github.kennethshackleton.skpokereval.evaluator;

import java.util.Arrays;
import java.util.Properties;

public class PropertiesArrayReader {

    public static int[] propToIntArray(Properties props, String arrayName) {
        final String[] values = props.getProperty(arrayName).split(",");
        final int[] ints = new int[values.length];
        Arrays.setAll(ints, i -> Integer.parseInt(values[i].trim()));
        return ints;
    }

    public static byte[] propToByteArray(Properties props, String arrayName) {
        final String[] values = props.getProperty(arrayName).split(",");
        final byte[] bytes = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            bytes[i] = Byte.parseByte(values[i].trim());
        }
        return bytes;
    }

    public static short[] propToShortArray(Properties props, String arrayName) {
        final String[] values = props.getProperty(arrayName).split(",");
        final short[] shorts = new short[values.length];
        for (int i = 0; i < values.length; i++) {
            shorts[i] = Short.parseShort(values[i].trim());
        }
        return shorts;
    }
}
