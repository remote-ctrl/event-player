package co.remotectrl.ctrl.event

interface CtrlCommand<TMutable : CtrlMutable<TMutable>> {

    fun makeEvent(): CtrlEvent<TMutable>

    fun validate(aggregate: TMutable, validation: CtrlValidation)
}

@Suppress("FINAL_UPPER_BOUND")
sealed class CtrlExecution<
        TMutable : CtrlMutable<TMutable>,
        out TEvent : CtrlEvent<TMutable>,
        out TInvalid : CtrlInvalidation
        > {
    data class Validated<TMutable : CtrlMutable<TMutable>, out TEvent : CtrlEvent<TMutable>>(
        val event: CtrlEvent<TMutable>
    ) : CtrlExecution<TMutable, TEvent, Nothing>()

    data class Invalidated<TMutable : CtrlMutable<TMutable>>(
        val items: Array<CtrlInvalidInput>
    ) : CtrlExecution<TMutable, Nothing, CtrlInvalidation>() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Invalidated<*>

            if (!items.contentEquals(other.items)) return false

            return true
        }

        override fun hashCode(): Int {
            return items.contentHashCode()
        }
    }
}

data class CtrlInvalidation(val items: Array<CtrlInvalidInput>) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as CtrlInvalidation

        if (!items.contentEquals(other.items)) return false

        return true
    }

    override fun hashCode(): Int {
        return items.contentHashCode()
    }
}

data class CtrlValidation(internal val invalidInputItems: MutableList<CtrlInvalidInput>) {
    fun assert(that: () -> Boolean, description: String) {
        when {
            !that() -> invalidInputItems.add(CtrlInvalidInput(description = description))
        }
    }
}

data class CtrlInvalidInput(val description: String)
