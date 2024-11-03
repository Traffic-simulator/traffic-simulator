package path_generator;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PathEvaluator {
    private final List<Building> allBuildings;
    private final List<Building> nonEmptyBuildings = new ArrayList<>();
    private final List<Building> nonFullBuildings = new ArrayList<>();
    private final Peoples peoples;
    private final static int SEED = 2;

    private final Random random = new Random(SEED);
    private double expectedValue;
    private int numberOfAllPaths = 0;

    public PathEvaluator(List<Building> buildings, Peoples peoples) {
        this.peoples = peoples;
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



    public void generatePaths(int ithHour) {
        Building from = allBuildings.get(random.nextInt(allBuildings.size()));
        Building to = null;
        //get weighed random
        long totalPriority = 0;
        for (Building build : nonFullBuildings) {
            totalPriority += build.getPriorityFunction().getIthPriority(ithHour);
        }

        double randomNumber = Math.ceil(random.nextDouble() * totalPriority);

        long cursor = 0;
        for (int i = 0; i < nonFullBuildings.size(); i++) {
            cursor += nonFullBuildings.get(i).getPriorityFunction().getIthPriority(ithHour);
            if (cursor >= randomNumber) {
                to = nonFullBuildings.get(i);
                break;
            }
        }
        //end
        if (from.getCurrentPeople() == 1) {
            nonEmptyBuildings.remove(from);
        }
        if (from.getCurrentPeople() == from.getCapacity()) {
            nonFullBuildings.add(from);
        }
        from.changeCurrentPeople(from.getCurrentPeople() - 1);
        assert to != null;
        if (to.getCurrentPeople() + 1 == to.getCapacity()) {
            nonFullBuildings.remove(to);
        }
        if (to.getCurrentPeople() == 0) {
            nonEmptyBuildings.add(to);
        }
        to.changeCurrentPeople(to.getCurrentPeople() + 1);

        Thread thread = getThread(to, from);
        thread.start();
        numberOfAllPaths++;
    }

    @NotNull
    private Thread getThread(Building to, Building from) {
        return new Thread(() -> {
            assert to != null;
            System.out.println("[" + numberOfAllPaths + "] car start from:" + from.getName() + " to " + to.getName());
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                System.out.println("Что-то пошло не так");
            }
            System.out.println("[" + numberOfAllPaths + "] car finish from:" + from.getName() + " to " + to.getName());
        });
    }


    //высчитывает мат ожидание на каждой итерации, и когда набирается целое число считает, что путь готов к генерации
    public int howManyPathsToGenerate(int ithHour) {
        expectedValue += (double)peoples.getInBuildingsPeople() * peoples.getPriorityFunction().getIthPriority(ithHour);
        if (expectedValue >= 1.0) {
            int returnValue = (int) expectedValue;
            expectedValue = expectedValue - (int) expectedValue;
            return returnValue;
        }
        return 0;
    }
}
