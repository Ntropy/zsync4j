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
package com.salesforce.zsync;

import com.salesforce.zsync.Zsync.Options;
import com.salesforce.zsync.http.ContentRange;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;

/**
 * Observes events over the course of a single zsync invocation.
 *
 * @author bstclair
 */
public class ZsyncObserver {

    public void zsyncStarted(URI requestedZsyncUri, Options options) {
    }

    public void controlFileDownloadingInitiated(URI uri) {
    }

    public void controlFileDownloadingStarted(URI uri, long length) {
    }

    public void controlFileDownloadingComplete() {
    }

    public void controlFileReadingStarted(Path path, long length) {
    }

    public void controlFileReadingComplete() {
    }

    public void outputFileWritingStarted(Path outputFile, long length) {
    }

    public void outputFileWritingCompleted() {
    }

    public void inputFileReadingStarted(Path inputFile, long length) {
    }

    public void inputFileReadingComplete() {
    }

    public void remoteFileDownloadingInitiated(URI uri, List<ContentRange> ranges) {
    }

    public void remoteFileDownloadingStarted(URI uri, long length) {
    }

    public void remoteFileRangeReceived(ContentRange range) {
    }

    public void remoteFileDownloadingComplete() {
    }

    public void bytesRead(long bytes) {
    }

    public void bytesDownloaded(long bytes) {
    }

    public void bytesWritten(long bytes) {
    }

    public void zsyncFailed(Exception exception) {
    }

    public void zsyncComplete() {
    }
}
