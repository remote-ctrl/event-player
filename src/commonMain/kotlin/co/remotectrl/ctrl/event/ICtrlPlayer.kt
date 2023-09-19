package co.remotectrl.ctrl.event

import java.util.TreeMap

interface ICtrlPlayer<TMutable,TResult>
where TMutable: CtrlMutable<TMutable> {
    fun execute(
           command: CtrlCommand<TMutable>
    ) : CtrlTry<TResult>
}