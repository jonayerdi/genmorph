package ch.usi.gassert.data.state.updater;

import java.lang.reflect.Constructor;
import java.util.List;

public class StatesUpdaterFactory {

    public static IStatesUpdater load(final String statesUpdaterClass, final List<String> args) {
        try {
            Class<?> statesUpdaterClazz = Class.forName(statesUpdaterClass);
            Constructor<?> ctor = statesUpdaterClazz.getDeclaredConstructor(List.class);
            Object instance = ctor.newInstance(args);
            return (IStatesUpdater)instance;
        } catch (Exception e) {
            throw new RuntimeException("Error loading IStatesUpdater class: " + statesUpdaterClass, e);
        }
    }

}
