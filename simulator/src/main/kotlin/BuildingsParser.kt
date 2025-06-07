import opendrive.OpenDRIVE
import opendrive.TUserData
import route_generator_new.BuildingTypes
import route_generator_new.discrete_function.Building

class BuildingsParser(val openDrive: OpenDRIVE) {

    public fun getBuildings() : List<Building> {
        val buildings: MutableList<Building> = mutableListOf()
        for (junction in openDrive.junction) {
            val buildingId = junction.id
            lateinit var buildingType : BuildingTypes
            var buildingCapacity : Int = -1
            var buildingCurrentPeople : Int = -1
            val additionalData = junction.gAdditionalData

            for (data in additionalData) {
                val tUser = data as TUserData
                when (tUser.code) {

                    "buildingType" -> buildingType = BuildingTypes.valueOf(tUser.value)
                    "buildingCapacity" -> buildingCapacity = tUser.value.toInt()
                    "buildingFullness" -> buildingCurrentPeople = tUser.value.toInt()
                    else -> {
                        throw Exception("unknown type + ${tUser.code}")
                    }
                }
            }

            if (buildingCapacity == -1 || buildingCurrentPeople == -1) {
                continue
            }
            buildings.add(Building(buildingType, buildingCapacity, buildingCurrentPeople, buildingId))
        }

        return buildings;
    }


}
