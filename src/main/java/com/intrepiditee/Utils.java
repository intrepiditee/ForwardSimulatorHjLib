package com.intrepiditee;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Utils {

    // 100 MB
    static int bufferSize = 100000000;

    static Random singletonRand = new Random();

    static int[] toIntArray(List<Integer> list) {
        int[] array = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    static int[] toIntArray(Integer[] integerArray) {
        int[] array = new int[integerArray.length];
        for (int i = 0; i < integerArray.length; i++) {
            array[i] = integerArray[i];
        }
        return array;
    }

    static byte[] toByteArray(List<Byte> list) {
        byte[] array = new byte[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    static String getPWD() {
        String pwd = null;
        try {
            URL jarPathURL = Main.class.getProtectionDomain().getCodeSource().getLocation();
            String jarPath = jarPathURL.toURI().toString();
            pwd = jarPath.substring(
                "file:".length(),
                jarPath.length() - "file:".length() -
                    "ForwardSimulatorHjLib-1.0-SNAPSHOT-shaded".length() + 1
            );
        } catch (URISyntaxException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        return pwd;
    }

    static File getFile(String filename) {
        return new File(getPWD() + filename);
    }


    static GZIPOutputStream getGZIPOutputStream(String filename) {
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

    public static Scanner getScannerFromGZip(String filename) {
        BufferedReader r = getBufferedGZipReader(filename);
        Scanner sc = new Scanner(r);
        return sc;
    }

    public static Scanner getScanner(String filename) {
        BufferedReader r = getBufferedReader(filename);
        Scanner sc = new Scanner(r);
        return sc;
    }


    public static BufferedReader getBufferedGZipReader(String filename) {
        return getBufferedGZipReader(filename, bufferSize);
    }


    public static BufferedWriter getBufferedGZipWriter(String filename) {
        return getBufferedGZipWriter(filename, bufferSize);
    }


    public static BufferedWriter getBufferedGZipWriter(String filename, int bufferSize) {
        GZIPOutputStream zip = getGZIPOutputStream(filename);
        BufferedWriter w = new BufferedWriter(new OutputStreamWriter(zip, StandardCharsets.UTF_8), bufferSize);
        return w;
    }


    public static BufferedReader getBufferedGZipReader(String filename, int bufferSize) {
        GZIPInputStream zip = getGZIPInputStream(filename);
        BufferedReader r = new BufferedReader(new InputStreamReader(zip, StandardCharsets.UTF_8), bufferSize);
        return r;
    }

    public static BufferedReader getBufferedReader(String filename, int bufferSize) {
        return new BufferedReader(new InputStreamReader(getBufferedInputStream(filename)), bufferSize);
    }

    public static BufferedReader getBufferedReader(String filename) {
        return new BufferedReader(new InputStreamReader(getBufferedInputStream(filename)), bufferSize);
    }

    public static BufferedWriter getBufferedWriter(String filename, int bufferSize) {
        return new BufferedWriter(new OutputStreamWriter(getBufferedOutputStream(filename)), bufferSize);
    }

    public static BufferedWriter getBufferedWriter(String filename) {
        return new BufferedWriter(new OutputStreamWriter(getBufferedOutputStream(filename)), bufferSize);
    }

    static void printUsage() {
        System.err.println(
            "Usage:\n" +
                "1) Simulation: bash run.sh --simulate numberOfGenerations " +
                   "numberOfGenerationsToStore generationSize numberOfThreads\n" +
                "2) VCF: bash run.sh --parse genomeLength generationSize " +
                   "numberOfGenerationsStored exclusiveLowerBound numberOfThreads\n" +
                "3) Pedigree: bash run.sh --pedigree numberOfGenerationsStored " +
                   "exclusiveUpperBound numberOfThreads\n"
        );
    }


}
