package com.hl.study.auto.load;

import com.hl.study.auto.load.common.ExtensionLoader;
import com.hl.study.auto.load.sample.Calculator;


public class Launcher {

    public static void main(String[] args) {

        System.out.println("");
        ExtensionLoader loader = ExtensionLoader.getExtensionLoader(Calculator.class);

        Calculator calculator1 = (Calculator) loader.getExtension("two", "");
        System.out.println(calculator1.cal());

        Calculator calculator2 = (Calculator) loader.getExtension("two", "/Users/lilyhuang/Work/Products/maui-sop-research/auto-loaded/test/auto-load-calculator-1.0.2-SNAPSHOT.jar");
        System.out.println(calculator2.cal());


        Calculator calculator3 = (Calculator) loader.getExtension("three", "/Users/lilyhuang/Work/Products/maui-sop-research/auto-loaded/test/auto-load-calculator-1.0.1-SNAPSHOT.jar");

        System.out.println(calculator3.cal());


    }

}
