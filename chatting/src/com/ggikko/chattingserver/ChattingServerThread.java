package com.ggikko.chattingserver;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

// 클라이언트 쓰레드 클래스
public class ChattingServerThread extends Thread {

	// 기본 소켓, 스트림, 버퍼, 대기방 선언 (StringBuffer vs StringBuilder => Builder가 빠르지만 Buffer가 Thread에 있어서 안정적임) 
	private Socket cst_socket;
	private DataInputStream cst_in;
	private DataOutputStream cst_out;
	private StringBuffer cst_buffer;
	private WaitingRoom cst_waitingroom;
	public String cst_ID;
	public int cst_roomNumber;

	// 구분자, 대기방
	private static final String SEPARATOR = "|";
	private static final String DELIMETER = "'";
	private static final int WAITINGROOM = 0;

	// 요청의 헤더?
	private static final int REQUEST_LOGON = 1001;
	private static final int REQUEST_CRATEROOM = 1011;
	private static final int REQUEST_ENTERROOM = 1021;
	private static final int REQUEST_QUITROOM = 1031;
	private static final int REQUEST_LOGOUT = 1041;
	private static final int REQUEST_SENDWORD = 1051;
	private static final int REQUEST_SENDWORDTO = 1052;
	private static final int REQUEST_COERCEOUT = 1053;
	// private static final int REQUEST_ = ; 파일보내기, 사진보내기 등등 확장성 고려해야함

	// 응답에 대한 처리 숫자? 으... 용어 어렵네 --> javadoc? 일단 PASS
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
	// private static final int ?? = 확장성 고려

	// 에러 메세지 헤더
	private static final int ERR_ALREADYUSER = 3001;
	private static final int ERR_SERVERFULL = 3002;
	private static final int ERR_ROOMSFULL = 3011;
	private static final int ERR_ROOMERFULL = 3021;
	private static final int ERR_PASSWORD = 3022;
	private static final int ERR_REJECTION = 3031;
	private static final int ERR_NOUSER = 3032;

	// 소켓받아서 스트림 열고, 닫고, 확장성을 위해 Data Stream 으로 생각중..
	public ChattingServerThread(Socket socket) {
		try {
			cst_socket = socket;
			cst_in = new DataInputStream(cst_socket.getInputStream());
			cst_out = new DataOutputStream(cst_socket.getOutputStream());
			cst_buffer = new StringBuffer(2048); // capacity = 2kbyte 설정 // 버퍼크기
													// 설정 디폴트
			cst_waitingroom = new WaitingRoom();

		} catch (Exception e) {
			System.out.println(e);
		}
	}

	// 에러 메세지 보냄
	private void sendErrorCode(int message, int errorCode) throws IOException { // IOException
		cst_buffer.setLength(0); // 초기화
		cst_buffer.append(message); // 에러메세지 헤더
		cst_buffer.append(SEPARATOR); // 구분자
		cst_buffer.append(errorCode); // 에러코드
		send(cst_buffer.toString()); // 헤더 | 에러코드 를 client에게 보냄
	}

	/*
	 * 대기방 수정 MODIFY_WAITINFORMATION | roomNumber=chattingRoom'roomNumber=chattingRoom | id'id
	 */
	private void modifyWaitRoom() throws IOException {
		cst_buffer.setLength(0);
		cst_buffer.append(MODIFY_WAITINFORMATION); // 방정보 수정 헤더
		cst_buffer.append(SEPARATOR);
		cst_buffer.append(cst_waitingroom.getWaitRoomInformation()); // 대기방 정보 얻어오기
		broadcast(cst_buffer.toString(), WAITINGROOM);
	}
	
	/* 대기방 인원 수정
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
	
	/* 채팅방에 있는 유저를 수정함
	 * return MODIFY_ROOMUSER | id | code | id'id'id'id 를 특정 roomNumber로 반환
	 */
	private void modifyRoomUser(int roomNumber, String id, int code) throws IOException{
		String ids = cst_waitingroom.getRoomInfo(roomNumber); // id'id'id (채팅룸의)
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
	
	

	// 각 처리 메세지 전송
	private void send(String sendData) throws IOException {
		synchronized (cst_out) {
			System.out.println(sendData);

			cst_out.writeUTF(sendData); // 보낼 데이터 쓰고
			cst_out.flush(); // 물내리고
		}

	}
	
	// 대기방 또는 채팅방의 유저에게 요청한 데이터를 방송
	private synchronized void broadcast(String sendData, int roomNumber) throws IOException{
		ChattingServerThread client;
		Hashtable clients = cst_waitingroom.getClients(roomNumber); // 대기방이면 대기방의 userHash, 채팅방이면 채팅방의 userHash 
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
	
	// 클라이언트 실행
	public void run(){
		try {
			while(true){
				String receiveData = cst_in.readUTF();
				System.out.println(receiveData); // 서버에서 클라에게 받은 데이터 확인 -> 로그처리 하는법을 몰라서 공부해야함
			
				//클라이언트로부터 넘어온 데이터 파싱
				StringTokenizer st = new StringTokenizer(receiveData, SEPARATOR);
				int command = Integer.parseInt(st.nextToken());
				switch(command){
				
				/* receive data : REQ_LOGON | id
				 * 로그온 요청의 경우
				 * 대기방에 유저 넣고, 
				 * 			return YES_LOGON | roomNumber=chatRoom'roomNumber=chatRoom
				 * 에러시에     return NO_LOGON | ERROR_SERVERFULL or ERROR_ALREADYUSER(대기) or ERROR_ALREADYUSER(채팅)
				 *			return MODIFY_WAITUSER | id'id'id
				 */			
				case REQUEST_LOGON : {
					cst_roomNumber = WAITINGROOM;
					int result;
					cst_ID = st.nextToken();
					result = cst_waitingroom.addUser(cst_ID, this); // 대기방에 유저를 더 한다. 중복체크 + 유저테이블에 id넣기
					cst_buffer.setLength(0);
					
					if(result == 0){
						cst_buffer.append(YES_LOGON);
						cst_buffer.append(SEPARATOR);
						cst_buffer.append(cst_waitingroom.getRooms());
						send(cst_buffer.toString());
						modifyWaitUser(); // MODIFY_WAITUSER | id'id'id 를 보내준다 각각
						System.out.println(cst_ID + "가 접속하였습니다");			
					} else {
						sendErrorCode(NO_LOGON, result);
					}
					break;
				}		
				
				/* receive data : REQUEST_CREATEROOM | logon id | roomName'roomMaxUser'isRock'password
				 * 방만들기 요청의 경우
				 * 채팅방 새로운 객체 생성, 채팅방에 id, client 추가, 대기방 삭제
				 * 
				 * return YES_CREATEROOM | roomNumber
				 * return MODIFY_WAITINFORMATION | roomNumber=chattingRoom'roomNumber=chattingRoom | id'id
				 * return MODIFY_ROOMUSER | id | 1 | id'id'id'id 를 특정 roomNumber로 반환
				 */
				case REQUEST_CRATEROOM : {
					String id, roomName, password;
					int roomMaxUser, result;
					boolean isRock;
					//파싱 시작
					id = st.nextToken(); 
					String roomInfomation = st.nextToken();
					StringTokenizer room = new StringTokenizer(roomInfomation, DELIMETER);
					roomName = room.nextToken();
					roomMaxUser = Integer.parseInt(room.nextToken());
					isRock = (Integer.parseInt(room.nextToken())==0) ? false : true;
					password = room.nextToken();
					
					//새로운 객체 생성
					ChattingRoom chattingRoom = new ChattingRoom(roomName, roomMaxUser, isRock, password,id);
					
					//대기방에 추가 roomVector, hash, count 추가 후 0리턴 or 에러 메세지 리턴
					result = cst_waitingroom.addRoom(chattingRoom);
					
					if(result==0){
						cst_roomNumber = ChattingRoom.getRoomNumber(); // hard
						boolean temporary = chattingRoom.addUser(cst_ID, this); // 채팅방에 입력된 id, client 추가 
						cst_waitingroom.delUser(cst_ID); //대기방에서 삭제
						
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
				 * 방입장 요청의 경우
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
					//대기중
					
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
