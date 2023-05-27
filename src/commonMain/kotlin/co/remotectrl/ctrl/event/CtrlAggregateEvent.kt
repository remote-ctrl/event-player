package co.remotectrl.ctrl.event

data class CtrlAggregateEvent<TMutable : CtrlMutable<TMutable>>(
    val eventId: CtrlEventId<TMutable>,
    val aggregateId: CtrlId<TMutable>,
    val version: Int,
    val mutableEvent: CtrlEvent<TMutable>
)