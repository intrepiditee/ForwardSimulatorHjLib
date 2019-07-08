package com.intrepiditee;

import java.io.IOException;

public class Main {

    /*
    Outputs may be:
    1) Generation_: Each block consists of an serialized integer representing an individual's id
       and two serialized BitSets representing their paternal and maternal genomes.
    2) Generation_Pedigree.txt: Each line consists of an individual's id, their father's id,
       and their mother's id, separated by space.
    where _ is the index of the generation.
    */
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        if (args.length == 0 || args[0].equals("-h") || args[0].equals("--help")) {

           Utils.printUsage();
           return;
        }

        switch (args[0]) {
            case "--all":
                VCFParser.main(new String[]{"--parse", args[3], args[5]});
                Simulator.main(args);
                VCFGenerator.main(args);
                GeneticMap.main(args);
                PedigreeGraph.main(args);
                break;
            case "--parse":
                VCFParser.main(args);
                break;
            case "--simulate":
                Simulator.main(args);
                break;
            case "--generate":
                VCFGenerator.main(args);
                break;
            case "--map":
                GeneticMap.main(args);
                break;
            case "--distance":
                PedigreeGraph.main(args);
                break;
            case "--ibd":
                Segment.main(args);
                break;
            default:
                Utils.printUsage();
                return;
        }
    }

}
