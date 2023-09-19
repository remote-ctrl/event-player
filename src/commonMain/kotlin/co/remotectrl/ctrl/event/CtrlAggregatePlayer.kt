package co.remotectrl.ctrl.event

class CtrlAggregatePlayer<TMutable : CtrlMutable<TMutable>>(
    var aggregate: CtrlAggregate<TMutable>,
) : ICtrlPlayer<TMutable, CtrlAggregateEventResult<TMutable>> {

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

    fun play(
        aggregateEvent: CtrlAggregateEvent<TMutable, CtrlEvent<TMutable>>,
        checkSequentialVersion: Boolean = false,
    ): CtrlTry<CtrlAggregateEventResult<TMutable>> = CtrlTry.invoke {
        if (aggregateEvent.aggregateId.value != aggregate.id.value) {
            return playerFailure(
                aggregateEvent = aggregateEvent,
                failureCause = AggregateEventInvalidAggregateIdCause(
                    aggregateEvent = aggregateEvent,
                    aggregate = aggregate
                )
            )
        }

        val expectedNextVersion = aggregate.latestVersion + 1
        if (checkSequentialVersion && aggregateEvent.version != (expectedNextVersion)) {
            return playerFailure(
                aggregateEvent = aggregateEvent,
                failureCause = AggregateEventInvalidVersionCause(
                    aggregateEvent = aggregateEvent,
                    expectedNextVersion = expectedNextVersion
                )
            )
        }

        return mutablePlay(aggregateEvent)
    }

    private fun playerFailure(
        aggregateEvent: CtrlAggregateEvent<TMutable, CtrlEvent<TMutable>>,
        failureCause: IFailureCause,
    ): CtrlTry<CtrlAggregateEventResult<TMutable>> {
        return CtrlTry.Failure(
            failureCause = CtrlAggregateEventFailure(
                failureCause,
                CtrlAggregateEventResult(aggregate, aggregateEvent)
            )
        )
    }


    fun play(
        aggregateEvents: List<CtrlAggregateEvent<TMutable, CtrlEvent<TMutable>>>,
    ): List<CtrlTry<CtrlAggregateEventResult<TMutable>>> = aggregateEvents.map {
        play(it)
    }

    fun playEither(
        aggregateEvents: List<CtrlAggregateEvent<TMutable, CtrlEvent<TMutable>>>,
    ): CtrlTry<CtrlAggregateEventResult<TMutable>> = CtrlTry.invoke {
        if (aggregateEvents.isEmpty()) {
            return CtrlTry.Failure(failureCause = EmptyPlayableListCause())
        }

        var lastEvent: CtrlAggregateEvent<TMutable, CtrlEvent<TMutable>>? = null
        for (i in aggregateEvents.indices) {
            lastEvent = aggregateEvents[i]
            when (val lastPlayed = play(lastEvent)) {
                is CtrlTry.Failure -> return lastPlayed
                is CtrlTry.Success -> continue
            }
        }

        return CtrlTry.Success(CtrlAggregateEventResult(aggregate, lastEvent!!))
    }

    override fun execute(
        command: CtrlCommand<TMutable>,
    ): CtrlTry<CtrlAggregateEventResult<TMutable>> = CtrlTry.invoke {
        return validate(command).flatMap { event ->
            mutablePlay(event)
        }
    }

    private fun mutablePlay(
        aggregateEvent: CtrlAggregateEvent<TMutable, CtrlEvent<TMutable>>,
    ): CtrlTry<CtrlAggregateEventResult<TMutable>> {
        return when (val mutablePlayed = mutablePlayer.play(aggregateEvent.mutableEvent!!)) {
            is CtrlTry.Failure -> playerFailure(
                aggregateEvent = aggregateEvent,
                failureCause = getMutableFailure(mutablePlayed)
            )
            is CtrlTry.Success -> {
                applyAggregate(aggregateEvent.version, mutablePlayed.result.mutable)
                CtrlTry.Success(
                    CtrlAggregateEventResult(aggregate = aggregate, aggregateEvent = aggregateEvent)
                )
            }
        }
    }

    private fun getMutableFailure(mutablePlayed: CtrlTry.Failure<CtrlMutableEventResult<TMutable>>) =
        when (val mutableEventFailure = mutablePlayed.failureCause) {
            is CtrlMutableEventFailure<*> -> mutableEventFailure.failureCause
            else -> mutableEventFailure
        }

    fun validate(
        command: CtrlCommand<TMutable>,
    ): CtrlTry<CtrlAggregateEvent<TMutable, CtrlEvent<TMutable>>> {
        return mutablePlayer.validate(command).map {
            CtrlAggregateEvent(
                eventId = CtrlEventId(""),
                aggregateId = aggregate.id,
                version = aggregate.latestVersion + 1,
                mutableEvent = it
            )
        }
    }

    fun execute(
        commands: List<CtrlCommand<TMutable>>,
    ) = commands.map {
        execute(it)
    }

    fun executeEither(
        commands: List<CtrlCommand<TMutable>>,
    ): CtrlTry<CtrlAggregateEventResult<TMutable>> {
        if (commands.isEmpty()) {
            return CtrlTry.Failure(failureCause = EmptyPlayableListCause())
        }

        var lastEvent: CtrlAggregateEvent<TMutable, CtrlEvent<TMutable>>? = null
        for (i in commands.indices) {
            val played = validate(commands[i]).flatMap {
                lastEvent = it
                play(it)
            }

            when (played) {
                is CtrlTry.Failure -> return played
                is CtrlTry.Success -> continue
            }
        }

        return CtrlTry.Success(CtrlAggregateEventResult(aggregate, lastEvent!!))
    }
}

data class CtrlAggregateEventFailure<TMutable : CtrlMutable<TMutable>>(
    override val failMessage: String,
    val failureCause: IFailureCause,
    val aggregateEventResult: CtrlAggregateEventResult<TMutable>,
) : IFailureCause {
    constructor(
        failureCause: IFailureCause,
        aggregateEventResult: CtrlAggregateEventResult<TMutable>,
    ) : this(
        failMessage = "player event encountered a failure: [${failureCause.failMessage}]",
        failureCause = failureCause,
        aggregateEventResult = aggregateEventResult
    )
}

data class AggregateEventInvalidAggregateIdCause<TMutable : CtrlMutable<TMutable>>(
    override val failMessage: String,
    val aggregateEvent: CtrlAggregateEvent<TMutable, CtrlEvent<TMutable>>,
    val aggregate: CtrlAggregate<TMutable>,
) : IFailureCause {
    constructor(
        aggregateEvent: CtrlAggregateEvent<TMutable, CtrlEvent<TMutable>>,
        aggregate: CtrlAggregate<TMutable>,
    ) : this(
        "trying to apply event with aggregate id [${aggregateEvent.aggregateId.value}] when the player is handling changes for aggregate id [${aggregate.id.value}]",
        aggregateEvent,
        aggregate
    )
}

data class AggregateEventInvalidVersionCause<TMutable : CtrlMutable<TMutable>>(
    override val failMessage: String,
    val aggregateEvent: CtrlAggregateEvent<TMutable, CtrlEvent<TMutable>>,
    val expectedNextVersion: Int,
) : IFailureCause {
    constructor(
        aggregateEvent: CtrlAggregateEvent<TMutable, CtrlEvent<TMutable>>,
        expectedNextVersion: Int,
    ) : this(
        "trying to apply event with version [${aggregateEvent.version}] when the player's current aggregate is expecting next version to be [${expectedNextVersion}]",
        aggregateEvent,
        expectedNextVersion
    )
}