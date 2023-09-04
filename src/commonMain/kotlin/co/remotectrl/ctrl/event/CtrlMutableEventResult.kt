package co.remotectrl.ctrl.event

data class CtrlMutableEventResult<TMutable : CtrlMutable<TMutable>>(
    val mutable: TMutable,
    val event: CtrlEvent<TMutable>
)
