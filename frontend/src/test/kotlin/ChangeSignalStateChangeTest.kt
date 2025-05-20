import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import ru.nsu.trafficsimulator.editor.changes.ChangeSignalStateChange
import ru.nsu.trafficsimulator.model.Layout
import ru.nsu.trafficsimulator.model.Signal

class ChangeSignalStateChangeTest {

    @Test
    fun `should apply and revert signal changes correctly`() {
        val initialOffset = 10
        val initialRed = 30
        val initialGreen = 60
        val signal = Signal()
        signal.redTimeSecs = initialRed
        signal.greenTimeSecs = initialGreen
        signal.redOffsetOnStartSecs = initialOffset

        val layout = Layout()

        val newOffset = 15
        val newRed = 35
        val newGreen = 45
        val change = ChangeSignalStateChange(signal, newOffset, newRed, newGreen)

        change.apply(layout)

        assertEquals(newOffset, signal.redOffsetOnStartSecs)
        assertEquals(newRed, signal.redTimeSecs)
        assertEquals(newGreen, signal.greenTimeSecs)

        change.revert(layout)

        assertEquals(initialOffset, signal.redOffsetOnStartSecs)
        assertEquals(initialRed, signal.redTimeSecs)
        assertEquals(initialGreen, signal.greenTimeSecs)
    }

    @Test
    fun `should handle multiple apply-revert cycles`() {
        val signal = Signal()
        signal.redTimeSecs = 5
        signal.greenTimeSecs = 20
        signal.redOffsetOnStartSecs = 40
        val layout = Layout()

        val change1 = ChangeSignalStateChange(signal, 10, 30, 50)
        change1.apply(layout)
        assertEquals(10, signal.redOffsetOnStartSecs)

        val change2 = ChangeSignalStateChange(signal, 15, 35, 55)
        change2.apply(layout)
        assertEquals(15, signal.redOffsetOnStartSecs)

        change2.revert(layout)
        assertEquals(10, signal.redOffsetOnStartSecs)

        change1.revert(layout)
        assertEquals(40, signal.redOffsetOnStartSecs)
    }

    @Test
    fun `should correctly identify as non-structural change`() {
        val signal = Signal()
        val change = ChangeSignalStateChange(signal, 0, 0, 0)

        assertFalse(change.isStructuralChange())
    }

    @Test
    fun `should handle edge cases in signal timing`() {
        val signal = Signal()
        signal.redTimeSecs = 0
        signal.greenTimeSecs = 1
        signal.redOffsetOnStartSecs = 1
        val layout = Layout()

        val change = ChangeSignalStateChange(signal, 20, 20, 20)
        assertDoesNotThrow {
            change.apply(layout)
            assertEquals(20, signal.redOffsetOnStartSecs)
            assertEquals(20, signal.redTimeSecs)
            assertEquals(20, signal.greenTimeSecs)

            change.revert(layout)
        }

        val largeChange = ChangeSignalStateChange(signal, 999, 999, 999)
        assertDoesNotThrow {
            largeChange.apply(layout)
            assertEquals(999, signal.redOffsetOnStartSecs)

            largeChange.revert(layout)
            assertEquals(1, signal.redOffsetOnStartSecs)
        }
    }
}
