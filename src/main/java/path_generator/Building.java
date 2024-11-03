package path_generator;

import jakarta.validation.constraints.Min;

/**
 * Класс описывающий любое здание
 */
public class Building {
    private String name = "";
    @Min(value = 0, message = "Must be more or equals than 0")
    private int capacity;
    @Min(value = 0, message = "Must be more or equals than 0")
    private int currentPeople;
    private DiscreteFunction discreteFunction;

    public Building(int capacity, int currentPeople, DiscreteFunction discreteFunction) {
        this.capacity = capacity;
        this.currentPeople = currentPeople;
        this.discreteFunction = discreteFunction;
    }

    public Building(String name, int capacity, int currentPeople, DiscreteFunction discreteFunction) {
        this(capacity, currentPeople, discreteFunction);
        this.name = name;
    }

    public void changeName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void changePriorityFunction(DiscreteFunction discreteFunction) {
        this.discreteFunction = discreteFunction;
    }

    public DiscreteFunction getPriorityFunction() {
        return discreteFunction;
    }

    public void changeCurrentPeople(int currentPeople) {
        if (currentPeople > this.capacity) {
            throw new IllegalArgumentException("The current people exceeds the capacity");
        }
        this.currentPeople = currentPeople;
    }

    public int getCurrentPeople() {
        return currentPeople;
    }

    public void changeCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getCapacity() {
        return capacity;
    }

}
