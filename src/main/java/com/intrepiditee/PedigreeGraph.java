package com.intrepiditee;

import edu.rice.hj.api.SuspendableException;
import edu.rice.hj.runtime.config.HjSystemProperty;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.intrepiditee.Configs.*;
import static com.intrepiditee.Utils.getBufferedGZipWriter;
import static edu.rice.hj.Module0.launchHabaneroApp;
import static edu.rice.hj.Module1.forall;

public class PedigreeGraph {
    private static int minID = Integer.MAX_VALUE;
    private static int maxID = Integer.MIN_VALUE;

    private static final Map<Integer, Set<Integer>> adjacencyList = new HashMap<>();

    private static final Map<Integer, Integer> individualToGeneration = new HashMap<>();

    public static void main(String[] args) {
        if (args.length < 4 || !args[0].equals("--pedigree")) {
            Utils.printUsage();
            System.exit(-1);
        }

        System.out.println();

        byte degree_or_meiosis = Byte.parseByte(args[1]);

        String[] fromTo = args[2].split("-");
        startGeneration = Integer.parseInt(fromTo[0]);
        endGeneration = Integer.parseInt(fromTo[1]);
        numGenerations = endGeneration - startGeneration + 1;

        int upperBound = Integer.parseInt(args[3]);
        numThreads = Integer.parseInt(args[4]);

        HjSystemProperty.setSystemProperty(HjSystemProperty.numWorkers, numThreads);

        for (int i = startGeneration; i <= endGeneration; i++) {
            addGenerationToGraph(i, degree_or_meiosis);
            System.out.println("Generation " + i + " added to graph");
        }

//        System.out.println(adjacencyList);

        launchHabaneroApp(() -> {
            computePairwiseLessThanAndWrite(upperBound, degree_or_meiosis);
            System.out.println("Degrees written");
        });

//        System.out.println(adjacencyList);
//        System.out.println(individualToGeneration);

    }

    private static void addGenerationToGraph(int generation, byte degree_or_meiosis) {
        String filename = "out/gen" + generation + "_pedigree.txt.gz";
        Scanner sc = Utils.getScannerFromGZip(filename);

        while (sc.hasNextInt()) {
            int id = sc.nextInt();
            minID = Math.min(minID, id);
            maxID = Math.max(maxID, id);
            individualToGeneration.put(id, generation);

            int fatherID = sc.nextInt();
            int motherID = sc.nextInt();

            Set<Integer> nbrs = adjacencyList.getOrDefault(id, new HashSet<>());
            Set<Integer> fatherNeighbors = adjacencyList.get(fatherID);
            Set<Integer> motherNeighbors = adjacencyList.get(motherID);
            if (fatherNeighbors != null && motherNeighbors != null) {
                nbrs.add(fatherID);
                nbrs.add(motherID);
                fatherNeighbors.add(id);
                motherNeighbors.add(id);
            }
            adjacencyList.put(id, nbrs);
        }

        if (generation != startGeneration) {
            if (degree_or_meiosis == DEGREE) {
                connectSiblingsFromGeneration(generation);
            }
        } else {
            generationSize = maxID - minID + 1;
        }
    }


    private static void connectSiblingsFromGeneration(int generation) {
        int generationStartID = minID + generationSize * (generation - startGeneration);
        int generationEndID = generationStartID + generationSize;

        Map<Integer, Set<Integer>> update = new HashMap<>();

        for (int id1 = generationStartID; id1 < generationEndID; id1++) {
            for (int id2 = id1 + 1; id2 < generationEndID; id2++) {
                Set<Integer> parents1 = adjacencyList.get(id1);
                Set<Integer> parents2 = adjacencyList.get(id2);

                if (parents1.equals(parents2)) {
                    // Have to create new sets of neighbors. Otherwise, there will be
                    // problems when there are more than two siblings.
                    Set<Integer> newNeighbors1 = update.getOrDefault(id1, new HashSet<>(parents1));
                    Set<Integer> newNeighbors2 = update.getOrDefault(id2, new HashSet<>(parents2));
                    newNeighbors1.add(id2);
                    newNeighbors2.add(id1);
                    update.put(id1, newNeighbors1);
                    update.put(id2, newNeighbors2);
                }
            }
        }

        adjacencyList.putAll(update);
    }


    private static void computePairwiseLessThanAndWrite(int upperBound, byte degree_or_meiosis) throws SuspendableException {
        BufferedWriter[] writers = new BufferedWriter[upperBound - 1];
        String pathPrefix = degree_or_meiosis == DEGREE ? "degree/" : "meiosis/";
        String filenamePrefix = degree_or_meiosis == DEGREE ? "degree_" : "meiosis_";
        for (int i = 1; i <= upperBound - 1; i++) {
            writers[i - 1] = getBufferedGZipWriter(pathPrefix + filenamePrefix + i + ".txt.gz");
        }

        AtomicInteger pairCount = new AtomicInteger(0);

        int numIndividuals = generationSize * (endGeneration - startGeneration + 1);
        int numIndividualsPerThread = numIndividuals / numThreads;
        forall(0, numThreads - 1, (i) -> {
            int startID = minID + i * numIndividualsPerThread;
            int endID = i == numThreads - 1 ? maxID + 1 : (startID + numIndividualsPerThread);

            for (int id1 = startID; id1 < endID; id1++) {
                if (id1 < 6000) {
                    continue;
                }
                for (int id2 = id1 + 1; id2 < maxID + 1; id2++) {
                    int degree = BFSLessThan(id2, id1, upperBound);
                    if (degree != -1) {
                        try {
                            writers[degree - 1].write(id1 + "\t" + id2 + "\t" + degree + "\n");
                        } catch (IOException e) {
                            e.printStackTrace();
                            System.exit(-1);
                        }
                    }

                    int c = pairCount.incrementAndGet();
                    if (c % 1000000 == 0) {
                        String s = String.valueOf(c / 1000000) +
                            "M out of " +
                            numIndividuals * (numIndividuals - 1) / 2 / 1000000 +
                            "M pairs finished";
                        System.out.println(s);
                    }
                }
            }
        });

        try {
            for (BufferedWriter w : writers) {
                w.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

    }

    private static int BFSLessThan(Integer start, Integer end, int upperBound) {

        // If can go up, can go down as well. If cannot go up, can go down.
        Map<Integer, Boolean> individualToCanGoUp = new HashMap<>();
        individualToCanGoUp.put(start, true);

        Deque<Integer> q = new ArrayDeque<>();
        Map<Integer, Integer> distances = new HashMap<>();

        q.addLast(start);
        distances.put(start, 0);

        while (!q.isEmpty()) {
            Integer current = q.removeFirst();
            int currentGeneration = individualToGeneration.get(current);
            Set<Integer> nbrs = adjacencyList.get(current);

            if (nbrs != null) {
                for (Integer nbr : nbrs) {
                    if (!distances.containsKey(nbr)) {

                        boolean canGoUp = individualToCanGoUp.get(current);
                        int nbrGeneration = individualToGeneration.get(nbr);
                        if (canGoUp) {
                            if (nbrGeneration <= currentGeneration) {
                                // Can still go up after going up
                                individualToCanGoUp.put(nbr, true);
                            } else {
                                // Cannot go up after going down
                                individualToCanGoUp.put(nbr, false);
                            }
                        } else {
                            // Can only go down but this neighbor is higher
                            if (nbrGeneration < currentGeneration) {
                                continue;
                            }
                            // Once go down, can only go down
                            individualToCanGoUp.put(nbr, false);
                        }

                        int distance = distances.get(current) + 1;
                        distances.put(nbr, distance);
                        if (distance < upperBound) {
                            if (nbr.equals(end)) {
                                return distance;
                            }
                            q.addLast(nbr);
                        }
                    }
                }
            }
        }

        return -1;
    }

}
