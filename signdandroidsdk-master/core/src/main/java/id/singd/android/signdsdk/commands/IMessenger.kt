package id.singd.android.signdsdk.commands

import kotlinx.coroutines.experimental.Deferred

interface IMessenger {

    fun register(commandName: String, executor: Any) //: Deferred<Unit>

    fun deregister(commandName: String, executor: Any)

    fun postCommand(command: ICommand) //: Deferred<Any>
}