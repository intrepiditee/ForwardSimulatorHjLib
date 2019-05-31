package com.intrepiditee;

import edu.rice.hj.api.SuspendableException;

import java.io.*;
import java.util.*;

import static edu.rice.hj.Module0.*;

public class GenomeParser {

    static int minID = Integer.MAX_VALUE;
    static int maxID = Integer.MIN_VALUE;

    static Integer[] variantSiteIndices;

    static Map<Byte, String> encoding;

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
        Configs.numGenerationsStore = Integer.parseInt(args[3]);
        int lowerBound = Integer.parseInt(args[4]);
        Configs.numThreads = Integer.parseInt(args[5]);

        encoding = new HashMap<>();
        encoding.put((byte) 0, "0|0");
        encoding.put((byte) 1, "0|1");
        encoding.put((byte) 2, "1|0");
        encoding.put((byte) 3, "1|1");

        String filename = "variantSiteIndices";
        File f = Utils.getFile(filename);
        if (f.exists()) {
            try {
                System.out.println("\nvariantSiteIndices file exists");
                ObjectInputStream in = Utils.getBufferedObjectInputStream(filename);
                minID = in.readInt();
                maxID = in.readInt();
                variantSiteIndices = (Integer[]) in.readUnshared();
                in.close();
                System.out.println("variantSiteIndices file read");
                System.out.println();

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                System.exit(-1);
            }
        }

        launchHabaneroApp(() -> {
            if (!f.exists()) {
                System.out.println("\nvariantSiteIndices file does not exist");
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

        final byte[][] idIndexToBases = new byte[maxID - minID + 1][numSites];

        forallPhased(0, Configs.numThreads - 1, (i) -> {
            int start = numSitesPerThread * i;
            int end = i == Configs.numThreads - 1 ? numSites : start + numSitesPerThread;

//            System.out.println(start);
//            System.out.println(end);

            // Store all base pairs at variant sites

            for (int n = 0; n < Configs.numGenerationsStore; n++) {
                String filename = i == 0 ? "Generation" + n : null;
                ObjectInputStream in = i == 0 ?
                    Utils.getBufferedObjectInputStream(filename) :
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

                            paternalGenome[0] = (BitSet) in.readUnshared();
                            maternalGenome[0] = (BitSet) in.readUnshared();

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
                                idIndexToBases[idIndex][j] = (byte) 0;
                            } else {
                                idIndexToBases[idIndex][j] = (byte) 1;
                            }
                        } else {
                            if (!maternalBase) {
                                idIndexToBases[idIndex][j] = (byte) 2;
                            } else {
                                idIndexToBases[idIndex][j] = (byte) 3;
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

        }); // end of parallel part


        // Generate and write out all the rows sequentially

        PrintWriter out = new PrintWriter(Utils.getBufferedWriter("out.vcf.gz"));

        out.print("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT");
        for (int i = minID; i <= maxID; i++) {
            out.print("\t" + i);
        }
        out.print("\n");


        int numRecordsWritten = 0;
        for (int i = 0; i < numSites; i++) {
            int variantSiteIndex = variantSiteIndices[i];

            StringBuilder s = new StringBuilder();
            s.append("22\t");
            s.append(variantSiteIndex + 1);
            s.append("\trs");
            s.append(variantSiteIndex + 1);
            s.append("\tA\tC\t.\tPASS\t.\tGT");

            for (int ID = minID; ID <= maxID; ID++) {
                int idIndex = ID - minID;
                String bases = encoding.get(idIndexToBases[idIndex][i]);

                s.append("\t");
                s.append(bases);
            }

            out.println(s.toString());

            numRecordsWritten += Configs.numThreads;
            if (numRecordsWritten % 1000 == 0) {
                System.out.println("Records: " + numRecordsWritten / 1000 + "k written");
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

            for (int n = 0; n < Configs.numGenerationsStore; n++) {
                String filename = "Generation" + n;
                ObjectInputStream in = i == 0 ? Utils.getBufferedObjectInputStream(filename) : null;

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

                            genome[0] = (BitSet) in.readUnshared();

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

            int numGenomes = Configs.numGenerationsStore * Configs.generationSize * 2;

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
        ObjectOutputStream o = Utils.getBufferedObjectOutputStream("variantSiteIndices");
        try {
            o.writeInt(minID);
            o.writeInt(maxID);
            o.writeUnshared(variantSiteIndices);
            o.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        System.out.println("variantSiteIndices written to file\n");
    }

}
