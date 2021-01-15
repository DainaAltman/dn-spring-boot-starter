package com.dn.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 方法相关的工具类, 简化方法的解析
 */
@Slf4j
public class MethodUtils {
    /**
     * 方法返回值泛型的缓存
     */
    public static final Map<String, List<Class>> genericTypeCache = new HashMap(64);

    /**
     * 获取方法的唯一表示, 类路径::方法名(参数名:参数类型=运行时传入值)
     *
     * @param method 方法对象
     * @param args   运行时传参
     * @return
     */
    public static String getMethodId(Method method, Object... args) {

        // 1. 获取方法所在的实体类的名称
        String className = method.getDeclaringClass().getName();

        // 2. 获取方法的所有参数类型
        Parameter[] parameters = method.getParameters();

        // 4. 拼接内容 类路径::方法名(
        StringBuilder idBuilder = new StringBuilder(className).append("::").append(method.getName()).append("(");

        for (int i = 0; i < parameters.length; i++) {
            String paramName = parameters[i].getName();
            String parameterTypeName = parameters[i].getType().getTypeName();

            idBuilder.append(paramName).append(":").append(parameterTypeName);
            if (args.length == parameters.length) {
                idBuilder.append("=").append(args[i].toString());
            }
            if (i != parameters.length - 1) {
                idBuilder.append(",");
            }
        }

        // 5. 拼接内容 )
        return idBuilder.append(")").toString();
    }


    /**
     * 获取方法括号中的参数名称, 这里我们是基于 Spring 的参数解析完成的
     *
     * @param method 方法对象
     * @return
     */
    public static String[] getMethodParamNames(Method method) {
        ParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();
        String[] parameterNames = discoverer.getParameterNames(method);
        return parameterNames;
    }


    /**
     * 获取方法返回值中的泛型
     *
     * @param method
     * @return
     */
    public static List<Class> getMethodGenericTypes(Method method) {
        // 1. 获取方法的唯一标识
        String methodId = getMethodId(method);
        List<Class> genericTypeClassList = new ArrayList();

        // 基于唯一标识进行上锁, 将锁细粒度化. 避免线程冲突
        synchronized (methodId) {
            // 2. 尝试从缓存中获取
            List<Class> cache = genericTypeCache.get(methodId);
            if (cache != null) {
                return cache;
            }

            // 3. 通过代码获取
            Type genericReturnType = method.getGenericReturnType();

            if (genericReturnType instanceof ParameterizedType) {
                Type[] actualTypeArguments = ((ParameterizedType) genericReturnType).getActualTypeArguments();
                for (Type type : actualTypeArguments) {
                    try {
                        genericTypeClassList.add(Class.forName(type.getTypeName()));
                    } catch (ClassNotFoundException e) {
                        log.error("类无法加载", e);
                    }
                }
            }

            // 4. 填充结果到缓存
            genericTypeCache.put(methodId, genericTypeClassList);
        }

        return genericTypeClassList;
    }

}
