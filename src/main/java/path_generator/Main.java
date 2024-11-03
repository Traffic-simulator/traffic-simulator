package path_generator;

import java.util.Random;

public class Main {
    public static void main(String[] args) {
        Random rand = new Random();
        int allUsersThatRun = 0;
        for (int i = 0; i < 10_000_000; i++) {
            for (int j = 0; j < 100; j++) {
                int curr = rand.nextInt(100);
                if (curr == 0) {
                    allUsersThatRun++;
                }
            }
        }
        System.out.println(allUsersThatRun);
        System.out.println((double) allUsersThatRun / (double)1_000_000_000);
    }
}
