package de.robv.android.xposed

import android.os.Bundle

interface IXposedHookLoadPackage {
    fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam)
}

class XC_LoadPackage {
    class LoadPackageParam {
        var packageName: String = ""
        var processName: String = ""
        var classLoader: ClassLoader? = null
        var isFirstApplication: Boolean = false
    }
}

object XposedHelpers {
    fun findAndHookMethod(
        className: String,
        classLoader: ClassLoader?,
        methodName: String,
        vararg parameterTypesAndCallback: Any
    ): XC_MethodHook.Unhook {
        return XC_MethodHook.Unhook()
    }

    fun findClass(className: String, classLoader: ClassLoader?): Class<*> {
        return Any::class.java
    }

    fun getObjectField(obj: Any, fieldName: String): Any? {
        return null
    }

    fun getIntField(obj: Any, fieldName: String): Int {
        return 0
    }

    fun setIntField(obj: Any, fieldName: String, value: Int) {}
}

abstract class XC_MethodHook {
    open class MethodHookParam {
        var method: Any? = null
        var thisObject: Any? = null
        var args: Array<Any>? = null
        private var result: Any? = null
        private var exception: Throwable? = null
        var returnEarly: Boolean = false

        fun getResult(): Any? = result
        fun setResult(res: Any?) {
            result = res
            returnEarly = true
        }
        fun getThrowable(): Throwable? = exception
        fun setThrowable(ex: Throwable?) {
            exception = ex
            returnEarly = true
        }
    }

    open fun beforeHookedMethod(param: MethodHookParam) {}
    open fun afterHookedMethod(param: MethodHookParam) {}

    class Unhook
}
