package ru.nsu.trafficsimulator.model.intsettings

data class BuildingIntersectionSettings(
    var type: BuildingType,
    var capacity: Int = 100,
) : IntersectionSettings {
    val fullness get() = (capacity * type.fullness).toInt()
}

enum class BuildingType(val fullness: Double) {
    HOME(1.0),
    SHOPPING(0.0),
    EDUCATION(0.0),
    WORK(0.0),
    ENTERTAINMENT(0.0);
}
