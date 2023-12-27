package org.example.server;

import org.example.requests.Request;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

class SocketHandler {
    private Socket socket;
    private final BlockingQueue<Request> responses = new LinkedBlockingQueue<>(); //потокобезопасная очередь ответов
    private final Thread requestReceiver;
    private final Thread responseSender;
    private final AtomicInteger maxCount = new AtomicInteger(); //максимальное количество одновременно заупщенных потоков
    private final AtomicInteger currentCount = new AtomicInteger(); //текущее количество одновременно запущенных потоков
    private final AtomicInteger sumCurrentCount = new AtomicInteger();
    private final AtomicInteger totalThreads = new AtomicInteger();
    private final AtomicInteger sumResponsesQueueSize = new AtomicInteger();

    public SocketHandler(Socket socket) {
        this.socket = socket;
        requestReceiver.start();
        responseSender.start();
    }

    {
        //поток, принимающий запросы от клиента и создающий для каждого поток обработчик
        requestReceiver = new Thread(() -> {
            try {
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                while (!socket.isClosed()) {
                    Object obj = in.readObject(); //блокируется, пока не придет объект от клиента
                    if (obj instanceof Request r) {
                        new Thread(() -> {
                            currentCount.incrementAndGet();
                            totalThreads.incrementAndGet();
                            sumCurrentCount.set(sumCurrentCount.get() + currentCount.get());
                            if (maxCount.get() < currentCount.get())
                                maxCount.set(currentCount.get());

                            Random random = new Random();
                            try {
                                r.execute();
                            } catch (Exception ignored) {
                                r.onException();
                            }
                            try {
                                Thread.sleep(random.nextInt(100, 150));
                                responses.put(r); //после выполнения запроса, добавляем ответ в очередь
                                sumResponsesQueueSize.set(sumResponsesQueueSize.get() + responses.size());
                            } catch (Exception ignored) {
                            }

                            currentCount.decrementAndGet();
                        }).start();
                    }

                }
            } catch (Exception ignored) {
            }
            System.out.println("Max threads count: " + maxCount.get());
            System.out.println("Avg threads count: " + (sumCurrentCount.get() / totalThreads.get()));
        });

    }

    {
        //поток, отправляющий ответы на запросы клиенту
        responseSender = new Thread(() -> {
            try {
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                while (!socket.isClosed()) {

                    Request response = responses.take(); //ждет, пока потоки обработчики не добавят ответ в очередь
                    out.writeObject(response);
                }
            } catch (Exception exception) {
            }
            System.out.println("Avg responses count in queue on server: " + (sumResponsesQueueSize.get() / totalThreads.get()));
        });
    }


    public synchronized void close() {
        //прерывает потоки
        requestReceiver.interrupt();
        responseSender.interrupt();
        if (socket != null && !socket.isClosed())
            try {
                //закрывает соединение
                socket.close();
            } catch (Exception exception) {

            }

    }


}