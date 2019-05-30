package com.intrepiditee;

import java.io.*;

import static edu.rice.hj.Module0.launchHabaneroApp;

public class Main {

    /*
    Usage may be:
    1) ./ForwardSimulator.jar geneticMapName numberOfGenerations numberOfGenerationsToStore
       populationSizePerGeneration numberOfThreads
    2)
    Outputs may be:
    1) Generation_: Each block consists of an serialized integer representing an individual's id
       and two serialized BitSets representing their paternal and maternal genomes.
    2) Generation_Pedigree.txt: Each line consists of an individual's id, their father's id,
       and their mother's id, separated by space.
    where _ is the index of the generation.
    */
    public static void main(String[] args) {

        if (args.length == 0 || args.length > 6 ||
            args[0].equals("-h") || args[0].equals("--help")) {

           Utils.printUsage();
           return;
        }

        if (args[0].equals("--test")) {
            Configs.genomeLength = 10000;
            Individual.randBound = (int) (100 * 1.2);
            Configs.numThreads = 4;
            Configs.geneticMapName = "test";
            Configs.numGenerations = 50;

        } else if (args[0].equals("--parse")) {
            GenomeParser.main(args);
            return;
        } else if (args[0].equals("--pedigree")) {
            PedigreeGraph.main(args);
            return;


        } else if (args.length == 4) {
            Configs.numGenerations = Integer.valueOf(args[1]);
            Configs.generationSize = Integer.valueOf(args[2]);
            Configs.numThreads = Integer.valueOf(args[3]);

        } else {

        }


        launchHabaneroApp(() -> {
            try {
                GeneticMap.initialize(Configs.geneticMapName);
                GeneticMap.parse();

                Generation next = null;

                for (int i = 0; i < Configs.numGenerations; i++) {
                    if (next == null) {
                        next = Generation.makeRandomGeneration();
                    } else {
                        next = next.evolveOneGenerationThenDestroy();
                    }

                    Generation toWrite = null;
                    String filename = null;

                    int generationIndex = i - (Configs.numGenerations - Configs.numGenerationsStore);
                    if (generationIndex >= 0) {
                        toWrite = next;
                        filename = "Generation" + generationIndex;
                    }

                    if (toWrite != null) {
                        BufferedWriter w = Utils.getBufferedWriter(filename + "Pedigree.txt");
                        ObjectOutputStream o = Utils.getBufferedObjectOutputStream(filename);

                        for (Individual ind : toWrite.males) {
                            o.writeInt(ind.id);
                            o.writeUnshared(ind.paternalGenome);
                            o.writeUnshared(ind.maternalGenome);
                            w.write(String.format("%s %s %s\n", ind.id, ind.fatherID, ind.motherID));
                        }

                        for (Individual ind : toWrite.females) {
                            o.writeInt(ind.id);
                            o.writeUnshared(ind.paternalGenome);
                            o.writeUnshared(ind.maternalGenome);
                            w.write(String.format("%s %s %s\n", ind.id, ind.fatherID, ind.motherID));
                        }

                        o.close();
                        w.close();
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
