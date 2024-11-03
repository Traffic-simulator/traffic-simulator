package PathGenerator;

import java.util.List;

public class PathEvaluator {
    private List<Building> allBuildings;
    private List<Building> nonEmptyBuildings;
    private List<Building> nonFullBuildings;
    private

    public PathEvaluator(List<Building> buildings) {
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
}
