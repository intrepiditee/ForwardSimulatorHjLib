package com.intrepiditee;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.*;

import static com.intrepiditee.Utils.getBufferedObjectOutputStream;
import static com.intrepiditee.Utils.singletonRand;
import static com.intrepiditee.Utils.toIntArray;
import static edu.rice.hj.Module0.forallPhased;
import static edu.rice.hj.Module0.launchHabaneroApp;
import static edu.rice.hj.Module0.next;

public class VCFParser {

    static String prefix = "ukb/";

    static String vcfPrefix = "ukb_hap_GP_removed/ukb_hap_chr";
    static String vcfPostfix = "_v2.vcf";

    static int[][] idIndicesArray = new int[1][];

    static String[] useless = new String[]{
        "#CHROM", "POS", "ID", "REF", "ALT",
        "QUAL", "FILTER", "INFO", "FORMAT"
    };

    public static void main(String[] args) {
        System.out.println();

        Configs.generationSize = Integer.parseInt(args[1]);
        Configs.numThreads = Integer.parseInt(args[2]);

        int numFilesPerThread = Configs.numChromosomes / Configs.numThreads;

        launchHabaneroApp(() -> {
            forallPhased(0, Configs.numThreads - 1, (n) -> {
                int start = n * numFilesPerThread;
                int end = n == Configs.numThreads - 1 ? Configs.numChromosomes : start + numFilesPerThread;

                for (int i = start + 1; i < end + 1; i++) {
                    String filename = vcfPrefix + i + vcfPostfix;
                    Scanner sc = Utils.getScanner(filename);
                    sc.nextLine();
                    sc.nextLine();
                    sc.nextLine();

                    if (i == 1) {
                        String[] fields = sc.nextLine().split("\t");
                        int seed = 0;
                        writeIDsAndIndices(fields, seed);
                    } else {
                        sc.nextLine();
                    }

                    next();

                    int count = 0;

                    ArrayList<Integer> sites = new ArrayList<>();
                    ObjectOutputStream o = getBufferedObjectOutputStream(prefix + "bases.chr"+ i);
                    while (sc.hasNextLine()) {
                        String[] fields = sc.nextLine().split("\t");
                        sites.add(Integer.parseInt(fields[1]));

                        byte[] basesArray = new byte[idIndicesArray[0].length];
                        for (int j = 0; j < idIndicesArray[0].length; j++) {
                            int index = idIndicesArray[0][j];
                            String bases = fields[index];
                            basesArray[j] = getEncodingFromBases(bases);
                        }

                        try {
                            o.writeUnshared(basesArray);
                        } catch (IOException e) {
                            e.printStackTrace();
                            System.exit(-1);
                        }

                        count++;
                        if (count % 1000 == 0) {
                            System.out.println(filename + " " + count / 1000 + "k sites parsed");
                        }

                    }

                    try {
                        o.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.exit(-1);
                    }

                    int[] sitesArray = toIntArray(sites);
                    o = getBufferedObjectOutputStream(prefix + "sites.chr"+ i);
                    try {
                        o.writeUnshared(sitesArray);
                        o.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.exit(-1);
                    }

                    System.out.println(filename + " parsed");

                } // End of all files
            });
        });

    }


    static void writeIDsAndIndices(String[] fields, int seed) {
        singletonRand.setSeed(seed);

        // Get Configs.generationSize number of random indices of ids
        Set<Integer> idIndices = new HashSet<>();
        while (idIndices.size() != Configs.generationSize) {
            int index = useless.length + singletonRand.nextInt(fields.length - useless.length);
            idIndices.add(index);
        }

        idIndicesArray[0] = new int[idIndices.size()];
        int j = 0;
        for (int index : idIndices.toArray(new Integer[0])) {
            idIndicesArray[0][j++] = index;
        }
        Arrays.sort(idIndicesArray[0]);

        // Get the actual ids from the indices
        int[] ids = new int[idIndices.size()];
        int k = 0;
        for (int index : idIndicesArray[0]) {
            ids[k++] = Integer.parseInt(fields[index]);
        }

        try {
            ObjectOutputStream o = getBufferedObjectOutputStream(prefix + "idIndices");
            o.writeUnshared(idIndicesArray[0]);
            o.close();

            o = getBufferedObjectOutputStream(prefix + "ids");
            o.writeUnshared(ids);
            o.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

    }


    static byte getEncodingFromBases(String bases) {
        byte encoding = -1;
        switch (bases) {
            case "0|0":
                encoding = 0;
                break;
            case "0|1":
                encoding = 1;
                break;
            case "1|0":
                encoding = 2;
                break;
            case "1|1":
                encoding = 3;
        }
        return encoding;
    }


    // Convert serialized ids to a text file where each line is an id
    static void idsToText(String filename) {
        try {
            ObjectInputStream in = Utils.getBufferedObjectInputStream(filename);
            int[] arr = (int[]) in.readUnshared();
            in.close();

            BufferedWriter w = Utils.getBufferedWriter(filename + ".txt");
            for (int id : arr) {
                w.write(String.valueOf(id));
                w.write("\n");
            }
            w.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

}
