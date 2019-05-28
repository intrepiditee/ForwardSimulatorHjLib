package com.intrepiditee;

import edu.rice.hj.Module1;
import edu.rice.hj.api.SuspendableException;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static edu.rice.hj.Module0.launchHabaneroApp;

public class GenomeParser {

    public static void main(String[] args) {
        int numGenerations = 4;

        boolean[] isDifferent = new boolean[Individual.genomeLength];

        launchHabaneroApp(() -> {

            System.out.println();

            // Read in all sequences
            for (int n = 0; n < numGenerations; n++) {

                String filename = "Generation" + n;
                ObjectInputStream i = Utils.getObjectInputStream(filename);

                BitSet prevGenome = null;
                try {
                    while (true) {
                        int id = i.readInt();

                        BitSet paternalGenome = (BitSet) (i.readObject());
                        if (prevGenome != null) {
                            BitSet prevGenomeFinal = prevGenome;
                            Module1.forallChunked(0, Individual.genomeLength - 1, (j) -> {
                                if (prevGenomeFinal.get(j) != paternalGenome.get(j)) {
                                    isDifferent[j] = true;
                                }
                            });
                        }

                        prevGenome = paternalGenome;
                        BitSet maternalGenome = (BitSet) (i.readObject());

                        BitSet prevGenomeFinal = prevGenome;
                        Module1.forallChunked(0, Individual.genomeLength - 1, (j) -> {
                            if (prevGenomeFinal.get(j) != maternalGenome.get(j)) {
                                isDifferent[j] = true;
                            }
                        });

                        prevGenome = maternalGenome;

                    }
                } catch (EOFException e) {
                    System.out.println(filename + " parsed");
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                    System.exit(-1);
                }

                try {
                    i.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    System.exit(-1);
                }
            }

            AtomicInteger numDifferentIndices = new AtomicInteger();
            Module1.forallChunked(0, Individual.genomeLength - 1, (i) -> {
                if (isDifferent[i]) {
                    numDifferentIndices.incrementAndGet();
                }
            });

            System.out.println(numDifferentIndices.get());
        });





    }
}
