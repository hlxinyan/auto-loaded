package com.hl.study.auto.load.common;

import com.hl.study.auto.load.agent.JavaDynAgent;
import com.hl.study.auto.load.sample.Calculator;
import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.management.ManagementFactory;

import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JavaAgent {

    public static final Logger logger = LoggerFactory.getLogger(JavaAgent.class);

    private static VirtualMachine vm;
    private static String pid;

    private static String javaAgentJarPath;

    private static String lock="lock";


    static {

        // 当前进程pid
        String name = ManagementFactory.getRuntimeMXBean().getName();
        pid = name.split("@")[0];
        logger.info("@@@@@@@@@@@@@@Current Process pid：{}", pid);

        javaAgentJarPath=getJarPath();

       }


    /**
     * 获取jar包路径
     * @return
     */
    public static String getJarPath() {
        // StringUtils是jar文件内容
        URL url = Calculator.class.getProtectionDomain().getCodeSource().getLocation();
        String filePath = null;
        try {
            filePath = URLDecoder.decode(url.getPath(), "utf-8");// 转化为utf-8编码
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (filePath.endsWith(".jar")) {// 可执行jar包运行的结果里包含".jar"
            // 截取路径中的jar包名
            filePath = filePath.substring(0, filePath.lastIndexOf("/") + 1);
        }

        File file = new File(filePath);

        filePath = file.getAbsolutePath();//得到windows下的正确路径
        logger.info("filePath:{}",filePath);
        return filePath;
    }


    public static void init()  {

        // attach

        try{


            synchronized (lock){
                if(vm ==null){
                    logger.info("begin attach pid:{}",pid);

                    vm = VirtualMachine.attach(pid);

                    logger.info(" attach pid:{} successfully",pid);
                    logger.info("beginLoadJavaAgentJar:{},jarName:{}",javaAgentJarPath,"agent-1.0.0-SNAPSHOT.jar");
                    vm.loadAgent(javaAgentJarPath + "/agent-1.0.0-SNAPSHOT.jar");
                }
            }
          }catch (Exception e){
            e.printStackTrace();
            logger.error("attach error pid:{} error",pid,e);
        }



    }

    private static void destroy() throws IOException {
        if (vm != null) {
            vm.detach();
            logger.info("detach ");
            vm=null;
        }
    }

    /**
     * reload class in fullJarPath
     *
     * @param
     * @throws Exception
     */
    public static void javaAgent(String fullJarPath,String clazzName) throws ClassNotFoundException, IOException, UnmodifiableClassException, AttachNotSupportedException, AgentLoadException, AgentInitializationException, InterruptedException {


        try {
            System.getProperties().put("full_jar",fullJarPath);
            System.getProperties().put("need_to_loaded",clazzName);

            init();


        } finally {
            destroy();
        }
    }







}
