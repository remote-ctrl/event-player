package co.remotectrl.ctrl.event

class CtrlAggregatePlayer<TMutable : CtrlMutable<TMutable>>(
    var aggregate: CtrlAggregate<TMutable>
) {

    private val mutablePlayer = CtrlMutablePlayer(
        mutable = aggregate.mutable
    )

    private fun applyAggregate(latestVersion: Int, played: TMutable) {
        aggregate = CtrlAggregate(
            id = aggregate.id,
            latestVersion = latestVersion,
            mutable = played
        )
    }

    fun playForEvent(
        aggregateEvent: CtrlAggregateEvent<TMutable>
    ): CtrlTry<CtrlAggregate<TMutable>> = CtrlTry.invoke {
        if (aggregateEvent.aggregateId.value != aggregate.id.value) {
            return CtrlTry.Failure(
                failureCause = AggregateEventInvalidAggregateIdCause(
                    aggregateEvent = aggregateEvent,
                    aggregate = aggregate
                )
            )
        }

        val expectedNextVersion = aggregate.latestVersion + 1
        if (aggregateEvent.version != (expectedNextVersion)) {
            return CtrlTry.Failure(
                failureCause = AggregateEventInvalidVersionCause(
                    aggregateEvent = aggregateEvent,
                    expectedNextVersion = expectedNextVersion
                )
            )
        }

        val applied = mutablePlayer.playForEvent(aggregateEvent.mutableEvent).map {
            applyAggregate(aggregateEvent.version, it)
            aggregate
        }

        return when (applied) {
            is CtrlTry.Failure -> CtrlTry.Failure(
                failureCause = ApplyingBadEventCause(
                    mutable = mutablePlayer.mutable,
                    event = aggregateEvent.mutableEvent,
                    causeMsg = applied.failureCause.failMessage
                )
            )

            is CtrlTry.Success -> applied
        }
    }

    fun playForEvents(
        aggregateEvents: List<CtrlAggregateEvent<TMutable>>
    ): CtrlTry<CtrlAggregate<TMutable>> = CtrlTry.invoke {

        val latestVersion = aggregateEvents.last().version
        return mutablePlayer.playForEvents(aggregateEvents.map { it.mutableEvent }).map {
            applyAggregate(latestVersion, it)
            aggregate
        }

    }

    fun playEventForCommand(
        command: CtrlCommand<TMutable>
    ): CtrlTry<Pair<CtrlAggregate<TMutable>, CtrlEvent<TMutable>>> = CtrlTry.invoke {
        return mutablePlayer.playEventForCommand(command).map {
            applyAggregate((aggregate.latestVersion + 1), it.first)
            Pair(aggregate, it.second)
        }
    }

    fun playEventsForCommands(
        commands: List<CtrlCommand<TMutable>>
    ): CtrlTry<Pair<CtrlAggregate<TMutable>, List<CtrlEvent<TMutable>>>> = CtrlTry.invoke {
        return mutablePlayer.playEventsForCommands(commands).map {
            applyAggregate((aggregate.latestVersion + commands.size), it.first)
            Pair(aggregate, it.second)
        }
    }

    fun eventForCommand(command: CtrlCommand<TMutable>): CtrlTry<CtrlEvent<TMutable>> {
        return mutablePlayer.eventForCommand(command)
    }
}

data class AggregateEventInvalidAggregateIdCause<TMutable : CtrlMutable<TMutable>>(
    override val failMessage: String,
    val aggregateEvent: CtrlAggregateEvent<TMutable>,
    val aggregate: CtrlAggregate<TMutable>
) : IFailureCause {
    constructor(
        aggregateEvent: CtrlAggregateEvent<TMutable>,
        aggregate: CtrlAggregate<TMutable>
    ) : this(
        "trying to apply event with aggregate id [${aggregateEvent.aggregateId.value}] when the player is handling changes for aggregate id [${aggregate.id.value}]",
        aggregateEvent,
        aggregate
    )
}

data class AggregateEventInvalidVersionCause<TMutable : CtrlMutable<TMutable>>(
    override val failMessage: String,
    val aggregateEvent: CtrlAggregateEvent<TMutable>,
    val expectedNextVersion: Int
) : IFailureCause {
    constructor(
        aggregateEvent: CtrlAggregateEvent<TMutable>,
        expectedNextVersion: Int
    ) : this(
        "trying to apply event with version [${aggregateEvent.version}] when the player's current aggregate is expecting next version to be [${expectedNextVersion}]",
        aggregateEvent,
        expectedNextVersion
    )
}

data class ApplyingBadEventCause<TMutable : CtrlMutable<TMutable>>(
    override val failMessage: String,
    val mutable: TMutable,
    val event: CtrlEvent<TMutable>
) : IFailureCause {
    constructor(
        mutable: TMutable,
        event: CtrlEvent<TMutable>,
        causeMsg: String
    ) : this(
        "trying to apply bad event [${event}] to mutable [${mutable}] but failed: [$causeMsg]",
        mutable,
        event
    )
}