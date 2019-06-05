package com.intrepiditee;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static com.intrepiditee.Utils.getBufferedObjectOutputStream;
import static com.intrepiditee.Utils.singletonRand;
import static edu.rice.hj.Module0.forallPhased;
import static edu.rice.hj.Module0.launchHabaneroApp;
import static edu.rice.hj.Module0.next;

public class VCFParser {

    static String prefix = "ukb/";

    static String vcfPrefix = "ukb_hap_GP_removed/ukb_hap_chr";
    static String vcfPostfix = "_v2.vcf";

    public static void main(String[] args) {
        System.out.println();

        Configs.generationSize = Integer.parseInt(args[1]);
        Configs.numThreads = Integer.parseInt(args[2]);

        String[] useless = new String[]{
            "#CHROM", "POS", "ID", "REF", "ALT",
            "QUAL", "FILTER", "INFO", "FORMAT"
        };

        int numFilesPerThread = Configs.numChromosomes / Configs.numThreads;
        int[][] idIndicesArray = new int[1][];

        launchHabaneroApp(() -> {
            forallPhased(0, Configs.numThreads - 1, (n) -> {
                int start = n * numFilesPerThread;
                int end = n == Configs.numThreads - 1 ? Configs.numChromosomes : start + numFilesPerThread;

                for (int i = start + 1; i < end + 1; i++) {
                    String filename = vcfPrefix + i + vcfPostfix;
                    System.out.println(filename);
                    Scanner sc = Utils.getScanner(filename);
                    sc.nextLine();
                    sc.nextLine();
                    sc.nextLine();

                    if (i == 1) {
                        String[] fields = sc.nextLine().split("\t");
                        singletonRand.setSeed(0);
                        Set<Integer> idIndices = new HashSet<>();
                        while (idIndices.size() != Configs.generationSize) {
                            int id = useless.length + singletonRand.nextInt(fields.length - useless.length);
                            System.out.println(id);
                            idIndices.add(id);
                        }

                        idIndicesArray[0] = new int[idIndices.size()];
                        int j = 0;
                        for (int index : idIndices.toArray(new Integer[0])) {
                            idIndicesArray[0][j++] = index;
                        }
                        Arrays.sort(idIndicesArray[0]);

                        int[] ids = new int[idIndices.size()];
                        int k = 0;
                        for (int index : idIndicesArray[0]) {
                            ids[k++] = Integer.parseInt(fields[index]);
                        }

                        try {
                            ObjectOutputStream o = getBufferedObjectOutputStream(prefix + "indices.chr"+ i);
                            o.writeUnshared(idIndicesArray[0]);
                            o.close();

                            o = getBufferedObjectOutputStream(prefix + "ids.chr" + i);
                            o.writeUnshared(ids);
                            o.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                            System.exit(-1);
                        }

                    } else {
                        sc.nextLine();
                    }

                    next();

                    ArrayList<Integer> sites = new ArrayList<>();
                    ObjectOutputStream o = getBufferedObjectOutputStream(prefix + "bases.chr"+ i);
                    while (sc.hasNextLine()) {
                        String[] fields = sc.nextLine().split("\t");
                        sites.add(Integer.parseInt(fields[1]));

                        ArrayList<Byte> bases = new ArrayList<>();
                        for (int index : idIndicesArray[0]) {
                            byte encoding = -1;
                            switch (fields[index]) {
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
                            bases.add(encoding);
                        }

                        byte[] basesArray = new byte[bases.size()];
                        for (int j = 0; j < bases.size(); j++) {
                            basesArray[j] = bases.get(j);
                        }

                        try {
                            o.writeUnshared(basesArray);
                        } catch (IOException e) {
                            e.printStackTrace();
                            System.exit(-1);
                        }

                    }

                    try {
                        o.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.exit(-1);
                    }

                    int[] sitesArray = new int[sites.size()];
                    for (int j = 0; j < sites.size(); j++)  {
                        sitesArray[j] = sites.get(j);
                    }

                    o = getBufferedObjectOutputStream(prefix + "sites.chr"+ i);
                    try {
                        o.writeUnshared(sitesArray);
                        o.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.exit(-1);
                    }

                    System.out.println(filename + " parsed");
                }
            });
        });

    }

}
