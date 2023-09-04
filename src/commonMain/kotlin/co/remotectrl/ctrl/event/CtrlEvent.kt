package co.remotectrl.ctrl.event

interface CtrlEvent<TMutable : CtrlMutable<TMutable>> {
    fun applyChangesTo(mutable: TMutable): TMutable
}

data class CtrlEventId<TMutable>(val value: String? = null) where TMutable : CtrlMutable<TMutable>