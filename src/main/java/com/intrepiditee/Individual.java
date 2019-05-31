package com.intrepiditee;

import edu.rice.hj.Module1;
import edu.rice.hj.api.HjSuspendable;
import edu.rice.hj.api.SuspendableException;

import java.util.BitSet;
import java.util.concurrent.atomic.AtomicInteger;

import static com.intrepiditee.Utils.singletonRand;


public class Individual {
    static int genomeLength = 10000;
    int id;
    byte sex;

    int fatherID;
    int motherID;

    BitSet paternalGenome;
    BitSet maternalGenome;

    static AtomicInteger nextID = new AtomicInteger(0);

    static double mutationRate = 1.1 * 10e-8;

    static int randBound = (int) (45 * 1.2);

    static byte MALE = 1;
    static byte FEMALE = 0;

    static Individual makeEmpty() {
        Individual ind = new Individual();
        return ind;
    }

    static Individual makeFromParents(Individual father, Individual mother) throws SuspendableException {
        Individual ind = makeEmpty();
        ind.fatherID = father.id;
        ind.motherID = mother.id;
        ind.recombineGenomes(father, mother);

        return ind;
    }


    private Individual() {
        id = nextID.getAndIncrement();
        paternalGenome = new BitSet(Configs.genomeLength);
        maternalGenome = new BitSet(Configs.genomeLength);
    }


    public Individual recombineGenomes(Individual father, Individual mother) throws SuspendableException {

        HjSuspendable paternalRunnable = () -> {
            boolean paternalIsRecombining = false;
            double paternalProb = 0.0;
            int paternalIndex = 0;
            int paternalPosition = GeneticMap.indices.get(paternalIndex);
            int paternalRand = singletonRand.nextInt(randBound);

//            int c = 0;

            for (int i = 0; i < Configs.genomeLength; i++) {
                if (i == paternalPosition) {
                    paternalProb += GeneticMap.probabilities.get(paternalIndex);
                    paternalIndex++;

                    if (paternalIndex < GeneticMap.indices.size()) {
                        paternalPosition = GeneticMap.indices.get(paternalIndex);
                    }

                    if (paternalRand < paternalProb) {
//                        c++;
                        paternalIsRecombining = !paternalIsRecombining;
                        if (paternalIndex < GeneticMap.indices.size()) {
                            paternalProb = GeneticMap.probabilities.get(paternalIndex);
                        }
                        paternalRand = singletonRand.nextInt(randBound);
                    }
                }

                if (paternalIsRecombining) {
                    paternalGenome.set(i, father.maternalGenome.get(i));
                } else {
                    paternalGenome.set(i, father.paternalGenome.get(i));
                }
            }

//            System.out.println(c);
        };



        HjSuspendable maternalRunnable = () -> {
            boolean maternalIsRecombining = false;
            double maternalProb = 0.0;
            int maternalIndex = 0;
            int maternalPosition = GeneticMap.indices.get(maternalIndex);
            int maternalRand = singletonRand.nextInt(randBound);

            for (int i = 0; i < Configs.genomeLength; i++) {
                if (i == maternalPosition) {
                    maternalProb += GeneticMap.probabilities.get(maternalIndex);

                    maternalIndex++;
                    if (maternalIndex < GeneticMap.indices.size()) {
                        maternalPosition = GeneticMap.indices.get(maternalIndex);
                    }

                    if (maternalRand < maternalProb) {
                        maternalIsRecombining = !maternalIsRecombining;
                        if (maternalIndex < GeneticMap.indices.size()) {
                            maternalProb = GeneticMap.probabilities.get(maternalIndex);
                        }
                        maternalRand = singletonRand.nextInt(randBound);
                    }
                }

                if (maternalIsRecombining) {
                    maternalGenome.set(i, mother.maternalGenome.get(i));
                } else {
                    maternalGenome.set(i, mother.paternalGenome.get(i));
                }
            }
        };

        Module1.finish(() -> {
            Module1.async(paternalRunnable);
            maternalRunnable.run();
        });

        double expectedNumMutations = Configs.genomeLength * mutationRate;

        if (expectedNumMutations > 1) {
            int numMutations = singletonRand.nextInt((int) (2 * expectedNumMutations));
            for (int i = 0; i < numMutations; i++) {
                int paternalMutatingPosition = singletonRand.nextInt(paternalGenome.size());
                paternalGenome.flip(paternalMutatingPosition);
            }

            numMutations = singletonRand.nextInt((int) (2 * expectedNumMutations));
            for (int i = 0; i < numMutations; i++) {
                int maternalMutatingPosition = singletonRand.nextInt(maternalGenome.size());
                maternalGenome.flip(maternalMutatingPosition);
            }

        } else {
            if (singletonRand.nextDouble() < expectedNumMutations) {
                int paternalMutatingPosition = singletonRand.nextInt(paternalGenome.size());
                paternalGenome.flip(paternalMutatingPosition);
            }

            if (singletonRand.nextDouble() < expectedNumMutations) {
                int maternalMutatingPosition = singletonRand.nextInt(maternalGenome.size());
                maternalGenome.flip(maternalMutatingPosition);
            }
        }

        return this;
    }

    public boolean isMale() {
        return sex == MALE;
    }

    public Individual setSex(byte sex) {
        this.sex = sex;
        return this;
    }


}
