package com.intrepiditee;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.BitSet;
import java.util.Random;
import java.util.Scanner;
import java.util.stream.IntStream;

public class Utils {

    static Random singletonRand = new Random();

    public static BitSet generateRandomSequence(int genomeLength) {
        Random sequenceRandom = new Random();
        BitSet sequence = new BitSet();
        for (int i = 0; i < genomeLength; i++) {
            sequence.set(i, sequenceRandom.nextBoolean());
        }

        return sequence;
    }


    public static String bitSetToString(BitSet s) {
        final StringBuilder buffer = new StringBuilder(s.size());
        IntStream.range(0, s.size()).mapToObj(i -> s.get(i) ? '1' : '0').forEach(buffer::append);
        return buffer.toString();
    }


    public static String getPWD() {
        String pwd = null;
        try {
            URL jarPathURL = Main.class.getProtectionDomain().getCodeSource().getLocation();
            String jarPath = jarPathURL.toURI().toString();
            pwd = jarPath.substring(5, jarPath.length() - 45);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        return pwd;
    }


    public static ObjectInputStream getObjectInputStream(String filename) {
        File f = new File(getPWD() + filename);
        ObjectInputStream i = null;
        try {
            i = new ObjectInputStream(new FileInputStream(f));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        return i;
    }

    public static ObjectOutputStream getObjectOutputStream(String filename) {
        File f = createEmptyFile(filename);

        ObjectOutputStream o = null;
        try {
            o = new ObjectOutputStream(new FileOutputStream(f));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        return o;
    }

    public static File createEmptyFile(String filename) {
        File f = new File(getPWD() + filename);
        f.delete();
        try {
            f.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        return f;
    }

    public static Scanner getScanner(String filename) {
        File f = new File(getPWD() + filename);
        Scanner sc = null;
        try {
            sc = new Scanner(f);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        return sc;
    }

    public static BufferedWriter getBufferedWriter(String filename) {
        return getBufferedWriter(filename, 8192);
    }

    public static BufferedWriter getBufferedWriter(String filename, int bufferSize) {
        File f = Utils.createEmptyFile(filename);
        BufferedWriter w = null;
        try {
            w = new BufferedWriter(new FileWriter(f), bufferSize);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        return w;
    }

}
