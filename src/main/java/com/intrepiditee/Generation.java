package com.intrepiditee;

import edu.rice.hj.api.HjSuspendable;
import edu.rice.hj.api.SuspendableException;

import java.util.*;

import static com.intrepiditee.Configs.*;
import static edu.rice.hj.Module0.finish;
import static edu.rice.hj.Module1.async;


class Generation {

    List<Individual> males;
    List<Individual> females;

    private static Generation makeEmpty() {
        return new Generation();
    }

    static Generation makeAncestors() {
        Generation gen = makeEmpty();

        int desiredNumMales = Configs.generationSize / 2;
        int numMales = 0;

        for (int i = 0; i < Configs.generationSize; i++) {
            Individual ind = Individual.make();
            if (numMales < desiredNumMales) {
                ind.sex = MALE;
                numMales++;
            } else {
                ind.sex = FEMALE;
            }
            gen.add(ind);
        }

        return gen;
    }

    private Generation() {
        males = new ConcurrentArrayList<>(Configs.generationSize / 2);
        females = new ConcurrentArrayList<>(Configs.generationSize / 2);
    }

    private void add(Individual ind) {
        if (ind.isMale()) {
            males.add(ind);
        } else {
            females.add(ind);
        }
    }

    /*
    There are generationSize / 2 couples. Starting from the first couple,
    they will have a child. There is 0.8 chance that the wife will have the child
    with her husband. There is 0.2 chance that the wife will have the child with another
    random man. The chance of having one more child after having one child is 0.5 of
    the chance of having the previous child. The chance of having the first child
    is 1.
    */
    Generation evolveOneGeneration() throws SuspendableException {
        Generation next = makeEmpty();

        int numChildrenPerThread = Configs.generationSize / numThreads;
        int numMalesPerThread = numChildrenPerThread / 2;
        int numCouples = Configs.generationSize / 2;
        int numCouplesPerThread = numCouples / numThreads;

        finish(() -> {
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
                    numCouples - n * numMalesPerThread :
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

                            mother = females.get(i);

                            int numChildrenToHave = getNumChildren(rand);
                            for (int c = 1; c <= numChildrenToHave; c++) {
                                if (numChildren == numChildrenPerThreadFinal) {
                                    break;
                                }

                                father = males.get(i);

                                double rr = rand.nextDouble();
                                while (areSiblings(father, mother) || rr > 0.8) {
                                    System.out.println(1);
                                    father = males.get(rand.nextInt(males.size()));
                                    rr = 0;
                                }

                                Individual child = Individual.makeFromParents(father, mother);
                                if (numberOfMales == numMalesPerThreadFinal) {
                                    child.setSex(FEMALE);
                                } else {
                                    child.setSex(MALE);
                                    numberOfMales++;
                                }
                                next.add(child);
                                numChildren++;
                            }

                        }
                    }
                };

                async(r);
            }
        });

        if (Utils.singletonRand.nextBoolean()) {
            Collections.shuffle(next.males);
        } else {
            Collections.shuffle(next.females);
        }

        return next;
    }


    private static boolean areSiblings(Individual ind1, Individual ind2) {
        return ind1.fatherID == ind2.fatherID && ind1.motherID == ind2.motherID;
    }


    private static int getNumChildren(Random rand) {
        double r = rand.nextDouble();
        for (int n = 0; n <= maxNumChildren; n++) {
            if (n == maxNumChildren) {
                return n;
            }

            double cumulativeProb = numChildrenProbabilitiesCumulative[n];
            double nextCumulativeProb = numChildrenProbabilitiesCumulative[n + 1];
            if (cumulativeProb <= r &&
                r < nextCumulativeProb) {
                return n;
            }
        }

        // Unreachable
        return -1;
    }

}
