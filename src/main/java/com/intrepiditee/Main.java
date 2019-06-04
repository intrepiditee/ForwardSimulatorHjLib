package com.intrepiditee;

public class Main {

    /*
    Outputs may be:
    1) Generation_: Each block consists of an serialized integer representing an individual's id
       and two serialized BitSets representing their paternal and maternal genomes.
    2) Generation_Pedigree.txt: Each line consists of an individual's id, their father's id,
       and their mother's id, separated by space.
    where _ is the index of the generation.
    */
    public static void main(String[] args) {
        if (args.length == 0 || args.length > 6 ||
            args[0].equals("-h") || args[0].equals("--help")) {

           Utils.printUsage();
           return;
        }

        switch (args[0]) {
            case "--all":
                Simulator.main(args);
                GenomeParser.main(args);
                PedigreeGraph.main(args);
                break;
            case "--simulate":
                Simulator.main(args);
                break;
            case "--parse":
                GenomeParser.main(args);
                break;
            case "--pedigree":
                PedigreeGraph.main(args);
        }
    }

}
