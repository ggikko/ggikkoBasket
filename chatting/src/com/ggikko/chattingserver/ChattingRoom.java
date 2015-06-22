package com.ggikko.chattingserver;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

public class ChattingRoom {
	
	//대기방 번호
	public static int roomNumber = 0;
	
	//구분자
	private static final String DELIMETER = "'";
	private static final String DELIMETER1 = "=";
	
	
	/*  userVector ID , userHash ID, Client
	 *  roomVector roomNumber, roomHash roomNumber, ChattingRoom
	 */
	private Vector userVector; 
	private Hashtable userHash;
	private String roomName; 
	private int roomMaxUser;
	private int roomUser;
	private boolean isRock;
	private String password;
	private String admin;
	
	
	//채팅룸을 새로 만들기 위한 생성자 , 변수 선언 및 userVector, Hash 최대 인원수
	public ChattingRoom(String roomName, int roomMaxUser, boolean isRock, String password, String admin){
		roomNumber++;
		this.roomName = roomName;
		this.roomUser = roomMaxUser;
		this.isRock = isRock;
		this.password = password;
		this.admin = admin;
		this.userVector = new Vector(roomMaxUser);
		this.userHash = new Hashtable(roomMaxUser);
		
	}
	
	
	// 채팅방의 클라이언트의 id와 thread 테이블을 반환
	public Hashtable getClients(){
		return userHash;
	}
	
	//채팅방에 있는 유저들의 아이디 반환 return id'id'id
	public synchronized String getUsers(){
		StringBuffer id = new StringBuffer();
		String ids;
		Enumeration enumeration = userVector.elements();
		while(enumeration.hasMoreElements()){
			id.append(enumeration.nextElement());
			id.append(DELIMETER);
		}
		
		try{
			ids = new String(id);
			ids = ids.substring(0, ids.length()-1);	
		}catch(StringIndexOutOfBoundsException e){
			return "";
		}
		return ids;
	}
	
	// 채팅방에 아이디가 존재하는지 않하는지 구분하기
	public boolean checkUserIDs(String id){
		Enumeration ids = userVector.elements();
		while(ids.hasMoreElements()){
			String temporaryId = (String) ids.nextElement();
			if(temporaryId.equals(id)) return true; // 아이디가 존재합니다.
		}
		return false; // 아이디가 존재하지 않습니다.
	}
	
	

}
