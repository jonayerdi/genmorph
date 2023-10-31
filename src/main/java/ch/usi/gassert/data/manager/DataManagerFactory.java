package ch.usi.gassert.data.manager;

import ch.usi.gassert.data.state.updater.IStatesUpdater;
import ch.usi.gassert.data.state.updater.NullStatesUpdater;

import java.lang.reflect.Constructor;

public class DataManagerFactory {

    public static IDataManager load(final String managerClass, final DataManagerArgs args) {
        return load(managerClass, args, NullStatesUpdater.INSTANCE);
    }

    public static IDataManager load(final String managerClass, final DataManagerArgs args, final IStatesUpdater statesUpdater) {
        try {
            Class<?> managerClazz = Class.forName(managerClass);
            Constructor<?> ctor = managerClazz.getDeclaredConstructor(DataManagerArgs.class, IStatesUpdater.class);
            Object instance = ctor.newInstance(args, statesUpdater);
            return (IDataManager)instance;
        } catch (Exception e) {
            throw new RuntimeException("Error loading IDataManager class: " + managerClass, e);
        }
    }

}
