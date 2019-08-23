package com.intrepiditee;

import edu.rice.hj.api.SuspendableException;
import edu.rice.hj.runtime.config.HjSystemProperty;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.intrepiditee.Configs.*;
import static com.intrepiditee.GeneticMap.getChromosomeNumbers;
import static com.intrepiditee.Utils.getBufferedGZipWriter;
import static com.intrepiditee.Utils.getBufferedObjectOutputStream;
import static edu.rice.hj.Module0.launchHabaneroApp;
import static edu.rice.hj.Module1.forallChunked;

public class Simulator {

    static String prefix = "out/gen";

    public static void main(String[] args) {
        if (args.length != 6 || (!args[0].equals("--simulate"))) {
            Utils.printUsage();
            return;
        }

        numGenerations = Integer.parseInt(args[1]);
        startGeneration = Integer.parseInt(args[2]);
        endGeneration = Integer.parseInt(args[3]);
        generationSize = Integer.parseInt(args[4]);
        numThreads = Integer.parseInt(args[5]);

        HjSystemProperty.setSystemProperty(HjSystemProperty.numWorkers, numThreads);

        System.out.println();

        launchHabaneroApp(() -> {
            List<Generation> toWrite = new ArrayList<>(endGeneration - startGeneration + 1);
            List<Integer> indices = new ArrayList<>(endGeneration - startGeneration + 1);

                GeneticMap.parseLengths();
                GeneticMap.makeFromChromosomeNumbers(getChromosomeNumbers());
                GeneticMap.parseAllMaps(BOTH);

                Generation next = null;

                for (int i = 0; i < Configs.numGenerations; i++) {
                    if (next == null) {
                        next = Generation.makeAncestors();
                    } else {
                        next = next.evolveOneGeneration();
                    }

                    int generationIndex = i - (Configs.numGenerations - (endGeneration - startGeneration + 1));
                    if (generationIndex >= 0) {
                        toWrite.add(next);
                        indices.add(i);
                    }

                    System.out.println("Generation " + i + " finished");
                } // end of all generations

            writeGenerations(toWrite, indices);
        });
    }


    private static void writeGenerations(
        List<Generation> generations, List<Integer> indices)
        throws SuspendableException {

        forallChunked(0, generations.size() - 1, i -> {
            try {
                Generation toWrite = generations.get(i);
                int generationIndex = indices.get(i);

                BufferedWriter pedigreeWriter = getBufferedGZipWriter(
                    prefix + generationIndex + "_pedigree.txt.gz"
                );

                for (int c = 1; c <= numChromosomes; c++) {
                    String chromosomeFilename = prefix + generationIndex + "_chr" + c;
                    String mutationFilename = prefix + generationIndex + "_chr" + c + "_mutations";

                    ObjectOutputStream chromosomeOut = getBufferedObjectOutputStream(chromosomeFilename);
                    ObjectOutputStream mutationOut = getBufferedObjectOutputStream(mutationFilename);

                    for (Individual ind : toWrite.males) {
                        Map<Byte, List<Segment>> chromosomesPair = ind.genome.get(c);
                        ind.mergeChromosomes();

                        chromosomeOut.writeInt(ind.id);
                        chromosomeOut.writeUnshared(chromosomesPair);

                        Map<Byte, List<Integer>> mutationIndicesPair = ind.mutationIndices.get(c);
                        mutationOut.writeInt(ind.id);
                        mutationOut.writeUnshared(mutationIndicesPair);


                        if (c == 1) {
                            pedigreeWriter.write(
                                ind.id + "\t" + ind.fatherID + "\t" + ind.motherID + "\n"
                            );
                        }
                    }

                    for (Individual ind : toWrite.females) {
                        Map<Byte, List<Segment>> chromosomesPair = ind.genome.get(c);
                        ind.mergeChromosomes();

                        chromosomeOut.writeInt(ind.id);
                        chromosomeOut.writeUnshared(chromosomesPair);

                        Map<Byte, List<Integer>> mutationIndicesPair = ind.mutationIndices.get(c);
                        mutationOut.writeInt(ind.id);
                        mutationOut.writeUnshared(mutationIndicesPair);

                        if (c == 1) {
                            pedigreeWriter.write(
                                ind.id + "\t" + ind.fatherID + "\t" + ind.motherID + "\n"
                            );
                        }
                    }

                    chromosomeOut.close();
                    mutationOut.close();

                } // end of all chromosomes

                pedigreeWriter.close();

                System.out.println("Generation " + generationIndex + " written");

            } catch (IOException e) {
                e.printStackTrace();
                System.exit(-1);
            }

        }); // end of all generations
    }
}
