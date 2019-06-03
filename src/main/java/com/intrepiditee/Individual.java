package com.intrepiditee;

import edu.rice.hj.api.SuspendableException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.intrepiditee.Segment.canMerge;
import static com.intrepiditee.Segment.merge;
import static com.intrepiditee.Utils.singletonRand;


public class Individual {
    int id;
    byte sex;

    int fatherID;
    int motherID;

    List<Segment> paternalChromosome;
    List<Segment> maternalChromosome;

    static AtomicInteger nextID = new AtomicInteger(0);

    static double mutationRate = 1.1 * 10e-8;

    static int randBound = (int) (45 * 1.2);

    static byte MALE = 1;
    static byte FEMALE = 0;

    static Individual make() {
        Individual ind = new Individual();
        return ind;
    }


    static Individual makeFromParents(Individual father, Individual mother) throws SuspendableException {
        Individual ind = make();
        ind.fatherID = father.id;
        ind.motherID = mother.id;
        ind.recombineGenomes(father, mother);

        return ind;
    }


    private Individual() {
        id = nextID.getAndIncrement();
        paternalChromosome = new ArrayList<>();
        maternalChromosome = new ArrayList<>();
        paternalChromosome.add(Segment.make(0, Configs.genomeLength));
        maternalChromosome.add(Segment.make(0, Configs.genomeLength));
    }

    private Individual mergeGenomes() {
        paternalChromosome = mergeOneGenome(paternalChromosome);
        maternalChromosome = mergeOneGenome(maternalChromosome);

        return this;
    }

    static List<Segment> mergeOneGenome(List<Segment> segments) {
        List<Segment> mergedSegments = new ArrayList<>(segments.size());

        int i = 0;
        int numSegments = segments.size();
        while (i < numSegments) {
            Segment mergedSegment = segments.get(i);
            if (i != numSegments - 1) {
                Segment nextSegment = segments.get(i + 1);
                while (canMerge(mergedSegment, nextSegment)) {
                    mergedSegment = merge(mergedSegment, nextSegment);
                    i++;
                    if (i == numSegments - 1) {
                        break;
                    } else {
                        nextSegment = segments.get(i + 1);
                    }
                }
            }
            i++;
            mergedSegments.add(mergedSegment);
        }

        return mergedSegments;
    }


    private Individual mutateGenomes() {
        mutateOneGenome(paternalChromosome);
        mutateOneGenome(maternalChromosome);
        return this;
    }

    static List<Segment> mutateOneGenome(List<Segment> segments) {
        double expectedNumMutations = Configs.genomeLength * mutationRate;
        int numMutations = 0;
        if (expectedNumMutations > 1.0) {
            numMutations = singletonRand.nextInt((int) (2 * expectedNumMutations));
        } else {
            if (singletonRand.nextDouble() < expectedNumMutations) {
                numMutations = 1;
            }
        }

        List<Integer> mutationIndices = new ArrayList<>(numMutations);
        for (int i = 0; i < numMutations; i++) {
            Integer index = singletonRand.nextInt(Configs.genomeLength);
            if (!mutationIndices.contains(index)) {
                mutationIndices.add(index);
            }
        }
        Collections.sort(mutationIndices);

        List<Segment> mutatedGenome = new ArrayList<>(segments.size());

        int i = 0;
        int mutationIndex = mutationIndices.get(i);

        for (Segment segment : segments) {
            List<Integer> excludingIndicies = null;
            while (segment.contains(mutationIndex) &&
                   mutationIndex != segment.start &&
                   mutationIndex != segment.end - 1) {

                if (excludingIndicies == null) {
                    excludingIndicies = new ArrayList<>();
                }
                excludingIndicies.add(mutationIndex);
                i++;
                mutationIndex = i >= numMutations ? -1 : mutationIndices.get(i);
            }

            if (excludingIndicies != null) {
                mutatedGenome.addAll(segment.split(excludingIndicies));
            } else {
                mutatedGenome.add(segment);
            }
        }

        return mutatedGenome;
    }

    static List<Segment> recombineOneGenome(List<Segment> oneSegmentList, List<Segment> anotherSegmentList) {
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
        paternalChromosome = recombineOneGenome(father.paternalChromosome, father.maternalChromosome);
        maternalChromosome = recombineOneGenome(mother.paternalChromosome, mother.maternalChromosome);
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
