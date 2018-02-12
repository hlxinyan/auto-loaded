package com.hl.study.auto.load.web;

import com.hl.study.auto.load.common.ExtensionLoader;
import com.hl.study.auto.load.sample.Calculator;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class TestServlet extends HttpServlet {

   private static final Logger log= LoggerFactory.getLogger(TestServlet.class);
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String calculatorStr=request.getParameter("name");

        if(StringUtils.isBlank(calculatorStr)){
            calculatorStr="default";
        }

        String fullJarName=request.getParameter("full_jar");

        log.info("fullJarName:{} name:{}",fullJarName,calculatorStr);



        ExtensionLoader loader  = ExtensionLoader.getExtensionLoader(Calculator.class);
        Calculator calculator=(Calculator) loader.getExtension(calculatorStr,fullJarName);

        String result=calculator.cal();

        response.getOutputStream().print(result);
    }
}
