package id.singd.android.signdsdk.commands

abstract class ExecuteOnCommand(private val action: (Any) -> Unit) : ICommand {

    override  fun execute(executor: Any) { //suspend
        action(executor)
    }
}