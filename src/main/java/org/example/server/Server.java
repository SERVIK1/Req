package org.example.server;

import org.example.server.SocketHandler;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Server {
    private final ServerSocket serverSocket;
    private final List<SocketHandler> handlers = Collections.synchronizedList(new ArrayList<>()); //список обработчиков

    public Server(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        new Thread(this::run).start(); //поток, устанавливающий связь с клиентами
    }

    private void run() {
        try {
            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept(); //принимаем клиента
                handlers.add(new SocketHandler(socket)); //создаем для клиента обработчик
            }
        } catch (Exception exception){
        }
    }

    //заглушить сервер
    public void close() {
        try {
            handlers.forEach(SocketHandler::close); //закрываем все обработчики клиентов
            serverSocket.close(); //закрываем соединение
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }


}