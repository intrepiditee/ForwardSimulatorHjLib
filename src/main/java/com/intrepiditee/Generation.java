package com.intrepiditee;

import edu.rice.hj.Module1;
import edu.rice.hj.api.HjSuspendable;
import edu.rice.hj.api.SuspendableException;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.intrepiditee.Configs.numThreads;


public class Generation {

    List<Individual> males;
    List<Individual> females;

    static Generation makeEmpty() {
        return new Generation();
    }

    static Generation makeRandomGeneration() throws SuspendableException {
        Generation ancestors = makeEmpty();

        BitSet sequence = Utils.generateRandomSequence(Configs.genomeLength);

        int desiredNumMales = Configs.generationSize / 2;

        AtomicInteger numMales = new AtomicInteger(0);

        Module1.forallChunked(1, Configs.generationSize, (i) -> {

            BitSet paternalSequence = (BitSet) sequence.clone();
            BitSet maternalSequence = (BitSet) sequence.clone();

            Individual ind = Individual.makeEmpty();
            ind.paternalGenome = paternalSequence;
            ind.maternalGenome = maternalSequence;

            if (numMales.getAndIncrement() < desiredNumMales) {
                ind.sex = Individual.MALE;
            } else {
                ind.sex = Individual.FEMALE;
            }

            ancestors.add(ind);
        });

        return ancestors;
    }

    private Generation() {
        males = new ConcurrentArrayList<>(Configs.generationSize / 2);
        females = new ConcurrentArrayList<>(Configs.generationSize / 2);
    }

    public Generation add(Individual ind) {
        if (ind.isMale()) {
            males.add(ind);
        } else {
            females.add(ind);
        }
        return this;
    }

    public Generation evolve(int numGenerations) throws SuspendableException {
        Generation next = this;
        for (int i = 0; i < numGenerations; i++) {
            next = next.evolveOneGenerationThenDestroy();
        }
        return next;
    }


    /*
    There are generationSize / 2 couples. Starting from the first couple,
    they will have a child. There is 0.8 chance that the wife will have the child
    with her husband. There is 0.2 chance that the wife will have the child with another
    random man. The chance of having one more child after having one child is 0.5 of
    the chance of having the previous child. The chance of having the first child
    is 1.
    */
    public Generation evolveOneGeneration() throws SuspendableException {
        Generation next = makeEmpty();

        int numChildrenPerThread = Configs.generationSize / numThreads;
        int numMalesPerThread = numChildrenPerThread / 2;
        int numCouples = Configs.generationSize / 2;
        int numMales = numCouples;
        int numCouplesPerThread = numCouples / numThreads;

        Module1.finish(() -> {
            for (int n = 0; n < numThreads; n++) {
                Random rand = new Random();

                int start = n * numCouplesPerThread;
                int end = n == numThreads - 1 ?
                    numCouples :
                    start + numCouplesPerThread;

                int numChildrenPerThreadFinal = n == numThreads - 1 ?
                    Configs.generationSize - n * numChildrenPerThread :
                    numChildrenPerThread;
                int numMalesPerThreadFinal = n == numThreads - 1 ?
                    numMales - n * numMalesPerThread :
                    numMalesPerThread;

                HjSuspendable r = () -> {

                    Individual father;
                    Individual mother;
                    int numberOfMales = 0;
                    int numChildren = 0;

                    while (numChildren != numChildrenPerThreadFinal) {
                        for (int i = start; i < end; i++) {
                            if (numChildren == numChildrenPerThreadFinal) {
                                break;
                            }

                            father = males.get(i);
                            mother = females.get(i);

                            if (rand.nextDouble() > 0.8) {
                                father = males.get(rand.nextInt(males.size()));
                            }

                            Individual child = Individual.makeFromParents(father, mother);
                            if (numberOfMales == numMalesPerThreadFinal) {
                                child.setSex(Individual.FEMALE);
                            } else {
                                child.setSex(Individual.MALE);
                                numberOfMales++;
                            }
                            next.add(child);
                            numChildren++;

                            double prob = 0.5;
                            while (prob > Math.random()) {
                                if (numChildren == numChildrenPerThreadFinal) {
                                    break;
                                }

                                if (rand.nextDouble() > 0.8) {
                                    father = males.get(rand.nextInt(males.size()));
                                }
                                child = Individual.makeFromParents(father, mother);
                                if (numberOfMales == numMalesPerThreadFinal) {
                                    child.setSex(Individual.FEMALE);
                                } else {
                                    child.setSex(Individual.MALE);
                                    numberOfMales++;
                                }
                                next.add(child);
                                numChildren++;

                                prob *= 0.5;
                            }
                        }
                    }
                };

                Module1.async(r);
            }
        });



        return next;
    }


    public Generation evolveOneGenerationThenDestroy() throws SuspendableException {
        Generation next = evolveOneGeneration();
        destroy();
        return next;
    }


    public int size() {
        return males.size() + females.size();
    }


    public void destroy() {
        for (Individual ind : males) {
            ind.paternalGenome = null;
            ind.maternalGenome = null;
        }
        males.clear();
        males = null;

        for (Individual ind : females) {
            ind.paternalGenome = null;
            ind.maternalGenome = null;
        }
        females.clear();
        females = null;
    }
}
