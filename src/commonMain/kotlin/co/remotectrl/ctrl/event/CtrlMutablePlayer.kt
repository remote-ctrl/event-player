package co.remotectrl.ctrl.event

class CtrlMutablePlayer<TMutable : CtrlMutable<TMutable>>(
    var mutable: TMutable
) {
    fun playForEvent(event: CtrlEvent<TMutable>): CtrlTry<TMutable> = CtrlTry.invoke {
        mutable = event.applyChangesTo(mutable)
        mutable
    }

    fun playForEvents(events: List<CtrlEvent<TMutable>>): CtrlTry<TMutable> {
        var failure: CtrlTry<TMutable>? = null
        for (evt in events) {
            when (val played = playForEvent(evt)) {
                is CtrlTry.Success -> continue
                is CtrlTry.Failure -> {
                    failure = played
                    break
                }
            }
        }

        return failure ?: CtrlTry.Success(mutable)
    }

    fun eventForCommand(
        command: CtrlCommand<TMutable>
    ): CtrlTry<CtrlEvent<TMutable>> = CtrlTry {
        return when (val result = CtrlTry(execute(command))){
            is CtrlTry.Failure -> CtrlTry.Failure(result.failureCause)
            is CtrlTry.Success -> CtrlTry.Success(result.result)
        }
    }

    fun execute(
        command: CtrlCommand<TMutable>
    ): CtrlExecution<TMutable, CtrlEvent<TMutable>, CtrlInvalidation> {
        val validation = CtrlValidation(mutableListOf())

        command.validate(mutable, validation)

        val validatedItems = validation.invalidInputItems.toTypedArray()

        return when {
            validatedItems.isNotEmpty() -> {
                CtrlExecution.Invalidated(items = validatedItems)
            }

            else -> {
                CtrlExecution.Validated(
                    event = command.makeEvent()
                )
            }
        }
    }

    fun playEventForCommand(
        command: CtrlCommand<TMutable>
    ): CtrlTry<Pair<TMutable, CtrlEvent<TMutable>>> {
        return eventForCommand(command).flatMap {
            when (val played = playForEvent(it)) {
                is CtrlTry.Failure -> CtrlTry.Failure(failureCause = played.failureCause)
                is CtrlTry.Success -> CtrlTry.Success(Pair(played.result, it))
            }
        }
    }

    fun playEventsForCommands(
        commands: List<CtrlCommand<TMutable>>
    ): CtrlTry<Pair<TMutable, List<CtrlEvent<TMutable>>>> {
        var error: CtrlTry<Pair<TMutable, List<CtrlEvent<TMutable>>>>? = null
        val evts = mutableListOf<CtrlEvent<TMutable>>()
        for (cmd in commands) {
            when (val played = playEventForCommand(cmd)) {
                is CtrlTry.Success -> {
                    evts.add(played.result.second!!)
                    continue
                }

                is CtrlTry.Failure -> {
                    error = CtrlTry.Failure(failureCause = played.failureCause)
                    break
                }
            }
        }

        return error ?: CtrlTry.Success(Pair(mutable, evts))
    }
}

