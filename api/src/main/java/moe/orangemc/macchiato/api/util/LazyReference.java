package moe.orangemc.macchiato.util;

public abstract class LazyReference<T> {
    private T reference = null;
    private boolean loaded = false;

    public T get() {
        if (!loaded) {
            this.reference = load();
            loaded = true;
        }

        return this.reference;
    }

    protected abstract T load();
    public boolean isLoaded() {
        return loaded;
    }
}
