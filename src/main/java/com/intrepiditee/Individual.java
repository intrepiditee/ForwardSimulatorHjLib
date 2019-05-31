package com.intrepiditee;

import edu.rice.hj.api.SuspendableException;

import java.util.ArrayList;
import java.util.Collections;
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

    private Individual mergeGenomes() {
        paternalGenome = mergeOneGenome(paternalGenome);
        maternalGenome = mergeOneGenome(maternalGenome);

        return this;
    }

    private List<Segment> mergeOneGenome(List<Segment> segments) {
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


    private Individual mutateGenomes() {

    }

    private List<Segment> mutateOneGenome(List<Segment> segments) {
        double expectedNumMutations = Configs.genomeLength * mutationRate;
        int numMutations = singletonRand.nextInt((int) (2 * expectedNumMutations));

        List<Integer> mutationIndices = new ArrayList<>(numMutations);
        for (int i = 0; i < numMutations; i++) {
            mutationIndices.add(singletonRand.nextInt(Configs.genomeLength));
        }
        Collections.sort(mutationIndices);

        List<Segment> mutatedGenome = new ArrayList<>(segments.size());

        int i = 0;
        int mutationIndex = mutationIndices.get(i);
        for (Segment segment : segments) {
            List<Integer> excludingIndicies = null;
            if (segment.contains(mutationIndex)) {
                if (excludingIndicies == null) {
                    excludingIndicies = new ArrayList<>();
                }
                excludingIndicies.add(mutationIndex);
                mutatedGenome.addAll(segment.split(excludingIndicies));
                mutationIndex = mutationIndices.get(++i);
            } else {
                mutatedGenome.add(segment);
            }
        }

        if (expectedNumMutations > 1.0) {
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

    }

    private List<Segment> recombineOneGenome(List<Segment> oneSegmentList, List<Segment> anotherSegmentList) {
        List<Integer> recombinationIndices = GeneticMap.getRecombinationIndices();

        if (singletonRand.nextBoolean()) {
            List<Segment> temp = oneSegmentList;
            oneSegmentList = anotherSegmentList;
            anotherSegmentList = temp;
        }

        int i = 0;
        int recombinationIndex;
        int prevRecombinationIndex = -1;
        int oneIndex = 0;
        int anotherIndex = 0;
        List<Segment> combinedGenome = new ArrayList<>(oneSegmentList.size());

        while (true) {
            Segment oneSegment = oneSegmentList.get(oneIndex);
            Segment anotherSegment = anotherSegmentList.get(anotherIndex);

            // After exhausting all recombination indices, set recombinationIndex to -1 so that
            // no segments can contain it. This way all remaining segments can be added.
            recombinationIndex = i >= recombinationIndices.size() ? - 1 : recombinationIndices.get(i);

            if (!oneSegment.contains(recombinationIndex)) {
                combinedGenome.add(Segment.make(oneSegment.start, oneSegment.end));
                oneIndex++;
                // While loop terminates here
                if (oneIndex == oneSegmentList.size()) {
                    break;
                }
            } else {
                // Need to add the upper part if there is an upper part
                if (oneSegment.start != recombinationIndex) {
                    // Start is the earlier of prevRecombinationIndex and oneSegment.start
                    int start = prevRecombinationIndex > oneSegment.start ?
                        prevRecombinationIndex : oneSegment.start;
                    // End is the later of recombinationIndex and oneSegment.end
                    int end = recombinationIndex < oneSegment.end ?
                        recombinationIndex : oneSegment.end;
                    combinedGenome.add(Segment.make(start, end));
                    if (end == oneSegment.end) {
                        i++;
                        oneIndex++;
                        continue;
                    }
                }
                while (!anotherSegment.contains(recombinationIndex)) {
                    anotherSegment = anotherSegmentList.get(++anotherIndex);
                }

                prevRecombinationIndex = recombinationIndex;
                i++;

                int tempIndex = oneIndex;
                oneIndex = anotherIndex;
                anotherIndex = tempIndex;

                List<Segment> tempList = oneSegmentList;
                oneSegmentList = anotherSegmentList;
                anotherSegmentList = tempList;
            }
        }

        return combinedGenome;
    }


    private Individual recombineGenomes(Individual father, Individual mother) throws SuspendableException {
        paternalGenome = recombineOneGenome(father.paternalGenome, father.maternalGenome);
        maternalGenome = recombineOneGenome(mother.paternalGenome, mother.maternalGenome);

        mergeGenomes();

        mutateGenomes();

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
