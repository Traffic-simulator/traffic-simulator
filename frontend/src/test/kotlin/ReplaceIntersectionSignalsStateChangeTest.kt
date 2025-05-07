import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import ru.nsu.trafficsimulator.editor.changes.ReplaceIntersectionSignalsStateChange
import ru.nsu.trafficsimulator.math.Spline
import ru.nsu.trafficsimulator.math.Vec2
import ru.nsu.trafficsimulator.model.Intersection
import ru.nsu.trafficsimulator.model.Layout
import ru.nsu.trafficsimulator.model.Road
import ru.nsu.trafficsimulator.model.Signal

class ReplaceIntersectionSignalsStateChangeTest {

    @Test
    fun `should replace signals and revert correctly`() {
        val layout = Layout()
        val intersection = Intersection(1, Vec2(0.0, 0.0))
        val intersection2 = Intersection(2, Vec2(10.0, 0.0))
        val spline = Spline()
        val road = Road(1, intersection, intersection2, 2, 2, spline)

        val initialSignal = Signal()
        initialSignal.redOffsetOnStartSecs = 10
        intersection.signals[road] = initialSignal

        val signal2 = Signal()
        signal2.redOffsetOnStartSecs = 20

        val newSignals = hashMapOf<Road, Signal>(
            road to signal2
        )
        val change = ReplaceIntersectionSignalsStateChange(intersection, newSignals)

        change.apply(layout)
        assertEquals(20, intersection.signals[road]?.redOffsetOnStartSecs)
        assertEquals(1, intersection.signals.size)

        change.revert(layout)
        assertEquals(10, intersection.signals[road]?.redOffsetOnStartSecs)
    }

    @Test
    fun `should handle empty signals map`() {
        val layout = Layout()
        val intersection = Intersection(1, Vec2(0.0, 0.0))
        val intersection2 = Intersection(2, Vec2(10.0, 0.0))
        val spline = Spline()
        val road = Road(1, intersection, intersection2, 2, 2, spline)

        intersection.signals[road] = Signal()

        val change = ReplaceIntersectionSignalsStateChange(intersection, hashMapOf())

        change.apply(layout)
        assertTrue(intersection.signals.isEmpty())

        change.revert(layout)
        assertEquals(1, intersection.signals.size)
    }

    @Test
    fun `should not be structural change`() {
        val change = ReplaceIntersectionSignalsStateChange(
            Intersection(1, Vec2(0.0, 0.0)),
            hashMapOf()
        )
        assertFalse(change.isStructuralChange())
    }
}
