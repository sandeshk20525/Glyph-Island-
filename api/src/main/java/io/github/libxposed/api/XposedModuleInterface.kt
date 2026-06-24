package io.github.libxposed.api

interface XposedModuleInterface {
    interface ModuleLoadedParam {
        val isSystem: Boolean
        val processName: String
    }

    interface OnPackageLoadedParam {
        val packageName: String
        val classLoader: ClassLoader
        val isFirstApplication: Boolean
        val processName: String
    }

    fun onPackageLoaded(param: OnPackageLoadedParam)
}
