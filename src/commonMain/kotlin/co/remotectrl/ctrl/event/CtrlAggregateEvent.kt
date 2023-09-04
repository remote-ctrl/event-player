package co.remotectrl.ctrl.event


data class CtrlAggregateEvent<
        TMutable : CtrlMutable<TMutable>,
        TEvent : CtrlEvent<TMutable>,
        >(
    val eventId: CtrlEventId<TMutable>,
    val aggregateId: CtrlId<TMutable>,
    val version: Int,
    val mutableEvent: TEvent?,
) {
    constructor () : this(
        eventId = CtrlEventId("0"),
        aggregateId = CtrlId("0"),
        version = 0,
        mutableEvent = null
    )
}