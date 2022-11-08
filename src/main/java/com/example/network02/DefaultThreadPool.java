package com.example.network02;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @program: network
 * @description: 自定义线程实现方法
 * @author: zys
 * @create: 2022-10-23 18:56
 **/

public class DefaultThreadPool<Job extends Runnable> implements ThreadPool<Job> {

    // 线程池的最大数量
    private static final int MAX_WORKER_NUMBERS = 10;

    // 线程池默认的数量
    private static final int DEDAULT_WORKER_NUMBERS = 5;

    // 线程池最小的数量
    private static final int MIN_WORKER_NUMBERS = 1;

    // 工作队列，将会向队列中加入工作
    private final LinkedList<Job> jobs = new LinkedList<>();

    // 工作队列的最大数量
    private static final int MAX_JOB_NUMS = 10;

    // 工作者列表
    private final List<Worker> workers = Collections.synchronizedList(new ArrayList<Worker>());

    // 工作者线程的数量
    private int workerNum = DEDAULT_WORKER_NUMBERS;

    // 线程编号生成
    private final AtomicLong threadNum = new AtomicLong();

    public DefaultThreadPool() {
        initializeWorkers(DEDAULT_WORKER_NUMBERS);
    }

    public DefaultThreadPool(int num) {
        workerNum = num > MAX_WORKER_NUMBERS ? MAX_WORKER_NUMBERS : Math.max(num, MIN_WORKER_NUMBERS);
        initializeWorkers(num);
    }

    @Override
    public void execute(Job job) {
        if(job != null){
            // 添加一个工作，然后进行通知
            synchronized (jobs){
                jobs.addLast(job);
                // 检测出队列已满，需要增加线程，扩容
//                if (getJobSize()>MAX_JOB_NUMS) {
//                    addWorkers(MAX_WORKER_NUMBERS-workers.size());
//                }
                jobs.notify();
            }

        }
    }

    @Override
    public void shutdown() {
        for (Worker worker : workers){
            worker.shutdown();
        }
    }

    @Override
    public void addWorkers(int num) {
        synchronized (jobs){
            // 限制新增的Worker数量不超过最大值
            if (num + this.workerNum > MAX_WORKER_NUMBERS) {
                num = MAX_WORKER_NUMBERS - this.workerNum;
            }
            initializeWorkers(num);
            this.workerNum += num;
        }
    }

    @Override
    public void removeWorker(int num) {
        synchronized (jobs) {
            // 删除工作者数量不能超过已存在数量
            if (num >= this.workerNum) {
                throw new IllegalArgumentException( "beyond workNum" );
            }

            // 按照给定的数量停止woekers
            int count = 0;
            while (count < num) {
                Worker worker = workers.get(count);
                if (workers.remove(worker)) {
                    worker.shutdown();
                    count++;
                }
            }
            this.workerNum -= count;
        }
    }

    @Override
    public int getJobSize() {
        return jobs.size();
    }

    // 初始化线程工作者
    private void initializeWorkers(int num){
        for(int i = 0; i < num; i++){
            Worker worker = new Worker();
            workers.add(worker);
            Thread thread = new Thread(worker, "ThreadPool-Worker-" + threadNum.incrementAndGet());
            thread.start();
        }
    }


    // 工作者 负责消费任务
    class Worker implements Runnable{

        // 是否工作
        private volatile boolean running = true;

        @Override
        public void run(){
            while(running){
                Job job = null;
                synchronized (jobs){
                    // 如果工作者列表是空的 ，那么就wait 等待被唤醒
                    while (jobs.isEmpty()) {
                        try{
                            jobs.wait();
                        }catch (InterruptedException exception){
                            // 感知到外界对WorkerThread的中断操作，返回
                            Thread.currentThread().interrupt();
                            return;
                        }

                    }
                    // 取出一个Job
                    job = jobs.removeFirst();
                }
                if (job!=null) {
                    try{
                        job.run();
                    }catch (Exception ex){
                        ex.printStackTrace();
                    }
                }

            }

        }

        // 停止消费
        public void shutdown(){
            running = false;
        }

    }

}
