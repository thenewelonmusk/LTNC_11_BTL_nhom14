package com.auction;
import java.util.Scanner;

import com.auction.dto.*;
import com.auction.model.*;
import com.auction.model.item.*;
import com.auction.model.user.*;
import com.auction.repository.*;
import com.auction.service.*;
import com.auction.service.impl.*;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.print("Dang ky\nNhap ten cua ban:");
        String name = sc.next();
        System.out.print("\nNhap mat khau:");
        String password = sc.next();

        InMemoryUserRepository inMemoryUserRepository = new InMemoryUserRepository();
        
        RegisterRequest registerRequest = new RegisterRequest(name, password, password, "SELLER");
        UserServiceImpl userServiceImpl = new UserServiceImpl(inMemoryUserRepository);

        RegisterResponse registerResponse = userServiceImpl.register(registerRequest);
        System.out.println(registerResponse.getMessage());
    }
}
