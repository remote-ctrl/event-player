package co.remotectrl.ctrl.event.stub

import co.remotectrl.ctrl.event.*

class StubChangedEvent : CtrlEvent<StubMutable> {
    override fun applyChangesTo(
        mutable: StubMutable
    ): StubMutable = mutable.copy(
        changeVal = mutable.changeVal + 1
    )
}

var cmdAttemptCountIterator = 0

class StubCountIteratorCommand : StubChangeCommand() {
    override fun validate(aggregate: StubMutable, validation: CtrlValidation) {
        if (cmdAttemptCountIterator > StubMutable.STUB_MAX_VAL) {
            throw Exception("tried to continue parsing commands after initial failure")
        }
        super.validate(aggregate, validation)
        cmdAttemptCountIterator++
    }
}

open class StubChangeCommand : CtrlCommand<StubMutable> {
    override fun makeEvent(): CtrlEvent<StubMutable> {
        return StubChangedEvent()
    }

    override fun validate(aggregate: StubMutable, validation: CtrlValidation) {
        validation.assert(
            { aggregate.changeVal < StubMutable.STUB_MAX_VAL },
            "stub value cannot be more than 3 for failure scenario"
        )
    }
}

data class StubMutable(
    val changeVal: Int
) : CtrlMutable<StubMutable> {
    companion object {
        const val STUB_MAX_VAL = 2
    }
}
