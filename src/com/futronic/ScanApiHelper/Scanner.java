/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.futronic.ScanApiHelper;

/**
 *
 * @author slyeung
 */
public class Scanner {
    public native boolean OpenDevice();
    public native boolean CloseDevice();
    public native String GetVersionInfo();
    public native boolean GetImageSize();
    public native boolean IsFingerPresent();
    public native boolean GetFrame(byte[] pImage);
    public native boolean GetImage2(int nDose, byte[] pImage);
    public native boolean SetOptions(int Mask, int Flag);
    public native int GetLastErrorCode();
    public native boolean Save7Bytes(byte[] buffer);
    public native boolean Restore7Bytes(byte[] buffer);
    public native boolean SetNewAuthorizationCode(byte[] SevenBytesAuthorizationCode);
    public native boolean SaveSecret7Bytes(byte[] SevenBytesAuthorizationCode, byte[] buffer);
    public native boolean RestoreSecret7Bytes(byte[] SevenBytesAuthorizationCode, byte[] buffer);
    public native boolean SetDiodesStatus(int GreenDiodeStatus, int RedDiodeStatus );
    public native boolean GetDiodesStatus( byte[] Status ); //2 bytes - 1st:Green, 2nd:Red
    public native boolean SetProperty(int nProperty, int nPropertyData);
    public native boolean GetProperty(int nProperty, int[] PropertyData);  //assign int[1], return the data depends on the nProperty
    //ftrMathAPI
    public native boolean ImageNFIQ(byte[] pImage, int Width, int Height);
    //ftrWSQ
    private native boolean WsqFromRawImage(int nWidth, int nHeight, float fBitrate, byte[] rawImg, byte[] wsqImg);
    private native boolean WsqGetImageParameters(byte[] wsqImg);
    private native boolean WsqToRawImage(byte[] wsqImg, byte[] rawImg);
    // options
    public final int FTR_ERROR_NO_ERROR = 0;
    public final int FTR_OPTIONS_CHECK_FAKE_REPLICA = 0x00000001;
    public final int FTR_OPTIONS_DETECT_FAKE_FINGER = FTR_OPTIONS_CHECK_FAKE_REPLICA;
    public final int FTR_OPTIONS_IMPROVE_IMAGE = 0x00000020; // for PIV compatible devices
    public final int FTR_OPTIONS_INVERT_IMAGE = 0x00000040;

    public final int FTR_LFD_LEVEL_1 = 1; /* default */
    public final int FTR_LFD_LEVEL_2 = 3;
   
    public final int FTR_PROPERTY_LFD_LEVEL =  2;
    public final int FTR_PROPERTY_LFD_SW_1_CALCULATED_DATA = 3;
    public final int FTR_PROPERTY_LFD_SW_1_PARAM = 4;

    // error code
    public final int FTR_ERROR_EMPTY_FRAME = 4306; /* ERROR_EMPTY */
    public final int FTR_ERROR_MOVABLE_FINGER = 0x20000001;
    public final int FTR_ERROR_NO_FRAME = 0x20000002;
    public final int FTR_ERROR_HARDWARE_INCOMPATIBLE = 0x20000004;
    public final int FTR_ERROR_FIRMWARE_INCOMPATIBLE = 0x20000005;
    public final int FTR_ERROR_INVALID_AUTHORIZATION_CODE = 0x20000006;
    public final int FTR_ERROR_WRITE_PROTECT = 19;

    public int GetImageWidth()
    {
        return m_ImageWidth;
    }
    public int GetImaegHeight()
    {
        return m_ImageHeight;
    }
    public int GetImaegNFIQ(byte[] pImage, int Width, int Height)
    {
        if(ImageNFIQ(pImage, Width, Height))
            return m_ImageNFIQ;
        else
        {
            String setErr = GetErrorMessage();
            return 0;
        }
    }
    //wsqImg - assign nWidth*nHeight bytes array
    //the real size of WSQ image will be hold in mWSQ_size after returned from JNIRawToWsqImage.
    public boolean ConvertRawToWsq(int nWidth, int nHeight, float fBitrate, byte[] rawImg, byte[] wsqImg)
    {
        if(rawImg.length != (nWidth*nHeight))
                return false;
        if(wsqImg.length != (nWidth*nHeight))
                return false;
        if( fBitrate > 2.25 || fBitrate < 0.75)
                return false;
        return WsqFromRawImage(nWidth, nHeight, fBitrate, rawImg, wsqImg);			
    }
    
    public int GetWsqImageSize()
    {
        return m_WSQ_size;
    }

    public int GetWsqImageRawSize(byte[] wsqImg)
    {
        if( !WsqGetImageParameters(wsqImg) )
                return 0;
        return (m_WSQ_RawWidth * m_WSQ_RawHeight);
    }
    
    public boolean ConvertWsqToRaw(byte[] wsqImg, byte[] rawImg)
    {
        if(rawImg.length < (m_WSQ_RawWidth * m_WSQ_RawHeight))
                return false;
        return WsqToRawImage(wsqImg, rawImg);			
    }
    
    public String GetErrorMessage()
    {
        int errcode = GetLastErrorCode();
        String strErrMsg;
        switch(errcode)
        {
            case FTR_ERROR_NO_ERROR:
                strErrMsg = "OK";
                break;
            case FTR_ERROR_EMPTY_FRAME:
                strErrMsg = "Empty Frame";
                break;
            case FTR_ERROR_MOVABLE_FINGER:
                strErrMsg = "Moveable Finger";
                break;
            case FTR_ERROR_NO_FRAME:
                strErrMsg = "Fake Finger";
                break;
            case FTR_ERROR_HARDWARE_INCOMPATIBLE:
                strErrMsg = "Hardware Incompatible";
                break;
            case FTR_ERROR_FIRMWARE_INCOMPATIBLE:
                strErrMsg = "Firmware Incompatible";
                break;
            case FTR_ERROR_INVALID_AUTHORIZATION_CODE:
                strErrMsg = "Invalid Authorization Code";
                break;
            case FTR_ERROR_WRITE_PROTECT:
                strErrMsg = "Write Protect";
                break;
            default:
                strErrMsg = String.format("Error code is %d", errcode);
                break;
        }
        return strErrMsg;
    }
    
    static {
    System.loadLibrary("ftrJSDK");
    }
    private int m_ImageWidth;
    private int m_ImageHeight;
    private int m_ImageNFIQ;
    private int m_WSQ_size;
    private int m_WSQ_RawWidth;
    private int m_WSQ_RawHeight;         
}
