package com.intrepiditee;

import java.io.BufferedWriter;
import java.io.ObjectOutputStream;

import static com.intrepiditee.Segment.segmentsToArray;
import static com.intrepiditee.Segment.segmentsToString;
import static edu.rice.hj.Module0.launchHabaneroApp;

public class Simulator {

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
                GeneticMap.initialize(Configs.geneticMapName);
                GeneticMap.parse();

                Generation next = null;

                for (int i = 0; i < Configs.numGenerations; i++) {
                    if (next == null) {
                        next = Generation.makeAncestors();
                    } else {
                        next = next.evolveOneGeneration();
                    }

                    Generation toWrite = null;
                    String filename = null;

                    int generationIndex = i - (Configs.numGenerations - Configs.numGenerationsStore);
                    if (generationIndex >= 0) {
                        toWrite = next;
                        filename = "Generation" + generationIndex;
                    }

                    if (toWrite != null) {
                        BufferedWriter w2 = Utils.getBufferedWriter(filename + ".txt.gz");
                        BufferedWriter w = Utils.getBufferedWriter(filename + "Pedigree.txt.gz");
                        ObjectOutputStream o = Utils.getBufferedObjectOutputStream(filename);

                        for (Individual ind : toWrite.males) {
                            o.writeInt(ind.id);
                            o.writeUnshared(segmentsToArray(ind.paternalChromosome));
                            o.writeUnshared(segmentsToArray(ind.maternalChromosome));
                            w2.write(ind.id);
                            w2.write("\n");
                            w2.write(segmentsToString(ind.paternalChromosome));
                            w2.write("\n");
                            w2.write(segmentsToString(ind.maternalChromosome));
                            w2.write("\n");
                            w.write(String.format("%s %s %s\n", ind.id, ind.fatherID, ind.motherID));
                        }

                        for (Individual ind : toWrite.females) {
                            o.writeInt(ind.id);
                            o.writeUnshared(segmentsToArray(ind.paternalChromosome));
                            o.writeUnshared(segmentsToArray(ind.maternalChromosome));
                            w2.write(ind.id);
                            w2.write("\n");
                            w2.write(segmentsToString(ind.paternalChromosome));
                            w2.write("\n");
                            w2.write(segmentsToString(ind.maternalChromosome));
                            w2.write("\n");
                            w.write(String.format("%s %s %s\n", ind.id, ind.fatherID, ind.motherID));
                        }

                        o.close();
                        w.close();
                        w2.close();
                    }

                    System.out.print("\nGeneration " + i + " finished");
                }

            } catch (Exception e) {
                e.printStackTrace();
                System.exit(-1);
            }
        });

    }
}
