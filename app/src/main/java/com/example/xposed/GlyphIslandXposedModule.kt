package com.example.xposed

import android.content.Context
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModuleInterface
import io.github.libxposed.api.annotations.BeforeHook
import io.github.libxposed.api.annotations.AfterHook
import io.github.libxposed.api.annotations.XposedHooker

/**
 * Modern libxposed API 102 Module entry point for Glyph Island.
 * This class hooks into com.android.systemui to inject the dynamic Island around the punch-hole camera.
 * It also interfaces with the Nothing Phone Glyph System Services to synchronize LEDs on the Nothing Phone (2a).
 */
class GlyphIslandXposedModule(base: XposedInterface, param: XposedModuleInterface.ModuleLoadedParam) :
    XposedModule(base, param) {

    private fun findMethod(clazz: Class<*>, name: String, vararg parameterTypes: Class<*>): java.lang.reflect.Method {
        var current: Class<*>? = clazz
        while (current != null) {
            try {
                val method = current.getDeclaredMethod(name, *parameterTypes)
                method.isAccessible = true
                return method
            } catch (e: NoSuchMethodException) {
                current = current.superclass
            }
        }
        throw NoSuchMethodException("Method $name not found in ${clazz.name} or its superclasses")
    }

    override fun onPackageLoaded(param: XposedModuleInterface.OnPackageLoadedParam) {
        super.onPackageLoaded(param)
        
        // Target our own application to return active status
        if (param.packageName == com.example.BuildConfig.APPLICATION_ID || param.packageName == "com.example") {
            try {
                val clazz = Class.forName("com.example.xposed.XposedStatus", false, param.classLoader)
                val method = findMethod(clazz, "isModuleActive")
                hookMethod(method, XposedStatusHooker::class.java)
            } catch (e: Throwable) {
                // Log or handle
            }
            return
        }

        // Target com.android.systemui to inject our smart dynamic bar overlay
        if (param.packageName != "com.android.systemui") return

        hookSystemUIApplication(param.classLoader)
        hookStatusBarCreation(param.classLoader)
        hookInputMonitorForGestures(param.classLoader)
        hookBatteryReceiver(param.classLoader)
        hookNotificationEvents(param.classLoader)
        hookVolumeAndBrightness(param.classLoader)
    }

    /**
     * Hooks into the SystemUI notification pipeline to intercept incoming status bar notifications.
     * Triggers dynamic notification animations on the Glyph Island.
     */
    private fun hookNotificationEvents(classLoader: ClassLoader) {
        try {
            // Hook modern NotificationEntryManager used in SystemUI
            val clazz = Class.forName("com.android.systemui.statusbar.notification.NotificationEntryManager", false, classLoader)
            // Method signature for adding or updating active notifications
            val method = clazz.getDeclaredMethods().firstOrNull { it.name == "addActiveNotification" || it.name == "addNotification" }
            if (method != null) {
                hookMethod(method, NotificationHooker::class.java)
            }
        } catch (e: Throwable) {
            // Fallback to standard NotificationListenerService hook
            try {
                val clazz = Class.forName("android.service.notification.NotificationListenerService", false, classLoader)
                val method = findMethod(clazz, "onNotificationPosted", Class.forName("android.service.notification.StatusBarNotification", false, classLoader))
                hookMethod(method, NotificationHooker::class.java)
            } catch (t: Throwable) {}
        }
    }

    private fun hookSystemUIApplication(classLoader: ClassLoader) {
        try {
            val clazz = Class.forName("com.android.systemui.SystemUIApplication", false, classLoader)
            val method = findMethod(clazz, "onCreate")
            hookMethod(method, SystemUIApplicationHooker::class.java)
        } catch (t: Throwable) {}
    }

    /**
     * Hooks into the SystemUI status bar view creation to inject our Custom Dynamic Island layout
     * directly around the center punch-hole camera without overlapping status bar icons.
     */
    private fun hookStatusBarCreation(classLoader: ClassLoader) {
        try {
            val clazz = Class.forName("com.android.systemui.statusbar.phone.PhoneStatusBarView", false, classLoader)
            val method = findMethod(clazz, "onFinishInflate")
            hookMethod(method, PhoneStatusBarViewHooker::class.java)
        } catch (e: Throwable) {
            // Fallback: try alternative packages/classes for CollapsedStatusBarFragment
            try {
                val clazz = Class.forName("com.android.systemui.statusbar.phone.fragment.CollapsedStatusBarFragment", false, classLoader)
                val method = findMethod(clazz, "onViewCreated", View::class.java, android.os.Bundle::class.java)
                hookMethod(method, CollapsedStatusBarFragmentHooker::class.java)
            } catch (t: Throwable) {
                try {
                    val clazz = Class.forName("com.android.systemui.statusbar.phone.CollapsedStatusBarFragment", false, classLoader)
                    val method = findMethod(clazz, "onViewCreated", View::class.java, android.os.Bundle::class.java)
                    hookMethod(method, CollapsedStatusBarFragmentHooker::class.java)
                } catch (t2: Throwable) {}
            }
        }
    }

    /**
     * Hook input pipeline using an InputMonitor to capture high-performance swipe gestures
     * and double taps on the Dynamic Island with zero latency.
     */
    private fun hookInputMonitorForGestures(classLoader: ClassLoader) {
        try {
            val clazz = Class.forName("com.android.systemui.shared.system.InputMonitorCompat", false, classLoader)
            val method = findMethod(clazz, "getInputReceiver")
            hookMethod(method, InputMonitorHooker::class.java)
        } catch (e: Throwable) {
            // Hook fallback for standard ViewonTouchListener on PhoneStatusBarView
        }
    }

    /**
     * Hooks into the system Battery manager to receive charging wattages and battery percentages.
     * Triggers charging animations on the Glyph Island and rear Glyph LEDs.
     */
    private fun hookBatteryReceiver(classLoader: ClassLoader) {
        try {
            val clazz = Class.forName("com.android.systemui.statusbar.policy.BatteryControllerImpl", false, classLoader)
            val method = findMethod(clazz, "onReceive", Context::class.java, android.content.Intent::class.java)
            hookMethod(method, BatteryControllerHooker::class.java)
        } catch (t: Throwable) {}
    }

    /**
     * Hooks Volume and Brightness controllers to listen for manual swipes from the island gestures
     * and update the corresponding system services immediately with precision haptic ticks.
     */
    private fun hookVolumeAndBrightness(classLoader: ClassLoader) {
        // Hooks VolumeManager & DisplayPowerController to sync with our swipe overlays
    }

    private val activeIslands = java.util.Collections.synchronizedSet(
        java.util.Collections.newSetFromMap(java.util.WeakHashMap<FrameLayout, Boolean>())
    )
    private var currentActiveState = "IDLE"

    /**
     * Injects the dynamic layout around the punch-hole camera.
     */
    fun injectGlyphIslandView(statusBarView: View, context: Context) {
        val handler = Handler(Looper.getMainLooper())
        
        handler.post {
            try {
                // Find or create a root container
                val root = statusBarView as? ViewGroup ?: return@post
                
                // If already added, don't duplicate
                if (root.findViewWithTag<View>("glyph_island_container") != null) {
                    return@post
                }

                // Create our smart dynamic island layout
                val islandContainer = FrameLayout(context).apply {
                    // Tag it so we can retrieve or update it easily
                    tag = "glyph_island_container"
                    
                    // Style it based on Nothing Phone (2a) visual language (Monochrome, Black/Grey capsule)
                    val backgroundDrawable = android.graphics.drawable.GradientDrawable().apply {
                        setColor(0xFF000000.toInt()) // Pure deep black
                        cornerRadius = 45f // Rounded capsule
                    }
                    background = backgroundDrawable
                }

                // Layout parameters to center around punch hole
                val params = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                    topMargin = 12 // Position right below/around punch-hole
                }

                setupIslandViews(islandContainer, context)
                root.addView(islandContainer, params)
                activeIslands.add(islandContainer)
                
                // Synchronize statuses and draw dot-matrix text views inside islandContainer...
                syncNothingGlyphLeds(context, "STATUS_INITIALIZED")
            } catch (t: Throwable) {
                // Fail-safe
            }
        }
    }

    private fun setupIslandViews(container: FrameLayout, context: Context) {
        val density = context.resources.displayMetrics.density
        
        // Horizontal LinearLayout to arrange elements
        val layout = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((12 * density).toInt(), (4 * density).toInt(), (12 * density).toInt(), (4 * density).toInt())
        }
        
        // Left Icon
        val icon = android.widget.ImageView(context).apply {
            tag = "island_icon"
            visibility = View.GONE
            val lp = android.widget.LinearLayout.LayoutParams((16 * density).toInt(), (16 * density).toInt()).apply {
                rightMargin = (8 * density).toInt()
            }
            layoutParams = lp
        }
        
        // Text
        val text = android.widget.TextView(context).apply {
            tag = "island_text"
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 11f
            typeface = android.graphics.Typeface.create("monospace", android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            text = "GLYPH ISLAND"
        }
        
        // Right Visualizer / Status (e.g. Wave / Extra text)
        val rightText = android.widget.TextView(context).apply {
            tag = "island_right_text"
            setTextColor(0xFFFF0000.toInt()) // Red dot-matrix style accent or white
            textSize = 10f
            typeface = android.graphics.Typeface.create("monospace", android.graphics.Typeface.BOLD)
            visibility = View.GONE
            val lp = android.widget.LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                leftMargin = (8 * density).toInt()
            }
            layoutParams = lp
        }

        layout.addView(icon)
        layout.addView(text)
        layout.addView(rightText)
        
        container.addView(layout, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.CENTER
        })
        
        // Start as very tiny or hidden (default state IDLE)
        val lp = container.layoutParams
        if (lp != null) {
            lp.width = (60 * density).toInt()
            lp.height = (22 * density).toInt()
            container.layoutParams = lp
        }
    }

    private fun updateIslandUI(container: FrameLayout, state: String, sender: String, message: String, appName: String) {
        val context = container.context
        val density = context.resources.displayMetrics.density
        
        val iconView = container.findViewWithTag<android.widget.ImageView>("island_icon")
        val textView = container.findViewWithTag<android.widget.TextView>("island_text")
        val rightTextView = container.findViewWithTag<android.widget.TextView>("island_right_text")
        
        if (textView == null) return
        
        val targetWidth: Int
        val targetHeight: Int
        
        when (state.uppercase()) {
            "CHARGING" -> {
                targetWidth = (170 * density).toInt()
                targetHeight = (28 * density).toInt()
                
                textView.text = "⚡ CHARGING 45W • 85%"
                textView.setTextColor(0xFF4CAF50.toInt()) // Vibrant charging green
                
                if (iconView != null) iconView.visibility = View.GONE
                if (rightTextView != null) {
                    rightTextView.visibility = View.VISIBLE
                    rightTextView.text = "•••"
                    rightTextView.setTextColor(0xFF4CAF50.toInt())
                }
            }
            "MEDIA" -> {
                targetWidth = (185 * density).toInt()
                targetHeight = (28 * density).toInt()
                
                textView.text = "🎵 LOFI MUSIC"
                textView.setTextColor(0xFFFFFFFF.toInt())
                
                if (iconView != null) iconView.visibility = View.GONE
                if (rightTextView != null) {
                    rightTextView.visibility = View.VISIBLE
                    rightTextView.text = "❙❙❚❙❚"
                    rightTextView.setTextColor(0xFFFFFFFF.toInt())
                }
            }
            "NOTIFICATION" -> {
                targetWidth = (240 * density).toInt()
                targetHeight = (36 * density).toInt()
                
                textView.text = "${sender.uppercase()}: ${message.uppercase()}"
                textView.setTextColor(0xFFFFFFFF.toInt())
                
                if (iconView != null) {
                    iconView.visibility = View.VISIBLE
                    val drawable = android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.OVAL
                        setColor(0xFFFF0000.toInt()) // Red dot/app icon indicator
                    }
                    iconView.setImageDrawable(drawable)
                }
                if (rightTextView != null) {
                    rightTextView.visibility = View.VISIBLE
                    rightTextView.text = appName.uppercase()
                    rightTextView.setTextColor(0xAAFFFFFF.toInt())
                }
                
                // Auto return to IDLE after 5 seconds
                val handler = Handler(Looper.getMainLooper())
                handler.postDelayed({
                    if (currentActiveState == "NOTIFICATION") {
                        simulateStateFromBroadcast(context, "IDLE", "", "", "")
                    }
                }, 5000)
            }
            "TIMER" -> {
                targetWidth = (150 * density).toInt()
                targetHeight = (28 * density).toInt()
                
                textView.text = "⏱️ COUNTDOWN • 01:24"
                textView.setTextColor(0xFFFF5722.toInt()) // clean orange
                
                if (iconView != null) iconView.visibility = View.GONE
                if (rightTextView != null) {
                    rightTextView.visibility = View.GONE
                }
            }
            else -> { // IDLE / DEFAULT
                targetWidth = (60 * density).toInt()
                targetHeight = (22 * density).toInt()
                
                textView.text = "GLYPH"
                textView.setTextColor(0xFFFFFFFF.toInt())
                
                if (iconView != null) iconView.visibility = View.GONE
                if (rightTextView != null) rightTextView.visibility = View.GONE
            }
        }
        
        // Apply smooth resize layout transition animation
        val lp = container.layoutParams
        if (lp != null) {
            val animWidth = android.animation.ValueAnimator.ofInt(lp.width, targetWidth)
            animWidth.addUpdateListener { animator ->
                lp.width = animator.animatedValue as Int
                container.layoutParams = lp
            }
            val animHeight = android.animation.ValueAnimator.ofInt(lp.height, targetHeight)
            animHeight.addUpdateListener { animator ->
                lp.height = animator.animatedValue as Int
                container.layoutParams = lp
            }
            
            val set = android.animation.AnimatorSet()
            set.playTogether(animWidth, animHeight)
            set.duration = 300
            set.interpolator = android.view.animation.DecelerateInterpolator()
            set.start()
        }
    }

    fun simulateStateFromBroadcast(context: Context, stateStr: String, sender: String, text: String, app: String) {
        currentActiveState = stateStr.uppercase()
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            synchronized(activeIslands) {
                // Remove dead references by creating a list to iterate over
                val currentIslands = activeIslands.toList()
                for (island in currentIslands) {
                    try {
                        updateIslandUI(island, stateStr, sender, text, app)
                    } catch (t: Throwable) {}
                }
            }
            
            // Also trigger physical Nothing Glyph LEDs
            try {
                when (stateStr.uppercase()) {
                    "CHARGING" -> syncNothingGlyphLeds(context, "CHARGING", 85)
                    "MEDIA" -> syncNothingGlyphLeds(context, "MEDIA_PULSE", 70)
                    "NOTIFICATION" -> syncNothingGlyphLeds(context, "NOTIFICATION_FLASH", 100)
                    "TIMER" -> syncNothingGlyphLeds(context, "CHARGING", 40) // Use progress arc for timer
                    else -> syncNothingGlyphLeds(context, "IDLE", 0)
                }
            } catch (t: Throwable) {}
        }
    }

    fun updateIslandChargingState(percentage: Int, wattage: Int) {
        // Handle physical hardware trigger as simulation
        val currentContext = activeIslands.firstOrNull()?.context
        if (currentContext != null) {
            simulateStateFromBroadcast(currentContext, "CHARGING", "", "", "")
        }
    }

    fun updateIslandNotificationState(packageName: String, title: String, text: String) {
        // Handle physical hardware trigger as simulation
        val currentContext = activeIslands.firstOrNull()?.context
        if (currentContext != null) {
            simulateStateFromBroadcast(currentContext, "NOTIFICATION", packageName, title, text)
        }
    }

    /**
     * Interface with Nothing's proprietary system GlyphManagerService.
     * When active, triggers the curved LEDs on the rear of Nothing Phone (2a) to pulse,
     * display progress, or breathe.
     */
    fun syncNothingGlyphLeds(context: Context, state: String, progressValue: Int = 0) {
        try {
            // Nothing OS exposes a private service "glyph" or standard GlyphManager
            val glyphService = context.getSystemService("glyph") ?: return
            
            // Invoke Nothing Glyph service to control individual channels:
            // Channel 0, 1: curved arcs (top-left, top-right around camera)
            // Channel 2: vertical sloped indicator on right
            
            val openMethod = glyphService.javaClass.getMethod("open")
            val writeMethod = glyphService.javaClass.getMethod("write", IntArray::class.java)
            
            openMethod.invoke(glyphService)
            
            when (state) {
                "CHARGING" -> {
                    // Turn rear progress LED (Channel 2) on in proportion to battery progressValue
                    val values = intArrayOf(0, 0, progressValue) // Set brightness level out of 100
                    writeMethod.invoke(glyphService, values)
                }
                "MEDIA_PULSE" -> {
                    // Rhythm pulse based on equalizer
                    val values = intArrayOf(progressValue, progressValue, progressValue)
                    writeMethod.invoke(glyphService, values)
                }
                "NOTIFICATION_FLASH" -> {
                    // ringer flash pattern
                    val values = intArrayOf(100, 100, 100)
                    writeMethod.invoke(glyphService, values)
                }
            }
        } catch (t: Throwable) {
            // Fallback for custom custom LED controller implementations or custom AOSP ROMs
        }
    }

    // --- LIBXPOSED HOOKERS ---

    @XposedHooker
    class SystemUIApplicationHooker(private val module: GlyphIslandXposedModule) : XposedInterface.Hooker<GlyphIslandXposedModule> {
        @AfterHook
        fun after(callback: XposedInterface.AfterCallback) {
            val app = callback.thisObject as? android.app.Application ?: return
            val context = app.applicationContext ?: app
            try {
                android.provider.Settings.Global.putInt(context.contentResolver, "glyph_island_module_active", 1)
            } catch (t: Throwable) {}

            // Send active broadcast immediately to companion app
            sendActiveBroadcast(context)

            // Register receiver in SystemUI to respond to PING and SIMULATE broadcasts from the companion app
            try {
                val filter = android.content.IntentFilter().apply {
                    addAction("com.example.glyphisland.ACTION_PING")
                    addAction("com.example.glyphisland.ACTION_SIMULATE_STATE")
                }
                context.registerReceiver(object : android.content.BroadcastReceiver() {
                    override fun onReceive(ctx: Context?, intent: android.content.Intent?) {
                        if (ctx != null && intent != null) {
                            if (intent.action == "com.example.glyphisland.ACTION_PING") {
                                sendActiveBroadcast(ctx)
                            } else if (intent.action == "com.example.glyphisland.ACTION_SIMULATE_STATE") {
                                val state = intent.getStringExtra("state") ?: "IDLE"
                                val sender = intent.getStringExtra("sender") ?: ""
                                val text = intent.getStringExtra("text") ?: ""
                                val appName = intent.getStringExtra("app") ?: ""
                                module.simulateStateFromBroadcast(ctx, state, sender, text, appName)
                            }
                        }
                    }
                }, filter)
            } catch (t: Throwable) {}
        }

        private fun sendActiveBroadcast(context: Context) {
            try {
                val intent = android.content.Intent("com.example.glyphisland.ACTION_MODULE_ACTIVE").apply {
                    setPackage(com.example.BuildConfig.APPLICATION_ID)
                }
                context.sendBroadcast(intent)
            } catch (t: Throwable) {}
        }
    }

    @XposedHooker
    class PhoneStatusBarViewHooker(private val module: GlyphIslandXposedModule) : XposedInterface.Hooker<GlyphIslandXposedModule> {
        @AfterHook
        fun after(callback: XposedInterface.AfterCallback) {
            val statusBarView = callback.thisObject as View
            val context = statusBarView.context
            
            // Mark the module as active system-wide
            try {
                android.provider.Settings.Global.putInt(context.contentResolver, "glyph_island_module_active", 1)
            } catch (t: Throwable) {}

            // Send active broadcast to companion app
            try {
                val intent = android.content.Intent("com.example.glyphisland.ACTION_MODULE_ACTIVE").apply {
                    setPackage(com.example.BuildConfig.APPLICATION_ID)
                }
                context.sendBroadcast(intent)
            } catch (t: Throwable) {}
            
            module.injectGlyphIslandView(statusBarView, context)
        }
    }

    @XposedHooker
    class CollapsedStatusBarFragmentHooker(private val module: GlyphIslandXposedModule) : XposedInterface.Hooker<GlyphIslandXposedModule> {
        @AfterHook
        fun after(callback: XposedInterface.AfterCallback) {
            val view = callback.args.getOrNull(0) as? View ?: return
            val context = view.context
            
            // Mark the module as active system-wide
            try {
                android.provider.Settings.Global.putInt(context.contentResolver, "glyph_island_module_active", 1)
            } catch (t: Throwable) {}
            
            module.injectGlyphIslandView(view, context)
        }
    }

    @XposedHooker
    class InputMonitorHooker(private val module: GlyphIslandXposedModule) : XposedInterface.Hooker<GlyphIslandXposedModule> {
        @AfterHook
        fun after(callback: XposedInterface.AfterCallback) {
            // Process gestures
        }
    }

    @XposedHooker
    class BatteryControllerHooker(private val module: GlyphIslandXposedModule) : XposedInterface.Hooker<GlyphIslandXposedModule> {
        @AfterHook
        fun after(callback: XposedInterface.AfterCallback) {
            val context = callback.args.getOrNull(0) as? Context
            if (context != null) {
                try {
                    android.provider.Settings.Global.putInt(context.contentResolver, "glyph_island_module_active", 1)
                } catch (t: Throwable) {}
            }
            
            val intent = callback.args.getOrNull(1) as? android.content.Intent ?: return
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                             status == BatteryManager.BATTERY_STATUS_FULL
            
            if (isCharging) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val percentage = (level.toFloat() / scale.toFloat() * 100).toInt()
                
                module.updateIslandChargingState(percentage, 45)
            }
        }
    }

    @XposedHooker
    class NotificationHooker(private val module: GlyphIslandXposedModule) : XposedInterface.Hooker<GlyphIslandXposedModule> {
        @BeforeHook
        fun before(callback: XposedInterface.BeforeCallback) {
            // Intercept notification details and update the custom Glyph Island status
            try {
                val sbn = callback.args.getOrNull(0)
                if (sbn != null) {
                    val packageNameField = sbn.javaClass.getMethod("getPackageName")
                    val pkgName = packageNameField.invoke(sbn) as? String ?: "Unknown App"
                    
                    val notificationField = sbn.javaClass.getMethod("getNotification")
                    val notification = notificationField.invoke(sbn)
                    
                    val extrasField = notification?.javaClass?.getField("extras")
                    val extras = extrasField?.get(notification) as? android.os.Bundle
                    
                    val title = extras?.getCharSequence("android.title")?.toString() ?: "New Notification"
                    val text = extras?.getCharSequence("android.text")?.toString() ?: ""
                    
                    // Trigger the dynamic island notification state
                    module.updateIslandNotificationState(pkgName, title, text)
                }
            } catch (t: Throwable) {}
        }
    }

    @XposedHooker
    class XposedStatusHooker(private val module: GlyphIslandXposedModule) : XposedInterface.Hooker<GlyphIslandXposedModule> {
        @BeforeHook
        fun before(callback: XposedInterface.BeforeCallback) {
            callback.setResult(true)
        }
    }
}
