package org.dinky.function.compiler;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Author: lwjhn
 * Date: 2024/6/20 15:29
 * Description:
 */
public class CompilerOptions {
    private static final Iterable<String> options;

    private static final String classpath;

    static {
        Set<String> paths = new LinkedHashSet<>();
        String javaClassPath = System.getProperty("java.class.path");
        if (javaClassPath != null && !javaClassPath.isEmpty()) {
            Collections.addAll(paths, javaClassPath.split(Pattern.quote(File.pathSeparator)));
        }
        // Include jars added at runtime via URLClassLoader#addURL (e.g. DinkyClassLoader).
        // Since JDK 9 the application class loader is no longer a URLClassLoader, so java.class.path
        // is the primary source and URLClassLoader chains are collected as a supplement.
        collectClasspathUrls(CompilerOptions.class.getClassLoader(), paths);
        collectClasspathUrls(Thread.currentThread().getContextClassLoader(), paths);
        paths.remove("");

        classpath = String.join(File.pathSeparator, paths);
        options = Collections.unmodifiableList(Arrays.asList("-encoding", "UTF-8", "-classpath", classpath));
    }

    private static void collectClasspathUrls(ClassLoader classLoader, Set<String> paths) {
        while (classLoader != null) {
            if (classLoader instanceof URLClassLoader) {
                for (URL url : ((URLClassLoader) classLoader).getURLs()) {
                    if (!"file".equalsIgnoreCase(url.getProtocol())) {
                        continue;
                    }
                    try {
                        paths.add(new File(url.toURI()).getAbsolutePath());
                    } catch (URISyntaxException | IllegalArgumentException e) {
                        paths.add(url.getPath());
                    }
                }
            }
            classLoader = classLoader.getParent();
        }
    }

    public static Iterable<String> getOptions() {
        return options;
    }

    public static String getClasspath() {
        return classpath;
    }
}
