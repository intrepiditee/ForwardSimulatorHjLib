package com.intrepiditee;

import edu.rice.hj.api.SuspendableException;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.intrepiditee.Configs.FEMALE;
import static com.intrepiditee.Configs.MALE;
import static com.intrepiditee.GeneticMap.*;
import static com.intrepiditee.Segment.canMerge;
import static com.intrepiditee.Segment.merge;
import static com.intrepiditee.Utils.singletonRand;


public class Individual {
    int id;
    byte sex;

    int fatherID;
    int motherID;

    Map<Integer, Map<Byte, List<Segment>>> genome;
    Map<Integer, Map<Byte, List<Integer>>> mutationIndices;

    static AtomicInteger nextID = new AtomicInteger(0);

    static double mutationRate = 1.1 * 10e-8;


    static Individual make() {
        Individual ind = new Individual();
        return ind;
    }


    static Individual makeFromParents(Individual father, Individual mother) throws SuspendableException {
        Individual ind = make();
        ind.fatherID = father.id;
        ind.motherID = mother.id;
        ind.meiosis(father, mother);

        return ind;
    }


    private Individual() {
        id = nextID.getAndIncrement();
        genome = new HashMap<>();
        mutationIndices = new HashMap<>();

        for (int c = 1; c <= numChromosomes; c++) {
            Map<Byte, List<Segment>> chromosomesPair = new HashMap<>();
            List<Segment> paternalChromosome = new ArrayList<>();
            List<Segment> maternalChromosome = new ArrayList<>();

            int chromosomeLen = chromosomeNumberToPhysicalLength.get(c);
            Segment seg = Segment.make(0, chromosomeLen - 1, id);
            paternalChromosome.add(seg);
            seg = Segment.make(0, chromosomeLen - 1, id);
            maternalChromosome.add(seg);
            chromosomesPair.put(MALE, paternalChromosome);
            chromosomesPair.put(FEMALE, maternalChromosome);
            genome.put(c, chromosomesPair);

            List<Integer> paternalMutationIndices = new ArrayList<>();
            List<Integer> maternalMutationIndices = new ArrayList<>();
            Map<Byte, List<Integer>> mutationIndicesPair = new HashMap<>();
            mutationIndicesPair.put(MALE, paternalMutationIndices);
            mutationIndicesPair.put(FEMALE, maternalMutationIndices);
            mutationIndices.put(c, mutationIndicesPair);
        }
    }

    Individual mergeChromosomes() {
        for (int c = 1; c <= numChromosomes; c++) {
            Map<Byte, List<Segment>> chromosomePair = genome.get(c);

            List<Segment> paternalChromosome = chromosomePair.get(MALE);
            List<Segment> maternalChromosome = chromosomePair.get(FEMALE);
            chromosomePair.put(MALE, mergeOneChromosome(paternalChromosome));
            chromosomePair.put(FEMALE, mergeOneChromosome(maternalChromosome));

            genome.put(c, chromosomePair);
        }

        return this;
    }

    static List<Segment> mergeOneChromosome(List<Segment> segments) {
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


    private static List<Integer> getMutationIndices(int chromosomeNumber) {
        int chromosomeLength = chromosomeNumberToPhysicalLength.get(chromosomeNumber);
        double expectedNumMutations = chromosomeLength * mutationRate;
        int numMutations = 0;
        if (expectedNumMutations > 1.0) {
            numMutations = singletonRand.nextInt((int) (2 * expectedNumMutations));
        } else {
            if (singletonRand.nextDouble() < expectedNumMutations) {
                numMutations = 1;
            }
        }

        Set<Integer> mutationIndices = new HashSet<>(numMutations);
        for (int i = 0; i < numMutations; i++) {
            Integer index = singletonRand.nextInt(chromosomeLength);
            while (mutationIndices.contains(index)) {
                index = singletonRand.nextInt(chromosomeLength);
            }
            mutationIndices.add(index);
        }
        List<Integer> indices = new ArrayList<>(mutationIndices);
        Collections.sort(indices);

        return indices;
    }

    private static void addMutationIndices(List<Integer> mutationIndices, int chromosomeNumber) {
        List<Integer> indicesToAdd = getMutationIndices(chromosomeNumber);
        for (Integer index : indicesToAdd) {
            int i = Collections.binarySearch(mutationIndices, index);
            if (i > 0) {
                // Two mutations cancel
                mutationIndices.remove(index);
            } else {
                int insertionPoint = -(i + 1);
                mutationIndices.add(insertionPoint, index);
            }
        }
    }


    // Start from oneSegmentList
    static List<Segment> recombineOneChromosome(
        List<Segment> oneSegmentList, List<Segment> anotherSegmentList,
        List<Integer> recombinationIndices) {

        List<Segment> combinedChromosome = new ArrayList<>(oneSegmentList.size());

        int prevRecombinationIndex = -1;
        int oneIndex = 0;
        int anotherIndex = 0;
        Segment oneSegment;

        for (int recombinationIndex : recombinationIndices) {
            oneSegment = oneSegmentList.get(oneIndex);

            while (oneSegment.end <= recombinationIndex) {
                if (oneSegment.end > prevRecombinationIndex) {
                    int start = Math.max(prevRecombinationIndex, oneSegment.start);
                    combinedChromosome.add(Segment.make(start, oneSegment.end, oneSegment.founderID));
                }
                // All segments have been added
                if (oneIndex == oneSegmentList.size() - 1) {
                    break;
                }
                oneSegment = oneSegmentList.get(++oneIndex);
            }

            if (oneSegment.contains(recombinationIndex)) {
                // Need to add the upper part if there is an upper part
                if (oneSegment.start != recombinationIndex) {
                    // Start is the later of prevRecombinationIndex and oneSegment.start
                    int start = Math.max(prevRecombinationIndex, oneSegment.start);
                    // End is the earlier of recombinationIndex and oneSegment.end
                    int end = Math.min(recombinationIndex, oneSegment.end);
                    combinedChromosome.add(Segment.make(start, end, oneSegment.founderID));
                }
            }

            int tempIndex = oneIndex;
            oneIndex = anotherIndex;
            anotherIndex = tempIndex;

            List<Segment> tempList = oneSegmentList;
            oneSegmentList = anotherSegmentList;
            anotherSegmentList = tempList;

            prevRecombinationIndex = recombinationIndex;
        }

        System.out.println("one " + oneSegmentList);
        System.out.println("another " + oneSegmentList);
        System.out.println("combined " + combinedChromosome);

        return combinedChromosome;
    }


    private void meiosisOneChromosome(Individual parent, int chromosomeNumber) {
        GeneticMap m = chromosomeNumberToGeneticMap.get(chromosomeNumber);
        List<Integer> recombinationIndices = m.getRecombinationIndices(chromosomeNumber);

        // They belong to the parent
        Map<Byte, List<Segment>> chromosomesPair = parent.genome.get(chromosomeNumber);
        List<Segment> paternalChromosome = chromosomesPair.get(MALE);
        List<Segment> maternalChromosome = chromosomesPair.get(FEMALE);
        Map<Byte, List<Integer>> mutationIndicesPair = parent.mutationIndices.get(chromosomeNumber);
        List<Integer> paternalMutationIndices = mutationIndicesPair.get(MALE);
        List<Integer> maternalMutationIndices = mutationIndicesPair.get(FEMALE);

        // Decide if recombination starts from paternal or maternal chromosome of the parent
        List<Segment> oneSegmentList = paternalChromosome;
        List<Segment> anotherSegmentList = maternalChromosome;
        List<Integer> oneMutationIndices = paternalMutationIndices;
        List<Integer> anotherMutationIndices = maternalMutationIndices;
        if (singletonRand.nextBoolean()) {
            List<Segment> temp = oneSegmentList;
            oneSegmentList = anotherSegmentList;
            anotherSegmentList = temp;

            List<Integer> tempp = oneMutationIndices;
            oneMutationIndices = anotherMutationIndices;
            anotherMutationIndices = tempp;
        }

        // Generate one chromosome for the child
        // Generate mutation indices on that chromosome for the child
        List<Integer> childMutationIndices = recombineMutationIndices(
            oneMutationIndices, anotherMutationIndices, recombinationIndices
        );
        List<Segment> childChromosome = recombineOneChromosome(
            oneSegmentList, anotherSegmentList, recombinationIndices
        );
        addMutationIndices(childMutationIndices, chromosomeNumber);

        Map<Byte, List<Segment>> childChromosomesPair = genome.get(chromosomeNumber);
        Map<Byte, List<Integer>> childMutationIndicesPair = mutationIndices.get(chromosomeNumber);
        // Determine if the newly generated chromosome is paternal or maternal
        byte sex = parent.id == fatherID ? MALE : FEMALE;

        childChromosomesPair.put(sex, childChromosome);
        childMutationIndicesPair.put(MALE, childMutationIndices);

    }

    private void meiosisOneParent(Individual parent) {
        for (int c = 1; c <= numChromosomes; c++) {
            meiosisOneChromosome(parent, c);
        }
    }

    private Individual meiosis(Individual father, Individual mother) {
        meiosisOneParent(father);
        meiosisOneParent(mother);

        return this;
    }

    static List<Integer> recombineMutationIndices(
        List<Integer> oneMutationIndices, List<Integer> anotherMutationIndices,
        List<Integer> recombinationIndices) {

        List<Integer> recombinedIndices = new ArrayList<>();

        int oneIndex = 0;
        int anotherIndex = 0;
        int prevRecombinationIndex = -1;

        for (int recombinationIndex : recombinationIndices) {
            if (oneIndex < oneMutationIndices.size()) {
                int oneMutationIndex = oneMutationIndices.get(oneIndex);
                while (oneMutationIndex < recombinationIndex) {
                    if (oneMutationIndex >= prevRecombinationIndex) {
                        recombinedIndices.add(oneMutationIndex);
                    }
                    oneIndex++;
                    if (oneIndex == oneMutationIndices.size()) {
                        break;
                    }
                    oneMutationIndex = oneMutationIndices.get(oneIndex);
                }
            }

            int tempIndex = oneIndex;
            oneIndex = anotherIndex;
            anotherIndex = tempIndex;

            List<Integer> tempList = oneMutationIndices;
            oneMutationIndices = anotherMutationIndices;
            anotherMutationIndices = tempList;

            prevRecombinationIndex = recombinationIndex;
        }

        return recombinedIndices;

    }

    boolean isMale() {
        return sex == MALE;
    }

    Individual setSex(byte sex) {
        this.sex = sex;
        return this;
    }


}
