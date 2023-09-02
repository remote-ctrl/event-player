package co.remotectrl.ctrl.event

data class CtrlAggregate<TMutable : CtrlMutable<TMutable>>(
    val id: CtrlId<TMutable>,
    val latestVersion: Int,
    val mutable: TMutable
)

data class CtrlId<TMutable>(val value: String? = null) where TMutable : CtrlMutable<TMutable>

data class NotFoundCause(
    override val failMessage: String,
    val id: CtrlId<*>
) : IFailureCause {
    constructor(id: CtrlId<*>) : this(
        failMessage = "couldn't find aggregateId [${id.value}]",
        id = id
    )
}
