package com.intrepiditee;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.*;

import static com.intrepiditee.Configs.generationSize;
import static com.intrepiditee.Utils.getBufferedObjectOutputStream;
import static com.intrepiditee.Utils.toIntArray;
import static edu.rice.hj.Module0.forallPhased;
import static edu.rice.hj.Module0.launchHabaneroApp;

public class VCFParser {

    static final String pathPrefix = "ukb/";

    private static final String vcfPrefix = "subsets/chr";
    private static final String vcfPostfix = ".recode.vcf";

    private static final String[] useless = new String[]{
        "#CHROM", "POS", "ID", "REF", "ALT",
        "QUAL", "FILTER", "INFO", "FORMAT"
    };

    public static void main(String[] args) {
        System.out.println();

        generationSize = Integer.parseInt(args[1]);
        Configs.numThreads = Integer.parseInt(args[2]);

        int numFilesPerThread = Configs.numChromosomes / Configs.numThreads;

        launchHabaneroApp(() -> {
            forallPhased(0, Configs.numThreads - 1, (n) -> {
                int start = n * numFilesPerThread;
                int end = n == Configs.numThreads - 1 ? Configs.numChromosomes : start + numFilesPerThread;

                for (int i = start + 1; i < end + 1; i++) {
                    String filename = vcfPrefix + i + vcfPostfix;
                    Scanner sc = Utils.getScanner(filename);
                    String line = sc.nextLine();
                    while (line.startsWith("##")) {
                        line = sc.nextLine();
                    }

                    int count = 0;

                    ArrayList<Integer> sites = new ArrayList<>();
                    ObjectOutputStream o = getBufferedObjectOutputStream(pathPrefix + "bases.chr"+ i);
                    while (sc.hasNextLine()) {
                        String[] fields = sc.nextLine().split("\t");
                        sites.add(Integer.parseInt(fields[1]));

                        byte[] basesArray = new byte[generationSize];
                        for (int j = useless.length; j < useless.length + generationSize; j++) {
                            String bases = fields[j];
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
                    o = getBufferedObjectOutputStream(pathPrefix + "sites.chr"+ i);
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


    private static byte getEncodingFromBases(String bases) {
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
