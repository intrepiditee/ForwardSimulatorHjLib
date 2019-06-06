package com.intrepiditee;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.ObjectInputStream;

public class temp {

    public static void main(String[] args) {

        try {
            ObjectInputStream in = Utils.getBufferedObjectInputStream("mualtor/ForwardSimulatorHjLib/target/ukb/ids.chr1");
            int[] arr = (int[]) in.readUnshared();
            in.close();

            BufferedWriter w = Utils.getBufferedWriter("mualtor/ForwardSimulatorHjLib/target/ukb/ids.txt");
            for (int id : arr) {
                w.write(String.valueOf(id));
                w.write("\n");
            }
            w.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            System.exit(-1);
        }

    }
}
