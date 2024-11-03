package path_generator.discrete_function;

/**
 * Дискретная функция, описывающая приоритет сущности в текущий момент.
 * В первом приближении приоритет меняется каждый час и функция описывает только один день.
 * Она переодическая, день заканчивается, приоритет с нулевого часа стартует по новой.
 */
public interface DiscreteFunction<T extends Number> {
    public static final Integer NUMBER_OF_SECTIONS = 24;
    public T getIthPriority (int ithHour);
    
}
