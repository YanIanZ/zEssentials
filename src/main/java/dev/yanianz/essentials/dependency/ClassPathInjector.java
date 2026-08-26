package dev.yanianz.essentials.dependency;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Queue;

/**
 * Pushes a jar into an already running classloader, so the classes of the jar
 * become usable without restarting the server.
 *
 * Modeled after the runtime library system of Intave (https://github.com/intave/intave):
 * on classic URLClassloaders the jar is added through the addURL method, on the
 * system classloader of modern JVMs the url list is mutated through sun.misc.Unsafe.
 */
public final class ClassPathInjector {

    private ClassPathInjector() {
    }

    /**
     * Adds the given jar to the classloader.
     *
     * @return true if the jar was added, false when every strategy failed.
     */
    public static boolean inject(ClassLoader classLoader, File jar) {
        URL url = toUrl(jar);
        if (url == null) return false;

        // Strategy 1: the plugin classloaders of Bukkit are URLClassloaders,
        // their addURL method is accessible through reflection
        if (classLoader instanceof URLClassLoader urlClassLoader) {
            try {
                Method addURL = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                addURL.setAccessible(true);
                addURL.invoke(urlClassLoader, url);
                return true;
            } catch (Exception ignored) {
            }
        }

        // Strategy 2: mutate the url class path of any Java 9+ BuiltinClassLoader through Unsafe
        return injectThroughUnsafe(classLoader, url);
    }

    private static boolean injectThroughUnsafe(ClassLoader classLoader, URL url) {
        try {
            Object unsafe = unsafe();
            if (unsafe == null) return false;

            Field ucpField = findField(classLoader.getClass(), "ucp");
            if (ucpField == null) return false;
            ucpField.setAccessible(true);

            Object urlClassPath = ucpField.get(classLoader);
            Field unopenedUrlsField = urlClassPath.getClass().getDeclaredField("unopenedUrls");
            unopenedUrlsField.setAccessible(true);

            Queue<URL> unopenedUrls = (Queue<URL>) unopenedUrlsField.get(urlClassPath);
            unopenedUrls.add(url);
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    /**
     * Resolves theUnsafe reflectively, this way the code compiles without access
     * to the jdk.unsupported module and keeps working while the API is deprecated.
     */
    private static Object unsafe() {
        try {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Field field = unsafeClass.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return field.get(null);
        } catch (Exception exception) {
            return null;
        }
    }

    private static Field findField(Class<?> clazz, String name) {
        for (Class<?> current = clazz; current != null; current = current.getSuperclass()) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
            }
        }
        return null;
    }

    private static URL toUrl(File jar) {
        try {
            return jar.toURI().toURL();
        } catch (Exception exception) {
            return null;
        }
    }
}
