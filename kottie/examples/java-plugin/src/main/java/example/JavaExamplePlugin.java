package example;

import io.kottie.api.Kottie;
import io.kottie.api.ecs.components.HealthComponent;
import io.kottie.api.ecs.components.ManaComponent;
import io.kottie.api.lifecycle.LifecycleState;

/**
 * Java sample (1.8) — shows component keys and runtime access.
 * Prefer Kotlin for suspend [io.kottie.api.lifecycle.PluginLifecycle] implementations.
 */
public final class JavaExamplePlugin {

    private final Kottie kottie;
    private LifecycleState state = LifecycleState.DISCOVERED;

    public JavaExamplePlugin(Kottie kottie) {
        this.kottie = kottie;
    }

    public LifecycleState getState() {
        return state;
    }

    public void markRunning() {
        state = LifecycleState.RUNNING;
    }

    public void markStopped() {
        state = LifecycleState.STOPPED;
    }

    public String manaKeyId() {
        return ManaComponent.KEY.getId();
    }

    public String healthKeyId() {
        return HealthComponent.KEY.getId();
    }

    public String describeRuntime() {
        return "eventBus=" + kottie.getEventBus().getClass().getSimpleName()
            + ", entityStore=" + kottie.getEntityStore().getClass().getSimpleName()
            + ", config=" + kottie.getConfig().getClass().getSimpleName();
    }
}
