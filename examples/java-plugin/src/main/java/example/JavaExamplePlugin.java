package example;

import io.kottie.api.Kottie;
import io.kottie.api.event.Subscription;
import io.kottie.api.lifecycle.LifecycleState;
import io.kottie.api.lifecycle.PluginLifecycle;
import kotlinx.coroutines.CoroutineScope;

import java.time.Duration;

/**
 * Minimal Java-facing example. Full non-suspend facade will be added later.
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
        state = LifecycleState.RUNNING;
        return kotlin.Unit.INSTANCE;
    }

    @Override
    public Object onDisable(Duration timeout, kotlin.coroutines.Continuation<? super kotlin.Unit> cont) {
        if (joinSub != null) {
            joinSub.close();
        }
        state = LifecycleState.STOPPED;
        return kotlin.Unit.INSTANCE;
    }
}
