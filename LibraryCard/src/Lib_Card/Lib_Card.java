
package Lib_Card;

import javacard.framework.*;
import javacard.security.*;
import javacardx.crypto.*;
import javacard.security.KeyBuilder;
import javacardx.apdu.ExtendedLength;
import javacardx.apdu.*;
import javacardx.framework.math.BigNumber;
public class Lib_Card extends Applet
{
	final static byte LIB_CLA = (byte)0xA4;
	final static short MAX_KEY_LENGTH = 128;
	final static short MAX_DATA_LENGTH = 64;
	
	final static short INS_INIT  = (byte)0x11;
	final static short INS_CHECKPIN = (byte)0x12;
	final static short INS_UNBLOCK_CARD = (byte)0x13;
	final static short INS_CHANGE_PIN = (byte)0x14;
	final static short INT_SET_INFO = (byte)0x15;
	final static short INT_SHOW_INFO = (byte)0x16;
	final static short INS_CHECK_CARD = (byte)0x17;
    final static short INS_UPLOAD_IMG = (byte)0x18;
    final static short INS_GET_IMG = (byte)0x19;
    final static short INS_GET_PUBKEY = (byte)0x1A;
    final static short INS_SIGN = (byte)0x1B;
    final static short INS_PAY = (byte)0x1C;
    final static short INS_GET_CARDID = (byte)0x1D;
    final static short INS_CHECK_DEFAULT_PIN = (byte)0x05;

    private byte[] id;
    private byte[] name;
    private byte[] address;
    private byte[] phone;
    byte[] pin;
    private static byte[] tempBuffer, subBuffer, changeBuffer, sigBuffer;
    private static byte[] add = {0x03};
    private static short id_len, name_len,phone_len,address_len,pin_len, image_len, counter, initcard;

    private static byte[] avatar;
	private static byte[] avatarBuffer;
	private static short len_avatar;
	private static final short MAX_AVATAR_SIZE = (short)(4096);

    private MessageDigest sha;

    private static final short aesBlock = (short)16;
	private static AESKey aesKey;
	private static Cipher cipher;
	private static short keyLen;
	private byte [] keyData;
	
    private static RSAPrivateKey rsaPri;
	private static RSAPublicKey rsaPub;
	private static byte x;
	private static byte[] rsaPubKey, rsaPriKey,tempPriKey;
	private static Signature rsaSig;
	private static short sigLen,rsaPubKeyLen, rsaPriKeyLen;
	//random
	private byte[] seed;
	private RandomData ranData;

	private static boolean block_card = false;
    private static byte[] default_pin = {0x31, 0x32, 0x33, 0x34};
    private byte[] logic;
    byte[] hashed_pin;

	protected Lib_Card(){
		id = new byte[aesBlock];
        genCardID();
        name = new byte[(short)(aesBlock * 3)];
        address = new byte[(short) (aesBlock * 5)];
        phone = new byte[aesBlock];
        pin = new byte[32];
        default_pin = new byte[]{0x31, 0x32, 0x33, 0x34};
        pin = default_pin;

        avatar = new byte[MAX_AVATAR_SIZE];

        tempBuffer = JCSystem.makeTransientByteArray((short) 128, JCSystem.CLEAR_ON_DESELECT);
        subBuffer = JCSystem.makeTransientByteArray((short) 128, JCSystem.CLEAR_ON_DESELECT);
        changeBuffer = JCSystem.makeTransientByteArray((short) 128, JCSystem.CLEAR_ON_DESELECT);
        tempPriKey = JCSystem.makeTransientByteArray((short) (128 * 2), JCSystem.CLEAR_ON_DESELECT);

        sha = MessageDigest.getInstance(MessageDigest.ALG_SHA, false);

        keyLen = (short) (KeyBuilder.LENGTH_AES_256 / 8);
        keyData = new byte[keyLen]; //256
		hashed_pin = new byte[keyLen];
        cipher = Cipher.getInstance(Cipher.ALG_AES_BLOCK_128_ECB_NOPAD, false);
        aesKey = (AESKey)KeyBuilder.buildKey(KeyBuilder.TYPE_AES,(short)(8*keyLen), false); //256
        
        sigLen = (short)(KeyBuilder.LENGTH_RSA_1024/8); //128
        rsaSig = Signature.getInstance(Signature.ALG_RSA_SHA_PKCS1, false);

        rsaPubKey = new byte[(short) (2*sigLen)]; //128 *2
        rsaPriKey = new byte[(short) (2*sigLen)]; 
        sigBuffer = new byte[(short) (2*sigLen)];
        rsaPubKeyLen = (short) 0;
        rsaPriKeyLen = (short) 0;

		register();
	}
	public static void install(byte[] bArray, short bOffset, byte bLength) 
	{
		new Lib_Card().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
	}

	public void process(APDU apdu)
	{
		if (selectingApplet())
		{
			return;
		}
		byte[] buf = apdu.getBuffer();
        short len = apdu.setIncomingAndReceive();
		switch (buf[ISO7816.OFFSET_INS])
		{
            case INS_INIT:
                init_card(apdu, len);
                break;
            case INS_CHECKPIN:
                check_pin(apdu, len);
                break;
            case INS_UNBLOCK_CARD:
                unblock_card(apdu, len);
                break;
            case INS_CHANGE_PIN:
                change_pin(apdu, len);
                break;
            case INT_SET_INFO:
                change_info(apdu, len);
                break;
            case INT_SHOW_INFO:
                show_info(apdu);
                break;
            case INS_CHECK_CARD:
                check_card(apdu);
                break;
            case INS_UPLOAD_IMG:
                upload_img(apdu, len);
                break;
            case INS_GET_IMG:
                get_img(apdu, len);
                break;
            case INS_GET_PUBKEY:
                get_pubkey(apdu, len);
                break;
            case INS_SIGN:
                signHandler(apdu, len);
                break;
            case INS_GET_CARDID:
                getCardID(apdu);
                break;
			case INS_CHECK_DEFAULT_PIN:
				verifyPin(apdu, len);
				break;
            default:
                ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
		
	}
    private void genRsaKeyPair(APDU apdu)
    {
        byte[] buffer = apdu.getBuffer();
        KeyPair keyPair = new KeyPair(KeyPair.ALG_RSA, (short) (sigLen * 8));
        keyPair.genKeyPair();
        JCSystem.beginTransaction();
        rsaPubKeyLen = 0;
        rsaPriKeyLen = 0;
        JCSystem.commitTransaction();

        RSAPublicKey pubKey = (RSAPublicKey)keyPair.getPublic();
        short pubKeyLen = 0;

        pubKeyLen += pubKey.getModulus(rsaPubKey, pubKeyLen); //N
        pubKeyLen += pubKey.getExponent(rsaPubKey, pubKeyLen); //E

        short priKeyLen = 0;
        RSAPrivateKey priKey = (RSAPrivateKey)keyPair.getPrivate();

        priKeyLen += priKey.getModulus(rsaPriKey, priKeyLen); //N
        priKeyLen += priKey.getExponent(rsaPriKey, priKeyLen); //D

        JCSystem.beginTransaction();
        rsaPubKeyLen = pubKeyLen;
        rsaPriKeyLen = priKeyLen;
        JCSystem.commitTransaction();

        JCSystem.requestObjectDeletion();

    }

    private void get_pubkey(APDU apdu, short len)
    {
        byte[] buffer = apdu.getBuffer();
        short offset = (short) 128;
        switch (buffer[ISO7816.OFFSET_P1])
		{
			case (byte) 0x01 :
				Util.arrayCopy(rsaPubKey, (short) 0, buffer, (short) 0, offset);
				apdu.setOutgoingAndSend((short) 0, offset);
				break;
			case (byte) 0x02 :
				short eLen = (short) (rsaPubKeyLen - offset);
				Util.arrayCopy(rsaPubKey, offset, buffer, (short) 0, eLen);
				apdu.setOutgoingAndSend((short) 0, eLen);
				break;
			default:
				ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
		}
    }

    private void init_card(APDU apdu, short len)
    {
        short flag1, flag2, flag3, flag4, flag5; //id, name, address, phone, pin
        flag1 = flag2 = flag3 = flag4 = flag5 = 0;
        byte[] buffer = apdu.getBuffer();
        for (short i = 0; i <(short)(len+5); i++)
        {
            if(buffer[i] == (byte)0x03)
            {
                if(flag1 == 0)
                {
                    flag1 = i;
                    id_len = (short)(flag1-5);
                }
                else if(flag2 == 0)
                {
                    flag2 = i;
                    name_len = (short)(flag2 - (short)(flag1+1));
                }
                else if(flag3 == 0)
                {
                    flag3 = i;
                    address_len = (short)(flag3 - (short)(flag2+1));
                   
                }
                else if(flag4 == 0)
                {
                    flag4 = i;
                    phone_len = (short)(flag4 - (short)(flag3+1));
                    
					pin_len = (short)(5 + (short)(len - (short)(flag4 + 1)));
                }
            }
        }

        genRsaKeyPair(apdu); 
        sha.reset();
        short ret = sha.doFinal(buffer,(short)(flag4+1), pin_len, tempBuffer,(short)0);
        
        Util.arrayCopy(tempBuffer, (short)0, hashed_pin, (short)0, (short)32);
        Util.arrayCopy(tempBuffer, (short)0, keyData, (short)0, (short)32);
        

        aesKey.setKey(keyData, (short)0);
        cipher.init(aesKey, Cipher.MODE_ENCRYPT);
        Util.arrayCopy(buffer, (short) (0x05), tempBuffer, (short) 0, id_len);
		cipher.doFinal(tempBuffer,(short)0,(short)aesBlock,id,(short) 0);        
		
		Util.arrayCopy(buffer, (short) (flag1 + 1), tempBuffer, (short) 0, name_len);
        cipher.doFinal(tempBuffer,(short) 0,(short)(aesBlock*3), name,(short) 0); 
        
        Util.arrayCopy(buffer, (short) (flag2 + 1), tempBuffer, (short) 0, address_len);
        cipher.doFinal(tempBuffer,(short) 0,(short)(aesBlock*5) ,address,(short) 0);

        Util.arrayCopy(buffer, (short) (flag3 + 1), tempBuffer, (short) 0, phone_len);
        cipher.doFinal(tempBuffer,(short) 0,aesBlock ,phone,(short) 0); 
        

        Util.arrayCopy(rsaPriKey, (short) 0, tempPriKey, (short) 0, rsaPriKeyLen);
        cipher.doFinal(tempPriKey,(short) 0,(short)(2*sigLen) ,rsaPriKey,(short) 0);
       
        
        initcard = 1;
    }

    private void show_info(APDU apdu)
    {
        byte[] buffer = apdu.getBuffer();
        apdu.setOutgoing();
        cipher.init(aesKey, Cipher.MODE_DECRYPT);
        apdu.setOutgoingLength((short)(id_len+(short)(name_len + (short)(phone_len+(short)(address_len+3)))));

        cipher.doFinal(id, (short) 0, aesBlock, tempBuffer, (short) 0);
        apdu.sendBytesLong(tempBuffer, (short) 0, id_len);
        apdu.sendBytesLong(add, (short) 0, (short) 1);

        cipher.doFinal(name, (short) 0, (short)(aesBlock*3), tempBuffer, (short) 0);
        apdu.sendBytesLong(tempBuffer, (short) 0, name_len);
        apdu.sendBytesLong(add, (short) 0, (short) 1);

        cipher.doFinal(address, (short) 0, (short)(aesBlock*5), tempBuffer, (short) 0);
        apdu.sendBytesLong(tempBuffer, (short) 0, address_len);
        apdu.sendBytesLong(add, (short) 0, (short) 1);

        cipher.doFinal(phone, (short) 0, aesBlock, tempBuffer, (short) 0);
        apdu.sendBytesLong(tempBuffer, (short) 0, phone_len);


    }

    private void getCardID(APDU apdu)
    {
        byte[] buffer = apdu.getBuffer();
        apdu.setOutgoing();
        cipher.init(aesKey, Cipher.MODE_ENCRYPT);
        apdu.setOutgoingLength((short)id_len);
        cipher.doFinal(id, (short) 0, aesBlock, tempBuffer, (short) 0);
        apdu.sendBytesLong(tempBuffer, (short) 0, id_len);
    }

    private void change_info(APDU apdu, short len)
    {
        short flag1, flag2, flag3; //name, address, phone
        flag1 = flag2 = flag3 = 0;
        byte[] buffer = apdu.getBuffer();
        Util.arrayFillNonAtomic(tempBuffer,(short)0,len,(byte) 0x00);
        Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, tempBuffer, (short) 0, len);
       
        for (short i = 0; i < len; i++) { 
            if (tempBuffer[i] == (byte) 0x03) {
                if (flag1 == 0) {
                    flag1 = i;
                    name_len = flag1;
                } 
                else if (flag2 == 0) {
                    flag2 = i;
                    address_len = (short)(flag2 - (short) (flag1 + 1));
                } 
                else if (flag3 == 0) {
                    flag3 = i;
                    phone_len = (short)(flag3 - (short) (flag2 + 1));
                }
            }
        }
        
        cipher.init(aesKey, Cipher.MODE_ENCRYPT);
        
        Util.arrayCopy(tempBuffer, (short) 0, changeBuffer, (short) 0, name_len);
        Util.arrayFillNonAtomic(name,(short)0,(short)(aesBlock*3),(byte) 0x00);
        cipher.doFinal(changeBuffer,(short) 0,(short)(aesBlock*3),name,(short) 0); 
        
        Util.arrayCopy(tempBuffer, (short) (flag1 + 1), changeBuffer, (short) 0, address_len);
        Util.arrayFillNonAtomic(address,(short)0,(short)(aesBlock*5),(byte) 0x00);
        cipher.doFinal(changeBuffer,(short) 0,(short)(aesBlock*5),address,(short) 0);  

        Util.arrayCopy(tempBuffer, (short) (flag2 + 1), changeBuffer, (short) 0, phone_len);
        Util.arrayFillNonAtomic(phone,(short)0,aesBlock,(byte) 0x00);
        cipher.doFinal(changeBuffer,(short) 0,aesBlock,phone,(short) 0);
    }

    private void check_pin(APDU apdu, short len) //logic 0A 0 1 2
    {
        byte[] buffer = apdu.getBuffer();
        apdu.setOutgoing();
        apdu.setOutgoingLength((short) 1);

        short ret = sha.doFinal(buffer, ISO7816.OFFSET_CDATA, len, subBuffer, (short) 0);
        if (Util.arrayCompare(subBuffer, (short) 0, pin, (short) 0, (short) 32) == 0)
        { 
            apdu.sendBytesLong(logic, (short) 2, (short) 1); //1 if pin true
            counter = 0;
        }
        else
        {
            counter++;
            if (counter >= 3)
            {
                block_card = true;
                apdu.sendBytesLong(logic, (short) 3, (short) 1); //2 if block card
            }
            else
            {
                logic[0]=(byte)(0x08-counter);
                apdu.sendBytesLong(logic, (short) 0, (short) 1); //0 if pin false
            }
        }
        
    }

    private void check_card(APDU apdu)
    {
        byte[] buffer = apdu.getBuffer();
        apdu.setOutgoing();
        apdu.setOutgoingLength((short) 1);
        if(initcard == 1)
        {
            apdu.sendBytesLong(logic, (short) 2, (short) 1); //1 if card init
        }
        else
        {
            apdu.sendBytesLong(logic, (short) 1, (short) 1); //0 if card not init
        }
    }
	
    private void unblock_card(APDU apdu, short len)
    {
        byte[] buffer = apdu.getBuffer();
        if (Util.arrayCompare(buffer, ISO7816.OFFSET_CDATA, default_pin, (short) 0, (short) len) == 0)
        {
            //short r = sha.doFinal(default_pin,(short)0x00, (short)0x06, tempBuffer, (short)0);
			Util.arrayCopy(tempBuffer, (short) 0, pin, (short) 0, (short)32);
			counter=0;
			block_card=false;
            apdu.sendBytesLong(logic, (short)2, (short) 1);
        }
        else
        {
            apdu.sendBytesLong(logic, (short)1, (short) 1);
        }
    }

    private void change_pin(APDU apdu, short len)
    {
        short flag1;
        flag1 = 0;
        short oldlenpin = 0; //oldpin 0x03 pinnew
        apdu.setOutgoing();

        byte[] buffer = apdu.getBuffer();
        for (short i = 5; i < (short)(len+5); i++)
        {
            if (buffer[i] == (byte)0x03)
            {
                flag1 = i;
                oldlenpin = (short)(flag1-5);
                pin_len = (short)(len + (short)(5-(short)(flag1+1)));
            }
        
        }
        short retold = sha.doFinal(buffer, (short)0x05, (short) oldlenpin, tempBuffer, (short)0);
        if (Util.arrayCompare(tempBuffer, (short)0, pin, (short)0, (short)32) == 0)
        {
            short ret = sha.doFinal(buffer, (short)(flag1+1), (short)pin_len, tempBuffer, (short)0); //hash new pin
            Util.arrayCopy(tempBuffer, (short)0, pin, (short)0, (short)32);
        }
        else
        {
            ISOException.throwIt(ISO7816.SW_WRONG_DATA);
        }

    }
// private void upload_img(APDU apdu, short len) {
    // byte[] buffer = apdu.getBuffer();
    // short dataLength = apdu.getIncomingLength();

    // // Khi to mng avatar và kim tra kích thc
    // Util.arrayFillNonAtomic(avatar, (short) 0, (short) MAX_AVATAR_SIZE, (byte) 0x00);
    
    // // Nu d liu quá ln, s gây li
    // if (dataLength > MAX_AVATAR_SIZE) {
        // ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
    // }

    // short dataOffset = apdu.getOffsetCdata();
    // short pointer = 0;

    // // Lp qua các gói d liu
    // while (len > 0) {
        // // Sao chép d liu vào mng avatar t buffer
        // Util.arrayCopyNonAtomic(buffer, dataOffset, avatar, pointer, len);

        // // Cp nht ch s pointer  lu tr d liu k tip
        // pointer += len;

        // // Nhn gói tip theo
        // len = apdu.receiveBytes(dataOffset);
    // }

    // // Cp nht  dài d liu ã nhn
    // len_avatar = (short) pointer;

    // // Gi d liu ã nhn v th hoc ng dng ngoài
    // apdu.setOutgoing();

    // // Thit lp chiu dài d liu gi
    // Util.setShort(buffer, (short) 0, len_avatar);
    // apdu.setOutgoingLength(len_avatar);

    // // Gi các byte ca avatar
    // apdu.sendBytesLong(avatar, (short) 0, len_avatar);
// }
private void upload_img(APDU apdu, short len)
    {
        byte[] buffer = apdu.getBuffer();
        short dataLength = apdu.getIncomingLength();
        Util.arrayFillNonAtomic(avatar, (short) 0, (short) MAX_AVATAR_SIZE, (byte) 0x00);
        if (dataLength > MAX_AVATAR_SIZE)
        {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
        short dataOffset = apdu.getOffsetCdata();
        short pointer = 0;
        while (len > 0){
            Util.arrayCopyNonAtomic(buffer, dataOffset, avatar, pointer, len);
            pointer += len;
            len = apdu.receiveBytes(dataOffset);
        }
        len_avatar = (short) pointer;

        apdu.setOutgoing();
        Util.setShort(buffer, (short) 0, len_avatar);
        apdu.setOutgoingLength((short) 5);
        apdu.sendBytes((short) 0, (short) 5);
    }



   private void get_img(APDU apdu, short len) {
    // // Kim tra nu cha có nh nào c lu
    // if (len_avatar == (short) 0) {
        // ISOException.throwIt((short) 0x6A32); // Không có nh
    // }

    // byte[] buffer = apdu.getBuffer();
    // short toSend = len_avatar;  //  dài d liu cn gi
    // short le = apdu.setOutgoing();  // Ly giá tr LE t APDU

    // // Thit lp  dài d liu s gi
    // apdu.setOutgoingLength((short) Math.min(toSend, le));  // m bo không vt quá LE

    // short sendLen = 0;
    // short pointer = 0;

    // // Lp qua tng phn d liu cn gi
    // while (toSend > 0) {
        // sendLen = (short) Math.min(toSend, le);  //  dài gói d liu cn gi
        // apdu.sendBytesLong(avatar, pointer, sendLen);  // Gi phn d liu
        // toSend -= sendLen;  // Cp nht  dài d liu còn li
        // pointer += sendLen;  // Cp nht ch s  gi phn tip theo
    // }
}

    private void signHandler(APDU apdu, short len)
    {
        byte[] buffer = apdu.getBuffer();
        short flag = 0;
        short lenPinReq = 0;
        short lenRandom = 0;
        Util.arrayCopy (buffer, ISO7816.OFFSET_CDATA, tempBuffer, (short) 0, len);
        for (short i = 0; i < len; i++)
        {
            if (tempBuffer[i] == (byte) 0x03)
            {
                flag = i;
                lenPinReq = (short) (flag - 5);
                lenRandom = (short) (len - (short) (flag + 1));
            }
        }
        short ret = sha.doFinal(tempBuffer, (short) 0, lenPinReq, subBuffer, (short) 0);
        if (Util.arrayCompare(subBuffer, (short) 0, pin, (short) 0, (short) 32) == 0)
        {
            cipher.init(aesKey, Cipher.MODE_ENCRYPT);
            cipher.doFinal(rsaPriKey, (short) 0, (short) (2 * sigLen), tempPriKey, (short) 0);
            short offset = (short) 128;
            RSAPrivateKey priKey = (RSAPrivateKey) KeyBuilder.buildKey(KeyBuilder.TYPE_RSA_PRIVATE, (short)(8*sigLen), false);
            priKey.setModulus(sigBuffer, (short) 0, offset);
            priKey.setExponent(sigBuffer, offset, offset);

            rsaSig.init(priKey, Signature.MODE_SIGN);
            rsaSig.sign(tempBuffer, (short)0, len, sigBuffer, (short)0);
            apdu.setOutgoing();
            apdu.setOutgoingLength((short) sigLen);
            apdu.sendBytesLong(sigBuffer, (short) 0, (short) sigLen);
        }
        else
        {
            ISOException.throwIt((short) 0x6A12);
        }
    }

	private void genCardID()
	{
		byte[] cardIDInit = {(byte)0x4B, (byte)0x4D, (byte)0x41, (byte)0x54, (byte)0x56, (byte)0x30, (byte)0x31};
		short idLen = (short) cardIDInit.length;
		Util.arrayCopy(cardIDInit, (short)0, id, (short)0, idLen);
	}
	private void verifyPin(APDU apdu, short len)
	{
		byte[] buf = apdu.getBuffer();
        short dataOffset = ISO7816.OFFSET_CDATA;
        short lc = buf[ISO7816.OFFSET_LC];
        if (lc <= 0 || lc > (short) (buf.length - dataOffset)) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
        byte[] text = new byte[lc];
        short attempts = (short)0;
        Util.arrayCopyNonAtomic(buf, dataOffset, text, (short) 0, lc);
        if (Util.arrayCompare(text, (short)0, default_pin, (short)0, (short)4) == 0){
	        attempts = 0;
	        apdu.setOutgoingAndSend((short)0, (short)0);
        }
        else{
	        attempts++;
	        if(attempts >= 3){
		        ISOException.throwIt((short) 0x6A82);
	        }
	        else{
		        ISOException.throwIt((short) 0x63C0);
	        }
        }

	}
}