package path_generator.discrete_function

/**
 * Дискретная функция, описывающая приоритет сущности в текущий момент.
 * В первом приближении приоритет меняется каждый час и функция описывает только один день.
 * Она переодическая, день заканчивается, приоритет с нулевого часа стартует по новой.
 */
interface DiscreteFunction<T : Number?> {
    fun getIthPriority(ithHour: Int): T

    companion object {
        const val NUMBER_OF_SECTIONS: Int = 24
    }
}
