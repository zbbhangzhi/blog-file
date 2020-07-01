package com.zbb.lizi.design.strategy;

import java.util.ArrayList;
import java.util.List;

/**
 * @author tiancha
 * @since 2020/6/30 16:08
 */
public class RuleEngine {

    public void invokeAll(Object context){
        List<BasicRule> rules = loadClass(BasicRule.class);
        for (BasicRule rule: rules){
            rule.evaluate(context);
        }
    }

    public List<BasicRule> loadClass(Class clazz){
        // 可借助spring容器-工厂机制获取某个类型下的所有实例
        // 或者通过反射获取继承某个类型的所有子类 todo
        List<BasicRule> rules = new ArrayList<BasicRule>();

        return rules;
    }
}
