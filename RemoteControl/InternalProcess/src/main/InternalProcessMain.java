package main;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class InternalProcessMain {

	public static void main(String[] args) {
		/**
		 * created by minercode
		 */
		// TODO Auto-generated method stub
		System.out.println("helloworld");
		
		String id = args[0];
		String pass= args[1];
		
		LoginWatingThread loginWatingThread = new LoginWatingThread(id ,pass);
		loginWatingThread.start();
		
		try {
			loginWatingThread.join();
			if(loginWatingThread.isAuthFlag()){
				System.out.println("Auth ok");
			}
			else{
				System.out.println("Auth not ok");
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Program End");
		
	}
	
}
class LoginWatingThread extends Thread{	
	private ServerSocket serverSocket = null;
	private String id = null;
	private String pass = null;
	private boolean authFlag = false; 
	
	public LoginWatingThread(String id, String pass){
		this.id = id;
		this.pass = pass;
	}
	
	public void run(){
		try {
			serverSocket = new ServerSocket(5000);
		
			while(true){
				System.out.println("listen 돌입");
				Socket socket = serverSocket.accept();
				
				InputStream inputStream = socket.getInputStream();
				DataInputStream dataInputStream = new DataInputStream(inputStream);
				
				
				JSONObject recvMsg;
				
				String result = "";
				result = dataInputStream.readUTF();
				System.out.println("result: " + result);
				JSONParser parser = new JSONParser();
				Object obj=null;
				try {
					obj = parser.parse(result);
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				recvMsg = (JSONObject)obj;
					
				String id = (String)recvMsg.get("id");
				String pass =(String)recvMsg.get("pass");
				
				
				
				JSONObject sendMsg = new JSONObject();
				if(this.id.equals(id) && this.pass.equals(this.pass)){
					this.authFlag = true;
					sendMsg.put("auth", "ok");
					this.authFlag = true;
				}
				else{
					sendMsg.put("auth", "no");
					this.authFlag = false;
				}
				System.out.println("Json string: "+sendMsg.toJSONString());
				DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
				dataOutputStream.writeUTF(sendMsg.toJSONString());
				System.out.println("수신완료");
				
				dataInputStream.close();
				dataOutputStream.close();
				socket.close();
				break;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public String getIdString() {
		return id;
	}

	public void setIdString(String id) {
		this.id = id;
	}

	public String getPassString() {
		return pass;
	}

	public void setPassString(String pass) {
		this.pass = pass;
	}

	public boolean isAuthFlag() {
		return authFlag;
	}

	public void setAuthFlag(boolean authFlag) {
		this.authFlag = authFlag;
	}
	
}