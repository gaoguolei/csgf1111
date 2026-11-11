package org.jeecg.modules.pcms.job;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.Date;

/**
 * @Author gongzhou
 * @Date 2023/11/24 下午1:12
 * @Version 1.5
 */
public class TestJob2 implements Job {
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        System.err.println(new Date());
    }
}
