package beast.pkgmgmt;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Facade for loading BEAST classes and managing services.
 *
 * <p>In BEAST 2.6–2.7 this was a {@code URLClassLoader} subclass that
 * dynamically added JARs. Starting with BEAST 3 / JPMS migration it is a
 * thin delegation layer that uses the boot {@link ModuleLayer}, optional
 * plugin {@code ModuleLayer}s (for external packages), and the context
 * class-loader.
 *
 * <p>All public API methods ({@link #forName}, {@link #loadService},
 * {@link #initServices}, etc.) are preserved so that existing BEAST code
 * and external packages compile without change.
 */
public class BEASTClassLoader {

    // ---- singleton ----

    /** Singleton instance retained for API compatibility. */
    static final public BEASTClassLoader classLoader = new BEASTClassLoader();

    // ---- internal state ----

    /** Maps service type name → set of provider class names. */
    static private Map<String, Set<String>> services = new HashMap<>();

    /** Maps provider class name → class-loader that can load it. */
    static private Map<String, ClassLoader> class2loaderMap = new HashMap<>();

    /** Plugin {@link ModuleLayer}s created for external BEAST packages. */
    static private List<ModuleLayer> pluginLayers = new ArrayList<>();

    /** Known namespaces (package prefixes) of registered providers. */
    static private Set<String> namespaces = new HashSet<>();

    private BEASTClassLoader() { }

    // ------------------------------------------------------------------
    //  Class loading
    // ------------------------------------------------------------------

    /**
     * The BEAST package alternative for {@code Class.forName()}.
     * Tries, in order:
     * <ol>
     *   <li>the class-to-loader map (populated from version.xml / addService)</li>
     *   <li>each plugin {@link ModuleLayer}'s class-loaders</li>
     *   <li>the thread context class-loader / system class-loader</li>
     * </ol>
     */
    public static Class<?> forName(String className) throws ClassNotFoundException {
        // 1. Explicit class→loader mapping
        if (class2loaderMap.containsKey(className)) {
            ClassLoader loader = class2loaderMap.get(className);
            return Class.forName(className, false, loader);
        }

        // 2. Plugin layers (external BEAST packages)
        for (ModuleLayer layer : pluginLayers) {
            for (Module module : layer.modules()) {
                try {
                    return Class.forName(className, false, module.getClassLoader());
                } catch (ClassNotFoundException | NoClassDefFoundError e) {
                    // try next module
                }
            }
        }

        // 3. Context / system class-loader (covers boot layer)
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) cl = ClassLoader.getSystemClassLoader();
        try {
            return Class.forName(className, false, cl);
        } catch (NoClassDefFoundError e) {
            throw new ClassNotFoundException(e.getMessage());
        }
    }

    /**
     * Load class {@code className} that was registered as a provider of
     * {@code service}.
     */
    public static Class<?> forName(String className, String service) throws ClassNotFoundException {
        if (!services.containsKey(service)) {
            if (services.isEmpty()) {
                services.put(service, new HashSet<>());
                initServices();
                return forName(className, service);
            } else {
                throw new IllegalArgumentException(
                    "Could not find service " + service +
                    " while trying to forName class " + className);
            }
        }
        if (!services.get(service).contains(className)) {
            throw new ClassNotFoundException(
                "Could not find class " + className + " as service " + service + "\n"
                + "Perhaps the package is missing or the package is not correctly configured by the developer "
                + (className.equals("beast.base.core.BEASTInterface")
                    ? "\nor there is an element without spec attribute\n" : "")
                + "(Developer: check by running beastfx.app.tools.PackageHealthChecker on the package)");
        }
        return forName(className);
    }

    // ------------------------------------------------------------------
    //  Service registry
    // ------------------------------------------------------------------

    /**
     * Return set of provider class names for the given service.
     */
    public static Set<String> loadService(Class<?> service) {
        Set<String> providers = services.get(service.getName());
        if (providers == null) {
            if (services.isEmpty()) {
                initServices();
            } else {
                services.put(service.getName(), new HashSet<>());
            }
            providers = services.get(service.getName());
        }
        return providers;
    }

    /** Initialise services by scanning the classpath for version.xml files. */
    public static void initServices() {
        String classPath = System.getProperty("java.class.path");
        try {
            classPath = URLDecoder.decode(classPath, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // ignore
        }
        initServices("/" + classPath + "/");
    }

    /** Initialise services from a given classpath string. */
    public static void initServices(String classPath) {
        for (String jarFileName : classPath.substring(1, classPath.length() - 1).split(File.pathSeparator)) {
            File jarFile = new File(jarFileName);
            try {
                String parentDir = jarFile.isDirectory()
                    ? (jarFile.getParentFile() == null ? File.pathSeparator : jarFile.getParentFile().getPath())
                    : (jarFile.getParentFile() == null || jarFile.getParentFile().getParentFile() == null
                        ? File.pathSeparator
                        : jarFile.getParentFile().getParentFile().getPath());
                if (new File(parentDir + File.separator + "version.xml").exists()) {
                    addServices(parentDir + File.separator + "version.xml");
                }
                if (new File(parentDir + File.separator + "beast.base.version.xml").exists()) {
                    addServices(parentDir + File.separator + "beast.base.version.xml");
                } else if (new File(parentDir + File.separator + "beast.base" + File.separator + "version.xml").exists()) {
                    addServices(parentDir + File.separator + "beast.base" + File.separator + "version.xml");
                }
                if (new File(parentDir + File.separator + "beast.app.version.xml").exists()) {
                    addServices(parentDir + File.separator + "beast.app.version.xml");
                } else if (new File(parentDir + File.separator + "beast.app" + File.separator + "version.xml").exists()) {
                    addServices(parentDir + File.separator + "beast.app" + File.separator + "version.xml");
                }
            } catch (Throwable e) {
                // ignore
            }
        }
    }

    /** Parse a version.xml file and add its services. */
    public static void addServices(String versionFile) {
        try {
            if (versionFile.endsWith("*")) {
                File vf = new File(versionFile.substring(0, versionFile.length() - 1));
                if (vf.exists() && vf.isDirectory()) {
                    for (File f : vf.listFiles()) {
                        if (f.isDirectory()) {
                            String inner = f.getAbsolutePath() + "/version.xml";
                            if (new File(inner).exists()) {
                                addServices(inner);
                            }
                        }
                    }
                }
                return;
            }
            File vf = new File(versionFile);
            if (vf.exists() && vf.isDirectory()) {
                addServices(versionFile + "/version.xml");
                return;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            Document doc = factory.newDocumentBuilder().parse(versionFile);
            Map<String, Set<String>> parsedServices = PackageManager.parseServices(doc);
            Element packageElement = doc.getDocumentElement();
            String packageName = packageElement.getAttribute("name");
            BEASTClassLoader.classLoader.addServices(packageName, parsedServices);
        } catch (Throwable e) {
            System.err.println(e.getMessage());
        }
    }

    /** Get the full services map. */
    public static Map<String, Set<String>> getServices() {
        return services;
    }

    /** Instance method: register services for a package. */
    public void addServices(String packageName, Map<String, Set<String>> services) {
        ClassLoader loader = fallbackClassLoader();
        for (String service : services.keySet()) {
            BEASTClassLoader.services.computeIfAbsent(service, k -> new HashSet<>());
            Set<String> providers = BEASTClassLoader.services.get(service);
            providers.addAll(services.get(service));
            for (String provider : services.get(service)) {
                class2loaderMap.put(provider, loader);
                if (provider.contains(".")) {
                    namespaces.add(provider.substring(0, provider.lastIndexOf('.')));
                }
            }
        }
    }

    /**
     * Add a single service provider — useful for testing.
     */
    public static void addService(String service, String className, String packageName) {
        if (!BEASTClassLoader.services.containsKey(service)) {
            if (BEASTClassLoader.services.isEmpty()) {
                initServices();
            }
            BEASTClassLoader.services.computeIfAbsent(service, k -> new HashSet<>());
        }
        BEASTClassLoader.services.get(service).add(className);
        class2loaderMap.put(className, fallbackClassLoader());
    }

    /**
     * Delete services — used when uninstalling packages.
     */
    public static void delService(Map<String, Set<String>> serviceMap, String packageName) {
        for (String service : serviceMap.keySet()) {
            Set<String> classNames = serviceMap.get(service);
            for (String provider : classNames) {
                Set<String> s = BEASTClassLoader.services.get(service);
                if (s != null) s.remove(provider);
                if (provider.contains(".")) {
                    namespaces.remove(provider.substring(0, provider.lastIndexOf('.')));
                }
            }
        }
    }

    /**
     * Check whether any class in the set uses an existing registered namespace.
     */
    public static String usesExistingNamespaces(Set<String> services) {
        for (String service : services) {
            if (service.contains(".")) {
                String namespace = service.substring(0, service.lastIndexOf('.'));
                if (namespaces.contains(namespace)) {
                    return namespace;
                }
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    //  Resource loading
    // ------------------------------------------------------------------

    /**
     * Get a resource by name using the context / system class-loader.
     * Replaces the old {@code URLClassLoader.getResource()} behaviour.
     */
    public URL getResource(String name) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl != null) {
            URL url = cl.getResource(name);
            if (url != null) return url;
        }
        return ClassLoader.getSystemResource(name);
    }

    /**
     * Get a resource for a specific BEAST package.
     */
    public static URL getResource(String packageName, String resourceName) {
        // Try plugin layers first
        for (ModuleLayer layer : pluginLayers) {
            for (Module module : layer.modules()) {
                ClassLoader cl = module.getClassLoader();
                if (cl != null) {
                    URL url = cl.getResource(resourceName);
                    if (url != null) return url;
                }
            }
        }
        // Fall back to context / system classloader
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl != null) {
            URL url = cl.getResource(resourceName);
            if (url != null) return url;
        }
        return ClassLoader.getSystemResource(resourceName);
    }

    // ------------------------------------------------------------------
    //  Plugin layer management (JPMS)
    // ------------------------------------------------------------------

    /**
     * Register a {@link ModuleLayer} created for an external BEAST package.
     * The layer's class-loaders become searchable by {@link #forName},
     * and the services are merged into the registry.
     */
    public static void registerPluginLayer(ModuleLayer layer, Map<String, Set<String>> layerServices) {
        pluginLayers.add(layer);
        if (layerServices != null) {
            for (Map.Entry<String, Set<String>> entry : layerServices.entrySet()) {
                services.computeIfAbsent(entry.getKey(), k -> new HashSet<>()).addAll(entry.getValue());
                for (String provider : entry.getValue()) {
                    for (Module module : layer.modules()) {
                        ClassLoader mcl = module.getClassLoader();
                        if (mcl != null) {
                            class2loaderMap.putIfAbsent(provider, mcl);
                        }
                    }
                    if (provider.contains(".")) {
                        namespaces.add(provider.substring(0, provider.lastIndexOf('.')));
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------
    //  Deprecated legacy methods (no-ops)
    // ------------------------------------------------------------------

    /** @deprecated No-op. Module path handles class loading now. */
    @Deprecated(forRemoval = true)
    public void addURL(URL url) {
        // no-op
    }

    /** @deprecated No-op. Use registerPluginLayer() for external packages. */
    @Deprecated(forRemoval = true)
    public void addURL(URL url, String packageName, Map<String, Set<String>> services) {
        if (services != null) {
            addServices(packageName, services);
        }
    }

    /** @deprecated No-op. Module layers handle inter-package dependencies. */
    @Deprecated(forRemoval = true)
    public void addParent(String packageName, String parentPackage) {
        // no-op
    }

    /** @deprecated No-op. Module path handles class loading now. */
    @Deprecated(forRemoval = true)
    public void addJar(String jarFile) {
        // no-op
    }

    /** @deprecated No-op. Use registerPluginLayer() for external packages. */
    @Deprecated(forRemoval = true)
    public void addJar(String jarFile, String packageName) {
        // no-op
    }

    // ------------------------------------------------------------------
    //  Internals
    // ------------------------------------------------------------------

    private static ClassLoader fallbackClassLoader() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        return cl != null ? cl : ClassLoader.getSystemClassLoader();
    }
}
