package org.example;

import org.example.server.Server;
import org.example.requests.Request;
import org.example.requests.ReversalRequest;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Main {
    public static void main(String[] args) throws IOException {
        test1();
        test2();
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

    public static void test2() throws IOException {
        Server server = new Server(3345);
        Client client = new Client("localhost", 3345);
        int size = 1000;
        AtomicInteger counter = new AtomicInteger();
        AtomicLong avg = new AtomicLong();
        for (int i = 0; i < size; i++) {
            new Thread(() -> {
                Request request = new ReversalRequest(String.valueOf(new Random().nextInt()));
                long t1 = System.currentTimeMillis();
                client.request(request);
                if (avg.get() == 0)
                    avg.set(System.currentTimeMillis() - t1);
                else avg.set((avg.get() + System.currentTimeMillis() - t1) / 2);
                counter.incrementAndGet();
            }).start();
        }
        while (counter.get() != size) ;
        System.out.println("avg: " + avg.get() + " ms");
        server.close();
        client.close();
    }
}