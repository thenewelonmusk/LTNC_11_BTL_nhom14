package com.auction.server;

import com.auction.dto.LoginRequest;
import com.auction.dto.LoginResponse;
import com.auction.dto.RegisterRequest;
import com.auction.dto.RegisterResponse;
import com.auction.repository.InMemoryUserRepository;
import com.auction.repository.UserRepository;
import com.auction.service.UserService;
import com.auction.service.impl.UserServiceImpl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.Buffer;

import javax.xml.transform.SourceLocator;

public class AuctionServer {
    private final UserService userService;

    public AuctionServer(){
        this.userService = new UserServiceImpl(new InMemoryUserRepository());
    }

    public void start(){
        try(ServerSocket serverSocket = new ServerSocket(5000)){
            while(true){
                Socket clientSocket = serverSocket.accept();
                System.out.println("Ket noi thanh cong " + clientSocket.getInetAddress().getHostAddress());

                new Thread(new ClientHandler(clientSocket, userService)).start();
            }
        }
        catch(IOException e){
            System.err.println(e.getMessage());
        }
    }    
    public static void main(String[] args) {
        new AuctionServer().start();
    }
}

class ClientHandler implements Runnable{
    private Socket socket;
    private UserService userService;

    public ClientHandler(Socket socket, UserService userService){
        this.socket = socket;
        this.userService = userService;
    }

    @Override
    public void run(){
        try ( 
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter output = new PrintWriter(socket.getOutputStream(), true);
        ){
            String requestLine;
            while ((requestLine = reader.readLine()) != null) {
                String[] parts = requestLine.split("\\|");
                String action = parts[0];
                //register|username|password|confirm|role
                if((action.equalsIgnoreCase("REGISTER")) && parts.length == 5){
                    RegisterResponse res =  userService.register(new RegisterRequest(parts[1], parts[2], parts[3], parts[4]));
                    output.println((res.isSuccess() ? "SUCCESS" : "FAIL") + " - " + res.getMessage());
                }
                //login|username|password
                else if((action.equalsIgnoreCase("LOGIN")) && parts.length == 3){
                    LoginResponse log = userService.login(new LoginRequest(parts[1], parts[2]));
                    output.println((log.isSuccess() ? "SUCCESS" : "FAIL") + " - " + log.getMessage());
                }
                else{
                    System.out.println("FAIL - yeu cau khong hop le");
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    
}