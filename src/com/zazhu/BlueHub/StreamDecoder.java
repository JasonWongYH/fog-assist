/*
   JavaBluetoothGateway
   Copyright (C) 2011:
         Zack Zhu, Wearable Computing Laboratory, ETH Zurich

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
 * StreamDecoder.java
 * 
 */

package com.zazhu.BlueHub;

import java.util.Observer;

/**
 * Defines the interface a StreamDecoder needs to support
 * 
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 * @adaptor Zack Zhu <zack.zhu@ife.ee.ethz.ch>
 */
public interface StreamDecoder {

   /**
    * read() is called by the stream reader, such as a BTConnection when it has 
    * received a byte. This data should be decoded by a StreamDecoder.
    * 
    * @param b the bytes read from the stream.
    * @param numBytes the number of bytes in b
    */
   public void read(byte[] b, int numBytes);
   
   /*
    * Registers an observer that will receive the decoded data packets. 
    * Recommended use is to use an Observable superclass.
    */
   public void addObserver(Observer obs);
   
}
