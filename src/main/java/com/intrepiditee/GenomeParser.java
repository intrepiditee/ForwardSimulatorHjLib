package com.intrepiditee;

import edu.rice.hj.api.SuspendableException;

import java.io.*;
import java.util.*;

import static com.intrepiditee.Utils.getBufferedWriter;
import static com.intrepiditee.Utils.singletonRand;
import static edu.rice.hj.Module0.*;

public class GenomeParser {

    static int minID = Integer.MAX_VALUE;
    static int maxID = Integer.MIN_VALUE;

    static int[] variantSiteIndices;

    public static void main(String[] args) {
        if (args.length < 6 || (!args[0].equals("--parse"))) {
            System.err.println(
                "Usage: bash run.sh --parse genomeLength generationSize numberOfGenerationsStored" +
                    " exclusiveLowerBound numThreads"
            );
            System.exit(-1);
        }

        Configs.chromosomeLength = Integer.parseInt(args[1]);
        Configs.generationSize = Integer.parseInt(args[2]);
        Configs.numGenerationsStore = Integer.parseInt(args[3]);
        int lowerBound = Integer.parseInt(args[4]);
        Configs.numThreads = Integer.parseInt(args[5]);

        String filename = "variantSiteIndices";
        File f = Utils.getFile(filename);
        if (f.exists()) {
            try {
                System.out.println("\nvariantSiteIndices file exists");
                ObjectInputStream in = Utils.getBufferedObjectInputStream(filename);
                minID = in.readInt();
                maxID = in.readInt();
                variantSiteIndices = (int[]) in.readUnshared();
                in.close();

                System.out.println("variantSiteIndices file read");
                System.out.println("Number of variant sites: " + variantSiteIndices.length);
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
        int[][][] idToChromosomes = readChromosomes();

        int numSites = variantSiteIndices.length;
        int siteBatchSize = Math.min(numSites, 10000);
        int numBatches = (int) Math.ceil(numSites / (double) siteBatchSize);
        int numSitesPerThread = siteBatchSize / Configs.numThreads;

        String[] batchRecords = new String[siteBatchSize];

        BufferedWriter out = getBufferedWriter("out.vcf.gz");
        try {
            out.write("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT");
            for (int i = minID; i <= maxID; i++) {
                out.write("\t" + i);
            }
            out.write("\n");
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        boolean[] basesIfInSegment = new boolean[numSites];
        for (int i = 0; i < numSites; i++) {
            basesIfInSegment[i] = singletonRand.nextBoolean();
        }

        // Generate record strings in batch. In each batch, records are generated
        // in parallel. After all records in a batch are generated, one task writes
        // then all out. Then, all the tasks continue to the next batch.
        forallPhased(0, Configs.numThreads - 1, (i) -> {
            int count = 0;

            for (int j = 0; j < numBatches; j++) {
                int offset = j * siteBatchSize;
                int start = offset + i * numSitesPerThread;
                if (start >= numSites) {
                    break;
                }
                int end = Math.min(
                    numSites,
                    start + (i == Configs.numThreads - 1 ?
                        siteBatchSize - i * numSitesPerThread : numSitesPerThread)
                );

                for (int k = start; k < end; k++) {
                    int variantSiteIndex = variantSiteIndices[k];
                    StringBuilder s = new StringBuilder();
                    s.append("22\t");
                    s.append(variantSiteIndex + 1);
                    s.append("\trs");
                    s.append(variantSiteIndex + 1);
                    s.append("\tA\tC\t.\tPASS\t.\tGT");

                    boolean baseIfInSegment = basesIfInSegment[k];
                    for (int ID = minID; ID <= maxID; ID++) {
                        int idIndex = ID - minID;
                        int[] paternalChromosome = idToChromosomes[idIndex][0];
                        int[] maternalChromosome = idToChromosomes[idIndex][1];

                        String bases = getBasesAt(
                            paternalChromosome, maternalChromosome,
                            variantSiteIndex, baseIfInSegment
                        );

                        s.append("\t");
                        s.append(bases);
                    }
                    s.append("\n");

                    batchRecords[k - offset] = s.toString();

                }

                // Wait for all tasks in this batch to finish
                next();

                // Task 0 writes out all records in this batch
                if (i == 0) {
                    for (String record : batchRecords) {
                        // Last batch may not be full
                        if (record == null) {
                            break;
                        }

                        try {
                            out.write(record);
                        } catch (IOException e) {
                            e.printStackTrace();
                            System.exit(-1);
                        }
                        count++;
                        if (count % 1000 == 0) {
                            StringBuilder s = new StringBuilder();
                            s.append("Records: ");
                            s.append(count / 1000);
                            s.append("k out of ");
                            s.append(numSites / 1000);
                            s.append("k written");
                            System.out.println(s.toString());
                        }
                    }
                }

                // Other tasks wait for task 0 to finish writing
                next();

            } // End of all batches

        });

        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }


    private static void getVariantSitesMoreThan(int lowerBound) throws SuspendableException {
        final int[][] chromosome = new int[1][];

        int[] numChromosomesWithIndexInSegment = new int[Configs.chromosomeLength];
        int[][] variantSiteIndicesLocal = new int[1][];
        List<Integer> variantSiteIndicesList = new ConcurrentArrayList<>();

        forallPhased(0, Configs.numThreads - 1, (i) -> {
            for (int n = 0; n < Configs.numGenerationsStore; n++) {
                String filename = "Generation" + n;
                ObjectInputStream in = i == 0 ? Utils.getBufferedObjectInputStream(filename) : null;

                int count = 0;

                boolean isInt = true;

                // Consumes all chromosomes in one generation
                while (true) {
                    // Task 0 reads in the next chromosome
                    if (i == 0) {
                        try {
                            if (isInt) {
                                int id = in.readInt();
                                minID = Math.min(minID, id);
                                maxID = Math.max(maxID, id);
                            }
                            isInt = !isInt;

                            chromosome[0] = (int[]) in.readUnshared();

                        } catch (EOFException e) {
                            chromosome[0] = null;
                            System.out.println("Generation" + n + " preprocessed");
                        } catch (IOException | ClassNotFoundException e) {
                            e.printStackTrace();
                            System.exit(-1);
                        }
                    }

                    next();

                    if (chromosome[0] == null) {
                        break;
                    }

                    int numSegments = chromosome[0].length / 2;
                    int numSegmentsPerThread = numSegments / Configs.numThreads;
                    int start = i * numSegmentsPerThread;
                    int end = i == Configs.numThreads - 1 ? numSegments : start + numSegmentsPerThread;
                    for (int j = start; j < end; j++) {
                        int segmentStartIndex = j * 2;
                        int segmentEndIndex = segmentStartIndex + 1;

                        int segmentStart = chromosome[0][segmentStartIndex];
                        int segmentEnd = chromosome[0][segmentEndIndex];

                        for (int k = segmentStart; k < segmentEnd; k++) {
                            numChromosomesWithIndexInSegment[k]++;
                        }
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

            } // end of all generations

            // Determine variant site indices
            int numGenomes = Configs.numGenerationsStore * Configs.generationSize * 2;
            int numBasesPerThread = Configs.chromosomeLength / Configs.numThreads;
            int start = i * numBasesPerThread;
            int end = i == Configs.numThreads - 1 ? Configs.chromosomeLength : start + numBasesPerThread;
            for (int j = start; j < end; j++) {
                int num = numChromosomesWithIndexInSegment[j];
                int minorityCount = num;

                if (num >= numGenomes / 2) {
                    minorityCount = numGenomes - num;
                }

                // Filter out variant sites where the number of chromosomes are
                // equal to or lower than the exclusive lower bound.
                if (minorityCount > lowerBound) {
                    variantSiteIndicesList.add(j);
                }
            }

            next();

            // Task 0 initializes an array for variant site indices
            if (i == 0) {
                variantSiteIndicesLocal[0] = new int[variantSiteIndicesList.size()];
            }

            next();

            // Fill in the array of variant site indices
            int numVariantSites = variantSiteIndicesList.size();
            int numVariantSitesPerThread = numVariantSites / Configs.numThreads;
            int s = i * numVariantSitesPerThread;
            int e = i == Configs.numThreads - 1 ? numVariantSites : s + numVariantSitesPerThread;
            for (int j = s; j < e; j++) {
                variantSiteIndicesLocal[0][j] = variantSiteIndicesList.get(j);
            }

        });

        variantSiteIndices = variantSiteIndicesLocal[0];

        // Sort the array of variant site indices
        Arrays.sort(variantSiteIndices);

        System.out.println("\nPreprocessing completed");
        System.out.println("Preprocessing summary:");
        System.out.println("Number of variant sites: " + variantSiteIndicesList.size());

        // Write out the array of variant site indices
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


    private static int[][][] readChromosomes() {
        System.out.println();

        int count = 0;
        int[][][] idToChromosomes = new int[Configs.generationSize * Configs.numGenerationsStore][2][];
        for (int i = 0; i < Configs.numGenerationsStore; i++) {
            String filename = "Generation" + i;
            ObjectInputStream in = Utils.getBufferedObjectInputStream(filename);
            while (true) {
                try {
                    int id = in.readInt();
                    idToChromosomes[id - minID][0] = (int[]) in.readUnshared();
                    idToChromosomes[id - minID][1] = (int[]) in.readUnshared();
                } catch (EOFException e) {
                    StringBuilder s = new StringBuilder();
                    s.append("Chromosomes: Generation");
                    s.append(i);
                    s.append(" read");
                    System.out.println(s.toString());
                    break;
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                    System.exit(-1);
                }
            }

            count += 2;
            if (count % 1000 == 0) {
                StringBuilder s = new StringBuilder();
                s.append("Chromosomes: ");
                s.append(count / 1000);
                s.append("k read");
                System.out.println(s.toString());
            }
        }

        return idToChromosomes;
    }

    private static boolean getIsInSegmentFromIndex(int index) {
        boolean base;
        if (index > 0) {
            // index can be the start or end of a segment
            if (index % 2 == 0) {
                // index is start of a segment, so included in it
                base = true;
            } else {
                // index is end of a segment, so not included in it
                base = false;
            }
        } else {
            int insertionPoint = -(index + 1);
            if (insertionPoint % 2 == 0) {
                // start of a segment is larger than index
                // index is not in any segment
                base = false;
            } else {
                // end of a segment is larger than index
                // index is in that segment
                base = true;
            }
        }
        return base;
    }

    private static String getBasesFromIsInSegment(
        boolean paternalIsInSegment, boolean maternalIsInSegment,
        boolean baseIfInSegment) {

        String bases;
        if (!paternalIsInSegment) {
            if (!maternalIsInSegment) {
                if (baseIfInSegment) {
                    bases = "0|0";
                } else {
                    bases = "1|1";
                }
            } else {
                if (baseIfInSegment) {
                    bases = "0|1";
                } else {
                    bases = "1|0";
                }
            }
        } else{
            if (!maternalIsInSegment) {
                if (baseIfInSegment) {
                    bases = "1|0";
                } else {
                    bases = "0|1";
                }
            } else {
                if (baseIfInSegment) {
                    bases = "1|1";
                } else {
                    bases = "0|0";
                }
            }
        }
        return bases;
    }

    private static String getBasesAt(
        int[] paternalChromosome, int[] maternalChromosome,
        int variantSiteIndex, boolean baseIfInSegment) {

        int paternalIndex = Arrays.binarySearch(paternalChromosome, variantSiteIndex);
        int maternalIndex = Arrays.binarySearch(maternalChromosome, variantSiteIndex);

        boolean paternalIsInSegment = getIsInSegmentFromIndex(paternalIndex);
        boolean maternalIsInSegment = getIsInSegmentFromIndex(maternalIndex);

        String bases = getBasesFromIsInSegment(paternalIsInSegment, maternalIsInSegment, baseIfInSegment);
        return bases;
    }


}
