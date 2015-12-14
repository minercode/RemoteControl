#include <WinSock2.h>
#include <Windows.h>
#include <io.h>
#include <iostream>
#include <fstream>
#include <vector>
#include <stdlib.h>
#include <string>
#include <process.h>
#include <TlHelp32.h>


#pragma comment(lib,"ws2_32")
#pragma warning(disable:4996) 

#define PORT_NUM 5000
#define BUF_SIZE 1024
#define HIMETRIC_INCH 2540

using namespace std;

typedef void(*PFN_HOOKSTART)();
typedef void(*PFN_HOOKSTOP)();
typedef void(*PFN_MOUSESTART)();
typedef void(*PFN_MOUSESTOP)();


typedef struct networkInfo{
	char userId[200];
	char userPass[200];
	int major;
	int minor;
	bool terminate;
	bool hookStart;
}NetworkInfo;


WSADATA wsaData;
SOCKET hServSock = 0, hClntSock = 0;
SOCKADDR_IN servAdr, clntAdr;

vector<char*> inputQ;
vector<char*> outputQ;
NetworkInfo mainInfo;

void ErrorHandling(char* str);
void CommandExecuter(const char *command);

//����� Id, pass �ʱ�ȭ �Լ�
void InitUserInfo();

//Network
void InitConnect();
void TerminateConnect();
void TerminateClient();

//������ ���ؽ�
HANDLE hMutex;
HANDLE hMutexHook;
//������ ������
HANDLE recvThread;
HANDLE execThread;
HANDLE hookThread;

//���μ��� �ڵ� ����
HANDLE hProcess = NULL;
PROCESSENTRY32 pe32 = { 0 };


//Thread ProcS
DWORD WINAPI RecvProc(LPVOID lpParam);
DWORD WINAPI ExecuteProc(LPVOID lpParam);
DWORD WINAPI ExecuteHook(LPVOID lpParam);

int main(){
	//step 1 ���� ���� ����
	InitUserInfo();
	while (true){
		mainInfo.terminate = false;
		InitConnect();


		DWORD recvID;
		DWORD execID;
		recvThread = CreateThread(NULL, 0, RecvProc, (LPVOID)1, 0, &recvID);
		execThread = CreateThread(NULL, 0, ExecuteProc, (LPVOID)2, 0, &execID);

		WaitForSingleObject(recvThread, INFINITE);
		WaitForSingleObject(execThread, INFINITE);

		TerminateClient();
		TerminateConnect();
	}
	return 0;
}

void InitUserInfo(){
	int existResult = access("login.dat", 00);
	if (existResult == -1){
		char idBuf[200];
		char passBuf[200];
		cout << "����� ������ �����ϴ�.���̵�� ��й�ȣ�� ������ �ֽʽÿ�." << endl;
		cout << "���̵�: ";
		cin >> idBuf;
		cout << "��й�ȣ: ";
		cin >> passBuf;

		ofstream fileMaker("login.dat");
		fileMaker << idBuf << endl;
		fileMaker << passBuf;
		fileMaker.close();
	}

	ifstream fileReader("login.dat");
	fileReader.getline(mainInfo.userId, 200);
	fileReader.getline(mainInfo.userPass, 200);
	fileReader.close();


	//beacon.dat�� �ڵ������� �����ϴ� ���μ����� �ʿ�
	ifstream beaconInfoReader("beacon.dat");
	char majorStr[200];
	char minorStr[200];
	beaconInfoReader.getline(majorStr, 200);
	beaconInfoReader.getline(minorStr, 200);
	mainInfo.major = atoi(majorStr);
	mainInfo.minor = atoi(minorStr);
	mainInfo.terminate = false;
	mainInfo.hookStart = false;
}

void ErrorHandling(char* str){
	cout << str << endl;
	exit(-1);
}
void CommandExecuter(const char* command){
	char copyCommand[BUF_SIZE];
	char *ptr = NULL;
	strcpy(copyCommand, command);
	copyCommand[strlen(command) - 1] = 0;
	ptr = strtok(copyCommand, "#");
	//�α���
	if (strcmp(ptr, "login") == 0){
		char *id = strtok(NULL, "#");
		char *pass = strtok(NULL, "#");


		if (strcmp(mainInfo.userId, id) == 0 && strcmp(mainInfo.userPass, pass) == 0){
			char major[10], minor[10];
			_itoa(mainInfo.major, major, 10);
			_itoa(mainInfo.minor, minor, 10);

			char buf[100] = "auth#";
			strcat(buf, major);
			strcat(buf, "#");
			strcat(buf, minor);
			strcat(buf, "\r\n");
			send(hClntSock, buf, sizeof(buf), 0);
		}
		else{
			char buf[100] = "no#";
			strcat(buf, "\r\n");

			send(hClntSock, buf, sizeof(buf), 0);
		}
	}
	//��������
	else if (strcmp(ptr, "exit") == 0){
		WaitForSingleObject(hMutex, INFINITE);
		mainInfo.terminate = true;
		ReleaseMutex(hMutex);
	}
	//��ŷ ����
	else if (strcmp(ptr, "start") == 0){
		cout << "start Command accept" << endl;
		WaitForSingleObject(hMutexHook, INFINITE);
		if (!mainInfo.hookStart) {
			mainInfo.hookStart = true;
			DWORD hookThreadID;
			hookThread = CreateThread(NULL, 0, ExecuteHook, (LPVOID)1, 0, &hookThreadID);
		}
		ReleaseMutex(hMutexHook);
	}

	else if (strcmp(ptr, "terminate") == 0){
		cout << "terminate Command accept" << endl;
		WaitForSingleObject(hMutexHook, INFINITE);
		mainInfo.hookStart = false;
		ReleaseMutex(hMutexHook);;
	}
	else if (strcmp(ptr, "processList") == 0){
		if (hProcess != NULL){
			CloseHandle(hProcess);
			hProcess = NULL;
		}
		
		pe32 = { 0 };
		hProcess = CreateToolhelp32Snapshot(TH32CS_SNAPPROCESS, 0);
		pe32.dwSize = sizeof(PROCESSENTRY32);
		if (Process32First(hProcess, &pe32)){
			int count = 0;
			do{
				WCHAR buf[100];
				
				wcscpy(buf, pe32.szExeFile);
				WCHAR *ptr = wcstok(buf, L"\0");
				count++;
				int nSize = wcslen(ptr) * 2 +1;
				char *str = new char[nSize];
				wcstombs(str, ptr, nSize);
				
				if (count == 0 || count == 1 || count == 2){
					
					delete[] str;
					continue;
				}
				
				strcat(str, "#\r\n");
				send(hClntSock, str, strlen(str), 0);
				delete[] str;
			} while (Process32Next(hProcess, &pe32));
		}
		char buf[50] = "end#";
		strcat(buf, "\r\n");
		send(hClntSock, buf, strlen(buf), 0);
		CloseHandle(hProcess);
	}
	else if (strcmp(ptr, "processEnd") == 0){
		char *processName = strtok(NULL, "#");
		if (hProcess != NULL){
			CloseHandle(hProcess);
			hProcess = NULL;
		}

		pe32 = { 0 };
		hProcess = CreateToolhelp32Snapshot(TH32CS_SNAPPROCESS, 0);
		pe32.dwSize = sizeof(PROCESSENTRY32);
		if (Process32First(hProcess, &pe32)){
			int count = 0;
			do{
				WCHAR buf[100];

				wcscpy(buf, pe32.szExeFile);
				WCHAR *ptr = wcstok(buf, L"\0");
				count++;
				
				int nSize = wcslen(ptr) * 2 + 1;
				char *str = new char[nSize];
				wcstombs(str, ptr, nSize);
				if (count == 0 || count == 1 || count == 2){

					delete[] str;
					continue;
				}
				if (stricmp(str, processName) == 0){
					HANDLE hTerminate = NULL;
					if (hTerminate = OpenProcess(PROCESS_TERMINATE, FALSE, pe32.th32ProcessID)){
						TerminateProcess(hTerminate, 0);
						CloseHandle(hTerminate);
						delete[] str;
						break;
					}
				}
				delete[] str;
			} while (Process32Next(hProcess, &pe32));
		}
		CloseHandle(hProcess);
	}
	else{
		cout << "Wrong Command" << endl;
	}
}

void InitConnect(){
	//���� ����
	//���ؽ� ������ �����Ѵ�.
	cout << "Welcome" << endl;
	hMutex = CreateMutex(NULL, FALSE, NULL);
	hMutexHook = CreateMutex(NULL, FALSE, NULL);
	if (WSAStartup(MAKEWORD(2, 2), &wsaData) != 0){
		ErrorHandling("Init wsa failed");
	}
	memset(&servAdr, 0, sizeof(servAdr));
	servAdr.sin_family = AF_INET;
	servAdr.sin_addr.s_addr = htonl(INADDR_ANY);
	servAdr.sin_port = htons(PORT_NUM);

	hServSock = socket(PF_INET, SOCK_STREAM, 0);

	if (bind(hServSock, (SOCKADDR*)&servAdr, sizeof(servAdr)) == SOCKET_ERROR) {
		ErrorHandling("bind() error");
	}
	if (listen(hServSock, 5) == SOCKET_ERROR){
		ErrorHandling("listen() error");
	}

	int szClntAdr = sizeof(clntAdr);

	hClntSock = accept(hServSock, (SOCKADDR*)&clntAdr, &szClntAdr);
	cout << "accpet ok" << endl;

	if (hClntSock == INVALID_SOCKET) {
		ErrorHandling("invalid client socket");
	}
}
void TerminateConnect(){
	//���α׷� ����� ���� ��ũ ����
	//���ؽ� ��ȯ�� ����
	CloseHandle(hMutex);
	CloseHandle(hMutexHook);
	closesocket(hServSock);
	WSACleanup();
}
void TerminateClient(){
	closesocket(hClntSock);
}

DWORD WINAPI RecvProc(LPVOID lpParam){
	while (!mainInfo.terminate){
		char *tempCommandBuffer = new char[BUF_SIZE];
		int recvByte = recv(hClntSock, tempCommandBuffer, BUF_SIZE, 0);
		if (recvByte > 0){
			tempCommandBuffer[recvByte] = 0;
			//Mutex ����ȭ ����
			WaitForSingleObject(hMutex, INFINITE);
			inputQ.push_back(tempCommandBuffer);
			ReleaseMutex(hMutex);
		}
		else{
			delete[] tempCommandBuffer;
		}
	}
	return 0;
}

DWORD WINAPI ExecuteProc(LPVOID lpParam){
	while (!mainInfo.terminate){
		//����� ���� ���
		WaitForSingleObject(hMutex, INFINITE);
		if (inputQ.size() > 0){
			//Mutext ����ȭ ����

			for (int i = inputQ.size() - 1; i > -1; i--){
				CommandExecuter(inputQ[i]);
				delete[] inputQ[i];
				inputQ.pop_back();
			}
		}
		ReleaseMutex(hMutex);
	}
	return 0;
}

DWORD WINAPI ExecuteHook(LPVOID lpParam){
	HMODULE hDll = NULL;
	PFN_HOOKSTART KeyBoardHookStart = NULL;
	PFN_HOOKSTOP KeyBoardHookStop = NULL;
	PFN_MOUSESTART MouseHookStart = NULL;
	PFN_MOUSESTOP MouseHookStop = NULL;
	LPCWSTR dir = L"KeyBoardHooker.dll";
	hDll = LoadLibrary(dir);

	if (hDll == NULL){
		cout << "No Linked DLL" << endl;
	}
	LPCSTR setKeyBoard = "SetKeyBoardHook";
	LPCSTR killKeyBoard = "KillKeyBoardHook";
	LPCSTR setMouseHook = "SetMouseHook";
	LPCSTR killMouseHook = "KillMouseHook";
	KeyBoardHookStart = (PFN_HOOKSTART)GetProcAddress(hDll, setKeyBoard);
	if (KeyBoardHookStart == NULL){
		cout << "keyboard hook not found" << endl;
	}
	POINT mousePoint;
	GetCursorPos(&mousePoint);

	KeyBoardHookStop = (PFN_HOOKSTOP)GetProcAddress(hDll, killKeyBoard);

	MouseHookStart = (PFN_MOUSESTART)GetProcAddress(hDll, setMouseHook);


	MouseHookStop = (PFN_MOUSESTOP)GetProcAddress(hDll, killMouseHook);

	KeyBoardHookStart();
	MouseHookStart();
	STARTUPINFO si = { 0, };
	PROCESS_INFORMATION pi;
	si.cb = sizeof(si);
	ZeroMemory(&pi, sizeof(pi));
	wchar_t command[20] = L"LockProgram.exe";
	BOOL state = CreateProcess(NULL, command, NULL, NULL, TRUE, 0, NULL, NULL, &si, &pi);
	while (1){
		WaitForSingleObject(hMutexHook, INFINITE);
		if (!mainInfo.hookStart){
			break;
		}
		ReleaseMutex(hMutexHook);
	}
	TerminateProcess(pi.hProcess, 0);
	CloseHandle(pi.hProcess);
	CloseHandle(pi.hThread);
	KeyBoardHookStop();
	MouseHookStop();
	FreeLibrary(hDll);
	//Ŀ�ǵ� ��ɾ� ����
	return 0;
}
