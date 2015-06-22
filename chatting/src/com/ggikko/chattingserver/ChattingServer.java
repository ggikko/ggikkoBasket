package com.ggikko.chattingserver;

import java.net.ServerSocket;
import java.net.Socket;

public class ChattingServer {

	public static final int server_port = 7777; // ���� ���� ��Ʈ��ȣ
	public static final int server_maxclient = 100; // �ִ� Ŭ���̾�Ʈ �޴� ��

	// �������� �����Ű�� �Լ�
	public static void main(String[] args) {
		try {
			ServerSocket server_socket = new ServerSocket(server_port);
			System.out.println("���� ���� ����");
			while (true) {
				Socket socket = null;
				ChattingServerThread client = null;
				try {
					socket = server_socket.accept(); // ������ �޴´�.
					client = new ChattingServerThread(socket); // ���ο� Ŭ���̾�Ʈ ������
																// ����
					client.start(); // run �޼ҵ� ����. ������ Ŭ���� ���� ����� ����
				} catch (Exception e) {
					e.printStackTrace();
					System.out.println(e); // ����ó�� ����� �� �𸣰��� ��ɷ� �ؾ��ϳ�? �����ؾ���
											// ���������� �͹̳δܿ��� �������ؼ� �ϴ� sysout .. �ϴ� PASS 

				}
				try {
					if (socket != null)
						socket.close(); // �ڿ��� �׻� ������ �����ؾ���

				} catch (Exception e) {
					System.out.println(e); // ����ó�����...
				} finally {
					socket = null;
				}

			}

		} catch (Exception e) {
			System.out.println(e); // ��������..
		}

	}

}
