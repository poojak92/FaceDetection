package id.singd.android.signdsdk.commands

import id.singd.android.signdsdk.core.ISigndCallback

class ExecuteOnSigndCallbackCommand(action: (Any) -> Unit) : ExecuteOnCommand(action) {
}