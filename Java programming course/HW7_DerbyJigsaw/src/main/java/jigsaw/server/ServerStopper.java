package jigsaw.server;

import java.util.Scanner;

public class ServerStopper implements Runnable {
    public void run() {
        Scanner in = new Scanner(System.in);
        System.out.println("ServerStopper: Введите \"stop\" для остановки сервера.");
        String input;
        do {
            input = in.nextLine();
        } while (!input.equals("stop"));
        MultithreadedGameServer.stopServer();
    }
}