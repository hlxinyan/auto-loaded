package com.hl.study.auto.load.common;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ExtensionLoader<T> {

    private static final Logger logger = LoggerFactory.getLogger(ExtensionLoader.class);

    private Class<?> type;

    private static final String SERVICES_DIRECTORY = "META-INF/services/";


    private static final ConcurrentMap<Class<?>, ExtensionLoader<?>> EXTENSION_LOADERS = new ConcurrentHashMap<Class<?>, ExtensionLoader<?>>();



    public static <T> ExtensionLoader<T> getExtensionLoader(Class<T> type) {
        if (type == null)
            throw new IllegalArgumentException("Extension type == null");
        if (!type.isInterface()) {
            throw new IllegalArgumentException("Extension type(" + type + ") is not interface!");
        }


        ExtensionLoader<T> loader = (ExtensionLoader<T>) EXTENSION_LOADERS.get(type);
        if (loader == null) {
            EXTENSION_LOADERS.putIfAbsent(type, new ExtensionLoader<T>(type));
            loader = (ExtensionLoader<T>) EXTENSION_LOADERS.get(type);
        }
        return loader;
    }


    private ExtensionLoader(Class<?> type) {
        this.type = type;
    }


    public T getExtension(String name,String fullJarName) {
        if (name == null || name.length() == 0)
            throw new IllegalArgumentException("Extension name == null");
        return (T) createExtension(fullJarName,name);
    }


    private T createExtension(String fullJarName,String name) {
        Class<?> clazz = loadExtensionClasses(fullJarName,name).get(name);
        if (clazz == null) {
            throw new RuntimeException(name +" not found");
        }
        try {
           return (T) clazz.newInstance();
        } catch (Throwable t) {
            throw new IllegalStateException("Extension instance(name: " + name + ", class: " +
                    type + ")  could not be instantiated: " + t.getMessage(), t);
        }
    }


    private static ClassLoader findClassLoader() {
        return ExtensionLoader.class.getClassLoader();
    }




    private Map<String, Class<?>> loadExtensionClasses(String fullJarName,String invokeName) {

        Map<String, Class<?>> extensionClasses = new HashMap<String, Class<?>>();

        loadFile(extensionClasses, SERVICES_DIRECTORY,fullJarName,invokeName);

        return extensionClasses;
    }

    private Map<String, IllegalStateException> exceptions = new ConcurrentHashMap<String, IllegalStateException>();


    private void loadFile(Map<String, Class<?>> extensionClasses, String dir,String fullJarName,String invokeKeyName) {
        String fileName = dir + type.getName();
        try {
            Enumeration<URL> urls;
            ClassLoader classLoader = findClassLoader();
            if (classLoader != null) {
                urls = classLoader.getResources(fileName);
            } else {
                urls = ClassLoader.getSystemResources(fileName);
            }
            if (urls != null) {
                while (urls.hasMoreElements()) {
                    java.net.URL url = urls.nextElement();
                    try {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), "utf-8"));
                        try {
                            String line = null;
                            while ((line = reader.readLine()) != null) {
                                final int ci = line.indexOf('#');
                                if (ci >= 0) line = line.substring(0, ci);
                                line = line.trim();
                                if (line.length() > 0) {
                                    try {
                                        String name = null;
                                        int i = line.indexOf('=');
                                        if (i > 0) {
                                            name = line.substring(0, i).trim();
                                            line = line.substring(i + 1).trim();
                                        }

                                        if(!name.equalsIgnoreCase(invokeKeyName)){
                                             continue;
                                        }
                                        if (line.length() > 0) {
                                             Class<?> clazz=getClazz(line,fullJarName);
                                            if (!type.isAssignableFrom(clazz)) {
                                                throw new IllegalStateException("Error when load extension class(interface: " +
                                                        type + ", class line: " + clazz.getName() + "), class "
                                                        + clazz.getName() + "is not subtype of interface.");
                                            }

                                            Class<?> c = extensionClasses.get(name);
                                            if (c == null) {
                                                extensionClasses.put(name, clazz);
                                            } else if (c != clazz) {
                                                throw new IllegalStateException("Duplicate extension " + type.getName() + " name " + name + " on " + c.getName() + " and " + clazz.getName());
                                            }

                                        }
                                  } catch (Throwable t) {
                                        IllegalStateException e = new IllegalStateException("Failed to load extension class(interface: " + type + ", class line: " + line + ") in " + url + ", cause: " + t.getMessage(), t);
                                        exceptions.put(line, e);
                                    }
                                }
                            } // end of while read lines
                        } finally {
                            reader.close();
                        }
                    } catch (Throwable t) {
                        logger.error("Exception when load extension class(interface: " +
                                type + ", class file: " + url + ") in " + url, t);
                    }
                } // end of while urls
            }
        } catch (Throwable t) {
            logger.error("Exception when load extension class(interface: " +
                    type + ", description file: " + fileName + ").", t);
        }
    }


    private  Class<?> loadNewJar(String line,String fullJarName){

       try {
            URLClassLoader child = new URLClassLoader (new URL[] {new File(fullJarName).toURL()}, ExtensionLoader.class.getClassLoader());
           return Class.forName(line, true, child);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }


    private Class<?> getClazz(String line,String fullJarName){

        try {
            logger.info("begin load class for line:{},fullJarName:{}",line,fullJarName);
            Class<?> clazz = Class.forName(line, true, ExtensionLoader.class.getClassLoader());


            if (!StringUtils.isBlank(fullJarName)){
                reloadClazz(fullJarName,line,clazz);

            }
            return clazz;

        } catch (ClassNotFoundException e) {
            return loadNewJar(line,fullJarName);
        }

    }


    private void reloadClazz(String fullJarName,String line,Class c){

        try{
                logger.info("begin load for fullJarName:{}",fullJarName);
               JavaAgent.javaAgent(fullJarName,line);

        }catch (Exception e){
            e.printStackTrace();
             logger.error("reload jar error",e);
        }


    }








}



