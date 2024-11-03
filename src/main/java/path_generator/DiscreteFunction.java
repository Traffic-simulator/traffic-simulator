package path_generator;

import java.util.ArrayList;
import java.util.List;

/**
 * Дискретная функция, описывающая приоритет сущности в текущий момент.
 * В первом приближении приоритет меняется каждый час и функция описывает только один день.
 * Она переодическая, день заканчивается, приоритет с нулевого часа стартует по новой.
 */
public class DiscreteFunction<T extends Number> {
    private final List<T> function;
    public static final Integer NUMBER_OF_SECTIONS = 24;
    public static final Double FUNCTION_MINIMUM_VALUE = 0.0;
    public static final Double FUNCTION_MAXIMUM_VALUE = 100.0;
    /**
     * Задает любую функцию с 24 отрезками.
     * @param function
     */
    public DiscreteFunction(List<T> function) {
        if (function.size() != NUMBER_OF_SECTIONS) {
            throw new IllegalArgumentException("List have size isn't 24.");
        }
        for (int i = 0; i < NUMBER_OF_SECTIONS; i++) {
            Double currentValue = (Double) function.get(i);
            if (currentValue <= FUNCTION_MINIMUM_VALUE) {
                throw new IllegalArgumentException("Function value is less than the minimum value.");
            }
            if (currentValue >= FUNCTION_MAXIMUM_VALUE) {
                throw new IllegalArgumentException("Function value is greater than the maximum value.");
            }
        }
        this.function = function;
    }

    /**
     * Задает функцию с константным значением.
     * @param constanta
     */
    public DiscreteFunction(final T constanta) {
        if ((Double)constanta < FUNCTION_MINIMUM_VALUE) {
            throw new IllegalArgumentException("Function value is less than the minimum value.");
        }
        if ((Double)constanta > FUNCTION_MAXIMUM_VALUE) {
            throw new IllegalArgumentException("Function value is greater than the maximum value.");
        }
        this.function = new ArrayList<>();
        for (int i = 0; i < NUMBER_OF_SECTIONS; i++) {
            this.function.add(constanta);
        }
    }

    public T getIthPriority(int ithHour) {
        return function.get(ithHour);
    }
}
