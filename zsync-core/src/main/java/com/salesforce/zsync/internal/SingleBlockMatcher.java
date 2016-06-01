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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.salesforce.zsync.internal.util.ReadableByteBuffer;

import java.util.List;
import java.util.Set;

import static com.salesforce.zsync.internal.BlockSum.getRsum;
import static com.salesforce.zsync.internal.SingleBlockMatcher.State.*;
import static com.salesforce.zsync.internal.util.ZsyncUtil.newMD4;

public class SingleBlockMatcher extends BlockMatcher {

    private final int blockSize;
    private final Set<Integer> rsumHashSet;
    private State state;
    private MutableBlockSum blockSum;
    private byte firstByte;
    public SingleBlockMatcher(ControlFile controlFile) {
        final Header header = controlFile.getHeader();
        this.blockSize = header.getBlocksize();
        this.rsumHashSet = ImmutableSet.copyOf(Iterables.transform(controlFile.getBlockSums(), getRsum));
        this.state = INIT;
        this.blockSum = new MutableBlockSum(newMD4(), this.blockSize, header.getRsumBytes(), header.getChecksumBytes());
    }

    @Override
    public int getMatcherBlockSize() {
        return this.blockSize;
    }

    @Override
    public int match(OutputFileWriter targetFile, ReadableByteBuffer buffer) {
        switch (this.state) {
            case INIT:
                this.blockSum.rsum.init(buffer);
                break;
            case MATCHED:
                this.blockSum.rsum.init(buffer);
                break;
            case MISSED:
                this.blockSum.rsum.update(this.firstByte, buffer.get(buffer.length() - 1));
                break;
            default:
                throw new RuntimeException("Unhandled case");
        }

        final int r = this.blockSum.rsum.toInt();
        // cheap negative check followed by more expensive positive check
        if (this.rsumHashSet.contains(r)) {
            // only compute strong checksum if weak matched some block
            this.blockSum.checksum.setChecksum(buffer);
            final List<Integer> matches = targetFile.getPositions(this.blockSum);
            if (!matches.isEmpty()) {
                for (Integer position : matches) {
                    targetFile.writeBlock(position, buffer);
                }
                this.state = MATCHED;
                return this.blockSize;
            }
        }
        this.state = MISSED;
        this.firstByte = buffer.get(0);
        return 1;
    }

    static enum State {
        INIT, MATCHED, MISSED;
    }

}
