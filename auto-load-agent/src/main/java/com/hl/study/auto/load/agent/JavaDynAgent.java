package com.hl.study.auto.load.agent;



import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JavaDynAgent {

     private static Object lockObject = new Object();


    public JavaDynAgent() {
    }

    public static void agentmain(String args, Instrumentation inst) {
        synchronized(lockObject) {

            System.out.println("init instrumentation" + inst +"  classload:"+JavaDynAgent.class.getClassLoader());


            Properties properties=System.getProperties();

            String fullJarFilePath=properties.getProperty("full_jar");
            String needToLoadedClass=properties.getProperty("need_to_loaded");

            System.out.println(" fullJarFilePath " + fullJarFilePath +"  needToLoadedClass: "+needToLoadedClass);


             if(fullJarFilePath==null || needToLoadedClass==null || "null".equals(fullJarFilePath) || "null".equals(needToLoadedClass)){
                 return;
             }

            Class clazz=getClazz(inst,needToLoadedClass);

            if(clazz==null) return;
            System.out.println("get clazz :"+clazz);

            try{
                List<ClassDefinition> classDefList=reloadJarFile(fullJarFilePath,needToLoadedClass,clazz);
                inst.redefineClasses(classDefList.toArray(new ClassDefinition[classDefList.size()]));

            }catch (Exception e){
                e.printStackTrace();
                System.out.println("reload class exception");
            }




        }
    }


    private static List<ClassDefinition> reloadJarFile(String fullJarFile, String needToLoadedClass, Class c) throws IOException, ClassNotFoundException {

        System.out.println("reload jar file:"+fullJarFile +" 11111 needToLoadedClass:"+needToLoadedClass);
        JarFile jarFile = new JarFile(fullJarFile);
        List<ClassDefinition> classDefList = new ArrayList<ClassDefinition>();
        try {

            Enumeration<JarEntry> e = jarFile.entries();


            while (e.hasMoreElements()) {
                JarEntry je = e.nextElement();
                if (je.isDirectory() || !je.getName().endsWith(".class")) {
                    continue;
                }
                String className = je.getName().substring(0,je.getName().length()-6).replaceAll("/",".");
                System.out.println("className in jarFile:"+className);
                 /* if(!needToReload(className,classAttr)){
                       continue;
                  }*/

                if(!className.equals(needToLoadedClass)){
                    continue;
                }
                InputStream jarFileInputStream = jarFile.getInputStream(je);
                byte[] bytesFromFile = getBytes(jarFileInputStream);
                ClassDefinition classDefinition = new ClassDefinition(c, bytesFromFile);
                classDefList.add(classDefinition);


            }


        } finally {
            jarFile.close();
        }

        return classDefList;
    }




    private static byte[] getBytes(InputStream is)
            throws IOException {
        byte[] buffer = new byte[8192];
        ByteArrayOutputStream baos = new ByteArrayOutputStream(2048);
        int n;
        while ((n = is.read(buffer, 0, buffer.length)) != -1) {
            baos.write(buffer, 0, n);
        }
        return baos.toByteArray();
    }

    private static Class getClazz(Instrumentation inst,String needToLoadedClass){
        Class[] classes = inst.getAllLoadedClasses();
        for (Class clazz : classes)
        {
            System.out.println("all loaded clazz in system:"+clazz.getName());
            if(clazz.getName().equals(needToLoadedClass)){
                return clazz;
            };
        }

        return null;
    }

}
