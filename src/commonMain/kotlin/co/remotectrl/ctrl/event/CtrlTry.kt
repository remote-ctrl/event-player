package co.remotectrl.ctrl.event

sealed class CtrlTry<T> {

    companion object {
        inline operator fun <TMutable : CtrlMutable<TMutable>> invoke(
            execution: CtrlExecution<TMutable, CtrlEvent<TMutable>, CtrlInvalidation>
        ): CtrlTry<CtrlEvent<TMutable>> {
            return when (execution) {
                is CtrlExecution.Validated -> {
                    Success(execution.event)
                }

                is CtrlExecution.Invalidated -> {
                    Failure(
                        InvalidCommandCause(invalidInputs = execution.items)
                    )
                }
            }
        }

        inline operator fun <T> invoke(func: () -> T): CtrlTry<T> =
            try {
                Success(func())
            } catch (exception: Exception) {
                Failure(failureCause = ThrowableCause(exception))
            } catch (error: Error) {
                Failure(failureCause = ThrowableCause(error))
            }
    }

    abstract fun <R> map(transform: (T) -> R): CtrlTry<R>
    abstract fun <R> flatMap(func: (T) -> CtrlTry<R>): CtrlTry<R>

    data class Success<T>(val result: T) : CtrlTry<T>() {
        override inline fun <R> map(transform: (T) -> R): CtrlTry<R> = CtrlTry { transform(result) }
        override inline fun <R> flatMap(func: (T) -> CtrlTry<R>): CtrlTry<R> = CtrlTry {
            func(result)
        }.let {
            when (it) {
                is Success -> it.result
                is Failure -> it as CtrlTry<R>
            }
        }
    }

    data class Failure<T>(val failureCause: IFailureCause) : CtrlTry<T>() {
        override inline fun <R> map(transform: (T) -> R): CtrlTry<R> = this as CtrlTry<R>
        override inline fun <R> flatMap(func: (T) -> CtrlTry<R>): CtrlTry<R> = this as CtrlTry<R>

    }
}

interface IFailureCause {
    val failMessage: String
}

data class InvalidCommandCause(
    override val failMessage: String,
    val invalidInputs: Array<CtrlInvalidInput>
) : IFailureCause {
    constructor(invalidInputs: Array<CtrlInvalidInput>) : this(
        failMessage = "validation failed:" + invalidInputs.map {
            "\n- ${it.description}"
        },
        invalidInputs = invalidInputs
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as InvalidCommandCause

        if (failMessage != other.failMessage) return false
        if (!invalidInputs.contentEquals(other.invalidInputs)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = failMessage.hashCode()
        result = 31 * result + invalidInputs.contentHashCode()
        return result
    }
}

data class ThrowableCause(override val failMessage: String, val throwable: Throwable) : IFailureCause {
    constructor(throwable: Throwable) : this(
        failMessage = throwable.message ?: "unknown",
        throwable
    )
}