package id.singd.android.signdsdk.commands

interface ICommand {
     fun execute(executor: Any) //suspend
}