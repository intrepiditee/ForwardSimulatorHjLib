package com.intrepiditee;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;
import java.util.Random;
import java.util.Scanner;
import java.util.stream.IntStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Utils {

    // 100 MB
    static int bufferSize = 100000000;

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

    public static File getFile(String filename) {
        return new File(getPWD() + filename);
    }


    public static GZIPOutputStream getGZIPOutputStream(String filename) {
        File f = createEmptyFile(filename);
        GZIPOutputStream o = null;
        try {
            o = new GZIPOutputStream(new FileOutputStream(f));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        return o;
    }


    public static GZIPInputStream getGZIPInputStream(String filename) {
        File f = getFile(filename);
        GZIPInputStream in = null;
        try {
            in = new GZIPInputStream(new FileInputStream(f));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        return in;
    }


    public static BufferedInputStream getBufferedInputStream(String filename) {
        File f = getFile(filename);
        BufferedInputStream bi = null;
        try {
            bi = new BufferedInputStream(new FileInputStream(f), bufferSize);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        return bi;
    }


    public static BufferedOutputStream getBufferedOutputStream(String filename) {
        File f = createEmptyFile(filename);
        BufferedOutputStream bo = null;
        try {
            bo = new BufferedOutputStream(new FileOutputStream(f), bufferSize);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        return bo;
    }


    public static ObjectInputStream getBufferedObjectInputStream(String filename) {
        ObjectInputStream oi = null;
        try {
            oi = new ObjectInputStream(getBufferedInputStream(filename));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        return oi;
    }

    public static ObjectOutputStream getBufferedObjectOutputStream(String filename) {
        ObjectOutputStream oo = null;
        try {
            oo = new ObjectOutputStream(getBufferedOutputStream(filename));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        return oo;
    }


    public static File createEmptyFile(String filename) {
        File f = getFile(filename);
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
        BufferedReader r = getBufferedReader(filename);
        Scanner sc = new Scanner(r);
        return sc;
    }


    public static BufferedReader getBufferedReader(String filename) {
        return getBufferedReader(filename, bufferSize);
    }


    public static BufferedWriter getBufferedWriter(String filename) {
        return getBufferedWriter(filename, bufferSize);
    }


    public static BufferedWriter getBufferedWriter(String filename, int bufferSize) {
        GZIPOutputStream zip = getGZIPOutputStream(filename);
        BufferedWriter w = new BufferedWriter(new OutputStreamWriter(zip, StandardCharsets.UTF_8), bufferSize);
        return w;
    }


    public static BufferedReader getBufferedReader(String filename, int bufferSize) {
        GZIPInputStream zip = getGZIPInputStream(filename);
        BufferedReader r = new BufferedReader(new InputStreamReader(zip, StandardCharsets.UTF_8), bufferSize);
        return r;
    }


    public static void printUsage() {
        System.err.println(
            "Usage:\n" +
                "1) Simulation: bash run.sh numberOfGenerations " +
                   "numberOfGenerationsToStore generationSize numberOfThreads\n" +
                "2) VCF: bash run.sh --parse genomeLength generationSize " +
                   "numberOfGenerationsStored exclusiveLowerBound numberOfThreads\n" +
                "3) Pedigree: bash run.sh --pedigree pedigreeFilename\n"
        );
    }
}
