package path_generator;

import java.util.List;

public class PathEvaluator {
    private List<Building> allBuildings;
    private List<Building> nonEmptyBuildings;
    private List<Building> nonFullBuildings;
    private Peoples peoples;
    private double expectedValue;
    // Шанс что человек захочет поехать куда-нибудь на текущем кадре и ему надо построить путь.
    private static final double HUMAN_WANTS_TO_GO = 0.1;
    public PathEvaluator(List<Building> buildings, Peoples peoples) {
        allBuildings = buildings;
        //инициализация вспомогательных массивов
        for (Building build : buildings) {
            int capacity = build.getCapacity();
            int currentPeople = build.getCurrentPeople();
            if (currentPeople > 0) {
                nonEmptyBuildings.add(build);
            }
            if (currentPeople < capacity) {
                nonFullBuildings.add(build);
            }
        }
    }

//    private int howManyPathsToGenerate() {
//
//    }
}
