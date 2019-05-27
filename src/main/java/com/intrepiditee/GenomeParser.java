package com.intrepiditee;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class GenomeParser {

    public static void main(String[] args) {
        int numGenerations = 4;

        String pwd = Utils.getPWD();
        List<BitSet> genomes = new ArrayList<>();

        // Read in all sequences
        for (int n = 0; n < numGenerations; n++) {
            String filename = "Generation" + n;
            ObjectInputStream i = Utils.getObjectInputStream(filename);

            while (true) {
                try {
                    int id = i.readInt();
                    BitSet paternalGenome = (BitSet) (i.readObject());
                    BitSet maternalGenome = (BitSet) (i.readObject());

                    genomes.add(paternalGenome);
                    genomes.add(maternalGenome);

                } catch (EOFException e) {
                    break;
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                    System.exit(-1);
                }
            }

        }

        // Count number of sites not the same across all sequences
        int genomeLength = genomes.get(0).length();
        int c = 0;
        for (int i = 0; i < genomeLength; i++) {
            boolean base = genomes.get(0).get(i);
            for (int j = 1; j < genomes.size(); j++) {
                if (base != genomes.get(j).get(i)) {
                    c++;
                    break;
                }
            }
        }

        System.out.println(c);


    }
}
