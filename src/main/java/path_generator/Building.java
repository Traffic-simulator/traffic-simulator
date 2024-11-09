package path_generator;

import jakarta.validation.constraints.Min;
import path_generator.discrete_function.DiscreteFunction;
import path_generator.discrete_function.DiscreteFunctionBuildings;
import path_generator.discrete_function.DiscreteFunctionPeople;

/**
 * Класс описывающий любое здание
 */
public class Building {
    private String name = "";
    @Min(value = 0, message = "Must be more or equals than 0")
    private int capacity;
    @Min(value = 0, message = "Must be more or equals than 0")
    private int currentPeople;
    //жизненно необходимо добавить поле inPath. Это люди для которых место запривачено, но мы их не можем отправить в путь.
    private DiscreteFunctionBuildings discreteFunction;

    public Building(int capacity, int currentPeople, DiscreteFunctionBuildings discreteFunction) {
        this.capacity = capacity;
        this.currentPeople = currentPeople;
        this.discreteFunction = discreteFunction;
    }

    public Building(String name, int capacity, int currentPeople, DiscreteFunctionBuildings discreteFunction) {
        this(capacity, currentPeople, discreteFunction);
        this.name = name;
    }

    public void changeName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void changePriorityFunction(DiscreteFunctionBuildings discreteFunction) {
        this.discreteFunction = discreteFunction;
    }

    public DiscreteFunctionBuildings getPriorityFunction() {
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
