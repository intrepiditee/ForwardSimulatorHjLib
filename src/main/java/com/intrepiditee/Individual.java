package com.intrepiditee;

import edu.rice.hj.api.SuspendableException;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

//import static com.intrepiditee.Segment.canMerge;
//import static com.intrepiditee.Segment.merge;
import static com.intrepiditee.GeneticMap.GENETIC_TO_PHYSICAL;
import static com.intrepiditee.Utils.singletonRand;


public class Individual {
    int id;
    byte sex;

    int fatherID;
    int motherID;

    List<Segment> paternalChromosome;
    List<Segment> maternalChromosome;

    List<Integer> paternalMutationIndices;
    List<Integer> maternalMutationIndices;

    Map<Integer, List<Segment>> genome;

    static AtomicInteger nextID = new AtomicInteger(0);

    static double mutationRate = 1.1 * 10e-8;

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
        ind.meiosis(father, mother);

        return ind;
    }


    private Individual() {
        id = nextID.getAndIncrement();
        paternalChromosome = new ArrayList<>();
        maternalChromosome = new ArrayList<>();
        paternalChromosome.add(Segment.make(0, Configs.chromosomeLength, id));
        maternalChromosome.add(Segment.make(0, Configs.chromosomeLength, id));

        paternalMutationIndices = new ArrayList<>();
        maternalMutationIndices = new ArrayList<>();
    }

//    private Individual mergeChromosomes() {
//        paternalChromosome = mergeOneChromosome(paternalChromosome);
//        maternalChromosome = mergeOneChromosome(maternalChromosome);
//
//        return this;
//    }

//    static List<Segment> mergeOneChromosome(List<Segment> segments) {
//        List<Segment> mergedSegments = new ArrayList<>(segments.size());
//
//        int i = 0;
//        int numSegments = segments.size();
//        while (i < numSegments) {
//            Segment mergedSegment = segments.get(i);
//            if (i != numSegments - 1) {
//                Segment nextSegment = segments.get(i + 1);
//                while (canMerge(mergedSegment, nextSegment)) {
//                    mergedSegment = merge(mergedSegment, nextSegment);
//                    i++;
//                    if (i == numSegments - 1) {
//                        break;
//                    } else {
//                        nextSegment = segments.get(i + 1);
//                    }
//                }
//            }
//            i++;
//            mergedSegments.add(mergedSegment);
//        }
//
//        return mergedSegments;
//    }


//    private Individual mutateChromosomes() {
//        paternalChromosome = mutateOneChromosome(paternalChromosome);
//        maternalChromosome = mutateOneChromosome(maternalChromosome);
//        return this;
//    }


    private static List<Integer> getMutationIndices() {
        double expectedNumMutations = Configs.chromosomeLength * mutationRate;
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
            Integer index = singletonRand.nextInt(Configs.chromosomeLength);
            if (!mutationIndices.contains(index)) {
                mutationIndices.add(index);
            }
        }
        List<Integer> indices = new ArrayList<>(mutationIndices);
        Collections.sort(indices);

        return indices;
    }

    private static void addMutationIndices(List<Integer> mutationIndices) {
        List<Integer> indicesToAdd = getMutationIndices();
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

//    static List<Segment> mutateOneChromosome(List<Segment> segments) {
//        List<Integer> mutationIndices = getMutationIndices();
//        return mutateOneChromosome(segments, mutationIndices);
//    }

//    static List<Segment> mutateOneChromosome(List<Segment> segments, List<Integer> mutationIndices) {
//        if (mutationIndices.isEmpty()) {
//            return segments;
//        }
//
//        List<Segment> mutatedChromosome = new ArrayList<>(segments.size());
//        int numMutations = mutationIndices.size();
//
//        int i = 0;
//        int mutationIndex = mutationIndices.get(i);
//
//        for (Segment segment : segments) {
//            List<Integer> excludingIndicies = null;
//            while (i < numMutations - 1 && segment.start > mutationIndex) {
//                i++;
//                mutationIndex = mutationIndices.get(i);
//            }
//
//            if (segment.contains(mutationIndex)) {
//                while (segment.contains(mutationIndex)) {
//                    if (excludingIndicies == null) {
//                        excludingIndicies = new ArrayList<>();
//                    }
//                    excludingIndicies.add(mutationIndex);
//                    i++;
//                    mutationIndex = i >= numMutations ? -1 : mutationIndices.get(i);
//                }
//            }
//
//            if (excludingIndicies != null) {
//                mutatedChromosome.addAll(segment.split(excludingIndicies));
//            } else {
//                mutatedChromosome.add(segment);
//            }
//        }
//
//        return mutatedChromosome;
//    }


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

        return combinedChromosome;
    }
//
//    static List<Segment> recombineOneChromosome(List<Segment> oneSegmentList, List<Segment> anotherSegmentList) {
//        GeneticMap m = GeneticMap.makeFromFilename("testGeneticMap");
//        List<Integer> recombinationIndices = m.getRecombinationIndices();
//        return recombineOneChromosome(oneSegmentList, anotherSegmentList, recombinationIndices);
//    }


    private void meiosisOneParent(Individual parent) {
        GeneticMap m = GeneticMap.makeFromFilename("testGeneticMap").parseDirection(GENETIC_TO_PHYSICAL);
        List<Integer> recombinationIndices = m.getRecombinationIndices();

        // Decide if starting from paternal or maternal chromosome
        List<Segment> oneSegmentList = parent.paternalChromosome;
        List<Segment> anotherSegmentList = parent.maternalChromosome;
        List<Integer> oneMutationIndices = parent.paternalMutationIndices;
        List<Integer> anotherMutationIndices = parent.maternalMutationIndices;
        if (singletonRand.nextBoolean()) {
            List<Segment> temp = oneSegmentList;
            oneSegmentList = anotherSegmentList;
            anotherSegmentList = temp;

            List<Integer> tempp = oneMutationIndices;
            oneMutationIndices = anotherMutationIndices;
            anotherMutationIndices = tempp;
        }

        if (parent.id == fatherID) {
            paternalMutationIndices = recombineMutationIndices(
                oneMutationIndices, anotherMutationIndices, recombinationIndices
            );
            paternalChromosome = recombineOneChromosome(
                oneSegmentList, anotherSegmentList, recombinationIndices
            );
            addMutationIndices(paternalMutationIndices);
        } else if (parent.id == motherID) {
            maternalMutationIndices = recombineMutationIndices(
                oneMutationIndices, anotherMutationIndices, recombinationIndices
            );
            maternalChromosome = recombineOneChromosome(
                oneSegmentList, anotherSegmentList, recombinationIndices
            );
            addMutationIndices(maternalMutationIndices);
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

    public boolean isMale() {
        return sex == MALE;
    }

    public Individual setSex(byte sex) {
        this.sex = sex;
        return this;
    }


}
