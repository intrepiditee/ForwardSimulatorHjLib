package com.intrepiditee;

import edu.rice.hj.Module0;
import edu.rice.hj.Module1;
import edu.rice.hj.Module2;
import edu.rice.hj.api.HjPhaser;
import edu.rice.hj.api.HjPhaserMode;
import edu.rice.hj.api.HjPhaserPair;
import edu.rice.hj.api.SuspendableException;
import edu.rice.hj.runtime.config.HjSystemProperty;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static edu.rice.hj.Module0.*;

public class GenomeParser {

    public static void main(String[] args) {
        int numGenerations = 4;

        boolean[] isDifferent = new boolean[Individual.genomeLength];

        launchHabaneroApp(() -> {

            System.out.println();

            int nThreads = Integer.parseInt(HjSystemProperty.numWorkers.getPropertyValue());

            final BitSet[] prevGenome = new BitSet[1];
            final BitSet[] genome = new BitSet[1];

            AtomicInteger numDifferentIndices = new AtomicInteger(0);

            int numBasesPerThread = Individual.genomeLength / nThreads;

            forallPhased(0, nThreads - 1, (i) -> {
                int start = numBasesPerThread * i;
                int end = i == nThreads - 1 ? Individual.genomeLength : start + numBasesPerThread;

                for (int n = 0; n < numGenerations; n++) {
                    String filename = "Generation" + n;
                    ObjectInputStream in = i == 0 ? in = Utils.getObjectInputStream(filename) : null;

                    AtomicInteger count = new AtomicInteger(0);

                    boolean isInt = true;

                    while (true) {

                        if (i == 0) {
                            try {
                                if (prevGenome[0] == null) {
                                    in.readInt();
                                    prevGenome[0] = (BitSet) in.readObject();
                                    count.incrementAndGet();
                                } else {
                                    if (isInt) {
                                        in.readInt();
                                        isInt = false;
                                    } else {
                                        isInt = true;
                                    }
                                }

                                genome[0] = (BitSet) in.readObject();
                            } catch (EOFException e) {
                                prevGenome[0] = null;
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
                                isDifferent[j] = true;
                            }
                        }

                        next();

                        if (i == 0) {
                            prevGenome[0] = genome[0];
                            int c = count.incrementAndGet();
                            if (c % 1000 == 0) {
                                System.out.println("Generation" + n + ": " + c / 1000 + "k processed");
                            }
                        }
                    }

                }

                for (int k = start; k < end; k++) {
                    if (isDifferent[k]) {
                        numDifferentIndices.incrementAndGet();
                    }
                }

            });

            System.out.println(numDifferentIndices.get());
        });


    }
}
