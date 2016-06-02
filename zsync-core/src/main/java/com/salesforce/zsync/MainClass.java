package com.salesforce.zsync;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MainClass {
    private static final long MEGABYTE = 1024L * 1024L;
    String fileName = "IpsToMatchFull.txt";

    public static long bytesToMegabytes(long bytes) {
        return bytes / MEGABYTE;
    }

    public static void main(String[] args) {

//        Path currentRelativePath = Paths.get("");
//        String s = currentRelativePath.toAbsolutePath().toString();
//        System.out.println("Current relative path is: " + s);

        Runtime runtime = Runtime.getRuntime();
        long memory = runtime.totalMemory() - runtime.freeMemory();
        runtime.gc();
        System.out.println("Used memory1 is megabytes: " + memory);

        MainClass mc = new MainClass();

        for (int i = 0; i < 1; i++) {
            mc.makeServerZsync();

            try {
                mc.downloadZsync();
            } catch (ZsyncException e) {
                e.printStackTrace();
            }
            System.out.println("total memory is megabytes: " + runtime.totalMemory());
            System.out.println("free memory is megabytes: " + runtime.totalMemory());
            memory = runtime.totalMemory() - runtime.freeMemory();
            System.out.println("Used memory is megabytes: " + (memory));
        }

        memory = runtime.totalMemory() - runtime.freeMemory();
        System.out.println("Used memory2 is megabytes: " + (memory));

        runtime.gc();
        memory = runtime.totalMemory() - runtime.freeMemory();
        System.out.println("Used memory3 is megabytes: " + (memory));

        try {
            Thread.sleep(0);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        runtime.gc();
        memory = runtime.totalMemory() - runtime.freeMemory();
        System.out.println("Used memory4 is megabytes: " + (memory));

    }

    public void makeServerZsync() {
        ZsyncMake zsyncmake = new ZsyncMake();
        Path file = Paths.get("/Users/zhiheng/Sites/test/" + fileName);
        Path output = zsyncmake.make(file);
        System.out.println("makeServerZsync output:" + output);

    }

    public void downloadZsync() throws ZsyncException {
        ZsyncStatsObserver observer = new ZsyncStatsObserver();

        Zsync.Options options = new Zsync.Options();
        Zsync zsync = new Zsync();
        URI zsyncFileURI = URI.create("http://localhost/~zhiheng/test/" + fileName + ".zsync");

        File downloaded = zsync.zsync(zsyncFileURI, null, observer);
        System.out.println("downloaded:" + downloaded);
        System.out.println("getTotalBytesDownloaded  :" + observer.build().getTotalBytesDownloaded());
        System.out.println("getBytesDownloadedForControlFile  :" + observer.build().getBytesDownloadedForControlFile());
        System.out.println("getBytesDownloadedFromRemoteFile  :" + observer.build().getBytesDownloadedFromRemoteFile());
        System.out.println("getTotalBytesRead  :" + observer.build().getTotalBytesRead());
        System.out.println("getTotalBytesReadByInputFile  :" + observer.build().getTotalBytesReadByInputFile());
        System.out.println("getTotalBytesWritten  :" + observer.build().getTotalBytesWritten());
        System.out.println("getTotalBytesWrittenByInputFile  :" + observer.build().getTotalBytesWrittenByInputFile());

        System.out.print("Options:" + options);
    }
}
