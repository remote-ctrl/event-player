package co.remotectrl.ctrl.event

import co.remotectrl.ctrl.event.harness.ICtrlPlayerAggregateHarness
import co.remotectrl.ctrl.event.harness.ICtrlPlayerMutableHarness
import co.remotectrl.ctrl.event.harness.ICtrlPlayerTestHarnessBuilderData
import co.remotectrl.ctrl.event.stub.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

data class StubMutableAggregateBuilderData(
        val changeVal: Int
) : ICtrlPlayerTestHarnessBuilderData

class ControlAggregatePlayerTest : ICtrlPlayerAggregateHarness<StubMutable, Int, StubMutableAggregateBuilderData> {
    override fun createMutable(extraData: StubMutableAggregateBuilderData?): StubMutable = StubMutable(
            changeVal = extraData?.changeVal ?: 0
    )

    override fun getValue(mutable: StubMutable): Int = mutable.changeVal

    @Test
    fun given_Player_when_Command_executed_then_create_Event() {
        val player = CtrlAggregatePlayer(aggregate = getAggregate(0))
        val played = player.execute(command = StubChangeCommand())

        assertPlayed(
            expectedAggregateEvent = getAggregateEvent("", "1", 1, StubChangedEvent()),
            expectedAggregate = getAggregate(1, 1),
            played
        )
        assertPlayer(
            expectedAggregate = getAggregate(1, 1),
            player
        )
    }

    private fun assertPlayer(
        expectedAggregate: CtrlAggregate<StubMutable>,
        player: CtrlAggregatePlayer<StubMutable>,
    ) {
        assertEquals(expectedAggregate.id.value, player.aggregate.id.value)
        assertEquals(expectedAggregate.latestVersion, player.aggregate.latestVersion)
        assertEquals(expectedAggregate.mutable.changeVal, player.aggregate.mutable.changeVal)
    }

    private fun assertPlayed(
        expectedAggregateEvent: CtrlAggregateEvent<StubMutable, CtrlEvent<StubMutable>>,
        expectedAggregate: CtrlAggregate<StubMutable>,
        played: CtrlTry<CtrlAggregateEventResult<StubMutable>>,
    ) {
        assertTrue(played is CtrlTry.Success)
        assertAggregateEventResult(
            expectedAggregateEvent,
            expectedAggregate = expectedAggregate,
            played.result
        )
    }

    private fun getAggregate(changeVal: Int, latestVersion: Int = 0) = CtrlAggregate(
        id = CtrlId("1"),
        latestVersion = latestVersion,
        mutable = StubMutable(
            changeVal = changeVal
        )
    )

    @Test
    fun given_Player_when_Event_played_then_apply_changes() {
        val player = CtrlAggregatePlayer(aggregate = getAggregate(0))

        val expectedAggregateEvent = getAggregateEvent("1", "1", 1, StubChangedEvent())
        val played = player.play(
            aggregateEvent = expectedAggregateEvent
        )

        val expectedAggregate = getAggregate(1, 1)
        assertPlayed(
            expectedAggregateEvent = expectedAggregateEvent,
            expectedAggregate = expectedAggregate,
            played
        )
        assertPlayer(
            expectedAggregate = expectedAggregate,
            player
        )
    }

    @Test
    fun given_Player_when_Event_with_different_aggregate_id_played_then_failure() {
        val expectedAggregateEvent = getAggregateEvent("1", "2", 1, StubChangedEvent())

        val expectedAggregate = getAggregate(0)
        val player = CtrlAggregatePlayer(aggregate = expectedAggregate)
        val played = player.play(
            aggregateEvent = expectedAggregateEvent
        )


        assertPlayedFailedData<AggregateEventInvalidAggregateIdCause<StubMutable>>(
            expectedAggregateEvent = expectedAggregateEvent,
            expectedAggregate = expectedAggregate,
            played
        )
        assertPlayer(
            expectedAggregate = expectedAggregate,
            player
        )
    }

    private inline fun <
            reified TFailureCause : IFailureCause,
            > assertPlayedFailedData(
        expectedAggregateEvent: CtrlAggregateEvent<StubMutable, CtrlEvent<StubMutable>>,
        expectedAggregate: CtrlAggregate<StubMutable>,
        played: CtrlTry<CtrlAggregateEventResult<StubMutable>>,
    ) {
        assertTrue(played is CtrlTry.Failure)
        val actualFailureCause = played.failureCause
        assertEquals(CtrlAggregateEventFailure::class.java.name, actualFailureCause::class.java.name)

        val playerFailure: CtrlAggregateEventFailure<StubMutable> =
            actualFailureCause as CtrlAggregateEventFailure<StubMutable>
        assertAggregateEventResult(
            expectedAggregateEvent = expectedAggregateEvent,
            expectedAggregate = expectedAggregate,
            result = playerFailure.aggregateEventResult
        )
        assertEquals(TFailureCause::class.java.name, playerFailure.failureCause::class.java.name)
    }

    private fun assertAggregateEventResult(
        expectedAggregateEvent: CtrlAggregateEvent<StubMutable, CtrlEvent<StubMutable>>,
        expectedAggregate: CtrlAggregate<StubMutable>,
        result: CtrlAggregateEventResult<StubMutable>,
    ) {
        val actualAggregateEvent = result.aggregateEvent
        assertEquals(expectedAggregateEvent.eventId.value, actualAggregateEvent.eventId.value)
        assertEquals(expectedAggregateEvent.aggregateId.value, actualAggregateEvent.aggregateId.value)
        assertEquals(expectedAggregateEvent.version, actualAggregateEvent.version)
        assertEquals(
            expectedAggregateEvent.mutableEvent!!::class.java.name,
            result.aggregateEvent.mutableEvent!!::class.java.name
        )

        val actualAggregate = result.aggregate
        assertEquals(expectedAggregate.id.value, actualAggregate.id.value)
        assertEquals(expectedAggregate.latestVersion, actualAggregate.latestVersion)
        assertEquals(expectedAggregate.mutable.changeVal, actualAggregate.mutable.changeVal)
    }

    @Test
    fun given_Player_when_Event_with_invalid_version_played_then_failure() {
        val expectedAggregate = getAggregate(0)
        val player = CtrlAggregatePlayer(aggregate = expectedAggregate)

        val expectedAggregateEvent = getAggregateEvent("1", "1", 2, StubChangedEvent())
        val played = player.play(
            aggregateEvent = expectedAggregateEvent,
            checkSequentialVersion = true
        )

        assertPlayedFailedData<AggregateEventInvalidVersionCause<StubMutable>>(
            expectedAggregateEvent = expectedAggregateEvent,
            expectedAggregate = expectedAggregate,
            played
        )
        assertPlayer(
            expectedAggregate = expectedAggregate,
            player
        )
    }

    @Test
    fun given_Player_when_Event_with_bad_data_played_then_failure() {
        val expectedAggregate = getAggregate(0)
        val player = CtrlAggregatePlayer(aggregate = expectedAggregate)

        val expectedAggregateEvent = getAggregateEvent("1", "1", 1, StubBadDivideEvent())
        val played = player.play(
            aggregateEvent = expectedAggregateEvent
        )

        assertPlayedFailedData<ThrowableCause>(
            expectedAggregateEvent = expectedAggregateEvent,
            expectedAggregate = expectedAggregate,
            played
        )
        assertPlayer(
            expectedAggregate = expectedAggregate,
            player
        )
    }

    @Test
    fun given_Player_when_Events_played_then_apply_changes() {
        val player = CtrlAggregatePlayer(aggregate = getAggregate(0))

        val expectedEvent1 = getAggregateEvent("1", "1", 1, StubChangedEvent())
        val expectedEvent2 = getAggregateEvent("2", "1", 2, StubBadDivideEvent())
        val expectedEvent3 = getAggregateEvent("3", "1", 3, StubChangedEvent())

        val played = player.play(
            aggregateEvents = listOf(
                expectedEvent1,
                expectedEvent2,
                expectedEvent3
            )
        )

        assertEquals(3, played.size)
        val expectedAggregate1 = getAggregate(1, 1)
        assertPlayed(
            expectedAggregate = expectedAggregate1,
            expectedAggregateEvent = expectedEvent1,
            played = played[0]
        )
        assertPlayedFailedData<ThrowableCause>(
            expectedAggregate = expectedAggregate1,
            expectedAggregateEvent = expectedEvent2,
            played = played[1]
        )

        val expectedAggregate2 = getAggregate(2, 3)
        assertPlayed(
            expectedAggregate = expectedAggregate2,
            expectedAggregateEvent = expectedEvent3,
            played = played[2]
        )
        assertPlayer(expectedAggregate = expectedAggregate2, player)
    }

    @Test
    fun given_Player_when_Events_played_either_then_apply_changes() {
        val player = CtrlAggregatePlayer(getAggregate(0))
        val expectedAggregateEvent = getAggregateEvent("2", "1", 2, StubChangedEvent())
        val played = player.playEither(
            aggregateEvents = listOf(
                getAggregateEvent("1", "1", 1, StubChangedEvent()),
                expectedAggregateEvent
            )
        )

        val expectedAggregate = getAggregate(2, 2)
        assertPlayed(
            expectedAggregateEvent = expectedAggregateEvent,
            expectedAggregate = expectedAggregate,
            played
        )
        assertPlayer(
            expectedAggregate = expectedAggregate,
            player
        )
    }

    @Test
    fun given_Player_when_validate_invalid_Command_then_fail() {
        val expectedAggregate = getAggregate(StubMutable.STUB_MAX_VAL)
        val player = CtrlAggregatePlayer(aggregate = expectedAggregate)
        val played = player.validate(StubChangeCommand())

        assertValidatedFailed<InvalidCommandCause>(played)
        assertPlayer(
            expectedAggregate = expectedAggregate,
            player
        )
    }

    @Test
    fun given_Player_when_invalid_Command_then_fail() {
        val expectedAggregate = getAggregate(StubMutable.STUB_MAX_VAL)
        val player = CtrlAggregatePlayer(
            aggregate = expectedAggregate
        )

        val played = player.execute(
            command = StubChangeCommand() // cannot increment higher
        )

        assertPlayedFailed<InvalidCommandCause>(played)

        assertPlayer(
            expectedAggregate = expectedAggregate,
            player
        )
    }

    @Test
    fun given_Player_when_valid_Command_executed_with_bad_event_then_fail() {
        val expectedAggregate = getAggregate(0, 0)
        val player = CtrlAggregatePlayer(aggregate = expectedAggregate)
        val played = player.execute(
            command = StubBadDivideCommand()
        )

        assertPlayedFailedData<ThrowableCause>(
            expectedAggregateEvent = getAggregateEvent("", "1", 1, StubBadDivideEvent()),
            expectedAggregate = expectedAggregate,
            played
        )
        assertPlayer(
            expectedAggregate = expectedAggregate,
            player
        )
    }

    @Test
    fun given_Player_when_valid_Commands_executed_then_apply_changes() {
        val player = CtrlAggregatePlayer(aggregate = getAggregate(0))
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
        val expectedAggregate1 = getAggregate(1, 1)
        assertPlayed(
            expectedAggregateEvent = getAggregateEvent("", "1", 1, StubChangedEvent()),
            expectedAggregate = expectedAggregate1,
            played[0]
        )
        assertPlayedFailedData<ThrowableCause>(
            expectedAggregateEvent = getAggregateEvent("", "1", 2, StubBadDivideEvent()),
            expectedAggregate = expectedAggregate1,
            played[1]
        )

        val expectedAggregate2 = getAggregate(2, 2)
        assertPlayed(
            expectedAggregateEvent = getAggregateEvent("", "1", 2, StubChangedEvent()),
            expectedAggregate = expectedAggregate2,
            played[2]
        )
        assertPlayedFailedData<ThrowableCause>(
            expectedAggregateEvent = getAggregateEvent("", "1", 3, StubBadDivideEvent()),
            expectedAggregate = expectedAggregate2,
            played[3]
        )
        assertPlayedFailed<InvalidCommandCause>(played[4])
        assertPlayer(
            expectedAggregate = expectedAggregate2,
            player
        )
    }

    private inline fun <reified TFailureCause : IFailureCause> assertValidatedFailed(
        played: CtrlTry<CtrlAggregateEvent<StubMutable, CtrlEvent<StubMutable>>>,
    ) {
        assertTrue(played is CtrlTry.Failure)
        val actualFailureCause = played.failureCause
        assertTrue(
            actualFailureCause is TFailureCause,
            "expected FailureCause to be [${TFailureCause::class.java.name}] but was [${actualFailureCause::class.java}]"
        )
    }

    private inline fun <reified TFailureCause : IFailureCause> assertPlayedFailed(
        played: CtrlTry<CtrlAggregateEventResult<StubMutable>>,
    ) {
        assertTrue(played is CtrlTry.Failure)
        val actualFailureCause = played.failureCause
        assertTrue(
            actualFailureCause is TFailureCause,
            "expected FailureCause to be [${TFailureCause::class.java.name}] but was [${actualFailureCause::class.java}]"
        )
    }

    private fun getAggregateEvent(
        eventIdVal: String,
        aggregateIdVal: String,
        version: Int,
        mutableEvent: CtrlEvent<StubMutable>,
    ): CtrlAggregateEvent<StubMutable, CtrlEvent<StubMutable>> =
        CtrlAggregateEvent(
            eventId = CtrlEventId(eventIdVal),
            aggregateId = CtrlId(aggregateIdVal),
            version = version,
            mutableEvent = mutableEvent
        )

    @Test
    fun given_Player_when_invalid_Commands_either_then_fail_without_continuing() {
        val player = CtrlAggregatePlayer(aggregate = getAggregate(0))
        val played = player.executeEither(
            commands = listOf(
                StubCountIteratorCommand(),
                StubCountIteratorCommand(),
                StubCountIteratorCommand(), // cannot increment higher
                StubCountIteratorCommand()
            )
        )

        assertPlayedFailed<InvalidCommandCause>(played)
        assertPlayer(expectedAggregate = getAggregate(2, 2), player)
    }

}
