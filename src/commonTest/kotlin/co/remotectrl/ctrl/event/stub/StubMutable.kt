package co.remotectrl.ctrl.event.stub

import co.remotectrl.ctrl.event.*

class StubChangedEvent : CtrlEvent<StubMutable> {
    override fun applyChangesTo(
        mutable: StubMutable
    ): StubMutable = mutable.copy(
        changeVal = mutable.changeVal + 1
    )
}

data class StubBadDivideEvent(val divideBadVal: Int = 0) : CtrlEvent<StubMutable> {
    override fun applyChangesTo(mutable: StubMutable): StubMutable {
        return mutable.copy(
            changeVal = 1 / divideBadVal
        )
    }
}

class StubCountIteratorCommand : StubChangeCommand() {
    override fun validate(mutable: StubMutable, validation: CtrlValidation) {
        if (mutable.cmdAttemptCountIterator > StubMutable.STUB_MAX_VAL) {
            throw Exception("tried to continue parsing commands after initial failure")
        }
        super.validate(mutable, validation)
        mutable.cmdAttemptCountIterator++
    }
}

open class StubChangeCommand : CtrlCommand<StubMutable> {
    override fun makeEvent(): CtrlEvent<StubMutable> {
        return StubChangedEvent()
    }

    override fun validate(mutable: StubMutable, validation: CtrlValidation) {
        validation.assert(
            { mutable.changeVal < StubMutable.STUB_MAX_VAL },
            "stub value cannot be more than 3 for failure scenario"
        )
    }
}

class StubBadDivideCommand : CtrlCommand<StubMutable> {
    override fun makeEvent(): CtrlEvent<StubMutable> {
        return StubBadDivideEvent()
    }

    override fun validate(mutable: StubMutable, validation: CtrlValidation) {
        //no validation
    }
}

data class StubMutable(
    val changeVal: Int,
    var cmdAttemptCountIterator: Int = 0
) : CtrlMutable<StubMutable> {
    companion object {
        const val STUB_MAX_VAL = 2
    }
}
