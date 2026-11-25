package br.com.tutorclinico

import android.content.Context
import com.k2fsa.sherpa.onnx.*
import java.io.File

/** TAGARELA PT-BR fine-tune, INT8 sherpa-onnx TDT export; no network. Caches recognizer to avoid repeated initialization. */
internal class TagarelaEngine(context: Context): AutoCloseable {
    data class Span(val text:String,val start:Long,val end:Long)
    private val recognizer: OfflineRecognizer

    companion object {
        @Volatile private var cachedInstance: TagarelaEngine? = null

        @Synchronized
        fun getOrCreate(context: Context): TagarelaEngine {
            val existing = cachedInstance
            if (existing != null) return existing
            val created = TagarelaEngine(context.applicationContext)
            cachedInstance = created
            return created
        }

        @Synchronized
        fun releaseCached() {
            cachedInstance?.close()
            cachedInstance = null
        }
    }

    init {
        val root=File(context.noBackupFilesDir,"tagarela-bf31b61")
        if(!root.exists()&&!root.mkdirs())error("Não foi possível preparar TAGARELA")
        for(name in listOf("encoder.int8.onnx","decoder.int8.onnx","joiner.int8.onnx","tokens.txt")){
            val file=File(root,name)
            val asset="tagarela/$name"
            val size=context.assets.openFd(asset).use { it.length }
            if(!file.isFile||file.length()!=size){
                val tmp=File(root,name+".tmp")
                try{
                    context.assets.open(asset).use { input->tmp.outputStream().use { input.copyTo(it) } }
                    check(tmp.length()==size){"Modelo TAGARELA incompleto"}
                    if(file.exists())check(file.delete())
                    check(tmp.renameTo(file))
                }finally{tmp.delete()}
            }
        }
        recognizer=OfflineRecognizer(config=OfflineRecognizerConfig(
            featConfig=FeatureConfig(sampleRate=16000,featureDim=128),
            modelConfig=OfflineModelConfig(
                transducer=OfflineTransducerModelConfig(
                    encoder=File(root,"encoder.int8.onnx").absolutePath,
                    decoder=File(root,"decoder.int8.onnx").absolutePath,
                    joiner=File(root,"joiner.int8.onnx").absolutePath),
                tokens=File(root,"tokens.txt").absolutePath,modelType="nemo_transducer",numThreads=4,debug=false,provider="cpu"),
            decodingMethod="greedy_search"))
    }
    fun decode(pcm:ByteArray):List<Span>{
        val audio=FloatArray(pcm.size/2){i->((pcm[2*i].toInt() and 255) or (pcm[2*i+1].toInt() shl 8)).toShort()/32768f}
        val stream=recognizer.createStream()
        try{
            stream.acceptWaveform(audio,16000);recognizer.decode(stream)
            val r=recognizer.getResult(stream);val text=r.text.trim()
            if(text.isEmpty())return emptyList()
            val max=pcm.size/32L
            // Use native token timestamps only when token reconstruction matches the actual transcript.
            val tokens=r.tokens;val times=r.timestamps
            fun clean(s:String)=s.replace('▁',' ').trim().replace(Regex("\\s+")," ")
            if(tokens.isEmpty()||tokens.size!=times.size||times.any{!it.isFinite()||it<0}||
                clean(tokens.joinToString(""))!=clean(text)||times.indices.drop(1).any{times[it]<times[it-1]})
                return listOf(Span(text,0,max))
            val out=mutableListOf<Span>();var first=0
            fun emit(last:Int){
                val s=clean(tokens.slice(first..last).joinToString(""))
                val start=(times[first]*1000).toLong().coerceIn(0,max)
                val next=if(last+1<times.size)(times[last+1]*1000).toLong() else max
                val end=next.coerceIn(start,max)
                if(s.isNotEmpty())out.add(Span(s,start,end))
            }
            for(i in 1 until tokens.size){
                val boundary=tokens[i].startsWith("▁")||tokens[i].startsWith(" ")
                if(boundary&&(times[i]-times[i-1]>0.65f||times[i]-times[first]>7f||tokens[i-1].endsWith(".")||tokens[i-1].endsWith("?")||tokens[i-1].endsWith("!"))){emit(i-1);first=i}
            }
            emit(tokens.lastIndex)
            return if(out.isNotEmpty()&&clean(out.joinToString(" "){it.text})==clean(text))out else listOf(Span(text,0,max))
        }finally{stream.release()}
    }
    override fun close(){recognizer.release()}
}
