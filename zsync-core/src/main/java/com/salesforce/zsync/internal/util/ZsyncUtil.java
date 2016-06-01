/**
 * Copyright (c) 2015, Salesforce.com, Inc. All rights reserved.
 * <p>
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 * <p>
 * Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 * <p>
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions
 * and the following disclaimer in the documentation and/or other materials provided with the
 * distribution.
 * <p>
 * Neither the name of Salesforce.com nor the names of its contributors may be used to endorse or
 * promote products derived from this software without specific prior written permission.
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.salesforce.zsync.internal.util;

import org.apache.mina.proxy.utils.MD4;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;

public class ZsyncUtil {

    private static final char[] HEX_CODE = "0123456789abcdef".toCharArray();
    private static final Provider md4Provider;

    static {
        md4Provider = new Provider("MD4Provider", 1d, "implements md4") {
            private static final long serialVersionUID = 6386613936557154160L;
        };
        md4Provider.put("MessageDigest.MD4", MD4.class.getName());
    }

    public static String toHexString(ByteBuffer buffer) {
        final StringBuilder r = new StringBuilder(buffer.remaining() * 2);
        while (buffer.hasRemaining()) {
            final byte b = buffer.get();
            r.append(HEX_CODE[(b >> 4) & 0xF]);
            r.append(HEX_CODE[(b & 0xF)]);
        }
        return r.toString();
    }

    public static int computeRsum(byte[] block) {
        short a = 0;
        short b = 0;
        for (int i = 0, l = block.length; i < block.length; i++, l--) {
            final short val = unsigned(block[i]);
            a += val;
            b += l * val;
        }
        return toInt(a, b);
    }

    public static int toInt(short x, short y) {
        return (x << 16) | (y & 0xffff);
    }

    public static long toLong(int x, int y) {
        return (((long) x) << 32) | (y & 0xffffffffL);
    }

    public static short unsigned(byte b) {
        return (short) (b < 0 ? b & 0xFF : b);
    }

    public static String computeSha1(ReadableByteChannel channel) throws IOException {
        final MessageDigest sha1 = newSHA1();
        final ByteBuffer buf = ByteBuffer.allocate(8192);
        while (channel.read(buf) != -1) {
            buf.flip();
            sha1.update(buf);
            buf.clear();
        }
        return toHexString(ByteBuffer.wrap(sha1.digest()));
    }

    public static MessageDigest newMD4() {
        try {
            return MessageDigest.getInstance("MD4", md4Provider);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD4 unavailable");
        }
    }

    public static MessageDigest newSHA1() {
        try {
            return MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-1");
        }
    }


    public static File getFile(URI uri) {
        if (uri == null)
            return null;
        return new File(uri.getPath());
    }

    public static void copy(InputStream in, File target) {
        if (target == null)
            throw new NullPointerException("Target file is null");
        if(target.exists())
            target.delete();

        OutputStream ostream;
    }

}
