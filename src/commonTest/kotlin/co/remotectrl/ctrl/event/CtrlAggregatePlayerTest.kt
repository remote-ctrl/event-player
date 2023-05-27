package co.remotectrl.ctrl.event

import co.remotectrl.ctrl.event.stub.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ControlAggregatePlayerTest {

    @Test
    fun given_Player_when_Command_then_create_Event() {
        val player = getPlayer()

        val played = player.eventForCommand(
            command = StubChangeCommand()
        )

        assertTrue(played is CtrlTry.Success)

        val actualAggregateEvent = played.result

        assertTrue(actualAggregateEvent is StubChangedEvent)
    }

    @Test
    fun given_Player_when_Event_played_then_apply_changes() {
        val player = getPlayer()

        val played = player.playForEvent(
            aggregateEvent = CtrlAggregateEvent(
                eventId = CtrlEventId("1"),
                aggregateId = CtrlId("1"),
                version = 1,
                mutableEvent = StubChangedEvent()
            )
        )

        assertTrue(played is CtrlTry.Success)

        assertEquals("1", played.result.id.value)
        assertEquals(1, played.result.latestVersion)
        assertEquals(1, played.result.mutable.changeVal)

        assertEquals("1", player.aggregate.id.value)
        assertEquals(1, player.aggregate.latestVersion)
        assertEquals(1, player.aggregate.mutable.changeVal)
    }

    @Test
    fun given_Player_when_Event_with_different_aggregate_id_played_then_failure() {
        val player = getPlayer()

        val played = player.playForEvent(
            aggregateEvent = CtrlAggregateEvent(
                eventId = CtrlEventId("1"),
                aggregateId = CtrlId("2"), //note the aggregate id loaded in the player
                version = 1,
                mutableEvent = StubChangedEvent()
            )
        )

        assertTrue(played is CtrlTry.Failure)
        assertTrue(played.failureCause is AggregateEventInvalidAggregateIdCause<*>)
        assertEquals(
            played.failureCause.failMessage,
            "trying to apply event with aggregate id [2] when the player is handling changes for aggregate id [1]"
            )
    }

    @Test
    fun given_Player_when_Event_with_invalid_version_played_then_failure() {
        val player = getPlayer()

        val played = player.playForEvent(
            aggregateEvent = CtrlAggregateEvent(
                eventId = CtrlEventId("1"),
                aggregateId = CtrlId("1"),
                version = 2, //not the aggregate id loaded in the player
                mutableEvent = StubChangedEvent()
            )
        )

        assertTrue(played is CtrlTry.Failure)
        assertTrue(played.failureCause is AggregateEventInvalidVersionCause<*>)
        assertEquals(
            played.failureCause.failMessage,
            "trying to apply event with version [2] when the player's current aggregate is expecting next version to be [1]"
        )
    }

    @Test
    fun given_Player_when_Event_with_bad_data_played_then_failure() {
        val player = getPlayer()

        val played = player.playForEvent(
            aggregateEvent = CtrlAggregateEvent(
                eventId = CtrlEventId("1"),
                aggregateId = CtrlId("1"),
                version = 1,
                mutableEvent = StubBadDivideEvent()
            )
        )

        assertTrue(played is CtrlTry.Failure)
        assertTrue(played.failureCause is ApplyingBadEventCause<*>)
        assertEquals(
            "trying to apply bad event [StubBadDivideEvent(divideBadVal=0)] to mutable [StubMutable(changeVal=0)] but failed: [/ by zero]",
            played.failureCause.failMessage
        )
    }

    data class StubBadDivideEvent(val divideBadVal: Int = 0) : CtrlEvent<StubMutable> {
        override fun applyChangesTo(mutable: StubMutable): StubMutable {
            return mutable.copy(
                changeVal = 1 / divideBadVal
            )
        }
    }


    @Test
    fun given_Player_when_Events_played_then_apply_changes() {
        val player = getPlayer()

        val played = player.playForEvents(
            aggregateEvents = listOf(
                CtrlAggregateEvent(
                    eventId = CtrlEventId("1"),
                    aggregateId = CtrlId("1"),
                    version = 1,
                    mutableEvent = StubChangedEvent()
                ),
                CtrlAggregateEvent(
                    eventId = CtrlEventId("2"),
                    aggregateId = CtrlId("1"),
                    version = 2,
                    mutableEvent = StubChangedEvent()
                )
            )
        )

        assertTrue(played is CtrlTry.Success)

        assertEquals("1", played.result.id.value)
        assertEquals(2, played.result.latestVersion)
        assertEquals(2, played.result.mutable.changeVal)

        assertEquals("1", player.aggregate.id.value)
        assertEquals(2, player.aggregate.latestVersion)
        assertEquals(2, player.aggregate.mutable.changeVal)
    }

    @Test
    fun given_Player_when_valid_Command_played_then_apply_changes() {
        val player = getPlayer()

        val played = player.playEventForCommand(
            command = StubChangeCommand()
        )

        assertTrue(played is CtrlTry.Success)

        assertTrue(played.result.second is StubChangedEvent)

        val playedAggregate = played.result.first
        assertEquals("1", playedAggregate.id.value)
        assertEquals(1, playedAggregate.latestVersion)
        assertEquals(1, playedAggregate.mutable.changeVal)

        assertEquals("1", player.aggregate.id.value)
        assertEquals(1, player.aggregate.latestVersion)
        assertEquals(1, player.aggregate.mutable.changeVal)
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

        val playedEvents = played.result.second
        assertEquals(2, playedEvents.size)
        assertTrue(playedEvents[0] is StubChangedEvent)
        assertTrue(playedEvents[1] is StubChangedEvent)

        val playedAggregate = played.result.first
        assertEquals("1", playedAggregate.id.value)
        assertEquals(2, playedAggregate.latestVersion)
        assertEquals(2, playedAggregate.mutable.changeVal)

        assertEquals("1", player.aggregate.id.value)
        assertEquals(2, player.aggregate.latestVersion)
        assertEquals(2, player.aggregate.mutable.changeVal)
    }

    @Test
    fun given_Player_when_invalid_Command_then_fail() {
        val player = getPlayer(StubMutable.STUB_MAX_VAL)

        val played = player.playEventForCommand(
            command = StubChangeCommand() // cannot increment higher
        )

        assertTrue(played is CtrlTry.Failure)

        assertEquals("1", player.aggregate.id.value)
        assertEquals(0, player.aggregate.latestVersion)
        assertEquals(StubMutable.STUB_MAX_VAL, player.aggregate.mutable.changeVal)
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

        assertEquals("1", player.aggregate.id.value)
        assertEquals(0, player.aggregate.latestVersion)
        assertEquals(0, player.aggregate.mutable.changeVal)
    }

    private fun getPlayer(changeVal: Int = 0): CtrlAggregatePlayer<StubMutable> {
        return CtrlAggregatePlayer(
            aggregate = CtrlAggregate(
                id = CtrlId("1"),
                latestVersion = 0,
                mutable = StubMutable(
                    changeVal = changeVal
                )
            ),
        )
    }
}
