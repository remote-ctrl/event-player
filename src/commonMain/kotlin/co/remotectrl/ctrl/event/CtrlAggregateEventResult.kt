package co.remotectrl.ctrl.event

data class CtrlAggregateEventResult<TMutable : CtrlMutable<TMutable>>(
    var aggregate: CtrlAggregate<TMutable>,
    val aggregateEvent: CtrlAggregateEvent<TMutable, CtrlEvent<TMutable>>,
)