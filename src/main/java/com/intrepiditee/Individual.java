package com.intrepiditee;

import edu.rice.hj.Module1;
import edu.rice.hj.api.HjSuspendable;
import edu.rice.hj.api.SuspendableException;

import java.util.BitSet;
import java.util.concurrent.atomic.AtomicInteger;

import static com.intrepiditee.MapReader.geneticMap;
import static com.intrepiditee.Utils.rand;

public class Individual {
    int id;
    byte sex;

    int fatherID;
    int motherID;

    BitSet paternalGenome;
    BitSet maternalGenome;

    static AtomicInteger nextID = new AtomicInteger(0);

    static int genomeLength = 51304566;
    static double mutationRate = 1.1 * 10e-8;

    static int randBound = 60;

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
        paternalGenome = new BitSet(genomeLength);
        maternalGenome = new BitSet(genomeLength);
    }


    public Individual recombineGenomes(Individual father, Individual mother) throws SuspendableException {
        HjSuspendable paternalRunnable = () -> {
            boolean paternalIsRecombining = false;
            double paternalProb = 0.0;
            int paternalIndex = 0;
            int paternalPosition = geneticMap.indices.get(paternalIndex);
            int paternalRand = rand.nextInt(randBound);

            for (int i = 0; i < genomeLength; i++) {
                if (i == paternalPosition) {
                    paternalProb += geneticMap.probabilities.get(paternalIndex);

                    paternalIndex++;
                    if (paternalIndex < geneticMap.indices.size()) {
                        paternalPosition = geneticMap.indices.get(paternalIndex);
                    }

                    if (paternalRand < paternalProb) {
                        paternalIsRecombining = !paternalIsRecombining;
                        if (paternalIndex < geneticMap.indices.size()) {
                            paternalProb = geneticMap.probabilities.get(paternalIndex);
                        }
                        paternalRand = rand.nextInt(randBound);
                    }
                }

                if (paternalIsRecombining) {
                    paternalGenome.set(i, father.maternalGenome.get(i));
                } else {
                    paternalGenome.set(i, father.paternalGenome.get(i));
                }
            }
        };

        HjSuspendable maternalRunnable = () -> {
            boolean maternalIsRecombining = false;
            double maternalProb = 0.0;
            int maternalIndex = 0;
            int maternalPosition = geneticMap.indices.get(maternalIndex);
            int maternalRand = rand.nextInt(randBound);

            for (int i = 0; i < genomeLength; i++) {
                if (i == maternalPosition) {
                    maternalProb += geneticMap.probabilities.get(maternalIndex);

                    maternalIndex++;
                    if (maternalIndex < geneticMap.indices.size()) {
                        maternalPosition = geneticMap.indices.get(maternalIndex);
                    }

                    if (maternalRand < maternalProb) {
                        maternalIsRecombining = !maternalIsRecombining;
                        if (maternalIndex < geneticMap.indices.size()) {
                            maternalProb = geneticMap.probabilities.get(maternalIndex);
                        }
                        maternalRand = rand.nextInt(randBound);
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

        double r = genomeLength * mutationRate;

        if (Math.random() < r) {
            int paternalMutatingPosition = rand.nextInt(paternalGenome.size());
            paternalGenome.flip(paternalMutatingPosition);
        }

        if (Math.random() < r) {
            int maternalMutatingPosition = rand.nextInt(maternalGenome.size());
            maternalGenome.flip(maternalMutatingPosition);
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
