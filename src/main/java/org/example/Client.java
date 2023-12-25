package org.example;


import org.example.requests.Request;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

public class Client {
    private final AtomicLong idGenerator = new AtomicLong(); //используется для инкрементирования id
    private final BlockingQueue<Request> requests = new LinkedBlockingQueue<>(); //Очередь запросов для обработки
    private final List<Request> awaitingResponseList = Collections.synchronizedList(new ArrayList<>()); //Запросы, ожидаюшие ответа
    private final Thread senderThread; //поток, посылающий запросы серверу
    private final Thread receiverThread; //поток, принимающий ответы от сервера
    private final Socket socket;
    int max = 0; //наибольшее количество запросов в очереди

    public Client(String ip, int port) throws IOException {
        socket = new Socket(ip, port);
        senderThread = new Thread(this::runSender);
        receiverThread = new Thread(this::runReceiver);
        senderThread.start();
        receiverThread.start();
    }

    private void runSender() {
        try (ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            while (!socket.isClosed()) {
                if (max < requests.size())
                    max = requests.size();
                Request request = requests.take(); //ждет, пока в очереди появится зарос
                out.writeObject(request); //отправляет запрос серверу
            }
        } catch (Exception ignored) {
        }
        System.out.println("max requests queue size: " + max);
    }

    private void runReceiver() {
        try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            while (!socket.isClosed()) {
                Object object = in.readObject(); //ждет объект от сервера
                if (object instanceof Request response) {
                    processResponse(response); //вызываем метод обработки ответа
                }
            }
        } catch (Exception exception) {
        }
    }

    private void processResponse(Request response) {
        Predicate<Request> predicate = r -> r.getId() == response.getId(); //условия соответствия запроса и ответа
        awaitingResponseList.stream()
                .filter(predicate)
                .forEach(r -> {
                    r.copy(response); //записывает данные из ответа в запрос
                    try {
                        r.onFinish(); //вызывает метод завершения у запроса
                    } catch (Exception exception) {
                        r.onException();
                    }
                    synchronized (r) {
                        r.notify(); //пробуждает поток запроса
                    }
                });
        awaitingResponseList.removeIf(predicate); //удаляет запросы из списка ожидающих
    }


    public synchronized void close() {
        try {
            //прерывает потоки
            senderThread.interrupt();
            receiverThread.interrupt();
            //закрывает соединение
            socket.close();
            requests.forEach(r -> {
                try {
                    r.onCancel(); //вызывает метод отмены для каждого запроса в очереди
                } catch (Exception exception) {
                    r.onException();
                }
                synchronized (r) {
                    r.notify(); //пробуждает поток, отправивший запрос
                }
            });
        } catch (Exception ignored) {

        }

    }

    public void request(Request request) {
        if (request == null) throw new NullPointerException();
        request.setId(idGenerator.getAndIncrement()); //генерируем id для запроса
        try {
            awaitingResponseList.add(request); //добавляем в список ожидающих
            requests.put(request); //добавляем запрос в очередь
            synchronized (request) {
                request.wait(); //блокируем поток
            }
        } catch (Exception exception) {
        }
    }
}
