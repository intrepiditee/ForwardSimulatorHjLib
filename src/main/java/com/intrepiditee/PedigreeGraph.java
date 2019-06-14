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
    private static int minID;
    private static int maxID;

    private static final Map<Integer, Set<Integer>> adjacencyList = new HashMap<>();

    private static final Map<Integer, Integer> individualToGeneration = new HashMap<>();

    static private byte[][] distances;

    public static void main(String[] args) {
        if (args.length < 7 || !args[0].equals("--pedigree")) {
            Utils.printUsage();
            System.exit(-1);
        }

        System.out.println();

        generationSize = Integer.parseInt(args[1]);

        String[] fromToFilesToRead = args[2].split("-");
        int fromToRead = Integer.parseInt(fromToFilesToRead[0]);
        int toToRead = Integer.parseInt(fromToFilesToRead[1]);

        String[] fromToToCompute = args[3].split("-");
        startGeneration = Integer.parseInt(fromToToCompute[0]);
        endGeneration = Integer.parseInt(fromToToCompute[1]);
        numGenerations = endGeneration - startGeneration + 1;
        minID = startGeneration * generationSize;
        maxID = (endGeneration + 1) * generationSize - 1;

        byte degree_or_meiosis = Byte.parseByte(args[4]);

        int maxDegree = Integer.parseInt(args[5]);
        numThreads = Integer.parseInt(args[6]);

        HjSystemProperty.setSystemProperty(HjSystemProperty.numWorkers, numThreads);

        distances = new byte[numGenerations * generationSize][numGenerations * generationSize];

        for (int i = fromToRead; i <= toToRead; i++) {
            addGenerationToGraph(i, i != fromToRead && degree_or_meiosis != MEIOSIS);
            System.out.println("Generation " + i + " added to graph");
        }

//        System.out.println(adjacencyList);

        launchHabaneroApp(() -> {
            computePairwiseLessThanOrEqualToAndWrite(maxDegree, degree_or_meiosis);
            System.out.println("Degrees written");
        });

//        System.out.println(adjacencyList);
//        System.out.println(individualToGeneration);

    }

    private static void addGenerationToGraph(int generation, boolean connectSibilings) {

        String filename = "out/gen" + generation + "_pedigree.txt.gz";
        Scanner sc = Utils.getScannerFromGZip(filename);

        while (sc.hasNextInt()) {
            int id = sc.nextInt();
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

        if (connectSibilings) {
            connectSiblingsFromGeneration(generation);
        }

    }


    private static void connectSiblingsFromGeneration(int generation) {
        int generationStartID = generation * generationSize;
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


    private static void computePairwiseLessThanOrEqualToAndWrite(int maxDegree, byte degree_or_meiosis) throws SuspendableException {
        BufferedWriter[] writers = new BufferedWriter[maxDegree];
        String pathPrefix = degree_or_meiosis == DEGREE ? "degree/" : "meiosis/";
        String filenamePrefix = degree_or_meiosis == DEGREE ? "degree_" : "meiosis_";
        for (int i = 1; i <= maxDegree; i++) {
            writers[i - 1] = getBufferedGZipWriter(pathPrefix + filenamePrefix + i + ".txt.gz");
        }

        AtomicInteger pairCount = new AtomicInteger(0);

        int numIndividuals = generationSize * numGenerations;
        int numIndividualsPerThread = numIndividuals / numThreads;
        forall(0, numThreads - 1, (i) -> {
            int startID = startGeneration * generationSize + i * numIndividualsPerThread;
            int endID = i == numThreads - 1 ? (endGeneration + 1) * generationSize : (startID + numIndividualsPerThread);

            for (int end = startID; end < endID; end++) {
                for (int start = maxID; start > end; start--) {
                    int degree = BFSLessThanOrEqualTo(start, end, maxDegree);
                    if (degree != -1) {
                        try {
                            writers[degree - 1].write(end + "\t" + start + "\t" + degree + "\n");
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

    private static int BFSLessThanOrEqualTo(Integer start, Integer end, int maxDegree) {
        if (distances[start][end] != 0) {
            return distances[start][end];
        }

        // If can go up, can go down as well. If cannot go up, can go down.
        Map<Integer, Boolean> individualToCanGoUp = new HashMap<>();
        individualToCanGoUp.put(start, true);

        Deque<Integer> q = new ArrayDeque<>();
        Map<Integer, Integer> localDistances = new HashMap<>();

        q.addLast(start);
        localDistances.put(start, 0);

        while (!q.isEmpty()) {
            Integer current = q.removeFirst();
            int currentGeneration = individualToGeneration.get(current);
            Set<Integer> nbrs = adjacencyList.get(current);

            if (nbrs != null) {
                for (Integer nbr : nbrs) {
                    if (!localDistances.containsKey(nbr)) {

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

                        int distance = localDistances.get(current) + 1;
                        localDistances.put(nbr, distance);
                        if (distance <= maxDegree) {
                            if (distances[start][nbr] == 0) {
                                distances[start][nbr] = (byte) distance;
                            }
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
