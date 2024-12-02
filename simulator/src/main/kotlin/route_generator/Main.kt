package route_generator

import route_generator.discrete_function.DiscreteFunctionBuildings
import route_generator.discrete_function.DiscreteFunctionPeople

object Main {
    @Throws(InterruptedException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        val buildings = ArrayList<Building>()
        var allPeople = 0
        var inBuildings = 0
        for (i in 0..4) {
            val build = Building("building$i", 4, DiscreteFunctionBuildings(5))
            allPeople += 5
            inBuildings += 4
            buildings.add(build)
        }
        val people = People(allPeople, inBuildings, DiscreteFunctionPeople(0.001))
        val pathEvaluator = PathEvaluator(buildings, people)

        while (true) {
            val generatePass = pathEvaluator.howManyPathsToGenerate(0)
            for (i in 0 until generatePass) {
                pathEvaluator.generatePaths(0)
            }
            Thread.sleep(50)
        }
    }
}
