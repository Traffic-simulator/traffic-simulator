package path_generator.discrete_function;

import java.util.ArrayList;
import java.util.List;

public class DiscreteFunctionPeople implements DiscreteFunction {
    private final List<Double> function;
    public static final Integer NUMBER_OF_SECTIONS = 24;
    public static final Double FUNCTION_MINIMUM_VALUE = 0.0;
    public static final Double FUNCTION_MAXIMUM_VALUE = 1.0;
    /**
     * Задает любую функцию с 24 отрезками.
     * @param function
     */
    public DiscreteFunctionPeople(List<Double> function) {
        if (function.size() != NUMBER_OF_SECTIONS) {
            throw new IllegalArgumentException("List have size isn't 24.");
        }
        for (int i = 0; i < NUMBER_OF_SECTIONS; i++) {
            Double currentValue = function.get(i);
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
    public DiscreteFunctionPeople(final Double constanta) {
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
    public Double getIthPriority(Integer ithHour) {
        return this.function.get(ithHour);
    }
}
