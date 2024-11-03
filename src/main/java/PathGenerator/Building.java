package PathGenerator;

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
    private PriorityFunction priorityFunction;

    public Building(int capacity, int currentPeople, PriorityFunction priorityFunction) {
        this.capacity = capacity;
        this.currentPeople = currentPeople;
        this.priorityFunction = priorityFunction;
    }

    public Building(String name, int capacity, int currentPeople, PriorityFunction priorityFunction) {
        this(capacity, currentPeople, priorityFunction);
        this.name = name;
    }

    public void changeName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void changePriorityFunction(PriorityFunction priorityFunction) {
        this.priorityFunction = priorityFunction;
    }

    public PriorityFunction getPriorityFunction() {
        return priorityFunction;
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
