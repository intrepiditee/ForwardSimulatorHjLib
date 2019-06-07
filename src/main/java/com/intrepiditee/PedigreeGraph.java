package com.intrepiditee;

import edu.rice.hj.api.SuspendableException;
import edu.rice.hj.runtime.config.HjSystemProperty;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.intrepiditee.Configs.generationSize;
import static com.intrepiditee.Configs.numGenerationsStore;
import static com.intrepiditee.Configs.numThreads;
import static edu.rice.hj.Module0.launchHabaneroApp;
import static edu.rice.hj.Module1.forall;
import static edu.rice.hj.Module1.forallChunked;

public class PedigreeGraph {
    private static int minID = Integer.MAX_VALUE;
    private static int maxID = Integer.MIN_VALUE;

    private static final Map<Integer, Set<Integer>> adjacencyList = new HashMap<>();

    private static final Map<Integer, Integer> individualToGeneration = new HashMap<>();

    private static final String pathPrefix = "out/";

    public static void main(String[] args) {
        if (args.length < 4 || !args[0].equals("--pedigree")) {
            Utils.printUsage();
            System.exit(-1);
        }

        System.out.println();

        numGenerationsStore = Integer.parseInt(args[1]);
        int upperBound = Integer.parseInt(args[2]);
        numThreads = Integer.parseInt(args[3]);

        HjSystemProperty.setSystemProperty(HjSystemProperty.numWorkers, numThreads);

        for (int i = 0; i < numGenerationsStore; i++) {
            addGenerationToGraph(i);
            System.out.println("Generation " + i + " added to graph");
        }

        generationSize = (maxID - minID + 1) / numGenerationsStore;

//        System.out.println(adjacencyList);

        launchHabaneroApp(() -> {
            connectSiblings();
            System.out.println("Siblings connected");

            computePairwiseDegreeLessThanAndWrite(upperBound);
            System.out.println("Degrees written");
        });

//        System.out.println(adjacencyList);
//        System.out.println(individualToGeneration);

    }

    private static void addGenerationToGraph(int generation) {
        String filename = pathPrefix + "gen" + generation + "_pedigree.txt.gz";
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

    }


    private static void connectSiblings() throws SuspendableException {
        forallChunked(1, numGenerationsStore - 1, (i) -> {
            int generationStartID = minID + generationSize * i;
            int generationEndID = generationStartID + generationSize;

            for (int id1 = generationStartID; id1 < generationEndID; id1++) {
                for (int id2 = id1 + 1; id2 < generationEndID; id2++) {
                    Set<Integer> parents1 = adjacencyList.get(id1);
                    Set<Integer> parents2 = adjacencyList.get(id2);

                    if (parents1.equals(parents2)) {
                        parents2.add(id1);
                        parents1.add(id2);
                    }
                }
            }
        });

    }


    private static void computePairwiseDegreeLessThanAndWrite(int upperBound) throws SuspendableException {
        BufferedWriter w = Utils.getBufferedGZipWriter(pathPrefix + "degrees.txt");

        AtomicInteger pairCount = new AtomicInteger(0);

        int numIndividuals = generationSize * numGenerationsStore;
        int numIndividualsPerThread = numIndividuals / numThreads;
        forall(0, numThreads - 1, (i) -> {
            int startID = minID + i * numIndividualsPerThread;
            int endID = i == numThreads - 1 ? maxID + 1 : startID + numIndividualsPerThread;

            System.out.println(startID);
            System.out.println(endID);

            for (int id1 = startID; id1 < endID; id1++) {
                for (int id2 = id1 + 1; id2 < endID; id2++) {
                    int degree = BFSLessThan(id2, id1, upperBound);
                    if (degree != -1) {
                        try {
                            w.write(String.format("%s %s %s\n", id1, id2, degree));
                        } catch (IOException e) {
                            e.printStackTrace();
                            System.exit(-1);
                        }
                    }

                    int c = pairCount.incrementAndGet();
                    if (i == 0) {
                        if (c % 1000000 == 0) {
                            String s = String.valueOf(c / 1000000) +
                                "M of out " +
                                numIndividuals * numIndividuals / 2 / 1000000 +
                                "M pairs finished";
                            System.out.println(s);
                        }
                    }
                }
            }
        });

        try {
            w.close();
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
