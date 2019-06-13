package com.intrepiditee;


class Configs {

    static int numThreads = 20;
    static int numGenerations = 10;

    static int startGeneration = 0;
    static int endGeneration = 9;

    static int generationSize = 1000;

    static int numChromosomes = 22;

    static byte FEMALE = 0;
    static byte MALE = 1;
    static byte BOTH = 2;

    static double[] numChildrenProbabilities = new double[]{0.22, 0.20, 0.26, 0.16, 0.08, 0.05, 0.02};
    static double[] numChildrenProbabilitiesCumulative = prefixSum(numChildrenProbabilities);
    static int maxNumChildren = numChildrenProbabilities.length - 1;

    private static double[] prefixSum(double[] nums) {
        double[] prefixSums = new double[nums.length];
        prefixSums[0] = nums[0];
        for (int i = 1; i < nums.length; i++) {
            prefixSums[i] = prefixSums[i - 1] + nums[i];
        }
        return prefixSums;
    }
}
