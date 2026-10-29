package example;

import io.kottie.api.Kottie;
import io.kottie.api.event.EventBus;
import io.kottie.api.event.Subscription;
import io.kottie.api.event.player.PlayerJoinEvent;
import io.kottie.api.lifecycle.LifecycleState;
import io.kottie.api.lifecycle.PluginLifecycle;
import kotlinx.coroutines.CoroutineScope;

/**
 * Minimal Java-facing example showing the callback-style usage.
 * Full Java interop helpers can be added later.
 */
public class JavaExamplePlugin implements PluginLifecycle {

    private final Kottie kottie;
    private LifecycleState state = LifecycleState.DISCOVERED;
    private Subscription joinSub;

    public JavaExamplePlugin(Kottie kottie) {
        this.kottie = kottie;
    }

    @Override
    public LifecycleState getState() {
        return state;
    }

    @Override
    public CoroutineScope getScope() {
        return kottie.getLifecycle().getScope();
    }

    @Override
    public Object onEnable(kotlin.coroutines.Continuation<? super kotlin.Unit> cont) {
        // Real Java plugins would use a non-suspend facade in a future release.
        state = LifecycleState.RUNNING;
        return kotlin.Unit.INSTANCE;
    }

    @Override
    public Object onDisable(kotlin.coroutines.Continuation<? super kotlin.Unit> cont) {
        if (joinSub != null) {
            joinSub.close();
        }
        state = LifecycleState.STOPPED;
        return kotlin.Unit.INSTANCE;
    }
}
