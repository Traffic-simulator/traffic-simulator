package path_generator

import java.util.*
import kotlin.math.ceil

class PathEvaluator(buildings: List<Building>, people: People) {
    private val allBuildings = buildings
    private val nonEmptyBuildings: MutableList<Building> = ArrayList()
    private val people: People = people
    private val random = Random(SEED.toLong())
    private var expectedValue = 0.0
    private var numberOfAllPaths = 0

    init {
        //инициализация вспомогательных массивов
        for (build in buildings) {
            val currentPeople: Int = build.currentPeople
            if (currentPeople > 0) {
                nonEmptyBuildings.add(build)
            }
        }
    }


    fun generatePaths(ithHour: Int) {
        val from = allBuildings[random.nextInt(allBuildings.size)]
        var to: Building? = null
        //get weighed random
        var totalPriority: Long = 0
        for (build in allBuildings) {
            totalPriority += build.priorityFunction.getIthPriority(ithHour).toLong()
        }

        val randomNumber = ceil(random.nextDouble() * totalPriority)

        var cursor: Long = 0
        for (i in allBuildings.indices) {
            cursor += allBuildings[i].priorityFunction.getIthPriority(ithHour).toLong()
            if (cursor >= randomNumber) {
                to = allBuildings[i]
                break
            }
        }
        //end
        if (from.currentPeople == 1) {
            nonEmptyBuildings.remove(from)
        }
        from.changeCurrentPeople(from.currentPeople - 1)
        checkNotNull(to)
        if (to.currentPeople == 0) {
            nonEmptyBuildings.add(to)
        }
        to.changeCurrentPeople(to.currentPeople + 1)

        val thread = getThread(to, from)
        thread.start()
        numberOfAllPaths++
    }

    private fun getThread(to: Building, from: Building): Thread {
        val finalNumber = numberOfAllPaths
        return Thread {
            checkNotNull(to)
            println("[" + finalNumber + "] car start from:" + from.name + " to " + to.name)
            try {
                Thread.sleep(10000)
            } catch (e: InterruptedException) {
                println("Что-то пошло не так")
            }
            println("[" + finalNumber + "] car finish from:" + from.name + " to " + to.name)
        }
    }


    //высчитывает мат ожидание на каждой итерации, и когда набирается целое число считает, что путь готов к генерации
    fun howManyPathsToGenerate(ithHour: Int): Int {
        expectedValue += people.inBuildingsPeople.toDouble() * people.getPriorityFunction()
            .getIthPriority(ithHour)
        if (expectedValue >= 1.0) {
            val returnValue = expectedValue.toInt()
            expectedValue = expectedValue - expectedValue.toInt()
            return returnValue
        }
        return 0
    }

    companion object {
        private const val SEED = 2
    }
}
