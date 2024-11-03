package path_generator.discrete_function;

import java.util.ArrayList;
import java.util.List;

public class DiscreteFunctionBuildings implements DiscreteFunction {
    private final List<Integer> function;
    public static final Integer NUMBER_OF_SECTIONS = 24;
    public static final Integer FUNCTION_MINIMUM_VALUE = 1;
    public static final Integer FUNCTION_MAXIMUM_VALUE = 100;
    /**
     * Задает любую функцию с 24 отрезками.
     * @param function
     */
    public DiscreteFunctionBuildings(List<Integer> function) {
        if (function.size() != NUMBER_OF_SECTIONS) {
            throw new IllegalArgumentException("List have size isn't 24.");
        }
        for (int i = 0; i < NUMBER_OF_SECTIONS; i++) {
            Integer currentValue = function.get(i);
            if (currentValue < FUNCTION_MINIMUM_VALUE) {
                throw new IllegalArgumentException("Function value is less than the minimum value.");
            }
            if (currentValue > FUNCTION_MAXIMUM_VALUE) {
                throw new IllegalArgumentException("Function value is greater than the maximum value.");
            }
        }
        this.function = function;
    }

    /**
     * Задает функцию с константным значением.
     * @param constanta
     */
    public DiscreteFunctionBuildings(final Integer constanta) {
        if (constanta < FUNCTION_MINIMUM_VALUE) {
            throw new IllegalArgumentException("Function value is less than the minimum value.");
        }
        if (constanta > FUNCTION_MAXIMUM_VALUE) {
            throw new IllegalArgumentException("Function value is greater than the maximum value.");
        }
        this.function = new ArrayList<>();
        for (int i = 0; i < NUMBER_OF_SECTIONS; i++) {
            this.function.add(constanta);
        }
    }

    @Override
    public Integer getIthPriority (Integer ithHour){
        return function.get(ithHour);
    }
}
