package co.remotectrl.ctrl.event

import co.remotectrl.ctrl.event.stub.StubChangeCommand
import co.remotectrl.ctrl.event.stub.StubChangedEvent
import co.remotectrl.ctrl.event.stub.StubCountIteratorCommand
import co.remotectrl.ctrl.event.stub.StubMutable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ControlMutablePlayerTest {

    @Test
    fun given_Player_when_Command_then_create_Event() {
        val player = getPlayer()

        val played = player.eventForCommand(
            command = StubChangeCommand()
        )

        assertTrue(played is CtrlTry.Success)
        assertTrue(played.result is StubChangedEvent)
    }

    @Test
    fun given_Player_when_Event_played_then_apply_changes() {
        val player = getPlayer()

        val played = player.playForEvent(
            event = StubChangedEvent()
        )

        assertTrue(played is CtrlTry.Success)

        assertEquals(1, played.result.changeVal)
        assertEquals(1, player.mutable.changeVal)
    }

    @Test
    fun given_Player_when_Events_played_then_apply_changes() {
        val player = getPlayer()

        val played = player.playForEvents(
            events = listOf(
                StubChangedEvent(),
                StubChangedEvent()
            )
        )

        assertTrue(played is CtrlTry.Success)

        assertEquals(2, played.result.changeVal)
        assertEquals(2, player.mutable.changeVal)
    }

    @Test
    fun given_Player_when_valid_Command_played_then_apply_changes() {
        val player = getPlayer()

        val played = player.playEventForCommand(
            command = StubChangeCommand()
        )

        assertTrue(played is CtrlTry.Success)

        assertTrue(played.result.second is StubChangedEvent)

        assertEquals(1, played.result.first.changeVal)
        assertEquals(1, player.mutable.changeVal)
    }

    @Test
    fun given_Player_when_valid_Commands_played_then_apply_changes() {
        val player = getPlayer()

        val played = player.playEventsForCommands(
            commands = listOf(
                StubChangeCommand(),
                StubChangeCommand()
            )
        )

        assertTrue(played is CtrlTry.Success)

        val actualEvts = played.result.second
        assertEquals(2, actualEvts.size)
        assertTrue(actualEvts[0] is StubChangedEvent)
        assertTrue(actualEvts[1] is StubChangedEvent)

        assertEquals(2, played.result.first.changeVal)
        assertEquals(2, player.mutable.changeVal)
    }

    @Test
    fun given_Player_when_invalid_Command_then_fail() {
        val player = getPlayer(StubMutable.STUB_MAX_VAL)

        val played = player.playEventForCommand(
            command = StubChangeCommand() // cannot increment higher
        )

        assertTrue(played is CtrlTry.Failure)
        assertEquals(StubMutable.STUB_MAX_VAL, player.mutable.changeVal)
    }

    @Test
    fun given_Player_when_invalid_Commands_then_fail() {
        val player = getPlayer()

        val played = player.playEventsForCommands(
            commands = listOf(
                StubCountIteratorCommand(),
                StubCountIteratorCommand(),
                StubCountIteratorCommand(), // cannot increment higher
                StubCountIteratorCommand()
            )
        )

        assertTrue(played is CtrlTry.Failure)
        assertEquals(0, player.mutable.changeVal)
    }

    private fun getPlayer(changeVal: Int = 0): CtrlMutablePlayer<StubMutable> {
        return CtrlMutablePlayer(
            mutable = StubMutable(
                changeVal = changeVal
            )
        )
    }
}
