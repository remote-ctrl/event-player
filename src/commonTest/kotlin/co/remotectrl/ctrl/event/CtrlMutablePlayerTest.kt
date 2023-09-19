package co.remotectrl.ctrl.event

import co.remotectrl.ctrl.event.harness.ICtrlPlayerMutableHarness
import co.remotectrl.ctrl.event.harness.ICtrlPlayerTestHarnessBuilderData
import co.remotectrl.ctrl.event.stub.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

object StubMutableBuilderData : ICtrlPlayerTestHarnessBuilderData

class ControlMutablePlayerTest : ICtrlPlayerMutableHarness<StubMutable, Int, StubMutableBuilderData> {

    override fun createMutable(extraData: StubMutableBuilderData?) : StubMutable = StubMutable(changeVal = 0)
    override fun getValue(mutable: StubMutable): Int = mutable.changeVal

    @Test
//    fun given_Player_when_Command_executed_then_create_Event() {
//        val mutable = StubMutable(changeVal = 0)
//        val player = CtrlMutablePlayer(mutable = mutable)
//        val played = player.execute(command = StubChangeCommand())
//
//        assertPlayed<StubChangedEvent>(expectedChangeVal = 1, played)
//        assertPlayer(expectedChangeVal = 1, player)
//    }
    fun given_Player_when_Command_executed_then_create_Event() = withPlayer(1) {
        StubChangeCommand().executeAssert(1, StubChangedEvent::class)
    }

    @Test
    fun given_Player_when_Event_played_then_apply_changes() {
        val mutable = StubMutable(changeVal = 0)
        val player = CtrlMutablePlayer(mutable = mutable)
        val played = player.play(event = StubChangedEvent())

        assertPlayed<StubChangedEvent>(expectedChangeVal = 1, played)
        assertPlayer(expectedChangeVal = 1, player)
    }

    @Test
    fun given_Player_when_Event_play_failed_then_no_applying_changes() {
        val mutable = StubMutable(changeVal = 0)
        val player = CtrlMutablePlayer(mutable = mutable)
        val played = player.play(event = StubBadDivideEvent())

        assertPlayedFailedData<StubBadDivideEvent, ThrowableCause>(expectedChangeVal = 0, played)
        assertPlayer(expectedChangeVal = 0, player)
    }

    @Test
    fun given_Player_when_Events_played_then_apply_changes() {
        val player = CtrlMutablePlayer(mutable = StubMutable(changeVal = 0))
        val played = player.play(
            events = listOf(
                StubChangedEvent(),
                StubBadDivideEvent(),
                StubChangedEvent()
            )
        )

        assertEquals(3, played.size)
        assertPlayed<StubChangedEvent>(expectedChangeVal = 1, played[0])
        assertPlayedFailedData<StubBadDivideEvent, ThrowableCause>(expectedChangeVal = 1, played[1])
        assertPlayed<StubChangedEvent>(expectedChangeVal = 2, played[2])
        assertPlayer(expectedChangeVal = 2, player)
    }

    @Test
    fun given_Player_when_Events_played_either_then_apply_changes() {
        val player = CtrlMutablePlayer(mutable = StubMutable(changeVal = 0))
        val played = player.playEither(
            events = listOf(
                StubChangedEvent(),
                StubChangedEvent()
            )
        )
        assertPlayed<StubChangedEvent>(expectedChangeVal = 2, played)
        assertPlayer(expectedChangeVal = 2, player)
    }

    @Test
    fun given_Player_when_no_Events_played_then_raise_failure_cause() {
        val player = CtrlMutablePlayer(mutable = StubMutable(changeVal = 0))
        val played = player.playEither(
            events = listOf()
        )

        assertTrue(played is CtrlTry.Failure)
        assertTrue(played.failureCause is EmptyPlayableListCause)

        assertEquals(EmptyPlayableListCause().failMessage, played.failureCause.failMessage)
    }

    @Test
    fun given_Player_when_failed_Events_played_then_raise_failure_cause() {
        val player = CtrlMutablePlayer(mutable = StubMutable(changeVal = 0))
        val played = player.playEither(
            events = listOf(
                StubChangedEvent(),
                StubBadDivideEvent(),
                StubChangedEvent()
            )
        )

        assertPlayedFailedData<StubBadDivideEvent, ThrowableCause>(
            expectedChangeVal = 1,
            played = played
        )
    }

    private fun assertPlayer(
        expectedChangeVal: Int,
        player: CtrlMutablePlayer<StubMutable>
    ) {
        assertEquals(expectedChangeVal, player.mutable.changeVal)
    }

    private inline fun <reified TEvent : CtrlEvent<StubMutable>> assertPlayed(
        expectedChangeVal: Int,
        played: CtrlTry<CtrlMutableEventResult<StubMutable>>
    ) {
        assertTrue(played is CtrlTry.Success)
        assertMutableEventResult<TEvent>(expectedChangeVal, played.result)
    }

    private inline fun <reified TEvent: CtrlEvent<StubMutable>> assertMutableEventResult(
        expectedChangeVal: Int,
        result: CtrlMutableEventResult<StubMutable>
    ) {
        assertEquals(TEvent::class.java.name, result.event::class.java.name)
        assertEquals(expectedChangeVal, result.mutable.changeVal)
    }

    @Test
    fun given_Player_when_valid_Commands_played_then_apply_changes() {
        val mutable = StubMutable(changeVal = 0)
        val player = CtrlMutablePlayer(mutable = mutable)
        val played = player.execute(
            commands = listOf(
                StubChangeCommand(),
                StubBadDivideCommand(),
                StubChangeCommand(),
                StubBadDivideCommand(),
                StubChangeCommand()
            )
        )

        assertEquals(5, played.size)
        assertPlayed<StubChangedEvent>(expectedChangeVal = 1, played[0])
        assertPlayedFailedData<StubBadDivideEvent, ThrowableCause>(expectedChangeVal = 1, played[1])
        assertPlayed<StubChangedEvent>(expectedChangeVal = 2, played[2])
        assertPlayedFailedData<StubBadDivideEvent, ThrowableCause>(expectedChangeVal = 2, played[3])
        assertPlayedFailed<InvalidCommandCause>(played[4])
        assertPlayer(expectedChangeVal = 2, player)
    }

    @Test
    fun given_Player_when_validate_invalid_Command_then_fail() {
        val expected = StubMutable(changeVal = StubMutable.STUB_MAX_VAL)
        val player = CtrlMutablePlayer(mutable = expected)
        val played = player.validate(StubChangeCommand())

        assertValidatedFailed<InvalidCommandCause>(played)
        assertPlayer(
            expectedChangeVal = StubMutable.STUB_MAX_VAL,
            player
        )
    }

    @Test
    fun given_Player_when_invalid_Command_then_fail() {
        val expected = StubMutable(changeVal = StubMutable.STUB_MAX_VAL)
        val player = CtrlMutablePlayer(mutable = expected)
        val played = player.execute(StubChangeCommand())

        assertPlayedFailed<InvalidCommandCause>(played)
        assertPlayer(
            expectedChangeVal = StubMutable.STUB_MAX_VAL,
            player
        )
    }

    @Test
    fun given_Player_when_valid_Command_executed_with_bad_event_then_fail() {
        val player = CtrlMutablePlayer(mutable = StubMutable(changeVal = 0))
        val played = player.execute(command = StubBadDivideCommand())

        assertPlayedFailedData<StubBadDivideEvent, ThrowableCause>(expectedChangeVal = 0, played)
        assertPlayer(expectedChangeVal = 0, player)
    }

    private inline fun <
            reified TEvent: CtrlEvent<StubMutable>,
            reified TFailureCause: IFailureCause
            > assertPlayedFailedData(
        expectedChangeVal: Int,
        played: CtrlTry<CtrlMutableEventResult<StubMutable>>
    ) {
        assertTrue(played is CtrlTry.Failure)
        val actualFailureCause = played.failureCause
        assertEquals(CtrlMutableEventFailure::class.java.name, actualFailureCause::class.java.name)

        val playerFailure: CtrlMutableEventFailure<StubMutable> = actualFailureCause as CtrlMutableEventFailure<StubMutable>
        assertMutableEventResult<TEvent>(
            expectedChangeVal = expectedChangeVal,
            result = playerFailure.mutableEventResult
        )
        assertEquals(TFailureCause::class.java.name, playerFailure.failureCause::class.java.name)
    }

    private inline fun <reified TFailureCause: IFailureCause> assertValidatedFailed(
        played: CtrlTry<CtrlEvent<StubMutable>>
    ){
        assertTrue(played is CtrlTry.Failure)
        val actualFailureCause = played.failureCause
        assertTrue(
            actualFailureCause is TFailureCause,
            "expected FailureCause to be [${TFailureCause::class.java.name}] but was [${actualFailureCause::class.java}]")
    }

    private inline fun <reified TFailureCause: IFailureCause> assertPlayedFailed(
        played: CtrlTry<CtrlMutableEventResult<StubMutable>>
    ){
        assertTrue(played is CtrlTry.Failure)
        val actualFailureCause = played.failureCause
        assertTrue(
            actualFailureCause is TFailureCause,
            "expected FailureCause to be [${TFailureCause::class.java.name}] but was [${actualFailureCause::class.java}]")
    }

    @Test
    fun given_Player_when_invalid_Commands_either_then_fail_without_continuing() {

        val mutable = StubMutable(changeVal = 0)
        val player = CtrlMutablePlayer(mutable = mutable)
        val played = player.executeEither(
            listOf(
                StubCountIteratorCommand(),
                StubCountIteratorCommand(),
                StubCountIteratorCommand(), // cannot increment higher
                StubCountIteratorCommand() // not played
            )
        )
        assertPlayedFailed<InvalidCommandCause>(played)
        assertPlayer(expectedChangeVal = 2, player)
    }

    @Test
    fun given_Player_when_failed_Events_either_then_fail_without_continuing() {
        val player = CtrlMutablePlayer(mutable = StubMutable(changeVal = 0))
        val played = player.executeEither(
            listOf(
                StubCountIteratorCommand(),
                StubBadDivideCommand(),
                StubCountIteratorCommand(), //not played
            )
        )

        assertPlayedFailedData<StubBadDivideEvent, ThrowableCause>(expectedChangeVal = 1, played)
    }
}
