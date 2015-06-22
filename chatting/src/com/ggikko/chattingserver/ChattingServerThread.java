package com.ggikko.chattingserver;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

// Ŭ���̾�Ʈ ������ Ŭ����
public class ChattingServerThread extends Thread {

	// �⺻ ����, ��Ʈ��, ����, ���� ���� (StringBuffer vs StringBuilder => Builder�� �������� Buffer�� Thread�� �־ ��������) 
	private Socket cst_socket;
	private DataInputStream cst_in;
	private DataOutputStream cst_out;
	private StringBuffer cst_buffer;
	private WaitingRoom cst_waitingroom;
	public String cst_ID;
	public int cst_roomNumber;

	// ������, ����
	private static final String SEPARATOR = "|";
	private static final String DELIMETER = "'";
	private static final int WAITINGROOM = 0;

	// ��û�� ���?
	private static final int REQUEST_LOGON = 1001;
	private static final int REQUEST_CRATEROOM = 1011;
	private static final int REQUEST_ENTERROOM = 1021;
	private static final int REQUEST_QUITROOM = 1031;
	private static final int REQUEST_LOGOUT = 1041;
	private static final int REQUEST_SENDWORD = 1051;
	private static final int REQUEST_SENDWORDTO = 1052;
	private static final int REQUEST_COERCEOUT = 1053;
	// private static final int REQUEST_ = ; ���Ϻ�����, ���������� ��� Ȯ�强 ����ؾ���

	// ���信 ���� ó�� ����? ��... ��� ��Ƴ� --> javadoc? �ϴ� PASS
	private static final int YES_LOGON = 2001;
	private static final int NO_LOGON = 2002;
	private static final int YES_CREATEROOM = 2011;
	private static final int NO_CREATEROOM = 2012;
	private static final int YES_ENTERROOM = 2021;
	private static final int NO_ETTERROOM = 2022;
	private static final int YES_QUITROOM = 2031;
	private static final int YES_LOGOUT = 2041;
	private static final int YES_SENDWORD = 2051;
	private static final int YES_SENDWORDTO = 2053;
	private static final int YES_COERCEOUT = 2054;
	private static final int MODIFY_WAITUSER = 2013;
	private static final int MODIFY_WAITINFORMATION = 2013;
	private static final int MODIFY_ROOMUSER = 2013;
	// private static final int ?? = Ȯ�强 ���

	// ���� �޼��� ���
	private static final int ERR_ALREADYUSER = 3001;
	private static final int ERR_SERVERFULL = 3002;
	private static final int ERR_ROOMSFULL = 3011;
	private static final int ERR_ROOMERFULL = 3021;
	private static final int ERR_PASSWORD = 3022;
	private static final int ERR_REJECTION = 3031;
	private static final int ERR_NOUSER = 3032;

	// ���Ϲ޾Ƽ� ��Ʈ�� ����, �ݰ�, Ȯ�强�� ���� Data Stream ���� ������..
	public ChattingServerThread(Socket socket) {
		try {
			cst_socket = socket;
			cst_in = new DataInputStream(cst_socket.getInputStream());
			cst_out = new DataOutputStream(cst_socket.getOutputStream());
			cst_buffer = new StringBuffer(2048); // capacity = 2kbyte ���� // ����ũ��
													// ���� ����Ʈ
			cst_waitingroom = new WaitingRoom();

		} catch (Exception e) {
			System.out.println(e);
		}
	}

	// ���� �޼��� ����
	private void sendErrorCode(int message, int errorCode) throws IOException { // IOException
		cst_buffer.setLength(0); // �ʱ�ȭ
		cst_buffer.append(message); // �����޼��� ���
		cst_buffer.append(SEPARATOR); // ������
		cst_buffer.append(errorCode); // �����ڵ�
		send(cst_buffer.toString()); // ��� | �����ڵ� �� client���� ����
	}

	/*
	 * ���� ���� MODIFY_WAITINFORMATION | roomNumber=chattingRoom'roomNumber=chattingRoom | id'id
	 */
	private void modifyWaitRoom() throws IOException {
		cst_buffer.setLength(0);
		cst_buffer.append(MODIFY_WAITINFORMATION); // ������ ���� ���
		cst_buffer.append(SEPARATOR);
		cst_buffer.append(cst_waitingroom.getWaitRoomInformation()); // ���� ���� ������
		broadcast(cst_buffer.toString(), WAITINGROOM);
	}
	
	/* ���� �ο� ����
	 * return MODIFY_WAITUSER | id'id'id 
	 */
	private void modifyWaitUser() throws IOException{
		String ids = cst_waitingroom.getUsers();
		cst_buffer.setLength(0);
		cst_buffer.append(MODIFY_WAITUSER);
		cst_buffer.append(SEPARATOR);
		cst_buffer.append(ids);
		broadcast(cst_buffer.toString(), WAITINGROOM);
	}
	
	/* ä�ù濡 �ִ� ������ ������
	 * return MODIFY_ROOMUSER | id | code | id'id'id'id �� Ư�� roomNumber�� ��ȯ
	 */
	private void modifyRoomUser(int roomNumber, String id, int code) throws IOException{
		String ids = cst_waitingroom.getRoomInfo(roomNumber); // id'id'id (ä�÷���)
		cst_buffer.setLength(0);
		cst_buffer.append(MODIFY_ROOMUSER);
		cst_buffer.append(SEPARATOR);
		cst_buffer.append(id);
		cst_buffer.append(SEPARATOR);
		cst_buffer.append(code);
		cst_buffer.append(SEPARATOR);
		cst_buffer.append(ids);
		broadcast(cst_buffer.toString(), roomNumber);
	}
	
	

	// �� ó�� �޼��� ����
	private void send(String sendData) throws IOException {
		synchronized (cst_out) {
			System.out.println(sendData);

			cst_out.writeUTF(sendData); // ���� ������ ����
			cst_out.flush(); // ��������
		}

	}
	
	// ���� �Ǵ� ä�ù��� �������� ��û�� �����͸� ���
	private synchronized void broadcast(String sendData, int roomNumber) throws IOException{
		ChattingServerThread client;
		Hashtable clients = cst_waitingroom.getClients(roomNumber); // �����̸� ������ userHash, ä�ù��̸� ä�ù��� userHash 
		Enumeration enumeration = clients.keys();
		while(enumeration.hasMoreElements()){
			client = (ChattingServerThread) clients.get(enumeration.nextElement());
			client.send(sendData);
		}
		
	}
	
	// receive data : REQ_LOGON | id 
	// receive data : REQUEST_CREATEROOM | logon id | roomName'roomMaxUser'isRock'password			
	// receive data : REQUEST_ENTERROOM | logon id | roomNumber | password			
	// receive data : REQUEST_QUITROOM | logon id | roomNumber 				
	// receive data : REQUEST_LOGOUT | logon id 				
	// receive data : REQUEST_SENDWORD | logon id | roomNumber | data 				
	// receive data : REQUEST_SENDWORDTO | logon id | roomNumber | idTo | data			
	// receive data : REQUEST_COERCEOUT | roomnumber | idTo
	
	// Ŭ���̾�Ʈ ����
	public void run(){
		try {
			while(true){
				String receiveData = cst_in.readUTF();
				System.out.println(receiveData); // �������� Ŭ�󿡰� ���� ������ Ȯ�� -> �α�ó�� �ϴ¹��� ���� �����ؾ���
			
				//Ŭ���̾�Ʈ�κ��� �Ѿ�� ������ �Ľ�
				StringTokenizer st = new StringTokenizer(receiveData, SEPARATOR);
				int command = Integer.parseInt(st.nextToken());
				switch(command){
				
				/* receive data : REQ_LOGON | id
				 * �α׿� ��û�� ���
				 * ���濡 ���� �ְ�, 
				 * 			return YES_LOGON | roomNumber=chatRoom'roomNumber=chatRoom
				 * �����ÿ�     return NO_LOGON | ERROR_SERVERFULL or ERROR_ALREADYUSER(���) or ERROR_ALREADYUSER(ä��)
				 *			return MODIFY_WAITUSER | id'id'id
				 */			
				case REQUEST_LOGON : {
					cst_roomNumber = WAITINGROOM;
					int result;
					cst_ID = st.nextToken();
					result = cst_waitingroom.addUser(cst_ID, this); // ���濡 ������ �� �Ѵ�. �ߺ�üũ + �������̺� id�ֱ�
					cst_buffer.setLength(0);
					
					if(result == 0){
						cst_buffer.append(YES_LOGON);
						cst_buffer.append(SEPARATOR);
						cst_buffer.append(cst_waitingroom.getRooms());
						send(cst_buffer.toString());
						modifyWaitUser(); // MODIFY_WAITUSER | id'id'id �� �����ش� ����
						System.out.println(cst_ID + "�� �����Ͽ����ϴ�");			
					} else {
						sendErrorCode(NO_LOGON, result);
					}
					break;
				}		
				
				/* receive data : REQUEST_CREATEROOM | logon id | roomName'roomMaxUser'isRock'password
				 * �游��� ��û�� ���
				 * ä�ù� ���ο� ��ü ����, ä�ù濡 id, client �߰�, ���� ����
				 * 
				 * return YES_CREATEROOM | roomNumber
				 * return MODIFY_WAITINFORMATION | roomNumber=chattingRoom'roomNumber=chattingRoom | id'id
				 * return MODIFY_ROOMUSER | id | 1 | id'id'id'id �� Ư�� roomNumber�� ��ȯ
				 */
				case REQUEST_CRATEROOM : {
					String id, roomName, password;
					int roomMaxUser, result;
					boolean isRock;
					//�Ľ� ����
					id = st.nextToken(); 
					String roomInfomation = st.nextToken();
					StringTokenizer room = new StringTokenizer(roomInfomation, DELIMETER);
					roomName = room.nextToken();
					roomMaxUser = Integer.parseInt(room.nextToken());
					isRock = (Integer.parseInt(room.nextToken())==0) ? false : true;
					password = room.nextToken();
					
					//���ο� ��ü ����
					ChattingRoom chattingRoom = new ChattingRoom(roomName, roomMaxUser, isRock, password,id);
					
					//���濡 �߰� roomVector, hash, count �߰� �� 0���� or ���� �޼��� ����
					result = cst_waitingroom.addRoom(chattingRoom);
					
					if(result==0){
						cst_roomNumber = ChattingRoom.getRoomNumber(); // hard
						boolean temporary = chattingRoom.addUser(cst_ID, this); // ä�ù濡 �Էµ� id, client �߰� 
						cst_waitingroom.delUser(cst_ID); //���濡�� ����
						
						cst_buffer.setLength(0);
						cst_buffer.append(YES_CREATEROOM);
						cst_buffer.append(SEPARATOR);
						cst_buffer.append(cst_roomNumber);
						send(cst_buffer.toString()); // YES_CREATEROOM | roomNumber
						modifyWaitRoom();
						modifyRoomUser(cst_roomNumber, id, 1);
					} else {
						sendErrorCode(NO_CREATEROOM, result); // NO_CREATEROOM | ERROR_ROOMSFULL
					}
					break;					
				}
				
				/* receive data : REQUEST_ENTERROOM | logon id | roomNumber | password
				 * ������ ��û�� ���
				 * 
				 * 
				 * 			
				 */
				case REQUEST_ENTERROOM : {
					String id, password;
					int roomNumber, result;
					id = st.nextToken();
					roomNumber = Integer.parseInt(st.nextToken());
					try {
						password = st.nextToken();
					} catch (NoSuchElementException e) {
						password = "0";
					}
					//�����
					
				}
				
				// receive data : REQUEST_QUITROOM | logon id | roomNumber 				
				case REQUEST_QUITROOM : {
					
				}
				
				// receive data : REQUEST_LOGOUT | logon id 				
				case REQUEST_LOGOUT : {
					
				}
				
				// receive data : REQUEST_SENDWORD | logon id | roomNumber | data 				
				case REQUEST_SENDWORD : {
					
					
				}
				
				// receive data : REQUEST_SENDWORDTO | logon id | roomNumber | idTo | data			
				case REQUEST_SENDWORDTO : {
					
				}
				
				// receive data : REQUEST_COERCEOUT | roomnumber | idTo
				case REQUEST_COERCEOUT : {
					
				}

				
				
				
				}
				
			}
			
			
	
			
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

}
