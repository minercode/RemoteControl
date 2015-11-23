package main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Scanner;
import java.io.FileWriter;

public class RemoteControlMain {

	public static void main(String[] args) {
		/**
		 * created by minercode
		 */
		// TODO Auto-generated method stub
		File authFile = new File("auth.dat");
		boolean authFlag= false;
		if (!authFile.exists()) {
			System.out.println("인증 가능한 아이디와 패스워드가 없습니다. 새로 생성하십시오");
			Scanner in = new Scanner(System.in);
			System.out.print("ID: ");
			String id = in.nextLine();
			System.out.print("Password: ");
			String password = in.nextLine();
			try {
				BufferedWriter bufferedWriter = new BufferedWriter(
						new FileWriter(authFile));

				bufferedWriter.write(id + "\n");
				bufferedWriter.write(password);
				bufferedWriter.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			authFlag  = true;
		} 
		else{
			authFlag  = true;
		}
		if(authFlag){
			String id = null;
			String pass = null;
			try {
				BufferedReader idReader = new BufferedReader(new FileReader("auth.dat"));
				
				id = idReader.readLine();
				pass = idReader.readLine();
				idReader.close();
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			ProcessBuilder processBuilder = new ProcessBuilder("java", "-jar",
					"InternalProcess.jar", id, pass);
			if (processBuilder.redirectErrorStream()) {
				System.out.println("not exist");
			}
			try {
				Process p = processBuilder.start();

				Scanner s = new Scanner(p.getErrorStream());
				BufferedReader bufferedReader = new BufferedReader(
						new InputStreamReader(p.getInputStream()));
				while (s.hasNextLine()) {
					System.out.println(s.nextLine());
				}
				String str = null;
				while ((str = bufferedReader.readLine()) != null) {
					System.out.println(str);
				}
				/*try {
					p.waitFor();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}*/
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
