package github.kakarot.Tools;

import com.google.common.collect.ImmutableSet;
import org.bukkit.event.Listener;
import github.kakarot.Main;

import java.io.IOException;
import java.net.URL;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClassesRegistration {
    public void loadListeners(String packageName) {
        for (Class<?> clazz : getClassesInPackage(packageName)) {
            if (isListener(clazz)) {
                try {
                    Main.instance.getServer().getPluginManager().registerEvents((Listener) clazz.newInstance(), Main.instance);
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
        }
    }

    public void loadCommands(String packageName) {
        for (Class<?> clazz : getClassesInPackage(packageName)) {
            try {
                clazz.newInstance();
            } catch (Exception exception) {
                //exception.printStackTrace();
                System.out.println("No se pudo cargar " + clazz);
            }
        }
    }

    public boolean isListener(Class<?> clazz) {
        for (Class<?> interfaze : clazz.getInterfaces()) {
            if (interfaze == Listener.class) {
                return true;
            }
        }

        return false;
    }

    public Collection<Class<?>> getClassesInPackage(String packageName) {
        JarFile jarFile;
        Collection<Class<?>> classes = new ArrayList<>();
        CodeSource codeSource = Main.instance.getClass().getProtectionDomain().getCodeSource();
        URL resource = codeSource.getLocation();

        String relPath = packageName.replace('.', '/');
        String resPath = resource.getPath().replace("%20", " ");
        String jarPath = resPath.replaceFirst("[.]jar[!].*", ".jar").replaceFirst("file:", "");

        try {
            jarFile = new JarFile(jarPath);
        } catch (IOException e) {
            throw new IllegalStateException("Unexpected IOException reading JAR File '" + jarPath + "'", e);
        }

        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String entryName = entry.getName();
            String className = null;
            if (entryName.endsWith(".class") && entryName.startsWith(relPath) && entryName.length() > relPath.length() + "/".length()) {
                className = entryName.replace('/', '.').replace('\\', '.').replace(".class", "");
            }
            if (className != null) {
                Class<?> clazz = null;
                try {
                    clazz = Class.forName(className);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                if (clazz != null) {
                    classes.add(clazz);
                }
            }
        }

        try {
            jarFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return ImmutableSet.copyOf(classes);
    }
}