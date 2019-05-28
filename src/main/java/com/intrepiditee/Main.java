package com.intrepiditee;

import edu.rice.hj.api.SuspendableException;

import java.io.*;
import java.util.Arrays;

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

        if (args.length == 1) {
            if (args[0].equals("--test")) {
                Individual.genomeLength = 10000;
                Individual.randBound = 1150;
                Configs.numThreads = 3;
                Configs.geneticMapName = "test";
                Configs.numGenerations = 10;

            } else if (args[0].equals("--parse")) {
                GenomeParser.main(Arrays.copyOfRange(args, 1, args.length));
                return;
            } else if (args[0].equals("--pedigree")) {
                PedigreeGraph.makeFromFile("Generation3Pedigree.txt");
                return;
            }

        } else if (args.length == 4) {
            Configs.numGenerations = Integer.valueOf(args[1]);
            Configs.geneticMapName = args[0];
            Configs.sizeOfPopulation = Integer.valueOf(args[2]);
            Configs.numThreads = Integer.valueOf(args[3]);

        } else {
            System.err.println(
                "Usage: ./ForwardSimulator.jar geneticMapName numberOfGenerations " +
                    "numberOfGenerationsToStore populationSizePerGeneration + numberOfThreads"
            );
            System.exit(-1);
        }


        launchHabaneroApp(() -> {
            try {
                MapReader.initialize(Configs.geneticMapName).parse();

                Generation next = Generation.makeAncestors(0);
                System.out.print("\nAncestor generation created");

                for (int i = 0; i < Configs.numGenerations; i++) {
                    next = next.evolveOneGenerationThenDestroy();

                    Generation toWrite = null;
                    String filename = null;

                    int generationIndex = i - (Configs.numGenerations - Configs.numGenerationsStore);
                    if (generationIndex >= 0) {
                        toWrite = next;
                        filename = "Generation" + generationIndex;
                    }

                    if (toWrite != null) {
                        BufferedWriter w = Utils.getBufferedWriter(filename + "Pedigree.txt");
                        ObjectOutputStream o = Utils.getObjectOutputStream(filename);

                        for (Individual ind : toWrite.males) {
                            o.writeInt(ind.id);
                            o.writeObject(ind.paternalGenome);
                            o.writeObject(ind.maternalGenome);
                            w.write(String.format("%s %s %s\n", ind.id, ind.fatherID, ind.motherID));
                        }

                        for (Individual ind : toWrite.females) {
                            o.writeInt(ind.id);
                            o.writeObject(ind.paternalGenome);
                            o.writeObject(ind.maternalGenome);
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
