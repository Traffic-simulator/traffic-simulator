package path_generator;

import path_generator.discrete_function.DiscreteFunction;
import path_generator.discrete_function.DiscreteFunctionPeople;

public class Peoples {
    private int allPeople;
    private int inBuildingsPeople;
    private DiscreteFunction priorityFunction;
    public Peoples(int allPeople, int inBuildingsPeople, DiscreteFunction priorityFunction) {
        this.allPeople = allPeople;
        this.inBuildingsPeople = inBuildingsPeople;
        this.priorityFunction = priorityFunction;
    }

    public int getAllPeople() {
        return allPeople;
    }
    public int getInBuildingsPeople() {
        return inBuildingsPeople;
    }
    public int getPeopleOnTheWay() {
        return allPeople - inBuildingsPeople;
    }
    public DiscreteFunction getPriorityFunction() {
        return priorityFunction;
    }

    public void setAllPeople(int allPeople) {
        this.allPeople = allPeople;
    }
    public void setInBuildingsPeople(int inBuildingsPeople) {
        this.inBuildingsPeople = inBuildingsPeople;
    }


}
