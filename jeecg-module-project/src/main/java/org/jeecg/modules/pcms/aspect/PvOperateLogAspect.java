package org.jeecg.modules.pcms.aspect.annotation;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.jeecg.modules.pcms.aspect.PvOperateLogStrategy;
import org.jeecg.modules.pcms.aspect.PvOperateLogStrategyFactory;
import org.jeecg.modules.pcms.project.service.IPvOperateLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 *
 * @author jun
 */
@Aspect
@Component
public class PvOperateLogAspect {
    @Autowired
    IPvOperateLogService pvOperateLogService;

    @Pointcut("@annotation(org.jeecg.modules.pcms.aspect.annotation.PvOperateLog)")
    public void operateLogPointCut(){

    }

//    @Before("operateLogPointCut()")
//    public void before(JoinPoint joinPoint) throws Throwable {
//
//        // 获得注解
//        PvOperateLog controllerOperateLog = getAnnotationLog(joinPoint);
//        if (controllerOperateLog == null) {
//            return;
//        }
//        PvOperateLogStrategy pvOperateLogStrategy = PvOperateLogStrategyFactory.getOperateLogStrategy("PROJECT");
//        pvOperateLogStrategy.addOperateLog(joinPoint.getArgs()[0]);
//    }

//    @Around("operateLogPointCut()")
//    public Object around(ProceedingJoinPoint point) throws Throwable {
//        // 获得注解
//        PvOperateLog controllerOperateLog = getAnnotationLog(point);
//        if (controllerOperateLog == null) {
//            return null;
//        }
//
//      Object args =   point.getArgs()[0];
//
//        long beginTime = System.currentTimeMillis();
//        //执行方法
//        Object result = point.proceed();
//        //执行时长(毫秒)
//        long time = System.currentTimeMillis() - beginTime;
//        PvOperateLogStrategy pvOperateLogStrategy = PvOperateLogStrategyFactory.getOperateLogStrategy("PROJECT");
//        pvOperateLogStrategy.addOperateLog(args);
//        return result;
//    }

    @AfterReturning("operateLogPointCut()")
    public void doAfterReturning(JoinPoint joinPoint) throws Throwable {

        // 获得注解
        PvOperateLog controllerOperateLog = getAnnotationLog(joinPoint);
        if (controllerOperateLog == null) {
            return;
        }
        PvOperateLogStrategy pvOperateLogStrategy = PvOperateLogStrategyFactory.getOperateLogStrategy("PROJECT");
        pvOperateLogStrategy.addOperateLog(joinPoint.getArgs()[0],controllerOperateLog.operateType());
    }

    /**
     * 是否存在注解，如果存在就获取
     */
    private PvOperateLog getAnnotationLog(JoinPoint joinPoint) {
        Signature signature = joinPoint.getSignature();
        MethodSignature methodSignature = (MethodSignature) signature;
        Method method = methodSignature.getMethod();

        if (method != null) {
            return method.getAnnotation(PvOperateLog.class);
        }
        return null;
    }
}
