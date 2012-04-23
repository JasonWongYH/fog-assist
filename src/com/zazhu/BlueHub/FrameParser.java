/*
   JavaBluetoothGateway
   Copyright (C) 2009:
         Clemens Lombriser and Daniel Roggen, Wearable Computing Laboratory, ETH Zurich

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/

/*
Contributions:
-Added packet checksum capabilities
Zack Zhu, Wearable Computing Laboratory, ETH Zurich
*/

/*
 * FrameParser.java
 *
 */

package com.zazhu.BlueHub;

import java.io.InvalidClassException;
import java.util.BitSet;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;
import java.util.Arrays;
import java.lang.Object;

import android.util.Log;

/**
 * This parser decodes a byte stream from the bluetooth sensor modules. This 
 * class follows the implementation of FrameParser2 of the DScope project by 
 * Daniel Roggen.
 * 
 *	A frame-based data stream has the following format:
 *
 *	<FRAMEHEADER><data0><data1><data2><data3>...(<CHECKSUM><FRAMEFOOTER>)
 *
 *	With :
 *		FRAMEHEADER:	A set of characters delimiting the start of the frame.
 *		data0...n		A set of payload data, each of which may be in a different data format.
 *		CHECKSUM:		Typically a frame checksum to ensure - Implemented by Zack Zhu
 *		FRAMEFOOTER:	Not implemented - optional - delimits frame end - The checksum or a longer header can play this role.
 *
 *		The 'format' string defines the frame header and data format as follows:
 *			format: "FRAMEHEADER;<data0><data1><data2>..."
 *		The frame header is delimited by a semicolon from the definition of the data types <data0><data1>....
 *		Data types have the following format:
 *			<datan>: [-]cÂ¦sÂ¦SÂ¦iÂ¦I
 *			-:		indicates that the next item is signed; by default unsigned is assumed.	
 *			c:		8-bit character
 *			s:		16-bit short, little endian
 *			S:		16-bit short, big endian
 *			i:		32-bit int, little endian
 *			I:		32-bit int, big endian
 * 
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch> Zack Zhu <zack.zhu@ife.ee.ethz.ch>
 */
public class FrameParser extends Observable implements StreamDecoder {

   private String m_sensorname; // the sensor name
   private String [] m_channels; // the names of the individual channels
   private String m_format; // contains the format of the frame
   
   // internal datatype representations - these are NOT the ones of the format string
   private static final char CHAR_8BIT_SIGNED                   ='c'; // encoded as  c
   private static final char SHORT_16BIT_LITTLE_ENDIAN_SIGNED   ='s'; // encoded as  s
   private static final char SHORT_16BIT_BIG_ENDIAN_SIGNED      ='S'; // encoded as  S
   private static final char INT_32BIT_LITTLE_ENDIAN_SIGNED     ='i'; // encoded as  i
   private static final char INT_32BIT_BIG_ENDIAN_SIGNED        ='I'; // encoded as  I
   private static final char CHAR_8BIT_UNSIGNED                 ='d'; // encoded as -c
   private static final char SHORT_16BIT_LITTLE_ENDIAN_UNSIGNED ='t'; // encoded as -s
   private static final char SHORT_16BIT_BIG_ENDIAN_UNSIGNED    ='T'; // encoded as -S
   private static final char INT_32BIT_LITTLE_ENDIAN_UNSIGNED   ='j'; // encoded as -i
   private static final char INT_32BIT_BIG_ENDIAN_UNSIGNED      ='J'; // encoded as -I
   
   //checksum
   private static final char CHAR_8BIT_CHECKSUM					='x'; // encoded as x
   private static final char SHORT_16BIT_CHECKSUM_LITTLE_ENDIAN ='f'; // encoded as f
   private static final char SHORT_16BIT_CHECKSUM_BIG_ENDIAN	='F'; // encoded as F
   
   private boolean m_CheckSum = false;
   
   private char m_CheckSumType;
   
   private byte[] m_frame;  // keeps the frame data
   private byte[] m_header; // keeps the header format
   
   private int m_error; // stores an error, ind case it occurred. Possible values are:
   public static final int ERROR_NONE = 0;
   public static final int ERROR_FRAMEFORMAT = 1;
   public static final int ERROR_CHECKSUMFORMAT = 2;
   private int mFrameBytes;
   
   public FrameParser(String sensorname, String format, String [] channels) {
      setFormat(format);
      m_sensorname = sensorname;
      m_channels = channels;
   }
   
   /**
    * Returns the last error that occurred.
    * @return one of the FrameParser.ERROR_* values
    */
   public int getLastError() {
      return m_error;
   }
   
   public int getFrameSizeWithoutCheckSum() {
	   if (!m_CheckSum) {
		   return m_format.length();
	   } else {
		   switch (m_CheckSumType) {
		   case CHAR_8BIT_CHECKSUM:
			   return m_format.length()-1;
		   case SHORT_16BIT_CHECKSUM_LITTLE_ENDIAN:
			   return m_format.length()-2;
		   case SHORT_16BIT_CHECKSUM_BIG_ENDIAN:
			   return m_format.length()-2;
		   default:
			   Log.e("FrameParser","Unknown checksum type in getFrameSizeWithoutCheckSum");
			   return 0;
		   }
	   }
   }
   /**
    * Parses the format string and initializes buffers
    */
   private void setFormat(String format) {
      
	  
      // get header
      int header_sep = format.indexOf(";");
      
      
      if (header_sep == -1 || header_sep == 0) {
         System.out.println("FrameParser: format header type invalid");
         m_error = ERROR_FRAMEFORMAT;
         return;
      }
      
    //int checksum_sep = format.lastIndexOf(";");
      int checksum_sep = format.indexOf(";", header_sep+1);
      
      if (checksum_sep != -1 && checksum_sep != 0) {
    	  m_CheckSum = true;
      }
      
      mFrameBytes = 0;
      
      m_header = format.substring(0,header_sep).getBytes();
      mFrameBytes += m_header.length;
      
      boolean bSigned=false;
      m_format = "";
      
      for (int i=header_sep+1; i<checksum_sep; i++) {
         char type = format.charAt(i);
         
         switch(type) {
            case '-':
               if (i==format.length()-1) {
                  System.out.println("FrameParser: found '-' without following type");
                  m_error = ERROR_FRAMEFORMAT;
                  return;
               }
               bSigned=true;
               continue; // no break here to avoid bSigned=false statement
            case 'c':
               m_format += bSigned? CHAR_8BIT_SIGNED : CHAR_8BIT_UNSIGNED;
               mFrameBytes += 1;
               break;
            case 's':
               m_format += bSigned? SHORT_16BIT_LITTLE_ENDIAN_SIGNED : SHORT_16BIT_LITTLE_ENDIAN_UNSIGNED;
               mFrameBytes += 2;
               break;
            case 'S':
               m_format += bSigned? SHORT_16BIT_BIG_ENDIAN_SIGNED : SHORT_16BIT_BIG_ENDIAN_UNSIGNED;
               mFrameBytes += 2;
               break;
            case 'i':
               m_format += bSigned? INT_32BIT_LITTLE_ENDIAN_SIGNED : INT_32BIT_LITTLE_ENDIAN_UNSIGNED;
               mFrameBytes += 4;
               break;
            case 'I':
               m_format += bSigned? INT_32BIT_BIG_ENDIAN_SIGNED : INT_32BIT_BIG_ENDIAN_UNSIGNED;
               mFrameBytes += 4;
               break;       	
            default:
               System.out.println("FrameParser: Unknown type in format");
               m_error = ERROR_FRAMEFORMAT;
               return;
         }
         
         bSigned = false;
         
      } // for i
      
      for (int i=checksum_sep+1; i<format.length(); i++) {
    	  char type = format.charAt(i);
    	  
    	  switch(type) {
    	  case 'x':
          	m_CheckSumType = CHAR_8BIT_CHECKSUM;
          	m_format += CHAR_8BIT_CHECKSUM;
          	mFrameBytes += 1;
          	break;
          case 'f':
          	m_CheckSumType = SHORT_16BIT_CHECKSUM_LITTLE_ENDIAN;
          	m_format += SHORT_16BIT_CHECKSUM_LITTLE_ENDIAN;
          	mFrameBytes += 2;
          	break;
          case 'F':
          	m_CheckSumType = SHORT_16BIT_CHECKSUM_BIG_ENDIAN;
          	m_format += SHORT_16BIT_CHECKSUM_BIG_ENDIAN;
          	mFrameBytes += 2;
          	break;
          default:
        	  System.out.println("FrameParser: Unknown type in checksum");
              m_error = ERROR_CHECKSUMFORMAT;
              return;
    	  }
      }
      //Log.i("FrameParser", "FrameParser: format read successfully: " + m_format);
      
      // initialize data buffers
      //Log.i("FrameSize" Integer.toString(framebytes));
      m_frame = new byte[mFrameBytes];
   }
   /**
    * Converts a signed byte to its unsigned BitSet representation
    * This is done so that the unsigned checksum can be correctly calculated
    * @param oneByte
    * @return
    */
//   public BitSet byteToBitset(byte [] bytes){
//	   BitSet bits = new BitSet();
//	   bits.
//	   //main loop for setting all the bits
//	   for (int i = 0; i < bytes.length * 8; i++){
//		   // Check to see if the byte is positive or negative
//		   if (bytes[i] < 0 ) {
//			   //negative case: change it to positive 
//			   
//			   
//		   } else {
//			   //positive case
//		   }
//	   }
//	   // Check to see if the byte is positive or negative
//	   
//		   
//		   // positive
//		   BitSet oneBitSet = new BitSet();
//		   
//		   String byteString = Integer.toBinaryString(oneByte);
//		   for (int i = 0; i<byteString.length(); i++) {
//			   if (byteString.charAt(i) == 1) {
//				   oneBitSet.set(i, true);
//			   } else {
//				   oneBitSet.set(i, false);				   
//			   }
//		   }
//		   return oneBitSet;
//	   }
//	   // Convert the byte to string
//	   
//	   
//	   
//   }
   // somehow this piece of code from the wikipedia doesn't work... look at it later since it's so nice and compact 
   public byte calculateLRC(byte[] data) {
       byte checksum = 0;
       for (int i = 0; i <= data.length - 1; i++) {
    	   Log.i("LRC", Integer.toString(data[i]));
           checksum = (byte) ((checksum + data[i]) & 0xFF);
       }
       checksum = (byte) (((checksum ^ 0xFF) + 1) & 0xFF);
       Log.i("LRC", "LRC Checksum: " + Integer.toString(checksum));
       return checksum;
   }  
   
   public static BitSet fromByteToBitSet(byte oneByte) {
	   BitSet bits = new BitSet();
	
	   for (int i = 0; i < 8; i++) {
		   if ((oneByte & (1 << i) ) > 0) {
			   bits.set(i);
		   }
	   }
	   //System.out.println("Byte: " + oneByte + " is converted to " + bits);
	   return bits;
   }
   /**
    * Reports bytes read from a stream. This function will try to synchronize to 
    * establish a frame, which is then decoded.
    * 
    * @param b the byte read.
    */
   public void read(byte[] bytes, int numBytes) {
      //System.out.println("Byte array in read: " + bytes.toString());
      for (int i=0; i<numBytes; i++) {
    	  
	  
         // shift through the data byte by byte
         shiftFrame(bytes[i]);
         //Log.i("CounterI", "Header bytes: " + Integer.toString(bytes[i]));
         
         if ( ! isHeaderLeading() ) continue;

         // remember time the packet has been detected
         Date recDate = new Date();
         
         // no need to check to see if we have a full frame
         // because if the headers are detected at the beginning of 
         // m_frame, then we must have pushed enough into the FIFO queue
         // such that we have a full frame         
         
         // we got a frame - decode it by the format we have stored

         
         Vector framedata = new Vector(m_format.length());
         
         
         BitSet payload = new BitSet();
         BitSet check = new BitSet();
         
         // Code for using calculateLRC function, but it doesn't seem to work...
         //byte[] frameWithoutCheckSum = new byte[m_frame.length-1];
         //System.arraycopy(m_frame, 0, frameWithoutCheckSum, 0, m_frame.length-1); 
         //L og.i("LRC", "CheckSum: " + Integer.toString(m_frame[m_frame.length-1]));
         //if ((byte) calculateLRC(frameWithoutCheckSum). == m_frame[m_frame.length-1]) Log.i("LRC", "LRC PASSED!");
         payload.set(0,7,false);
         //payload.xor(fromByteToBitSet(m_frame[0]));
         //payload.xor(fromByteToBitSet(m_frame[1]));
         //payload.xor(fromByteToBitSet(m_frame[2]));
         
         int framepos = m_header.length;
         int length = (!m_CheckSum)?m_format.length():getFrameSizeWithoutCheckSum();
         
         // TODO: Move the checksum parts from below up here and exit without parsing if the checksum doesn't match
         for (int c = 0; c<m_frame.length-1; c++) {
        	 payload.xor(fromByteToBitSet(m_frame[c]));
         }
         
         byte sensorCheck = (new Byte((byte)m_frame[m_frame.length-1]));
         check = fromByteToBitSet(sensorCheck);
         //Log.i("CheckSum", "Calculations from the payload: " + payload + 
         //		 " CheckSum Transmitted: " + sensorCheck);
         
         if (!payload.equals(check)) {
        	 Log.i("CheckSum", "Payload didn't match!");
        	 continue;
         }
        // Log.i("xx", "m_CheckSum= " + m_CheckSum + " m_format length is: "
        //		 + m_format.length() + " while length is selected as: " + length);
        
         
         for(int j=0; j<length; j++) {
        	 
        	
            switch(m_format.charAt(j)) {
            	
               case CHAR_8BIT_SIGNED:
            	  
            	  //Log.i("CounterI", "m_frame at " + framepos + " is " + Integer.toString(m_frame[framepos]));
            	  //payload.xor(fromByteToBitSet(m_frame[framepos]));
                  framedata.add(new Character((char)m_frame[framepos++]));
                  break;
               case SHORT_16BIT_LITTLE_ENDIAN_SIGNED: {
            	   //Log.i("CounterI", "m_frame at " + framepos + " is " + Integer.toString(m_frame[framepos]));
            	   //payload.xor(fromByteToBitSet(m_frame[framepos]));
                  short a = (short) ((short)0xFF & (short)m_frame[framepos++]);
                  //Log.i("CounterI", "m_frame at " + framepos + " is " + Integer.toString(m_frame[framepos]));
                  //payload.xor(fromByteToBitSet(m_frame[framepos]));
                  short b = (short) ((short)0xFF & (short)m_frame[framepos++]);
                  framedata.add(new Short( (short)(a | (b<<8) ))  );
                  break;
               }
               case SHORT_16BIT_BIG_ENDIAN_SIGNED: {
            	   //Log.i("CounterI", "m_frame at " + framepos + " is " + Integer.toString(m_frame[framepos]));
            	   // payload.xor(fromByteToBitSet(m_frame[framepos]));
            	  short a = (short) ((short)0xFF & (short)m_frame[framepos++]);
            	 // Log.i("CounterI", "m_frame at " + framepos + " is " + Integer.toString(m_frame[framepos]));
            	   //payload.xor(fromByteToBitSet(m_frame[framepos]));
            	  short b = (short) ((short)0xFF & (short)m_frame[framepos++]);
                  framedata.add(new Short( (short)( b | (a<<8) ) )  );
                  break;
               }
               case INT_32BIT_LITTLE_ENDIAN_SIGNED: {
            	   //Log.i("CounterI", "m_frame at " + framepos + " is " + Integer.toString(m_frame[framepos]));
            	   // payload.xor(fromByteToBitSet(m_frame[framepos]));
            	  short a = (short) ((short)0xFF & (short)m_frame[framepos++]);
            	  //Log.i("CounterI", "m_frame at " + framepos + " is " + Integer.toString(m_frame[framepos]));
            	   //payload.xor(fromByteToBitSet(m_frame[framepos]));
                  short b = (short) ((short)0xFF & (short)m_frame[framepos++]);
                  //Log.i("CounterI", "m_frame at " + framepos + " is " + Integer.toString(m_frame[framepos]));
                  // payload.xor(fromByteToBitSet(m_frame[framepos]));
                  short c = (short) ((short)0xFF & (short)m_frame[framepos++]);
                 // Log.i("CounterI", "m_frame at " + framepos + " is " + Integer.toString(m_frame[framepos]));
                  // payload.xor(fromByteToBitSet(m_frame[framepos]));
                  short d = (short) ((short)0xFF & (short)m_frame[framepos++]);
                  framedata.add(new Integer(  a | (b<<8) | (c<<16) | (d<<24)  )  );
                  break;
               }
               case INT_32BIT_BIG_ENDIAN_SIGNED: {
            	 //   payload.xor(fromByteToBitSet(m_frame[framepos]));
                  short a = (short) ((short)0xFF & (short)m_frame[framepos++]);
                 // payload.xor(fromByteToBitSet(m_frame[framepos]));
                  short b = (short) ((short)0xFF & (short)m_frame[framepos++]);
                 // payload.xor(fromByteToBitSet(m_frame[framepos]));
                  short c = (short) ((short)0xFF & (short)m_frame[framepos++]);
                 // payload.xor(fromByteToBitSet(m_frame[framepos]));
                  short d = (short) ((short)0xFF & (short)m_frame[framepos++]);
                  framedata.add(new Integer(  d | (c<<8) | (b<<16) | (a<<24)  )  );
                  break;
               }
               case CHAR_8BIT_UNSIGNED:
            	//  Log.i("CounterI", "m_frame at " + framepos + " is " + Integer.toString(m_frame[framepos]));
            	//  payload.xor(fromByteToBitSet(m_frame[framepos]));
                  framedata.add(new Character((char)m_frame[framepos++]));
                  break;
               case SHORT_16BIT_LITTLE_ENDIAN_UNSIGNED: {
            	//  Log.i("CounterI", "m_frame at " + framepos + " is " + Integer.toString(m_frame[framepos]));
            	//  payload.xor(fromByteToBitSet(m_frame[framepos]));
                  short a = (short) ((short)0xFF & (short)m_frame[framepos++]);
                //  Log.i("CounterI", "m_frame at " + framepos + " is " + Integer.toString(m_frame[framepos]));
               //   payload.xor(fromByteToBitSet(m_frame[framepos]));
                  short b = (short) ((short)0xFF & (short)m_frame[framepos++]);
                  framedata.add(new Short( (short)( a | (b<<8) ) )  );
                  break;
               }
               case SHORT_16BIT_BIG_ENDIAN_UNSIGNED: {
            	//  Log.i("CounterI", "m_frame at " + framepos + " is " + Integer.toString(m_frame[framepos]));
            	//  payload.xor(fromByteToBitSet(m_frame[framepos]));
                  short a = (short) ((short)0xFF & (short)m_frame[framepos++]);
               //   Log.i("CounterI", "m_frame at " + framepos + " is " + Integer.toString(m_frame[framepos]));
               //   payload.xor(fromByteToBitSet(m_frame[framepos]));
                  short b = (short) ((short)0xFF & (short)m_frame[framepos++]);
                  framedata.add(new Short( (short)( b | (a<<8) ) )  );
                  break;
               }
               case INT_32BIT_LITTLE_ENDIAN_UNSIGNED: {
            //	   Log.i("CounterI", "m_frame at " + framepos + " is " + Integer.toString(m_frame[framepos]));
            //	    payload.xor(fromByteToBitSet(m_frame[framepos]));
                  short a = (short) ((short)0xFF & (short)m_frame[framepos++]);
             //     Log.i("CounterI", "m_frame at " + framepos + " is " + Integer.toString(m_frame[framepos]));
             //      payload.xor(fromByteToBitSet(m_frame[framepos]));
                  short b = (short) ((short)0xFF & (short)m_frame[framepos++]);
             //     Log.i("CounterI", "m_frame at " + framepos + " is " + Integer.toString(m_frame[framepos])); 
             //      payload.xor(fromByteToBitSet(m_frame[framepos]));
                  short c = (short) ((short)0xFF & (short)m_frame[framepos++]);
             //     Log.i("CounterI", "m_frame at " + framepos + " is " + Integer.toString(m_frame[framepos])); 
             //      payload.xor(fromByteToBitSet(m_frame[framepos]));
                  short d = (short) ((short)0xFF & (short)m_frame[framepos++]);
                  framedata.add(new Integer(  a | (b<<8) | (c<<16) | (d<<24)  )  );
                  break;
               }
               case INT_32BIT_BIG_ENDIAN_UNSIGNED: {
           // 	  Log.i("CounterI", "m_frame at " + framepos + " is " + Integer.toString(m_frame[framepos])); 
            //	   payload.xor(fromByteToBitSet(m_frame[framepos]));
            	  short a = (short) ((short)0xFF & (short)m_frame[framepos++]);
              //    Log.i("CounterI", "m_frame at " + framepos + " is " + Integer.toString(m_frame[framepos]));
              //     payload.xor(fromByteToBitSet(m_frame[framepos]));
                  short b = (short) ((short)0xFF & (short)m_frame[framepos++]);
              //    Log.i("CounterI", "m_frame at " + framepos + " is " + Integer.toString(m_frame[framepos]));
               //    payload.xor(fromByteToBitSet(m_frame[framepos]));
                  short c = (short) ((short)0xFF & (short)m_frame[framepos++]);
                //  Log.i("CounterI", "m_frame at " + framepos + " is " + Integer.toString(m_frame[framepos]));
               //    payload.xor(fromByteToBitSet(m_frame[framepos]));
                  short d = (short) ((short)0xFF & (short)m_frame[framepos++]);
                  
                  framedata.add(new Integer(  d | (c<<8) | (b<<16) | (a<<24)  )  );
                  break;
               }
//               // Parse the checksum
//               case CHAR_8BIT_CHECKSUM: {
//            	   
//               }
               default:
            	  
                  System.out.println("PANIC: Encountered unknown symbol " + m_format.charAt(j) +
                		  " frame in frame format. The frame so far is: " + framedata.toString() + " Exiting");
                  System.exit(-1);
               
            }
            // For a format string of mmFormat = "DX9;cs-s-s-ssss;x";
        	// The framedata vector has 9 positions (as defined above): 8 for the data entries and 1 for checksum
        	
         } // for j
         
         // test the checksum before outputting
         //Log.i("CheckSum", "m_format length = " + m_format.length() + " m_frame length = " + m_frame.length);
         
         
//         if (m_CheckSum) {
//        	 Log.i("CheckSum", "Detected checksum type of " + m_CheckSumType);
//        	 switch (m_CheckSumType) {
//        	 case SHORT_8BIT_CHECKSUM:
//        		
//				payload = checksum(m_frame);
//				
//        		 check = m_frame[m_format.length()-1];
//        		 break;
//        	 case SHORT_16BIT_CHECKSUM_LITTLE_ENDIAN:
//        		 payload = fletcher(framedata,getFrameSizeWithoutCheckSum());
//        		 break;
//        	 case SHORT_16BIT_CHECKSUM_BIG_ENDIAN:
//        		 payload = fletcher(framedata,getFrameSizeWithoutCheckSum());
//        		 break;
//        	 default:
//        		 Log.e("FrameParser", "Error! Unknown checksum type detected");
//        		 continue;
//        	 }
//         }
         
         
         
         // create packet
         DataPacket dp = new DataPacket(recDate, framedata, m_sensorname, m_channels);
         //Log.i("FrameParser", "Frame break: " + dp.toString());
         
         
      
         
         
         setChanged();
         notifyObservers(dp);
         
         
      } // for i
      
   }
   
private int fletcher(Vector framedata, int frameSizeWithoutCheckSum) {
	// TODO Auto-generated method stub
	return 0;
}

//private int checksum(Vector obj){
//	
//	int lrc = 0;
//	for (int i=2; i<obj.size(); i++){
//		int value;
//		if (obj.elementAt(i) instanceof Character) {
//			value = (int) ((Character)obj.elementAt(i)).charValue();
//		} else if (obj.elementAt(i) instanceof Short) {
//			value = (int) ((Short)obj.elementAt(i)).shortValue();
//		} if (obj.elementAt(i) instanceof Integer) {
//			value = (int) ((Integer)obj.elementAt(i)).intValue();
//		} else {
//			throw new InvalidClassException("Unkown data type within DataPacket value");
//		}
//		lrc
//		Log.i("CheckSumFunction", "Xoring " + Integer.toString(lrc) + " and " + framedata.elementAt(i));
//		
//		lrc = lrc ^ (byte) framedata.elementAt(i);
//		//lrc = new Short((short) ((short)0xFF & (short) lrc));		
//		
//	}
//	return lrc;
//}





private void shiftFrame(byte b) {
	  
      for(int i=0; i<m_frame.length-1;i++) {
         m_frame[i] = m_frame[i+1];
      }
      m_frame[m_frame.length-1] = b;
   }
   
   /**
    * Checks whether m_frame starts with a 
    * @return
    */
   private boolean isHeaderLeading() {
      for(int i=0; i<m_header.length; i++) {
         if (m_header[i] != m_frame[i]) {
            return false;
         }
      }
      return true;
   }

   /**
    * Adds an observer that will received the decoded frames.
    * 
    * @param obs the obserer interested in the resulting frames.
    */
//   public void addObserver(Observer obs) {
//      super.addObserver(obs);
//  }
   
   /**
    * This is a test function for the class
    * @param args
    */
   public static void main(String [] args) {
      String format = "DX4;cs-s-s-s-s-s-s-s-s-s-s-s";

      byte [] data = {'D','X','4',
                      'a', // character
                      (byte)0x80, 0x01, // unsigned short, little endian
                      0x01, (byte)0x80, // signed short, little endian
                      0x01, 0x02, 
                      0x01, 0x00,
                      0x00, 0x01, 
                      0x00, 0x01,
                      0x02, 0x03, 
                      0x04, 0x05,
                      0x06, 0x07, 
                      0x08, 0x09,
                      0x0a, 0x0b, 
                      0x0c, 0x0d,
                     };
      
      
      System.out.println("Running FrameParser test");
      System.out.println("Passing format: " + format);
      
      String [] channels = {"1","2","3","4","5","6","7","8","9","10","11","12","13"};
      FrameParser fp = new FrameParser("ch_test",format,channels);
      
      fp.addObserver(new Observer() {
         public void update(Observable o, Object arg) {
            System.out.println( ((DataPacket)arg));
         }
      });

      System.out.println("Sending data");
      fp.read(data, 28);
      
   }

}
