package cn.jun.thread.pool;


import cn.jun.thread.exception.DiscardException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ThreadPool extends Thread{
    private static volatile int seq = 0;

    private int size;

    private final int queueSize;

    private final static int DEFAULT_TASK_QUEUE_SIZE = 2000;

    private final static String THREAD_PRFIX = "SIMPEL_THREAD_POOL-" ;

    private final static LinkedList<Runnable> TASK_QUEUE = new LinkedList<>(); // 工作队列

    private final static ThreadGroup  GROUP = new ThreadGroup("PoolGroup");

    private final static List<WorkThread> THREAD_QUEUE = new ArrayList<WorkThread>() ;

    private static DiscardPolicy discardPolicy = null;

    public final static DiscardPolicy DEFAULT_DISCARD_POLICY = new DiscardPolicy() {
        @Override
        public void discard() throws DiscardException {
            throw new DiscardException("Discard this Task!");
        }
    };

    private volatile boolean destroy = false;

    private int  min;

    private int  max;

    private int active;

    public ThreadPool(){
        this(4,8,16, DEFAULT_TASK_QUEUE_SIZE, DEFAULT_DISCARD_POLICY);

    }
    public ThreadPool(int min,int active,int max,int queueSize,DiscardPolicy discardPolicy){
        this.min = min;
        this.active = active;
        this.max = max;
        this.queueSize = queueSize;
        this.discardPolicy = discardPolicy;
        init();
    }

    private void init() {
        for(int i=0;i<min;i++){
            createWorkThread();
        }
        this.size = min;
        this.start();
    }

    @Override
    public void run() {
        while(!isDestroy()){
            StringBuffer sbuf = new StringBuffer();
            sbuf.append("Pool#Min:").append(this.min)
                    .append(" Active:").append(this.active)
                    .append(" Max:").append(this.max)
                    .append(" Current:").append(this.size)
                    .append(" QueueSize:").append(TASK_QUEUE.size());
            System.out.println(sbuf.toString());
            try {
                TimeUnit.SECONDS.sleep(2);
                if(TASK_QUEUE.size()>active && size<active){
                    for(int i=size;i<active;i++){
                        createWorkThread();
                    }
                    System.out.println("Thread pool increment to active!");
                    this.size = active;
                }else if(TASK_QUEUE.size()>max && size<max){
                    for(int i=size;i<active;i++){
                        createWorkThread();
                    }
                    System.out.println("Thread pool increment to max!");
                    this.size = max;
                }

                if(size>active && TASK_QUEUE.isEmpty()){
                    synchronized (TASK_QUEUE){
                        int releaseSize = size - active;
                        for(Iterator<WorkThread> it = THREAD_QUEUE.iterator();it.hasNext();){
                            if(releaseSize<0){
                                break;
                            }
                            WorkThread task = it.next();
                            task.close();
                            task.interrupt();
                            it.remove();
                            releaseSize--;

                        }
                        size= active;
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void submit(Runnable runnable){
        if(isDestroy()){
            throw new IllegalArgumentException("The thread pool already distory and not allow submit thread");
        }
        synchronized (TASK_QUEUE){
            if(TASK_QUEUE.size()>queueSize){
                discardPolicy.discard();
            }
            TASK_QUEUE.addLast(runnable);
            TASK_QUEUE.notifyAll();
        }
    }

    public void createWorkThread(){
        WorkThread task = new WorkThread(GROUP,THREAD_PRFIX+"_"+ seq++);
        task.start();;
        THREAD_QUEUE.add(task);
    }

    public interface DiscardPolicy{
        public void discard() throws DiscardException;
    }

    private class WorkThread extends Thread{
        private volatile TaskState taskState = TaskState.FREE;

        public WorkThread(ThreadGroup threadGroup,String name){
            super(threadGroup,name);
        }

        @Override
        public void run() {
            OUTTER:
            while(TaskState.DEAD != getTaskState()){
                Runnable task ;
                synchronized (TASK_QUEUE){
                    while(TASK_QUEUE.isEmpty()){
                        try {
                            taskState = TaskState.BLOCKED;
                            TASK_QUEUE.wait();
                        } catch (InterruptedException e) {
                            System.out.println("Closed");
                            break OUTTER ;
                        }
                    }
                    task = TASK_QUEUE.removeFirst();
                }
                if(task != null){
                    taskState = TaskState.RUNNING;
                    task.run();
                    taskState = TaskState.FREE;
                }
            }
        }

        public TaskState getTaskState(){
            return this.taskState;
        }

        public void close(){
            this.taskState = TaskState.DEAD;
        }
    }

    public void shutdown() throws InterruptedException {
        while(!TASK_QUEUE.isEmpty()){
            Thread.sleep(50);
        }

        int initValue = THREAD_QUEUE.size();
        while(initValue>0){
            for(WorkThread task:THREAD_QUEUE){
                if(task.getTaskState()==TaskState.BLOCKED){
                    task.interrupt();
                    task.close();
                    initValue--;
                }else{
                    Thread.sleep(50);
                }
            }
            System.out.println("The thread pool disposed！");
        }
    }

    public boolean isDestroy(){
        return this.destroy;
    }

    public int getQueueSize() {
        return queueSize;
    }

    private enum TaskState{
        FREE,RUNNING,BLOCKED,DEAD
    }
}
