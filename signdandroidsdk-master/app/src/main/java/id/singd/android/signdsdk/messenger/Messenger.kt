package id.singd.android.signdsdk.messenger

import android.util.Log
import id.singd.android.signdsdk.commands.ICommand
import id.singd.android.signdsdk.commands.IMessenger
import id.singd.android.signdsdk.core.ILogger
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import java.util.*
import kotlin.collections.HashMap

class Messenger(private val logger: ILogger) : IMessenger {

    private val synchronizer: Any = Any()
    private val TAG = this@Messenger.javaClass.simpleName
    private val executors = HashMap<String, MutableList<Any>>()
    private val queue = HashMap<String, Stack<ICommand>>()

    override fun register(commandName: String, executor: Any) {
        //  return async {
        Log.d(TAG, commandName);
        synchronized(synchronizer) {
            /*  if (executors.containsKey(commandName)) {
                    if (!executors[commandName].orEmpty().contains(executor)) {
                        executors[commandName]!!.add(executor)
                        checkForMessages(commandName, executor)
                    }
                } else {
                    executors[commandName] = mutableListOf(executor)
                    checkForMessages(commandName, executor)
                }
                logger.debug(TAG, "${executor.javaClass.simpleName} registered for $commandName")*/
            try {
                if (executors.containsKey(commandName)) {
                    if (!executors[commandName].orEmpty().contains(executor)) {
                        executors[commandName]!!.add(executor)
                        checkForMessages(commandName, executor)
                    }
                } else {
                    executors[commandName] = mutableListOf(executor)
                   checkForMessages(commandName, executor)
                }
                logger.debug(TAG, "${executor.javaClass.simpleName} registered for $commandName")
            } catch (e: IllegalStateException) {
            }

        }
   // }

    }

    override fun deregister(commandName: String, executor: Any) {
        synchronized(synchronizer) {
            if (executors.containsKey(commandName)) {
                if (executors[commandName].orEmpty().contains(executor)) {
                    executors[commandName]!!.remove(executor)
                    logger.debug(TAG, "${executor.javaClass.simpleName} deregistered for $commandName")
                    if (executors[commandName]!!.isEmpty()) {
                        executors.remove(commandName)
                    }
                }
            }
        }
    }

   /* override fun postCommand(command: ICommand): Deferred<Any> {
        return async {
            val commandType = command::class.qualifiedName.orEmpty()
            logger.debug(TAG, "posted: $commandType")
            synchronized(synchronizer) {
                when {
                    executors.containsKey(commandType) -> for (executor in executors[commandType].orEmpty()) {
                        logger.debug(TAG, "executed: $commandType on ${executor.javaClass.simpleName}")
                        command.execute(executor)
                    }
                    queue.containsKey(commandType) && queue[commandType] != null -> {
                        logger.debug(TAG, "pushed: $commandType")
                        queue[commandType]!!.push(command)
                    }
                    else -> {
                        logger.debug(TAG, "pushed: $commandType")
                        val stack = Stack<ICommand>()
                        stack.push(command)
                        queue[commandType] = stack
                    }
                }
            }
        }
    }*/

    override fun postCommand(command: ICommand) {
        //return async {
            val commandType = command::class.qualifiedName.orEmpty()
            logger.debug(TAG, "posted: $commandType")
            synchronized(synchronizer) {
                when {
                    executors.containsKey(commandType) -> for (executor in executors[commandType].orEmpty()) {
                        logger.debug(TAG, "executed: $commandType on ${executor.javaClass.simpleName}")
                        command.execute(executor)
                    }
                    queue.containsKey(commandType) && queue[commandType] != null -> {
                        logger.debug(TAG, "pushed: $commandType")
                        queue[commandType]!!.push(command)
                    }
                    else -> {
                        logger.debug(TAG, "pushed: $commandType")
                        val stack = Stack<ICommand>()
                        stack.push(command)
                        queue[commandType] = stack
                    }
                }
            }
       // }
    }

    //suspend
    private  fun checkForMessages(type: String, executor: Any) {
        if (queue.containsKey(type)) {
            for (command in queue[type].orEmpty()) {
                logger.debug(
                    TAG,
                    "executed: ${command.javaClass.simpleName} on ${executor.javaClass.simpleName}"
                )
                command.execute(executor)
            }
        }
    }
}