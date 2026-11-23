package br.com.tutorclinico;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Record first, transcribe after stop. Only the capture thread owns AudioRecord:
 * the UI thread never calls stop/release concurrently with a blocking read.
 *
 * Audio: 16 kHz, mono, 16-bit PCM WAV, app-private temporary cache. Not encrypted
 * independently; use fictitious audio until security review. A recording is
 * deleted after successful transcription or explicit discard, never silently
 * deleted on a processing error.
 */
final class SessionRecorder {
    interface Events {
        void status(String value);
        void ready(File temporaryWav, long durationMs);
        void error(String value);
    }
    private static final int RATE = 16000;
    private static final long MAX_BYTES = 120L * 60L * RATE * 2L;
    static final String PREFIX = "eco_recording_";
    private final Context context;
    private final Events events;
    private final AtomicBoolean recording = new AtomicBoolean(false);
    private volatile Thread worker;

    SessionRecorder(Context context, Events events) {
        this.context=context.getApplicationContext();
        this.events=events;
    }

    boolean isRecording(){return recording.get();}
    synchronized void start(){
        if(worker!=null && worker.isAlive())throw new IllegalStateException("Gravação em andamento");
        recording.set(true);
        worker=new Thread(this::capture, "eco-record-then-transcribe");
        worker.start();
    }
    void finish(){
        recording.set(false); // AudioRecord is stopped/released only by capture thread.
        events.status("FINALIZANDO GRAVAÇÃO · PREPARANDO TRANSCRIÇÃO");
    }

    private void capture(){
        AudioRecord audio=null;
        RandomAccessFile wav=null;
        File file=null;
        long bytes=0;
        boolean good=false;
        try{
            int min=AudioRecord.getMinBufferSize(RATE, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
            if(min<=0)throw new IllegalStateException("Entrada PCM 16kHz indisponível");
            audio=new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, RATE,
                    AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,
                    Math.max(16384,min*2));
            if(audio.getState()!=AudioRecord.STATE_INITIALIZED)
                throw new IllegalStateException("Microfone não inicializado");
            file=File.createTempFile(PREFIX, ".wav", context.getCacheDir());
            wav=new RandomAccessFile(file,"rw");
            writeHeader(wav,0);
            audio.startRecording();
            if(audio.getRecordingState()!=AudioRecord.RECORDSTATE_RECORDING)
                throw new IllegalStateException("Microfone não iniciou a gravação");
            events.status("GRAVANDO ÁUDIO LOCAL · PARAR PARA TRANSCREVER");
            byte[] buffer=new byte[8192];
            long lastNotice=System.currentTimeMillis();
            while(recording.get()){
                int n=audio.read(buffer,0,buffer.length,AudioRecord.READ_NON_BLOCKING);
                if(n<0)throw new IllegalStateException("Falha leitura áudio: "+n);
                if(n>1){
                    n-=n%2;
                    if(bytes+n>MAX_BYTES)throw new IllegalStateException("Limite de 120 minutos");
                    wav.write(buffer,0,n);bytes+=n;
                } else {
                    try{Thread.sleep(20);}catch(InterruptedException ignored){Thread.currentThread().interrupt();}
                }
                if(System.currentTimeMillis()-lastNotice>=3000){
                    lastNotice=System.currentTimeMillis();
                    events.status("GRAVANDO · "+(bytes/(RATE*2L))+" s · PARAR PARA TRANSCREVER");
                }
            }
            if(bytes==0)throw new IllegalStateException("Nenhum áudio capturado");
            writeHeader(wav,bytes);
            wav.getFD().sync();
            good=true;
        }catch(Throwable e){
            events.error("ERRO GRAVAÇÃO: "+(e.getMessage()==null?e.getClass().getSimpleName():e.getMessage()));
        }finally{
            recording.set(false);
            if(audio!=null){
                try{if(audio.getRecordingState()==AudioRecord.RECORDSTATE_RECORDING)audio.stop();}
                catch(Exception ignored){}
                try{audio.release();}catch(Exception ignored){}
            }
            if(wav!=null)try{wav.close();}catch(Exception ignored){}
            if(good && file!=null)events.ready(file,bytes*1000L/(RATE*2L));
            else if(file!=null && !file.delete())events.status("AVISO · ÁUDIO TEMPORÁRIO NÃO ELIMINADO");
        }
    }

    static void cleanAbandoned(Context context){
        File[] old=context.getCacheDir().listFiles((dir,name)->name.startsWith(PREFIX));
        if(old!=null)for(File file:old)if(file.isFile())file.delete();
    }

    private static void le16(RandomAccessFile f,int value)throws Exception{
        f.write(value&255);f.write((value>>8)&255);
    }
    private static void le32(RandomAccessFile f,long value)throws Exception{
        for(int i=0;i<4;i++)f.write((int)(value>>(i*8))&255);
    }
    private static void writeHeader(RandomAccessFile f,long length)throws Exception{
        if(length>0xFFFFFFFFL-36)throw new IllegalArgumentException("Áudio excede WAV RIFF");
        long end=f.length();
        f.seek(0);
        f.writeBytes("RIFF");le32(f,length+36);
        f.writeBytes("WAVEfmt ");le32(f,16);
        le16(f,1);le16(f,1);
        le32(f,RATE);le32(f,RATE*2L);
        le16(f,2);le16(f,16);
        f.writeBytes("data");le32(f,length);
        f.seek(Math.max(44,end));
    }
}
