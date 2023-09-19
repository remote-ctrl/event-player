package co.remotectrl.ctrl.event

class CtrlMutablePlayer<TMutable : CtrlMutable<TMutable>>(
    var mutable: TMutable,
) : ICtrlPlayer<TMutable, CtrlMutableEventResult<TMutable>>{

    fun play(
        event: CtrlEvent<TMutable>
    ): CtrlTry<CtrlMutableEventResult<TMutable>> {
        val played = CtrlTry.invoke {
            mutable = event.applyChangesTo(mutable)
            CtrlMutableEventResult(mutable, event)
        }

        return when(played){
            is CtrlTry.Failure -> playerFailure(
                event = event,
                failureCause = played.failureCause
            )
            is CtrlTry.Success -> played
        }
    }

    fun play(
        events: List<CtrlEvent<TMutable>>
    ): List<CtrlTry<CtrlMutableEventResult<TMutable>>> = events.map {
        play(it)
    }

    fun playEither(
        events: List<CtrlEvent<TMutable>>
    ): CtrlTry<CtrlMutableEventResult<TMutable>> {
        if(events.isEmpty()){
            return CtrlTry.Failure(failureCause = EmptyPlayableListCause())
        }

        var lastEvent: CtrlEvent<TMutable>? = null
        for (i in events.indices) {
            lastEvent = events[i]
            when(val lastPlayed = play(lastEvent)){
                is CtrlTry.Failure -> return lastPlayed
                is CtrlTry.Success -> continue
            }
        }

        return CtrlTry.Success(CtrlMutableEventResult(mutable, lastEvent!!))
    }

    private fun playerFailure(
        event: CtrlEvent<TMutable>,
        failureCause: IFailureCause,
    ): CtrlTry<CtrlMutableEventResult<TMutable>> {
        return CtrlTry.Failure(
            failureCause = CtrlMutableEventFailure(
                failureCause,
                CtrlMutableEventResult(mutable, event)
            )
        )
    }

    fun validate(
        command: CtrlCommand<TMutable>,
    ): CtrlTry<CtrlEvent<TMutable>> {
        val validation = CtrlValidation(mutableListOf())

        command.validate(mutable, validation)

        val validatedItems = validation.invalidInputItems.toTypedArray()

        return CtrlTry(when {
            validatedItems.isNotEmpty() -> {
                CtrlExecution.Invalidated(items = validatedItems)
            }

            else -> {
                CtrlExecution.Validated(
                    event = command.makeEvent()
                )
            }
        })
    }

    override fun execute(
        command: CtrlCommand<TMutable>,
    ): CtrlTry<CtrlMutableEventResult<TMutable>> = validate(command).flatMap {
        play(it)
    }

    fun execute(
        commands: List<CtrlCommand<TMutable>>
    ): List<CtrlTry<CtrlMutableEventResult<TMutable>>> = commands.map {
        execute(it)
    }

    fun executeEither(
        commands: List<CtrlCommand<TMutable>>
    ): CtrlTry<CtrlMutableEventResult<TMutable>> {
        if(commands.isEmpty()){
            return CtrlTry.Failure(failureCause = EmptyPlayableListCause())
        }

        var lastEvent: CtrlEvent<TMutable>? = null
        for (i in commands.indices) {
            val played = validate(commands[i]).flatMap {
                lastEvent = it
                play(it)
            }

            when(played){
                is CtrlTry.Failure ->
                    return played
                is CtrlTry.Success ->
                    continue
            }
        }

        return CtrlTry.Success(CtrlMutableEventResult(mutable, lastEvent!!))
    }

}

data class CtrlMutableEventFailure<TMutable : CtrlMutable<TMutable>>(
    override val failMessage: String,
    val failureCause: IFailureCause,
    val mutableEventResult: CtrlMutableEventResult<TMutable>,
) : IFailureCause {
    constructor(
        failureCause: IFailureCause,
        mutableEventResult: CtrlMutableEventResult<TMutable>
    ) : this(
        "trying to apply bad event [${mutableEventResult.event}] to mutable [${mutableEventResult.mutable}] but failed: [${failureCause.failMessage}]",
        failureCause,
        mutableEventResult
    )
}

data class EmptyPlayableListCause(override val failMessage: String = "play requires at least one item") : IFailureCause

