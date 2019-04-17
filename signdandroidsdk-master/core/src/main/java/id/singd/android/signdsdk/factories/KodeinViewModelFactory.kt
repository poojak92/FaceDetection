package id.singd.android.signdsdk.factories

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import id.singd.android.signdsdk.commands.IMessenger
import id.singd.android.signdsdk.core.ILogger
import id.singd.android.signdsdk.core.SingletonHolder
import org.kodein.di.Kodein
import org.kodein.di.direct
import org.kodein.di.fullErasedName
import org.kodein.di.generic.instance
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.createType
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.jvm.javaType

class KodeinViewModelFactory constructor(private val container: Kodein, private val logger: ILogger) :
    ViewModelProvider.NewInstanceFactory() {


    companion object : SingletonHolder<KodeinViewModelFactory, Kodein, ILogger>({arg1: Kodein, arg2: ILogger -> KodeinViewModelFactory(arg1, arg2) }) {

    }

    /*companion object {

        val instance get() = internalInstance

        private lateinit var internalInstance: KodeinViewModelFactory

        fun init(container: Kodein, logger: ILogger) {
            internalInstance = KodeinViewModelFactory(container, logger)
        }

    }*/

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val ret = super.create(modelClass)
        ret::class.declaredMemberProperties
            .filter { property -> (property.returnType.isSubtypeOf(IMessenger::class.createType()) || property.returnType.isSubtypeOf(ILogger::class.createType()))&& property is KMutableProperty<*> }
            .forEach { property ->
                val setter = (property as KMutableProperty<*>).setter
                logger.debug(this.javaClass.simpleName, "Resolving: ${ret.javaClass.simpleName}: ${setter.parameters.joinToString(",") { it.type.javaType.fullErasedName() }}")
                when {
                    property.returnType.isSubtypeOf(IMessenger::class.createType()) -> setter.call(ret, container.direct.instance<IMessenger>())
                    property.returnType.isSubtypeOf(ILogger::class.createType()) -> setter.call(ret, container.direct.instance<ILogger>())
                }
            }
        /*for(property in ret::class.declaredMemberProperties){
            if (property.returnType.isSubtypeOf(IMessenger::class.createType()) && property is KMutableProperty<*>) {
                property.setter.call(container.direct.instance<IMessenger>())
            }
        }*/
        return ret
    }
}