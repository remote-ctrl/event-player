package co.remotectrl.ctrl.event.harness

import co.remotectrl.ctrl.event.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import kotlin.reflect.KClass

abstract class ACtrlPlayerDSL<Player, TMutable, TValue, TResult>
        where Player : ICtrlPlayer<TMutable, TResult>,
              TMutable : CtrlMutable<TMutable>,
              TValue : Any? {
    abstract val player: Player
    abstract val harness: ICtrlPlayerTestHarness<TMutable, TValue, *>
    abstract var eventCount: Int
    fun CtrlCommand<TMutable>.execute() = player.execute(this)

    fun <TEvent: CtrlEvent<TMutable>> CtrlCommand<TMutable>.executeAssert(expected: TValue, event: KClass<TEvent>) {
        val possibleResult = player.execute(command = this)
        assertTrue(possibleResult is CtrlTry.Success)
        val result = possibleResult as CtrlTry.Success<TResult>
        executeAssertAdditionalChecks(expected,event, result)
    }

    abstract fun <TEvent: CtrlEvent<TMutable>> executeAssertAdditionalChecks(
            expected: TValue,
            event: KClass<TEvent>,
            result: CtrlTry.Success<TResult>
    )
}

class CtrlPlayerMutableDSL<TMutable : CtrlMutable<TMutable>, TValue : Any?>(
        override val player: CtrlMutablePlayer<TMutable>,
        override val harness: ICtrlPlayerTestHarness< TMutable, TValue, *>,
        override var eventCount: Int = 0
) : ACtrlPlayerDSL<CtrlMutablePlayer<TMutable>, TMutable, TValue, CtrlMutableEventResult<TMutable>>() {

    override fun <TEvent:CtrlEvent<TMutable>> executeAssertAdditionalChecks(expected: TValue,
                                               event: KClass<TEvent>,
                                               result: CtrlTry.Success<CtrlMutableEventResult<TMutable>>
    ) {
        assertEquals(event.java.name, result.result.event::class.java.name)
        assertMutableEventResult(
                expected,
                result.result
        ) {
            harness.getValue(result.result.mutable)
        }
    }

    fun assertMutableEventResult(
            expectedChangeVal: TValue,
            result: CtrlMutableEventResult<TMutable>,
            extract: CtrlMutableEventResult<TMutable>.() -> TValue
    ) {
        assertEquals(expectedChangeVal, result.extract())
    }
}

interface ICtrlPlayerTestHarness<
        TMutable: CtrlMutable<TMutable>,
        TValue : Any?,
        TMutableBuilderData : ICtrlPlayerTestHarnessBuilderData
        > {
    abstract fun getValue(mutable:TMutable): TValue
    abstract fun createMutable(extraData: TMutableBuilderData?): TMutable
}

interface ICtrlPlayerTestHarnessBuilderData
object CtrlPlayerTestHarnessBuilderDataNoOp :  ICtrlPlayerTestHarnessBuilderData

interface ICtrlPlayerAggregateHarness<
        TMutable: CtrlMutable<TMutable>,
        TValue:Any?,
        TMutableBuilderData : ICtrlPlayerTestHarnessBuilderData
>: ICtrlPlayerTestHarness<
        TMutable,
        TValue,

TMutableBuilderData

> {

}


interface ICtrlPlayerMutableHarness<
        TMutable : CtrlMutable<TMutable>,
        TValue : Any?,
        TMutableBuilderData : ICtrlPlayerTestHarnessBuilderData
        > : ICtrlPlayerTestHarness<TMutable, TValue, TMutableBuilderData> {



    fun withPlayer(
            expectedResult: TValue,
            mutable: TMutable = createMutable(null),
            block: CtrlPlayerMutableDSL<
                    TMutable,
                    TValue,
                    >.() -> Unit
    ) {
        val player = CtrlMutablePlayer(mutable)
        val state = CtrlPlayerMutableDSL(player, this)
        state.block()
        assertEquals(expectedResult, getValue(player.mutable))
    }

}
