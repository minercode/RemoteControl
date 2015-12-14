
//include opencv headers
#include <opencv2\opencv.hpp>
#include <opencv2\highgui.hpp>
#include <Windows.h>



void main(void){
	int width = GetSystemMetrics(SM_CXSCREEN);
	int height = GetSystemMetrics(SM_CYSCREEN);
	int caption_height = GetSystemMetrics(SM_CYCAPTION);
	IplImage *image = cvLoadImage("catLock.jpg");
	while (true){

		cvNamedWindow("Lock", 0);
		cvResizeWindow("Lock", width, height);
		cvMoveWindow("Lock", -8, -8 - caption_height);
		cvShowImage("Lock", image);

		cvWaitKey(0);
		cvReleaseImage(&image);
	}
	cvDestroyWindow("Lock");
	return;
}


