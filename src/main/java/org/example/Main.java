package org.example;

import org.example.requests.Request;
import org.example.requests.ReversalRequest;
import org.example.server.Server;


import java.io.IOException;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Main {
    public static void main(String[] args) throws IOException {
        //test2(1, 5000);
        //test2(10, 5000);
        //test2(100, 5000);
        test2(2000, 5000);
    }

    public static void test1() throws IOException {
        Server server = new Server(3345);
        Client client = new Client("localhost", 3345);
        int size = 5;
        AtomicInteger counter = new AtomicInteger();
        for (int i = 0; i < size; i++) {
            new Thread(() -> {
                Request request = new ReversalRequest(String.valueOf(new Random().nextInt()));
                System.out.println("before " + Thread.currentThread().getName() + " " + request);
                client.request(request);
                System.out.println("after " + Thread.currentThread().getName() + " " + request);
                counter.incrementAndGet();
            }).start();
        }
        while (counter.get() != size) ;
        server.close();
        client.close();
    }

    public static void test2(int threadsNumber, long executionTime) throws IOException {
        Server server = new Server(3345);
        Client client = new Client("localhost", 3345);
        AtomicInteger responsesCount = new AtomicInteger();
        AtomicInteger requestsCount = new AtomicInteger();

        AtomicLong timeSum = new AtomicLong();
        long startTime = System.currentTimeMillis();

        new Thread(() -> {
            for (int i = 0; i < threadsNumber; i++) {
                new Thread(() -> {
                    while (System.currentTimeMillis() <= startTime + executionTime) {

                        Request request = new ReversalRequest(String.valueOf(new Random().nextInt()));
                        requestsCount.incrementAndGet();
                        long t1 = System.currentTimeMillis();
                        client.request(request);
                        long t2 = System.currentTimeMillis();
                        timeSum.set(timeSum.get() + t2 - t1);
                        responsesCount.incrementAndGet();
                    }
                }).start();

            }
        }).start();

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (System.currentTimeMillis() > startTime + executionTime) {
                    timer.cancel();
                    while (requestsCount.get() != responsesCount.get());
                    System.out.println("Avg: " + (timeSum.get() / responsesCount.get()));
                    System.out.println("Requests executed: " + responsesCount.get());
                    client.close();
                    server.close();

                }
            }
        }, 0, 50);

    }
}