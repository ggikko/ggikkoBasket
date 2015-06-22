package com.ggikko.chattingserver;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.omg.CORBA.portable.Delegate;

public class WaitingRoom {

	private static final int MAX_ROOM = 10; // �� ���� �ִ� ����
	private static final int MAX_USER = 100; // ���� �ִ� ���� �ο�
	private static final String SEPARATOR = "|"; // ������
	private static final String DELIMETER = "'"; // ������ 1
	private static final String DELIMETER1 = "="; // ������ 2

	private static final int ERROR_ALREADYUSER = 3001; // ���ǿ� ������ ���� ��� ����
	private static final int ERROR_SERVERFULL = 3002; // ������ Ǯ�� ��
	private static final int ERROR_ROOMSFULL = 3011; // �� ������ �ִ� �� ��
	private static final int ERROR_ROOMERFULL = 3021; // ���� �ο��� �ִ��� ��
	private static final int ERROR_PASSWORD = 3022; // ��й�ȣ�� Ʋ���� ��

	/*
	 * userVector ID , userHash ID, Client roomVector roomNumber, roomHash
	 * roomNumber, ChattingRoom
	 */
	private static Vector userVector, roomVector; // ������ ����ȭ, ����ȭ�� ���� VECTOR vs
													// ARRAYLIST ���.. �� �˾ƺ�����
	private static Hashtable userHash, roomHash; // ���� HashMap�� �˻����� ��������
													// HashTable�� ����ȭ�ϸ� �� ���� ������
													// �����غ�����.

	private static int userCount; // ������ ���� ����
	private static int roomCount; // ������ ������� �� ����

	// ���� �ʱ�ȭ
	static {
		userVector = new Vector(MAX_USER); // id
		roomVector = new Vector(MAX_ROOM); // roomNumber
		userHash = new Hashtable(MAX_USER); // key = id, value = client (Thread)
		roomHash = new Hashtable(MAX_ROOM); // key = roomNumber, value =
											// chattingRoom
		userCount = 0;
		roomCount = 0;
	}

	/*
	 * ����� ������ �޼ҵ� return roomNumber = chattingRoom ' roomNumber = chattingRoom
	 * return empty
	 */
	public synchronized String getRooms() {
		StringBuffer room = new StringBuffer();
		String rooms;
		Integer roomNumber;
		Enumeration enumeration = roomHash.keys();
		while (enumeration.hasMoreElements()) {
			roomNumber = (Integer) enumeration.nextElement();
			ChattingRoom temporaryRoom = (ChattingRoom) roomHash
					.get(roomNumber);
			room.append(String.valueOf(roomNumber));
			room.append(DELIMETER1);
			room.append(temporaryRoom.toString());
			room.append(DELIMETER);
		}
		try {
			rooms = new String(room);
			rooms = rooms.substring(0, rooms.length() - 1);
		} catch (StringIndexOutOfBoundsException e) {
			return "empty";
		}

		return rooms;
	}

	/*
	 * ���� ���̵� ��ȯ�ϴ� �޼ҵ� return id'id
	 */
	public synchronized String getUsers() {
		StringBuffer id = new StringBuffer();
		String ids;
		Enumeration enumeration = userVector.elements();
		while (enumeration.hasMoreElements()) {
			id.append(enumeration.nextElement());
			id.append(DELIMETER);
		}

		try {
			ids = new String(id);
			ids = ids.substring(0, ids.length() - 1);
		} catch (StringIndexOutOfBoundsException e) {
			return "";
		}

		return ids;
	}

	// ���� �Ǵ� ä�ù濡 ���� Ŭ���̾�Ʈ�� hashTable�� ��ȯ��.
	public synchronized Hashtable getClients(int roomNum) {
		if (roomNum == 0)
			return userHash; // ���� ���� ��ȣ��

		Integer roomNumber = new Integer(roomNum);
		ChattingRoom room = (ChattingRoom) roomHash.get(roomNumber); // ���� ä�ù�
																		// ��ȣ��
																		// �־�����
		return room.getClients(); // �� ä�ù� ��ȭ�� ���� Ŭ���̾�Ʈ id�� thread ��ȯ
	}

	/*
	 * ������ ������ �޾ƿ��� �޼ҵ� return
	 * roomNumber=chattingRoom'roomNumber=chattingRoom|id'id
	 */
	public String getWaitRoomInformation() {
		StringBuffer roomInformation = new StringBuffer();
		roomInformation.append(getRooms()); // roomNumber = chattingRoom '
											// roomNumber = chattingRoom
		roomInformation.append(SEPARATOR);
		roomInformation.append(getUsers()); // id'id'id
		return roomInformation.toString();
	}

	// ä�ù��� ���� id���� ��ȯ, return id'id'id (ä�÷���)
	public String getRoomInfo(int roomNum) {
		Integer roomNumber = new Integer(roomNum);
		ChattingRoom room = (ChattingRoom) roomHash.get(roomNumber);
		return room.getUsers();
	}

	/*
	 * ó�� �α��ν� ������ userVector, userHash�� �ְ� ���� ������ �����忡 id��, roomnumber = 0����
	 * �ٲ۴�. �ִ� ������, ����� ä�ù濡 ������ ���̵� ������ üũ�Ѵ�.
	 */
	public synchronized int addUser(String id, ChattingServerThread client) {
		if (userCount == MAX_USER)
			return ERROR_SERVERFULL; // �ִ� ���� Ȯ��

		Enumeration ids = userVector.elements();
		while (ids.hasMoreElements()) {
			String temporaryId = (String) ids.nextElement(); // ���濡 ���� ���̵�
																// �����ϴ��� Ȯ��
			if (temporaryId.equals(id))
				return ERROR_ALREADYUSER;
		}
		Enumeration rooms = roomVector.elements();
		while (rooms.hasMoreElements()) {
			ChattingRoom temporaryRoom = (ChattingRoom) rooms.nextElement(); // ä�ù濡
																				// ����
																				// ���̵�
																				// �����ϴ���
																				// Ȯ��
			if (temporaryRoom.checkUserIDs(id))
				return ERROR_ALREADYUSER;
		}

		userVector.addElement(id); // vector, hash�� �ְ� id, roomnumber�� �ִ´�.
		userHash.put(id, client);
		client.cst_ID = id;
		client.cst_roomNumber = 0;
		userCount++;

		return 0;
	}

	// ���濡 �� ����Ʈ�� ����
	public synchronized int addRoom(ChattingRoom chattingRoom) {
		if (roomCount == MAX_ROOM)
			return ERROR_ROOMSFULL; // �� �ִ� ũ�� üũ

		roomVector.addElement(chattingRoom); // roomVector�� ä�ù� �߰�
		roomHash.put(new Integer(ChattingRoom.roomNumber), chattingRoom); // roomNumber��,
																			// ChattingRoom
																			// �߰�
		roomCount++;

		return 0;
	}

	// ���濡�� ������ ����
	public synchronized void delUser(String id) {
		userVector.removeElement(id);
		userHash.remove(id);
		userCount--;
	}
	
	// ä�ù��� ��й�ȣ������ Ȯ��, �н�����Ȯ��, ä�ù濡 ������ ���� 
	public synchronized int joinRoom(String id, ChattingServerThread client,
			int roomNumber, String password) {

		Integer roomNum = new Integer(roomNumber);
		ChattingRoom room = (ChattingRoom) roomHash.get(roomNum);
		if (room.isRockecd()) { // ���� ������� true, ���� �������� false
			if (room.checkPassword(password)) { // �н����尡 ������ false
				if (!room.addUser(id, client)) { // ���� ��á���� false
					return ERROR_ROOMERFULL;
				}
			} else {
				return ERROR_PASSWORD;
			}
		}else if(!room.addUser(id, client)){ // ���� ������ִµ� ������ ������ ����
			return ERROR_ROOMERFULL;
		}

		userVector.removeElement(id);
		userHash.remove(id);
		
		return 0;

	}
	
	// �濡�� ���´�. ä�ù��� ��������� ���� ���ְ� �� ���ڸ� ���δ�. �׸��� ���濡 user vector, hash �߰�
	public synchronized boolean quitRoom(String id, int roomNumber, ChattingServerThread client){
		boolean returnValue = false;
		Integer roomNum = new Integer(roomNumber);
		ChattingRoom room = (ChattingRoom) roomHash.get(roomNum); //ä�÷��� ���ؿ´�
		if(room.delUser(id)){ //ä�ù��� ��������� true��ȯ
			roomVector.removeElement(room);
			roomHash.remove(roomNum);
			roomCount--;
			returnValue	= true;
		}
		userVector.addElement(id);
		userHash.put(id, client);
		return returnValue;
		
	}
}
