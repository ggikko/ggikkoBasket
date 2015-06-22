package com.ggikko.chattingserver;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

public class ChattingRoom {
	
	//���� ��ȣ
	public static int roomNumber = 0;
	
	//������
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
	
	
	//ä�÷��� ���� ����� ���� ������ , ���� ���� �� userVector, Hash �ִ� �ο���
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
	
	
	// ä�ù��� Ŭ���̾�Ʈ�� id�� thread ���̺��� ��ȯ
	public Hashtable getClients(){
		return userHash;
	}
	
	//ä�ù濡 �ִ� �������� ���̵� ��ȯ return id'id'id
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
	
	// ä�ù濡 ���̵� �����ϴ��� ���ϴ��� �����ϱ�
	public boolean checkUserIDs(String id){
		Enumeration ids = userVector.elements();
		while(ids.hasMoreElements()){
			String temporaryId = (String) ids.nextElement();
			if(temporaryId.equals(id)) return true; // ���̵� �����մϴ�.
		}
		return false; // ���̵� �������� �ʽ��ϴ�.
	}
	
	//���� roomNumber�� return
	public static synchronized int getRoomNumber(){
		return roomNumber;
	}
	
	// id�� client�� �ް� ä�ù��� �� �ִ�ġ üũ�ϰ�, ä�ù濡 user vector, hash, count �� ���Ѵ�.
	public boolean addUser(String id, ChattingServerThread client){
		if(roomUser == roomMaxUser){
			return false;
		}
		
		userVector.addElement(id);
		userHash.put(id,client);
		roomUser++;
		return true;
	}
	
	//���� ������ �������� üũ��
	public boolean isRockecd(){
		return isRock;
	}
	
	//�н����� üũ
	public boolean checkPassword(String passwd){
		return password.equals(passwd);
	}
	
	//ä�ù濡�� ���� �����, user vector, hash ���� ����� roomuser--, ������� �Ⱥ������ ��ȯ
	public boolean delUser(String id){
		userVector.removeElement(id);
		userHash.remove(id);
		roomUser--;
		return userVector.isEmpty();
	}
	
	

	

}
