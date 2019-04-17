package id.singd.android.signdsdk.messenger

import id.singd.android.signdsdk.commands.ExecuteOnCommand

class ExecuteOnInitActivityCommand(action: (Any) -> Unit) : ExecuteOnCommand(action)