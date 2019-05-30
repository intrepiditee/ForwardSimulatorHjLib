package com.intrepiditee;

import javafx.util.Pair;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

public class PedigreeGraph {
    static int minID = Integer.MAX_VALUE;
    static int maxID = Integer.MIN_VALUE;

    static Map<Integer, Set<Integer>> adjacencyList = new HashMap<>();

    static Map<Integer, Integer> individualToGeneration = new HashMap<>();

    public static void main(String[] args) {
        Configs.numGenerations = Integer.valueOf(args[1]);
        String pedigreeFilename = args[2];
        Configs.numThreads = Integer.valueOf(args[3]);

        Scanner sc = Utils.getScanner(pedigreeFilename);

        // Add edges from children to parents.
        while (sc.hasNextInt()) {
            int id = sc.nextInt();
            minID = Math.min(minID, id);
            maxID = Math.max(maxID, id);

            int fatherID = sc.nextInt();
            int motherID = sc.nextInt();

            Set<Integer> nbrs = adjacencyList.getOrDefault(id, new HashSet<>());
            nbrs.add(fatherID);
            nbrs.add(motherID);
            adjacencyList.put(id, nbrs);
        }

        int numIndividuals = maxID - minID + 1;
        int generationSize = numIndividuals / Configs.numGenerations;

        int ancestorMaxID = minID + generationSize - 1;
        for (int i = minID; i <= maxID; i++) {
            // Discard parents of ancestors
            if (i <= ancestorMaxID) {
                adjacencyList.put(i, new HashSet<>());
            }

            // Map individuals to their generations
            individualToGeneration.put(i, i / generationSize);
        }

        System.out.println(adjacencyList);
        System.out.println(individualToGeneration);

        // Add edges between siblings.
        for (Integer id1 : adjacencyList.keySet()) {
            for (Integer id2 : adjacencyList.keySet()) {

                if (id1.compareTo(id2) < 0) {
                    Set<Integer> parents1 = adjacencyList.get(id1);
                    Set<Integer> parents2 = adjacencyList.get(id2);

                    if ((!parents1.isEmpty()) && (!parents2.isEmpty()) && parents1.equals(parents2)) {
                        parents2.add(id1);
                    }
                }

            }
        }

        // Complete undirected edges.
        for (Integer id1 : adjacencyList.keySet()) {
            for (Integer id2 : adjacencyList.keySet()) {

                if (id1.compareTo(id2) < 0) {
                    Set<Integer> nbrs1 = adjacencyList.get(id1);
                    Set<Integer> nbrs2 = adjacencyList.get(id2);

                    if (nbrs1.contains(id2) || nbrs2.contains(id1)) {
                        nbrs1.add(id2);
                        nbrs2.add(id1);
                    }
                }

            }
        }

        try {
            BufferedWriter w = Utils.getBufferedWriter("degrees.txt");

            // Compute pairwise degree
            for (Integer id1 : adjacencyList.keySet()) {
                for (Integer id2 : adjacencyList.keySet()) {

                    if (id1.compareTo(id2) < 0) {
                        int distance = BFS(id1, id2, 3);
                        if (distance != -1) {
                            w.write(String.format("%s %s %s\n", id1, id2, distance));
                        }
                    }
                }
            }

            w.close();

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

    }


    public static int BFS(Integer start, Integer end, int maxDistance) {
        Pair<Integer, Integer> pair = new Pair<>(start, end);

        // If can go up, can go down as well. If cannot go up, can go down.
        Map<Integer, Boolean> individualToCanGoUp = new HashMap<>();
        individualToCanGoUp.put(start, true);

        Deque<Integer> q = new ArrayDeque<>();
        Map<Integer, Integer> distances = new HashMap<>();

        q.addLast(start);
        distances.put(start, 0);

        int startGeneration = individualToGeneration.get(start);

        while (!q.isEmpty()) {
            Integer current = q.removeFirst();
            Set<Integer> nbrs = adjacencyList.get(current);

            if (nbrs != null) {
                for (Integer nbr : nbrs) {
                    if (!distances.containsKey(nbr)) {

                        boolean canGoUp = individualToCanGoUp.get(current);
                        int nbrGeneration = individualToGeneration.get(nbr);
                        if (canGoUp && nbrGeneration <= startGeneration) {
                            individualToCanGoUp.put(nbr, true);
                        } else if (!canGoUp) {
                            individualToCanGoUp.put(nbr, false);
                        } else {
                            continue;
                        }

                        int distance = distances.get(current) + 1;

                        if (distance <= maxDistance) {
                            distances.put(nbr, distance);
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
