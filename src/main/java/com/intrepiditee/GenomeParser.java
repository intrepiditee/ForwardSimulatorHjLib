package com.intrepiditee;

import edu.rice.hj.api.SuspendableException;
import edu.rice.hj.runtime.config.HjSystemProperty;
import edu.rice.hj.runtime.util.Pair;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static edu.rice.hj.Module0.*;

public class GenomeParser {

    static int minID = Integer.MAX_VALUE;
    static int maxID = Integer.MIN_VALUE;

    static Set<Integer> variantSiteIndices = Collections.newSetFromMap(new ConcurrentHashMap<>());

    static Map<Pair<Integer, Integer>, Byte> idIndexPairToEncoding = new ConcurrentHashMap<>();

    static Map<Byte, String> genotypeEncoding = new HashMap<>();

    public static void main(String[] args) {
        if (args.length < 3 || (!args[0].equals("--parse"))) {
            System.err.println(
                "Usage: bash run.sh --parse genomeLength numberOfGenerations"
            );
            System.exit(-1);
        }

        Configs.genomeLength = Integer.parseInt(args[1]);
        Configs.numGenerations = Integer.parseInt(args[2]);

        genotypeEncoding.put((byte) 0, "0/0");
        genotypeEncoding.put((byte) 1, "0/1");
        genotypeEncoding.put((byte) 2, "1/0");
        genotypeEncoding.put((byte) 3, "1/1");

        launchHabaneroApp(() -> {
            System.out.println(getNumVariantSites(Configs.numGenerations));
            writeVCF(Configs.numGenerations);
        });


    }

    private static void writeVCF(int numGenerations) throws SuspendableException {
        int bufferSize = 200000;

        PrintWriter out = new PrintWriter(Utils.getBufferedWriter("out.vcf", bufferSize));

        System.out.println();

        out.print("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT");
        for (int i = minID; i <= maxID; i++) {
            out.print("\t" + i);
        }
        out.print("\n");

        Integer[] indices = variantSiteIndices.toArray(new Integer[0]);
        Arrays.sort(indices);

        for (int n = 0; n < numGenerations; n++) {
            int count = 0;

            String filename = "Generation" + n;
            ObjectInputStream in = Utils.getObjectInputStream(filename);

            while (true) {
                try {
                    int id = in.readInt();
                    BitSet paternalGenome = (BitSet) in.readObject();
                    BitSet maternalGenome = (BitSet) in.readObject();

                    for (int i : indices) {
                        boolean paternalBase = paternalGenome.get(i);
                        boolean maternalBase = maternalGenome.get(i);

                        Pair<Integer, Integer> idIndexPair = new Pair<>(id, i);

                        if (!paternalBase) {
                            if (!maternalBase) {
                                // "0/0"
                                idIndexPairToEncoding.put(idIndexPair, (byte) 0);
                            } else {
                                // "0/1"
                                idIndexPairToEncoding.put(idIndexPair, (byte) 1);
                            }
                        } else {
                            if (!maternalBase) {
                                // "1/0"
                                idIndexPairToEncoding.put(idIndexPair, (byte) 2);
                            } else {
                                // "1/1"
                                idIndexPairToEncoding.put(idIndexPair, (byte) 3);
                            }
                        }

                    }

                    count += 2;
                    if (count % 1000 == 0) {
                        System.out.println("Generation" + n + ": " + count / 1000 + "k processed");
                    }


                } catch (EOFException e) {
                    break;
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                    System.exit(-1);
                }
            }

            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(-1);
            }
        }


        int count = 0;

        for (int i : indices) {
            StringBuilder s = new StringBuilder();
            s.append("22\t");
            s.append(i);
            s.append("\trs");
            s.append(i);
            s.append("\tA\tC\t.\tPASS\t.\tGT");

            for (int id = minID; id <= maxID; id++) {
                byte encoding = idIndexPairToEncoding.get(new Pair<>(id, i));

                s.append("\t");
                s.append(genotypeEncoding.get(encoding));
            }
            out.println(s.toString());

            count += 1;
            if (count % 1000 == 0) {
                System.out.println("Sites: " + count / 1000 + " k written");
            }
        }

        out.close();

    }

    private static int getNumVariantSites(int numGenerations) throws SuspendableException {

        System.out.println();

        int nThreads = Integer.parseInt(HjSystemProperty.numWorkers.getPropertyValue());

        final BitSet[] prevGenome = new BitSet[1];
        final BitSet[] genome = new BitSet[1];

        int numBasesPerThread = Configs.genomeLength / nThreads;

        forallPhased(0, nThreads - 1, (i) -> {
            int start = numBasesPerThread * i;
            int end = i == nThreads - 1 ? Configs.genomeLength : start + numBasesPerThread;

            for (int n = 0; n < numGenerations; n++) {
                String filename = "Generation" + n;
                ObjectInputStream in = i == 0 ? in = Utils.getObjectInputStream(filename) : null;

                AtomicInteger count = new AtomicInteger(0);

                boolean isInt = true;

                while (true) {
                    if (i == 0) {
                        try {
                            if (prevGenome[0] == null) {
                                int id = in.readInt();
                                minID = Math.min(minID, id);
                                maxID = Math.max(maxID, id);

                                prevGenome[0] = (BitSet) in.readObject();
                                count.incrementAndGet();
                            } else {
                                if (isInt) {
                                    int id = in.readInt();
                                    minID = Math.min(minID, id);
                                    maxID = Math.max(maxID, id);

                                    isInt = false;
                                } else {
                                    isInt = true;
                                }
                            }

                            genome[0] = (BitSet) in.readObject();
                        } catch (EOFException e) {
                            prevGenome[0] = null;
                            System.out.println("Generation" + n + " preprocessed");
                        } catch (IOException | ClassNotFoundException e) {
                            e.printStackTrace();
                            System.exit(-1);
                        }
                    }

                    next();

                    if (prevGenome[0] == null) {
                        break;
                    }

                    for (int j = start; j < end; j++) {
                        if (prevGenome[0].get(j) != genome[0].get(j)) {
                            variantSiteIndices.add(j);
                        }
                    }

                    next();

                    if (i == 0) {
                        prevGenome[0] = genome[0];
                        int c = count.incrementAndGet();
                        if (c % 1000 == 0) {
                            System.out.println("Generation" + n + ": " + c / 1000 + "k preprocessed");
                        }
                    }
                }

            }

        });

        return variantSiteIndices.size();
    }
}
