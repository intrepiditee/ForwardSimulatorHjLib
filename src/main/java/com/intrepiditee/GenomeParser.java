package com.intrepiditee;

import edu.rice.hj.Module1;
import edu.rice.hj.api.SuspendableException;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static edu.rice.hj.Module0.launchHabaneroApp;

public class GenomeParser {

    public static void main(String[] args) {
        int numGenerations = 4;

        Set<Integer> indicesSet = Collections.newSetFromMap(new ConcurrentHashMap<>());

        launchHabaneroApp(() -> {

            // Read in all sequences
            Module1.forallChunked(0, numGenerations - 1, (n) -> {

                String filename = "Generation" + n;
                ObjectInputStream i = Utils.getObjectInputStream(filename);

                BitSet prevGenome = null;
                while (true) {
                    try {
                        int id = i.readInt();

                        BitSet paternalGenome = (BitSet) (i.readObject());
                        if (prevGenome != null) {
                            BitSet prevGenomeFinal = prevGenome;
                            Module1.forallChunked(0, Individual.genomeLength - 1, (j) -> {
                                if (prevGenomeFinal.get(j) != paternalGenome.get(j)) {
                                    indicesSet.add(j);
                                }
                            });
                        }

                        prevGenome = paternalGenome;
                        BitSet maternalGenome = (BitSet) (i.readObject());

                        BitSet prevGenomeFinal = prevGenome;
                        Module1.forallChunked(0, Individual.genomeLength - 1, (j) -> {
                            if (prevGenomeFinal.get(j) != maternalGenome.get(j)) {
                                indicesSet.add(j);
                            }
                        });

                        prevGenome = maternalGenome;

                    } catch (EOFException e) {
                        break;
                    } catch (IOException | ClassNotFoundException e) {
                        e.printStackTrace();
                        System.exit(-1);
                    }
                }
            });
        });

        int numDifferentIndices = indicesSet.size();

        System.out.println(numDifferentIndices);

    }
}
