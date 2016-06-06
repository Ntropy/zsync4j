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
package com.salesforce.zsync.internal;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import com.salesforce.zsync.http.ContentRange;
import com.salesforce.zsync.internal.util.HttpClient.RangeReceiver;
import com.salesforce.zsync.internal.util.ReadableByteBuffer;
import com.salesforce.zsync.internal.util.TransferListener;
import com.salesforce.zsync.internal.util.TransferListener.ResourceTransferListener;
import com.salesforce.zsync.internal.util.ZsyncUtil;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.List;

public class OutputFileWriter implements RangeReceiver, Closeable {

    // immutable state
    private final File path;
    private final File tempPath;

    private final int blockSize;
    private final int lastBlockSize;
    private final long length;
    private final String sha1;
    private final long mtime;
    private final List<BlockSum> blockSums;
    private final ListMultimap<BlockSum, Integer> positions;
    // mutable state
    private final FileChannel channel;
    private final boolean[] completed;
    private int blocksRemaining;
    private TransferListener listener;

    public OutputFileWriter(File path, ControlFile controlFile, ResourceTransferListener<File> listener)
            throws IOException {
        this.path = path;
        this.listener = listener;

        final Header header = controlFile.getHeader();
        this.blockSize = header.getBlocksize();
        this.length = header.getLength();
        this.lastBlockSize = (int) (this.length % this.blockSize == 0 ? this.blockSize : this.length % this.blockSize);
        this.sha1 = header.getSha1();
        this.mtime = header.getMtime().getTime();

        listener.start(this.path, this.length);

        final String tmpName = path.getName().toString() + ".part";
        final File parent = path.getParentFile();
        if (parent != null) {
            if (!parent.isDirectory()) {
                parent.mkdirs();
            }
            this.tempPath = new File(parent, tmpName);
        } else {
            this.tempPath = new File(tmpName);
        }
        //this.channel = FileChannel.open(this.tempPath, CREATE, WRITE, READ);
        this.channel = new RandomAccessFile(this.tempPath, "rw").getChannel();


        this.blockSums = ImmutableList.copyOf(controlFile.getBlockSums());
        this.positions = indexPositions(this.blockSums);
        this.completed = new boolean[this.blockSums.size()];
        this.blocksRemaining = this.completed.length;
    }

    static ListMultimap<BlockSum, Integer> indexPositions(List<BlockSum> blockSums) {
        final ImmutableListMultimap.Builder<BlockSum, Integer> b = ImmutableListMultimap.builder();
        for (int i = 0; i < blockSums.size(); i++) {
            b.put(blockSums.get(i), i);
        }
        return b.build();
    }

    public int getNumBlocks() {
        return this.blockSums.size();
    }

    public BlockSum get(int index) {
        return this.blockSums.get(index);
    }

    public List<Integer> getPositions(BlockSum sum) {
        return this.positions.get(sum);
    }

    public boolean writeBlock(int position, ReadableByteBuffer data) {
        return this.writeBlock(position, data, 0);
    }

    public boolean writeBlock(int position, ReadableByteBuffer data, int offset) {
        if (this.completed[position]) {
            return false;
        }
        final int l = position == this.completed.length - 1 ? this.lastBlockSize : this.blockSize;
        try {
            this.channel.position(position * this.blockSize);
            data.write(this.channel, offset, l);
            this.listener.transferred(l);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read block at position " + position, e);
        }
        this.blocksRemaining--;
        return this.completed[position] = true;
    }

    public List<ContentRange> getMissingRanges() {
        final ImmutableList.Builder<ContentRange> b = ImmutableList.builder();
        long start = -1;
        for (int i = 0; i < this.completed.length; i++) {
            if (this.completed[i]) {
                // if we're in a range, end it
                if (start != -1) {
                    b.add(new ContentRange(start, i * this.blockSize - 1));
                    start = -1;
                }
            } else {
                // if we're not in a range, start one
                if (start == -1) {
                    start = i * this.blockSize;
                }
                // if this is the last block in the file map, we need to end the range
                if (i == this.completed.length - 1) {
                    b.add(new ContentRange(start, this.length - 1));
                }
            }
        }
        return b.build();
    }

    public boolean isComplete() {
        return this.blocksRemaining == 0;
    }

    @Override
    public void receive(ContentRange range, InputStream in) throws IOException {
        if (range.first() % this.blockSize != 0) {
            throw new RuntimeException("Invalid range received: first byte not block aligned");
        }
        if ((range.last() + 1) % this.blockSize != 0 && range.last() + 1 != this.length) {
            throw new RuntimeException("Invalid range received: last byte not block aligned");
        }

        final ReadableByteChannel src = Channels.newChannel(in);
        final long length = range.length();
        long remaining = length;
        long position = range.first();
        do {
            long transferred = this.channel.transferFrom(src, position, length);
            remaining -= transferred;
            position += transferred;
            this.listener.transferred(transferred);
        } while (remaining > 0);

        final int first = (int) (range.first() / this.blockSize);
        final int last =
                (int) (range.last() + 1 == this.length ? this.completed.length - 1 : (range.last() + 1) / this.blockSize - 1);
        for (int i = first; i <= last; i++) {
            if (!this.completed[i]) {
                this.blocksRemaining--;
                this.completed[i] = true;
            }
        }
    }

    @Override
    public void close() throws IOException {
        try {
            this.channel.position(0); // reset channel to beginning to compute full SHA1
            String calculatedSha1 = ZsyncUtil.computeSha1(this.channel);
            if (!this.sha1.equals(calculatedSha1)) {
                throw new ChecksumValidationIOException(this.sha1, calculatedSha1);
            }
            try {
                //Files.move(this.tempPath, this.path, REPLACE_EXISTING, ATOMIC_MOVE);
                this.tempPath.renameTo(this.path);
            } catch (Exception e) {
                e.printStackTrace();
            }
//            catch (AtomicMoveNotSupportedException e) {
//                Files.move(this.tempPath, this.path, REPLACE_EXISTING);
//            }
            //Files.setLastModifiedTime(this.path, fromMillis(this.mtime));
            this.path.setLastModified(this.mtime);
        } finally {
            this.channel.close();
            this.listener.close();
        }
    }

}
