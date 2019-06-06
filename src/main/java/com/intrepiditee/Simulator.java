package com.intrepiditee;

import java.io.BufferedWriter;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;

import static com.intrepiditee.Configs.FEMALE;
import static com.intrepiditee.Configs.MALE;
import static com.intrepiditee.GeneticMap.GENETIC_TO_PHYSICAL;
import static com.intrepiditee.GeneticMap.getChromosomeNumbers;
import static com.intrepiditee.GeneticMap.numChromosomes;
import static com.intrepiditee.Segment.segmentsToString;
import static com.intrepiditee.Utils.getBufferedGZipWriter;
import static com.intrepiditee.Utils.getBufferedObjectOutputStream;
import static com.intrepiditee.Utils.getBufferedWriter;
import static edu.rice.hj.Module0.launchHabaneroApp;

public class Simulator {

    private static String prefix = "out/gen";

    public static void main(String[] args) {
        if (args.length < 5 || (!args[0].equals("--simulate"))) {
            Utils.printUsage();
            return;
        }

        Configs.numGenerations = Integer.parseInt(args[1]);
        Configs.numGenerationsStore = Integer.parseInt(args[2]);
        Configs.generationSize = Integer.parseInt(args[3]);
        Configs.numThreads = Integer.parseInt(args[4]);

        launchHabaneroApp(() -> {
            try {
                GeneticMap.parseLengths();
                GeneticMap.makeFromChromosomeNumbers(getChromosomeNumbers());
                GeneticMap.parseAllMaps(GENETIC_TO_PHYSICAL);

                Generation next = null;

                for (int i = 0; i < Configs.numGenerations; i++) {
                    if (next == null) {
                        next = Generation.makeAncestors();
                    } else {
                        next = next.evolveOneGeneration();
                    }

                    Generation toWrite = null;

                    int generationIndex = i - (Configs.numGenerations - Configs.numGenerationsStore);
                    if (generationIndex >= 0) {
                        toWrite = next;
                    }

                    if (toWrite != null) {
                        BufferedWriter pedigreeWriter = getBufferedGZipWriter(
                            prefix + generationIndex + "_pedigree.txt.gz"
                        );

                        for (int c = 1; c <= numChromosomes; c++) {
                            String filename = prefix + generationIndex + "_chr" + c;

                            ObjectOutputStream genomeOut = getBufferedObjectOutputStream(filename);
                            BufferedWriter genomeWriter = getBufferedWriter(filename + ".txt");

                            for (Individual ind : toWrite.males) {
                                Map<Byte, List<Segment>> chromosomesPair = ind.genome.get(c);
                                ind.mergeChromosomes();

                                genomeOut.writeInt(ind.id);
                                genomeOut.writeUnshared(chromosomesPair);

                                genomeWriter.write(String.valueOf(ind.id));
                                genomeWriter.write("\n");
                                genomeWriter.write(segmentsToString(chromosomesPair.get(MALE)));
                                genomeWriter.write("\n");
                                genomeWriter.write(segmentsToString(chromosomesPair.get(FEMALE)));
                                genomeWriter.write("\n");

                                if (c == 1) {
                                    pedigreeWriter.write(
                                        String.format("%s %s %s\n", ind.id, ind.fatherID, ind.motherID)
                                    );
                                }
                            }

                            for (Individual ind : toWrite.females) {
                                Map<Byte, List<Segment>> chromosomesPair = ind.genome.get(c);
                                ind.mergeChromosomes();

                                genomeOut.writeInt(ind.id);
                                genomeOut.writeUnshared(chromosomesPair);

                                genomeWriter.write(String.valueOf(ind.id));
                                genomeWriter.write("\n");
                                genomeWriter.write(segmentsToString(chromosomesPair.get(MALE)));
                                genomeWriter.write("\n");
                                genomeWriter.write(segmentsToString(chromosomesPair.get(FEMALE)));
                                genomeWriter.write("\n");

                                if (c == 1) {
                                    pedigreeWriter.write(
                                        String.format("%s %s %s\n", ind.id, ind.fatherID, ind.motherID)
                                    );
                                }
                            }

                            genomeOut.close();
                            genomeWriter.close();
                        } // end of all chromosomes

                        pedigreeWriter.close();
                    } // end of writing for this generation

                    System.out.print("\nGeneration " + i + " finished");
                } // end of all generations

            } catch (Exception e) {
                e.printStackTrace();
                System.exit(-1);
            }
        });

    }
}
