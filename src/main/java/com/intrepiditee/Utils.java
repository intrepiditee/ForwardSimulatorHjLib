package com.intrepiditee;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

class Utils {

    // 10 MB
    private static final int bufferSize = 10000000;

    static Random singletonRand = new Random();


    static byte[] readByteArray(String filename) {
        ObjectInputStream in = Utils.getBufferedObjectInputStream(filename);
        byte[] array = null;
        try {
            array = (byte[]) in.readUnshared();
            in.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        return array;
    }

    static int[] readIntArray(String filename) {
        ObjectInputStream in = Utils.getBufferedObjectInputStream(filename);
        int[] array = null;
        try {
            array = (int[]) in.readUnshared();
            in.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        return array;
    }

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

    private static String getPWD() {
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


    private static GZIPOutputStream getGZIPOutputStream(String filename) {
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


    private static GZIPInputStream getGZIPInputStream(String filename) {
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


    private static BufferedInputStream getBufferedInputStream(String filename) {
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


    private static BufferedOutputStream getBufferedOutputStream(String filename) {
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


    static ObjectInputStream getBufferedObjectInputStream(String filename) {
        ObjectInputStream oi = null;
        try {
            oi = new ObjectInputStream(getBufferedInputStream(filename));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        return oi;
    }

    static ObjectOutputStream getBufferedObjectOutputStream(String filename) {
        ObjectOutputStream oo = null;
        try {
            oo = new ObjectOutputStream(getBufferedOutputStream(filename));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        return oo;
    }


    private static File createEmptyFile(String filename) {
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

    static Scanner getScannerFromGZip(String filename) {
        BufferedReader r = getBufferedGZipReader(filename);
        return new Scanner(r);
    }

    static Scanner getScanner(String filename) {
        BufferedReader r = getBufferedReader(filename);
        return new Scanner(r);
    }


    private static BufferedReader getBufferedGZipReader(String filename) {
        return getBufferedGZipReader(filename, bufferSize);
    }


    static BufferedWriter getBufferedGZipWriter(String filename) {
        return getBufferedGZipWriter(filename, bufferSize);
    }


    static BufferedWriter getBufferedGZipWriter(String filename, int bufferSize) {
        GZIPOutputStream zip = getGZIPOutputStream(filename);
        return new BufferedWriter(new OutputStreamWriter(zip, StandardCharsets.UTF_8), bufferSize);
    }


    private static BufferedReader getBufferedGZipReader(String filename, int bufferSize) {
        GZIPInputStream zip = getGZIPInputStream(filename);
        return new BufferedReader(new InputStreamReader(zip, StandardCharsets.UTF_8), bufferSize);
    }

    static BufferedReader getBufferedReader(String filename, int bufferSize) {
        return new BufferedReader(new InputStreamReader(getBufferedInputStream(filename)), bufferSize);
    }

    private static BufferedReader getBufferedReader(String filename) {
        return new BufferedReader(new InputStreamReader(getBufferedInputStream(filename)), bufferSize);
    }

    static BufferedWriter getBufferedWriter(String filename, int bufferSize) {
        return new BufferedWriter(new OutputStreamWriter(getBufferedOutputStream(filename)), bufferSize);
    }

    static BufferedWriter getBufferedWriter(String filename) {
        return new BufferedWriter(new OutputStreamWriter(getBufferedOutputStream(filename)), bufferSize);
    }

    static void printUsage() {
        System.err.println(
            "Usage:\n" +
                "1) Parse UK Biobank VCFs: bash run.sh --parse generationSize " +
                   "numberOfThreads\n" +
                "2) Simulate: bash run.sh --simulate numberOfGenerations " +
                   "from-to generationSize numberOfThreads\n" +
                "3) Generate VCFs: bash run.sh --generate generationSize " +
                   "from-to numberOfThreads\n" +
                "4) Generate mapping files for RaPID: bash run.sh --map\n" +
                "5) Compute pairwise relationship: bash run.sh --pedigree generationSize " +
                   "from-toToRead from-toToCompute degree=3/meiosis=4 maxDegree numberOfThreads\n" +
                "6) Compute pairwise ibds: bash run.sh --ibd from-to numberOfThreads\n\n" +

                "0) Do all of the above in order: bash run.sh " +
                   "--parse generationSize numberOfThreads " +
                   "--simulate numberOfGenerations from-to generationSize numberOfThreads " +
                   "--generate generationSize from-to numberOfThreads" +
                   "--map " +
                   "--pedigree degree=3/meiosis=5 from-to exclusiveUpperBound numberOfThreads " +
                   "--ibd from-to numberOfThreads\n"
        );
    }


}
