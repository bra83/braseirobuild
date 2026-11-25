package br.com.tutorclinico;

import java.io.*;
import java.util.function.BooleanSupplier;

/** Reads only fsynced encrypted frames published by the capture writer.
 * No PCM queue, no duplicate full recording, bounded read-ahead. */
final class LiveSpoolInput extends InputStream {
    static final class Boundary {
        private long published;
        private boolean closed;
        synchronized void publish(long bytes,boolean finished){
            if(bytes<published)throw new IllegalArgumentException("Spool boundary regressed");
            published=bytes;closed=finished;notifyAll();
        }
        synchronized long availableFrom(long pos,BooleanSupplier active) throws IOException{
            while(pos>=published&&!closed&&active.getAsBoolean()){
                try{wait(100);}catch(InterruptedException e){Thread.currentThread().interrupt();throw new InterruptedIOException();}
            }
            return active.getAsBoolean()?Math.max(0,published-pos):0;
        }
    }
    private final RandomAccessFile file;
    private final Boundary boundary;
    private final BooleanSupplier active;
    private long pos;
    LiveSpoolInput(File path,Boundary boundary,BooleanSupplier active) throws IOException{
        this.file=new RandomAccessFile(path,"r");this.boundary=boundary;this.active=active;
    }
    @Override public int read() throws IOException{
        byte[] one=new byte[1];return read(one,0,1)<0?-1:one[0]&255;
    }
    @Override public int read(byte[] out,int off,int len) throws IOException{
        if(len==0)return 0;
        long available=boundary.availableFrom(pos,active);
        if(available==0)return -1;
        file.seek(pos);int n=file.read(out,off,(int)Math.min(len,available));
        if(n<0)throw new EOFException("Published encrypted frame missing");
        pos+=n;return n;
    }
    long finalHeaderLength() throws IOException{
        long old=file.getFilePointer();
        try{file.seek(24);long bytes=0;for(int i=0;i<8;i++)bytes|=(long)file.readUnsignedByte()<<(8*i);return bytes;}
        finally{file.seek(old);}
    }
    @Override public void close() throws IOException{file.close();}
}
