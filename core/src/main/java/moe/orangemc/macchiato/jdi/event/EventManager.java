package macchiato.jdi.event;

import com.sun.jdi.event.Event;
import macchiato.MacchiatoMain;
import macchiato.cli.CliColor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class EventManager {
    private final Map<Class<? extends Event>, Map<ListenerPriority, Map<Method, Set<Listener>>>> listeners = new HashMap<>();

    @SuppressWarnings("unchecked")
    public void registerListener(Listener listener) {
        Class<? extends Listener> listenerClass = listener.getClass();

        for (Method m : listenerClass.getMethods()) {
            if (m.getAnnotation(EventHandler.class) == null) {
                continue;
            }
            if (m.getParameterCount() != 1) {
                throw new IllegalArgumentException("Wrong arguments on method " + m + " in class " + listenerClass);
            }

            Class<?> paramClass = m.getParameters()[0].getType();
            if (!paramClass.isAssignableFrom(Event.class)) {
                throw new IllegalArgumentException("Invalid parameter " + paramClass + " in method " + m + " in class " + listenerClass);
            }

            Class<? extends Event> eventClass = (Class<? extends Event>) paramClass;
            EventHandler annotation = m.getAnnotation(EventHandler.class);

            putListener(eventClass, annotation, listener, m);
        }
    }

    private void putListener(Class<? extends Event> eventClass, EventHandler annotation, Listener listener, Method listeningMethod) {
        if (!listeners.containsKey(eventClass)) {
            listeners.put(eventClass, new HashMap<>());
        }
        Map<ListenerPriority, Map<Method, Set<Listener>>> listenerMap = listeners.get(eventClass);

        if (!listenerMap.containsKey(annotation.listenerPriority())) {
            listenerMap.put(annotation.listenerPriority(), new HashMap<>());
        }

        Map<Method, Set<Listener>> methodMapper = listenerMap.get(annotation.listenerPriority());
        if (!methodMapper.containsKey(listeningMethod)) {
            methodMapper.put(listeningMethod, new HashSet<>());
        }

        methodMapper.get(listeningMethod).add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.forEach((ec, priorityMap) -> {
            priorityMap.forEach((p, methodMap) -> {
                methodMap.forEach((m, listenerSet) -> {
                    listenerSet.remove(listener);
                });
            });
        });
    }

    void callEvent(Event event) {
        AtomicReference<Map<ListenerPriority, Map<Method, Set<Listener>>>> listenerMap = new AtomicReference<>();
        listeners.forEach((c, lm) -> {
            if (event.getClass().isAssignableFrom(c)) {
                listenerMap.set(lm);
            }
        });

        if (listenerMap.get() == null) {
            return;
        }

        for (ListenerPriority priority : ListenerPriority.values()) {
            Map<Method, Set<Listener>> methodMap = listenerMap.get().get(priority);
            methodMap.forEach((m, ll) -> {
                for (Listener listener : ll) {
                    try {
                        m.invoke(listener, event);
                    } catch (IllegalAccessException e) {
                        MacchiatoMain.getCliManager().println(CliColor.YELLOW + "Wrong access on listener: " + listener.getClass());
                        MacchiatoMain.getCliManager().printException(e, CliColor.YELLOW);
                    } catch (InvocationTargetException e) {
                        MacchiatoMain.getCliManager().println(CliColor.YELLOW + "Error while executing the event handler: " + listener.getClass());
                        MacchiatoMain.getCliManager().printException(e, CliColor.YELLOW);
                    }
                }
            });
        }
    }

    public <T extends Event> T awaitEvent(Class<T> eventClass) {
        return awaitEvent(eventClass, null);
    }

    public <T extends Event> T awaitEvent(Class<T> eventClass, Function<T, Boolean> condition) {
        Object lock = new Object();
        AtomicBoolean awake = new AtomicBoolean(false);
        AtomicReference<T> eventReference = new AtomicReference<>();

        registerListener(new Listener() {
            @SuppressWarnings("unused")
            @EventHandler
            public void onEvent(T event) {
                eventReference.set(event);
                awake.set(true);
                lock.notifyAll();
                EventManager.this.removeListener(this); // To avoid unnecessary reflections and calls.
            }
        });

        try {
            while (!awake.get() || (condition != null && !condition.apply(eventReference.get()))) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {

                }
            }
        } catch (Exception e) {
            MacchiatoMain.getCliManager().println(CliColor.YELLOW + "Macchiato has run into an error while waiting for event: " + eventClass);
            MacchiatoMain.getCliManager().printException(e, CliColor.YELLOW);
        }

        return eventReference.get();
    }
}
