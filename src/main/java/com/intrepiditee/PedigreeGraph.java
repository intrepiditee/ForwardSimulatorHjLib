package com.intrepiditee;

import javafx.util.Pair;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

public class PedigreeGraph {

    Map<Integer, Set<Integer>> adjacencyList;

    Map<Pair<Integer, Integer>, Integer> shortedPathLengths;

    public static PedigreeGraph makeFromFile(String filename) {
        PedigreeGraph g = new PedigreeGraph();

        Scanner sc = Utils.getScanner(filename);

        // Add edges from children to parents.
        while (sc.hasNextInt()) {
            int id = sc.nextInt();
            int fatherID = sc.nextInt();
            int motherID = sc.nextInt();

            Set<Integer> nbrs = g.adjacencyList.getOrDefault(id, new HashSet<>());
            nbrs.add(fatherID);
            nbrs.add(motherID);
            g.adjacencyList.put(id, nbrs);
        }

        // Add edges between siblings.
        for (Integer id1 : g.adjacencyList.keySet()) {
            for (Integer id2 : g.adjacencyList.keySet()) {

                if (id1.compareTo(id2) < 0) {
                    Set<Integer> parents1 = g.adjacencyList.get(id1);
                    Set<Integer> parents2 = g.adjacencyList.get(id2);

                    if (parents1.equals(parents2)) {
                        parents1.add(id2);
                        parents2.add(id1);
                    }
                }

            }
        }

        // Complete undirected edges.
        for (Integer id1 : g.adjacencyList.keySet()) {
            for (Integer id2 : g.adjacencyList.keySet()) {

                if (id1.compareTo(id2) < 0) {
                    Set<Integer> nbrs1 = g.adjacencyList.get(id1);
                    Set<Integer> nbrs2 = g.adjacencyList.get(id2);

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
            for (Integer id1 : g.adjacencyList.keySet()) {
                for (Integer id2 : g.adjacencyList.keySet()) {

                    if (id1.compareTo(id2) < 0) {
                        int distance = g.BFS(id1, id2, 3);
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

        return g;
    }


    public int BFS(Integer start, Integer end, int maxDistance) {
        Pair<Integer, Integer> pair = new Pair<>(start, end);
        if (shortedPathLengths.containsKey(pair)) {
            return shortedPathLengths.get(pair);
        }
        Pair<Integer, Integer> reversePair = new Pair<>(end, start);
        if (shortedPathLengths.containsKey(reversePair)) {
            return shortedPathLengths.get(reversePair);
        }

        Deque<Integer> q = new ArrayDeque<>();
        Map<Integer, Integer> distances = new HashMap<>();

        q.addLast(start);
        distances.put(start, 0);

        while (!q.isEmpty()) {
            Integer current = q.removeFirst();
            Set<Integer> nbrs = adjacencyList.get(current);

            if (nbrs != null) {
                for (Integer nbr : nbrs) {
                    if (!distances.containsKey(nbr)) {
                        int distance = distances.get(current) + 1;
                        shortedPathLengths.put(pair, distance);
                        shortedPathLengths.put(reversePair, distance);

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

        shortedPathLengths.put(pair, -1);
        shortedPathLengths.put(reversePair, -1);
        return -1;
    }

    public PedigreeGraph() {
        adjacencyList = adjacencyList = new HashMap<>();
        shortedPathLengths = new HashMap<>();
    }
}
