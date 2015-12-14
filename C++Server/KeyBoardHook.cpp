#include <Windows.h>
#include <windowsx.h>
HINSTANCE hlnDll;
#pragma data_seg("HookProc")

HHOOK hKBHook = NULL;
HHOOK hMSHook = NULL;
HWND ghServerWnd = NULL;
HCURSOR hOldCursor= NULL;
#pragma data_seg()

#pragma comment(linker,"/SECTION:HookProc,RWS")

char TempBuf[256];

LRESULT CALLBACK KBHookProc(int nCode, WPARAM wParam, LPARAM lParam);
LRESULT CALLBACK MSHookProc(int nCode, WPARAM wParam, LPARAM lParam);

BOOL APIENTRY DllMain(HANDLE hModule, DWORD ul_reason_for_call, LPVOID lpReserved){
	if (ul_reason_for_call == DLL_PROCESS_ATTACH){
		hlnDll = (HINSTANCE)hModule;
	}
	return true;
}

extern "C" __declspec(dllexport)void SetKeyBoardHook(){
	hKBHook = NULL;
	hKBHook = SetWindowsHookEx(WH_KEYBOARD, KBHookProc, hlnDll, NULL);
	if (hKBHook == NULL){
		LPCWSTR msg = L"unable in keyboard";
		LPCWSTR error = L"error";
		MessageBox(NULL, msg, error, MB_OK);
	}
}

extern "C" __declspec(dllexport)void KillKeyBoardHook(){
	UnhookWindowsHookEx(hKBHook);
	hKBHook = NULL;
}

extern "C" __declspec(dllexport)void SetMouseHook(){
	hMSHook = NULL;
	hMSHook = SetWindowsHookEx(WH_MOUSE, MSHookProc, hlnDll, NULL);
	
	if (hMSHook == NULL){
		LPCWSTR msg = L"unable in mouse";
		LPCWSTR error = L"error";
		MessageBox(NULL, msg, error, MB_OK);
	}

}

extern "C" __declspec(dllexport)void KillMouseHook(){
	UnhookWindowsHookEx(hMSHook);
}

LRESULT CALLBACK KBHookProc(int nCode, WPARAM wParam, LPARAM lParam){
	if (nCode >= 0){
		wParam = 0;
		lParam = 0;
		nCode = 0;
		return 1;
	}
	
	if (nCode == HC_ACTION){
		KBDLLHOOKSTRUCT *kbhook = (KBDLLHOOKSTRUCT*)lParam;
		if (kbhook->flags & LLKHF_ALTDOWN){
			switch (kbhook->vkCode)
			{
			case VK_TAB:
			case VK_F4:
				wParam = 0;
				lParam = 0;
				nCode = 0;
			}
		}
		else{
			switch (kbhook->vkCode)
			{
			case VK_LWIN:
			case VK_RWIN:
				wParam = 0;
				lParam = 0;
				nCode = 0;
			}
		}
	}

	return CallNextHookEx(hKBHook, nCode, wParam, lParam);
}

LRESULT CALLBACK MSHookProc(int nCode, WPARAM wParam, LPARAM lParam){
	if (nCode >= 0)
	{
		if (wParam == WM_LBUTTONDOWN || wParam == WM_RBUTTONDOWN){
			nCode = 0;
			wParam = 0;
			lParam = 0;

			return 1;
		}

	}
	return CallNextHookEx(hKBHook, nCode, wParam, lParam);
}