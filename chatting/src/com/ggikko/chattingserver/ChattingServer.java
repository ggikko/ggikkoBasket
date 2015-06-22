package com.ggikko.chattingserver;

import java.net.ServerSocket;
import java.net.Socket;

public class ChattingServer {

	public static final int server_port = 7777; // 집컴 서버 포트번호
	public static final int server_maxclient = 100; // 최대 클라이언트 받는 수

	// 서버에서 실행시키는 함수
	public static void main(String[] args) {
		try {
			ServerSocket server_socket = new ServerSocket(server_port);
			System.out.println("서버 소켓 열음");
			while (true) {
				Socket socket = null;
				ChattingServerThread client = null;
				try {
					socket = server_socket.accept(); // 접속을 받는다.
					client = new ChattingServerThread(socket); // 새로운 클라이언트 쓰레드
																// 생성
					client.start(); // run 메소드 실행. 쓰레드 클래스 따로 빼논거 실행
				} catch (Exception e) {
					e.printStackTrace();
					System.out.println(e); // 에러처리 방법을 잘 모르겠음 어떤걸로 해야하나? 공부해야함
											// 리눅스에서 터미널단에서 보기위해서 일단 sysout .. 일단 PASS 

				}
				try {
					if (socket != null)
						socket.close(); // 자원은 항상 쓰고나면 헤제해야지

				} catch (Exception e) {
					System.out.println(e); // 에러처리방법...
				} finally {
					socket = null;
				}

			}

		} catch (Exception e) {
			System.out.println(e); // 마찬가지..
		}

	}

}
