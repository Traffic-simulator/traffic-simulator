package path_generator;

import path_generator.discrete_function.DiscreteFunctionBuildings;
import path_generator.discrete_function.DiscreteFunctionPeople;

import java.util.ArrayList;
import java.util.Random;

public class Main {
    public static void main(String[] args) throws InterruptedException {

        ArrayList<Building> buildings = new ArrayList<>();
        int allPeople = 0;
        int inBuildings = 0;
        for (int i = 0; i < 5; i++) {
            Building build = new Building("building" + i, 5, 4, new DiscreteFunctionBuildings(5));
            allPeople += 5;
            inBuildings += 4;
            buildings.add(build);
        }
        Peoples peoples = new Peoples(allPeople, inBuildings, new DiscreteFunctionPeople(0.001));
        PathEvaluator pathEvaluator = new PathEvaluator(buildings, peoples);

        while(true) {
            int generatePass = pathEvaluator.howManyPathsToGenerate(0);
            for (int i = 0; i < generatePass; i++) {
                pathEvaluator.generatePaths(0);
            }
            Thread.sleep(50);
        }
    }
}
