package cn.jun.thread.pool;

import java.util.concurrent.TimeUnit;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) throws InterruptedException {

        //ThreadPool threadPool = new ThreadPool(6,10,ThreadPool.DEFAULT_DISCARD_POLICY);
        ThreadPool threadPool = new ThreadPool();
        for (int i=0;i<40;i++){
            threadPool.submit(new Runnable() {
                @Override
                public void run() {
                    System.out.println("the runnable thread be serviced by"+Thread.currentThread()+" start");
                    try {
                        TimeUnit.SECONDS.sleep(2);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.out.println("the runnable thread be serviced by"+Thread.currentThread()+" finished");
                }
            });
        }


        //Thread.sleep(4000);
       // threadPool.shutdown();



    }
}
