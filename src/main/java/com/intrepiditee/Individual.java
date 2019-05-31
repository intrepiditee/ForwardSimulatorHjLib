package com.intrepiditee;

import edu.rice.hj.api.SuspendableException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.intrepiditee.Segment.intersect;
import static com.intrepiditee.Segment.merge;
import static com.intrepiditee.Utils.singletonRand;


public class Individual {
    int id;
    byte sex;

    int fatherID;
    int motherID;

    List<Segment> paternalGenome;
    List<Segment> maternalGenome;

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
        paternalGenome = new ArrayList<>();
        maternalGenome = new ArrayList<>();
    }

    public Individual mergeSegments() {
        paternalGenome = mergeOneSegment(paternalGenome);
        maternalGenome = mergeOneSegment(maternalGenome);

        return this;
    }

    public List<Segment> mergeOneSegment(List<Segment> segments) {
        List<Segment> mergedSegments = new ArrayList<>(segments.size());

        int i = 0;
        int numPaternalSegments = paternalGenome.size();
        while (i < numPaternalSegments) {
            Segment mergedSegment = paternalGenome.get(i);
            if (i != numPaternalSegments - 1) {
                Segment nextSegment = paternalGenome.get(++i);
                while (intersect(mergedSegment, nextSegment)) {
                    mergedSegment = merge(mergedSegment, nextSegment);
                    i++;
                    if (i == numPaternalSegments) {
                        break;
                    } else {
                        nextSegment = paternalGenome.get(i);
                    }
                }
            }
            mergedSegments.add(mergedSegment);
        }

        return mergedSegments;
    }



    public Individual recombineGenomes(Individual father, Individual mother) throws SuspendableException {
        List<Integer> indices = GeneticMap.getRecombinationIndices();

        List<Segment> one = father.paternalGenome;
        List<Segment> another = father.maternalGenome;
        if (singletonRand.nextBoolean()) {
            one = father.maternalGenome;
            another = father.paternalGenome;
        }

        int i = 0;
        int recombinationIndex;
        int prevRecombinationIndex = -1;
        int oneIndex = 0;
        int anotherIndex = 0;
        List<Segment> combinedGenome = new ArrayList<>(one.size());

        boolean exhausted = false;

        while (true) {
            Segment oneSegment = one.get(oneIndex);
            Segment anotherSegment = another.get(anotherIndex);

            recombinationIndex = exhausted ? -1 : indices.get(i);

            if (!oneSegment.contains(recombinationIndex)) {
                combinedGenome.add(Segment.make(oneSegment.start, oneSegment.end));
                oneIndex++;
                if (oneIndex == one.size()) {
                    break;
                }
            } else {
                // Need to add the upper part
                if (oneSegment.start != recombinationIndex) {
                    // Start is the earlier of prevRecombinationIndex and oneSegment.start
                    int start = prevRecombinationIndex > oneSegment.start ?
                        prevRecombinationIndex : oneSegment.start;
                    // End is the later of recombinationIndex and oneSegment.end
                    int end = recombinationIndex < oneSegment.end ?
                        recombinationIndex : oneSegment.end;
                    combinedGenome.add(Segment.make(start, end));
                    if (end == oneSegment.end) {
                        recombinationIndex = indices.get(++i);
                        oneIndex++;
                        continue;
                    }
                }
                while (!anotherSegment.contains(recombinationIndex)) {
                    anotherSegment = another.get(++anotherIndex);
                }

                prevRecombinationIndex = recombinationIndex;
                i++;
                if (i == indices.size()) {
                    exhausted = true;
                }

                int tempIndex = oneIndex;
                oneIndex = anotherIndex;
                anotherIndex = tempIndex;

                List<Segment> tempList = one;
                one = another;
                another = tempList;
            }
        }




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
