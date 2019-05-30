package com.intrepiditee;

import edu.rice.hj.api.SuspendableException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPOutputStream;

import static com.intrepiditee.Configs.numGenerations;
import static edu.rice.hj.Module0.*;

public class GenomeParser {

    static int minID = Integer.MAX_VALUE;
    static int maxID = Integer.MIN_VALUE;

    static Integer[] variantSiteIndices;

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        if (args.length < 6 || (!args[0].equals("--parse"))) {
            System.err.println(
                "Usage: bash run.sh --parse genomeLength generationSize numberOfGenerationsStored" +
                    " exclusiveLowerBound numThreads"
            );
            System.exit(-1);
        }

        Configs.genomeLength = Integer.parseInt(args[1]);
        Configs.generationSize = Integer.parseInt(args[2]);
        numGenerations = Integer.parseInt(args[3]);
        int lowerBound = Integer.parseInt(args[4]);
        Configs.numThreads = Integer.parseInt(args[5]);

        launchHabaneroApp(() -> {
            File f = Utils.getFile("variantSiteIndices");
            if (f.exists()) {
                try {
                    System.out.println("variantSiteIndices file exists");
                    System.out.println();
                    ObjectInputStream in = Utils.getObjectInputStream("variantSiteIndices");
                    minID = in.readInt();
                    maxID = in.readInt();
                    variantSiteIndices = (Integer[]) in.readObject();

                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                    System.exit(-1);
                }
            } else {
                System.out.println("variantSiteIndices file does not exist");
                System.out.println();
                getVariantSitesMoreThan(lowerBound);
            }

            writeVCF();
        });


    }

    private static void writeVCF() throws SuspendableException {
        Arrays.sort(variantSiteIndices);

        int numSites = variantSiteIndices.length;
        int numSitesPerThread = numSites / Configs.numThreads;

        final int[] id = new int[1];
        final BitSet[] paternalGenome = new BitSet[1];
        final BitSet[] maternalGenome = new BitSet[1];

        final StringBuilder[] records = new StringBuilder[numSites];

        final String[][] idIndexToBases = new String[maxID - minID + 1][numSites];

        forallPhased(0, Configs.numThreads - 1, (i) -> {
            int start = numSitesPerThread * i;
            int end = i == Configs.numThreads - 1 ? numSites : start + numSitesPerThread;

//            System.out.println(start);
//            System.out.println(end);

            // Store all base pairs at variant sites

            for (int n = 0; n < numGenerations; n++) {
                String filename = i == 0 ? "Generation" + n : null;
                ObjectInputStream in = i == 0 ?
                    Utils.getObjectInputStream(filename) :
                    null;

                int numProcessed = 0;

                while (true) {
                    if (i == 0) {
                        try {
                            // Read one individual
                            id[0] = in.readInt();
                            minID = Math.min(minID, id[0]);
                            maxID = Math.max(maxID, id[0]);

//                            System.out.println(id[0]);

                            paternalGenome[0] = (BitSet) in.readObject();
                            maternalGenome[0] = (BitSet) in.readObject();

                        } catch (EOFException e) {
                            id[0] = -1;
                            System.out.println("Generation" + n + " processed");
                        } catch (IOException | ClassNotFoundException e) {
                            e.printStackTrace();
                            System.exit(-1);
                        }
                    }

                    next();

                    if (id[0] == -1) {
                        break;
                    }

                    for (int j = start; j < end; j++) {
                        int variantSiteIndex = variantSiteIndices[j];

                        boolean paternalBase = paternalGenome[0].get(variantSiteIndex);
                        boolean maternalBase = maternalGenome[0].get(variantSiteIndex);

                        int idIndex = id[0] - minID;
                        if (!paternalBase) {
                            if (!maternalBase) {
                                idIndexToBases[idIndex][j] = "0|0";
                            } else {
                                idIndexToBases[idIndex][j] = "0|1";
                            }
                        } else {
                            if (!maternalBase) {
                                idIndexToBases[idIndex][j] = "1|0";
                            } else {
                                idIndexToBases[idIndex][j] = "1|1";
                            }
                        }
                    }

                    next();

                    if (i == 0) {
                        numProcessed += 2;
                        if (numProcessed % 1000 == 0) {
                            System.out.println("Generation" + n + ": " + numProcessed / 1000 + "k processed");
                        }
                    }

                } // end of while


                if (i == 0) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.exit(-1);
                    }
                }

            } // end of all generations


            // Generate all the rows

            int numRecordsGenerated = 0;
            for (int j = start; j < end; j++) {
                int variantSiteIndex = variantSiteIndices[j];

                records[j] = new StringBuilder();
                records[j].append("22\t");
                records[j].append(variantSiteIndex + 1);
                records[j].append("\trs");
                records[j].append(variantSiteIndex + 1);
                records[j].append("\tA\tC\t.\tPASS\t.\tGT");

                for (int ID = minID; ID <= maxID; ID++) {
                    int idIndex = ID - minID;
                    String bases = idIndexToBases[idIndex][j];

                    records[j].append("\t");
                    records[j].append(bases);
                }

                if (i == 0) {
                    numRecordsGenerated += Configs.numThreads;
                    if (numRecordsGenerated % 1000 == 0) {
                        System.out.println("Records: " + numRecordsGenerated / 1000 + " k generated");
                    }
                }
            }

        }); // end of parallel part


        // Write all the rows sequentially

        int bufferSize = 200000;
        PrintWriter out = null;
        try {
            GZIPOutputStream zip = new GZIPOutputStream(
                new FileOutputStream(Utils.createEmptyFile("out.vcf.zip")));
            BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(zip, StandardCharsets.UTF_8), bufferSize);

            out = new PrintWriter(writer);

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        out.print("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT");
        for (int i = minID; i <= maxID; i++) {
            out.print("\t" + i);
        }
        out.print("\n");

        int count = 0;
        for (StringBuilder s : records) {
            out.println(s.toString());

            count += 1;
            if (count % 1000 == 0) {
                System.out.println("Records: " + count / 1000 + " k written");
            }
        }

        out.close();
    }

    private static void getVariantSitesMoreThan(int lowerBound) throws SuspendableException {
        final BitSet[] genome = new BitSet[1];

        int numBasesPerThread = Configs.genomeLength / Configs.numThreads;

        int[] variantSiteCounts = new int[Configs.genomeLength];
        List<Integer> variantSiteIndicesArray = new ConcurrentArrayList<>();

        forallPhased(0, Configs.numThreads - 1, (i) -> {
            int start = numBasesPerThread * i;
            int end = i == Configs.numThreads - 1 ? Configs.genomeLength : start + numBasesPerThread;

//            System.out.println(start);
//            System.out.println(end);

            for (int n = 0; n < numGenerations; n++) {
                String filename = "Generation" + n;
                ObjectInputStream in = i == 0 ? Utils.getObjectInputStream(filename) : null;

                int count = 0;

                boolean isInt = true;

                while (true) {
                    if (i == 0) {
                        try {
                            if (isInt) {
                                int id = in.readInt();
                                minID = Math.min(minID, id);
                                maxID = Math.max(maxID, id);
                            }
                            isInt = !isInt;

                            genome[0] = (BitSet) in.readObject();

                        } catch (EOFException e) {
                            genome[0] = null;
                            System.out.println("Generation" + n + " preprocessed");
                        } catch (IOException | ClassNotFoundException e) {
                            e.printStackTrace();
                            System.exit(-1);
                        }
                    }

                    next();

                    if (genome[0] == null) {
                        break;
                    }

                    for (int j = start; j < end; j++) {
                        variantSiteCounts[j] += genome[0].get(j) ? 1 : 0;
                    }

                    if (i == 0) {
                        count++;
                        if (count % 1000 == 0) {
                            System.out.println("Generation" + n + ": " + count / 1000 + "k preprocessed");
                        }
                    }

                    next();
                }

                if (i == 0) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.exit(-1);
                    }
                }

            }

            int numGenomes = Configs.numGenerations * Configs.generationSize * 2;

            for (int j = start; j < end; j++) {

                int numOnes = variantSiteCounts[j];
                int minorityCount = numOnes;

                if (numOnes >= numGenomes / 2) {
                    minorityCount = numGenomes - numOnes;
                }

                if (minorityCount > lowerBound) {
                    variantSiteIndicesArray.add(j);
                }
            }

        });

        variantSiteIndices = variantSiteIndicesArray.toArray(new Integer[0]);

        System.out.println("\nPreprocessing completed");
        System.out.println("Preprocessing summary:");
        System.out.println("Number of variant sites: " + variantSiteIndicesArray.size());

        System.out.println("Writing variantSiteIndices to file");
        ObjectOutputStream o = Utils.getObjectOutputStream("variantSiteIndices");
        try {
            o.writeInt(minID);
            o.writeInt(maxID);
            o.writeObject(variantSiteIndices);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        System.out.println("variantSiteIndices written to file\n");


    }
}
