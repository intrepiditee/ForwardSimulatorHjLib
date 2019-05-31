package com.intrepiditee;

import edu.rice.hj.api.SuspendableException;
import edu.rice.hj.runtime.config.HjSystemProperty;
import javafx.util.Pair;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static edu.rice.hj.Module0.launchHabaneroApp;
import static edu.rice.hj.Module1.forall;
import static edu.rice.hj.Module1.forallChunked;
import static edu.rice.hj.Module1.forseq;

public class PedigreeGraph {
    static int minID = Integer.MAX_VALUE;
    static int maxID = Integer.MIN_VALUE;

    static Map<Integer, Set<Integer>> adjacencyList = new HashMap<>();

    static Map<Integer, Integer> individualToGeneration = new HashMap<>();

    public static void main(String[] args) {
        Configs.numGenerationsStore = Integer.parseInt(args[1]);
        int upperBound = Integer.parseInt(args[2]);
        Configs.numThreads = Integer.parseInt(args[3]);

        System.out.println();
        for (int i = 0; i < Configs.numGenerationsStore; i++) {
            addGenerationToGraph(i);
            System.out.println("Generation " + i + " added to graph");
        }

        Configs.generationSize = (maxID - minID + 1) / Configs.numGenerationsStore;

//        System.out.println(adjacencyList);

        launchHabaneroApp(() -> {
            connectSiblings();
            System.out.println("Siblings connected");

            computePairwiseDegreeLessThanThenWrite(upperBound);
            System.out.println("Degrees written");
        });

//        System.out.println(adjacencyList);
//        System.out.println(individualToGeneration);


    }

    public static void addGenerationToGraph(int generation) {
        String filename = "Generation" + generation + "Pedigree.txt.gz";
        Scanner sc = Utils.getScanner(filename);

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


    public static void connectSiblings() throws SuspendableException {
        forall(1, Configs.numGenerationsStore - 1, (i) -> {
            int generationStartID = minID + Configs.generationSize * i;
            int generationEndID = generationStartID + Configs.generationSize;

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


    public static void computePairwiseDegreeLessThanThenWrite(int upperBound) throws SuspendableException {
        int numIndividuals = Configs.generationSize * Configs.numGenerationsStore;
        byte[][] degrees = new byte[numIndividuals][numIndividuals];

        forall(0, Configs.numThreads - 1, (i) -> {
            int count = 0;

            int startID = minID + i * Configs.generationSize;
            int endID = startID + Configs.generationSize;

            for (int id1 = startID; id1 < endID; id1++) {
                for (int id2 = id1 + 1; id2 < endID; id2++) {
                    int degree = BFSLessThan(id2, id1, upperBound);
                    if (degree != -1) {
                        degrees[id2 - minID][id1 - minID] = (byte) degree;
                    }

                    if (i == 0) {
                        count += 1;
                        if (count % 1000000 == 0) {
                            StringBuilder s = new StringBuilder();
                            s.append(count / 1000000);
                            s.append("M out of ");
                            s.append(numIndividuals * numIndividuals / 2 / 1000000);
                            s.append("M pairs computed");

                            System.out.println(s.toString());
                        }
                    }
                }
            }
        });

        // Write sequentially
        int count = 0;
        BufferedWriter w = Utils.getBufferedWriter("degrees.txt.gz");
        for (int id1 = minID; id1 <= maxID; id1++) {
            for (int id2 = id1 + 1; id2 <= maxID; id2++) {
                try {
                    w.write(String.format("%s %s %s\n", id1, id2, degrees[id2 - minID][id1 - minID]));
                    count++;
                    if (count % 1000000 == 0) {
                        System.out.println(count / 1000000 + "M pairs written");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    System.exit(-1);
                }
            }
        }

        try {
            w.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

    }

    public static int BFSLessThan(Integer start, Integer end, int upperBound) {

        // If can go up, can go down as well. If cannot go up, can go down.
        Map<Integer, Boolean> individualToCanGoUp = new HashMap<>();
        individualToCanGoUp.put(start, true);

        Deque<Integer> q = new ArrayDeque<>();
        Map<Integer, Integer> distances = new HashMap<>();

        q.addLast(start);
        distances.put(start, 0);

        while (!q.isEmpty()) {
            Integer current = q.removeFirst();
            Integer currentGeneration = individualToGeneration.get(current);
            if (currentGeneration == null) {
                System.err.println("currentGeneration: current is " + current);
                System.exit(-1);
            }
            Set<Integer> nbrs = adjacencyList.get(current);

            if (nbrs != null) {
                for (Integer nbr : nbrs) {
                    if (!distances.containsKey(nbr)) {

                        Boolean canGoUp = individualToCanGoUp.get(current);
                        if (canGoUp == null) {
                            System.err.println("canGoUp: current is " + current);
                            System.exit(-1);
                        }
                        Integer nbrGeneration = individualToGeneration.get(nbr);
                        if (nbrGeneration == null) {
                            System.err.println("nbrGeneration: nbr is " + nbr);
                            System.exit(-1);
                        }
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

                        Integer distance = distances.get(current) + 1;
                        if (distance == null) {
                            System.err.println("distance: current and nbr are " + current + " " + nbr);
                            System.exit(-1);
                        }
                        if (distance <= upperBound) {
                            distances.put(nbr, distance);
                            if (nbr.equals(end)) {
                                return distance;
                            }
                            if (distance < upperBound) {
                                q.addLast(nbr);
                            }
                        }
                    }
                }
            }
        }

        return -1;
    }

}
