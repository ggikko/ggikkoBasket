package com.ggikko.chattingserver;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.omg.CORBA.portable.Delegate;

public class WaitingRoom {

	private static final int MAX_ROOM = 10; // 방 만들 최대 개수
	private static final int MAX_USER = 100; // 대기방 최대 유저 인원
	private static final String SEPARATOR = "|"; // 구분자
	private static final String DELIMETER = "'"; // 구분자 1
	private static final String DELIMETER1 = "="; // 구분자 2

	private static final int ERROR_ALREADYUSER = 3001; // 대기실에 유저가 있을 경우 에러
	private static final int ERROR_SERVERFULL = 3002; // 서버가 풀일 때
	private static final int ERROR_ROOMSFULL = 3011; // 방 개수가 최대 일 때
	private static final int ERROR_ROOMERFULL = 3021; // 방의 인원이 최대일 때
	private static final int ERROR_PASSWORD = 3022; // 비밀번호가 틀렸을 때

	/*
	 * userVector ID , userHash ID, Client roomVector roomNumber, roomHash
	 * roomNumber, ChattingRoom
	 */
	private static Vector userVector, roomVector; // 데이터 직렬화, 동기화에 능함 VECTOR vs
													// ARRAYLIST 등등.. 더 알아봐야함
	private static Hashtable userHash, roomHash; // 물론 HashMap이 검색에는 빠르지만
													// HashTable은 동기화하면 더 빠름 이유는
													// 공부해봐야함.

	private static int userCount; // 대기방의 유저 숫자
	private static int roomCount; // 대기방의 만들어진 방 숫자

	// 변수 초기화
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
	 * 룸들을 얻어오는 메소드 return roomNumber = chattingRoom ' roomNumber = chattingRoom
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
	 * 유저 아이디 반환하는 메소드 return id'id
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

	// 대기방 또는 채팅방에 대한 클라이언트의 hashTable이 반환됨.
	public synchronized Hashtable getClients(int roomNum) {
		if (roomNum == 0)
			return userHash; // 만약 대기방 번호면

		Integer roomNumber = new Integer(roomNum);
		ChattingRoom room = (ChattingRoom) roomHash.get(roomNumber); // 만약 채팅방
																		// 번호가
																		// 주어지면
		return room.getClients(); // 그 채팅방 번화에 대한 클라이언트 id와 thread 반환
	}

	/*
	 * 대기방의 정보를 받아오는 메소드 return
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

	// 채팅방의 유저 id정보 반환, return id'id'id (채팅룸의)
	public String getRoomInfo(int roomNum) {
		Integer roomNumber = new Integer(roomNum);
		ChattingRoom room = (ChattingRoom) roomHash.get(roomNumber);
		return room.getUsers();
	}

	/*
	 * 처음 로그인시 유저를 userVector, userHash에 넣고 새로 생성된 쓰레드에 id와, roomnumber = 0으로
	 * 바꾼다. 최대 유저와, 대기방과 채팅방에 동일한 아이디가 없도록 체크한다.
	 */
	public synchronized int addUser(String id, ChattingServerThread client) {
		if (userCount == MAX_USER)
			return ERROR_SERVERFULL; // 최대 유저 확인

		Enumeration ids = userVector.elements();
		while (ids.hasMoreElements()) {
			String temporaryId = (String) ids.nextElement(); // 대기방에 같은 아이디가
																// 존재하는지 확인
			if (temporaryId.equals(id))
				return ERROR_ALREADYUSER;
		}
		Enumeration rooms = roomVector.elements();
		while (rooms.hasMoreElements()) {
			ChattingRoom temporaryRoom = (ChattingRoom) rooms.nextElement(); // 채팅방에
																				// 같은
																				// 아이디가
																				// 존재하는지
																				// 확인
			if (temporaryRoom.checkUserIDs(id))
				return ERROR_ALREADYUSER;
		}

		userVector.addElement(id); // vector, hash에 넣고 id, roomnumber도 넣는다.
		userHash.put(id, client);
		client.cst_ID = id;
		client.cst_roomNumber = 0;
		userCount++;

		return 0;
	}

	// 대기방에 룸 리스트를 더함
	public synchronized int addRoom(ChattingRoom chattingRoom) {
		if (roomCount == MAX_ROOM)
			return ERROR_ROOMSFULL; // 룸 최대 크기 체크

		roomVector.addElement(chattingRoom); // roomVector에 채팅방 추가
		roomHash.put(new Integer(ChattingRoom.roomNumber), chattingRoom); // roomNumber와,
																			// ChattingRoom
																			// 추가
		roomCount++;

		return 0;
	}

	// 대기방에서 유저를 지움
	public synchronized void delUser(String id) {
		userVector.removeElement(id);
		userHash.remove(id);
		userCount--;
	}
	
	// 채팅방이 비밀번호방인지 확인, 패스워드확인, 채팅방에 유저를 더함 
	public synchronized int joinRoom(String id, ChattingServerThread client,
			int roomNumber, String password) {

		Integer roomNum = new Integer(roomNumber);
		ChattingRoom room = (ChattingRoom) roomHash.get(roomNum);
		if (room.isRockecd()) { // 방이 잠겼으면 true, 방이 열렸으면 false
			if (room.checkPassword(password)) { // 패스워드가 같으면 false
				if (!room.addUser(id, client)) { // 방이 꽉찼으면 false
					return ERROR_ROOMERFULL;
				}
			} else {
				return ERROR_PASSWORD;
			}
		}else if(!room.addUser(id, client)){ // 방이 안잠겨있는데 유저가 꽉차도 에러
			return ERROR_ROOMERFULL;
		}

		userVector.removeElement(id);
		userHash.remove(id);
		
		return 0;

	}
	
	// 방에서 나온다. 채팅방이 비어있으면 방을 없애고 방 숫자를 줄인다. 그리고 대기방에 user vector, hash 추가
	public synchronized boolean quitRoom(String id, int roomNumber, ChattingServerThread client){
		boolean returnValue = false;
		Integer roomNum = new Integer(roomNumber);
		ChattingRoom room = (ChattingRoom) roomHash.get(roomNum); //채팅룸을 구해온다
		if(room.delUser(id)){ //채팅방이 비어있으면 true반환
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
