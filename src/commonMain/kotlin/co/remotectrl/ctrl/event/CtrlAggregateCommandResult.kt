package co.remotectrl.ctrl.event

data class CtrlAggregateCommandResult<TMutable : CtrlMutable<TMutable>>(
    var aggregate: CtrlAggregate<TMutable>,
    val command: CtrlCommand<TMutable>,
)