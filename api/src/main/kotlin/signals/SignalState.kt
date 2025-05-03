package signals

enum class SignalState {
    RED,
    RED_YELLOW,
    YELLOW,
    GREEN;

    fun invert() : SignalState {
        return if (this == GREEN) {
            RED
        } else {
            GREEN
        }
    }
}
