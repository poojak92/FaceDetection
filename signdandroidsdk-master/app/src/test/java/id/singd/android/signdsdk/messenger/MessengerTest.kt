package id.singd.android.signdsdk.messenger

import android.app.Activity
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import id.singd.android.signdsdk.commands.ExecuteOnActivityCommand
import id.singd.android.signdsdk.core.ILogger
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Test

import org.junit.Before
import org.mockito.Mockito.times

class MessengerTest {

    private lateinit var messenger: Messenger
    private val logger = mock<ILogger>()

    @Before
    fun setUp() {
        messenger = Messenger(logger)
    }

    @Test
    fun testCanRegisterAndDeregister() {
        val executer = Any()
        messenger.register("id.signd.android.commands.testcommand", executer)
        messenger.deregister("id.signd.android.commands.testcommand", executer)
    }

    @Test
    fun testNoErrorOnDeregisterUnregistered() {
        messenger.deregister("id.signd.android.commands.testcommand", Any())
    }

    @Test
    fun testCommandStoredTillRegistration() {
        val mockActivity = mock<Activity> {
            //verify(it, times(1)).finish()
        }
        runBlocking {
            messenger.postCommand(ExecuteOnActivityCommand {
                if (it is Activity) {
                    it.finish()
                } else {
                    throw IllegalArgumentException()
                }
            }).join()
            messenger.register(
                ExecuteOnActivityCommand::class.qualifiedName.orEmpty(),
                mockActivity
            ).join()
        }
        //verify(mockActivity, times(1)).finish()
    }

    @Test
    fun testCommandExecutedOnAllRegistered() {
        val mockActivity1 = mock<Activity>()
        val mockActivity2 = mock<Activity>()
        runBlocking {
            val await1 = messenger.register(
                ExecuteOnActivityCommand::class.qualifiedName.orEmpty(),
                mockActivity1
            )
            val await2 = messenger.register(
                ExecuteOnActivityCommand::class.qualifiedName.orEmpty(),
                mockActivity2
            )
            await1.join()
            await2.join()
            val await3 = messenger.postCommand(ExecuteOnActivityCommand {
                if (it is Activity) {
                    it.finish()
                } else {
                    throw IllegalArgumentException()
                }
            }).await()
            //await3.join()
        }
        //verify(mockActivity1, times(1)).finish()
        //verify(mockActivity2, times(1)).finish()
    }

}