// Utility to decode raw PCM data from Gemini TTS
export const decodeBase64ToBytes = (base64: string): Uint8Array => {
  const binaryString = atob(base64);
  const len = binaryString.length;
  const bytes = new Uint8Array(len);
  for (let i = 0; i < len; i++) {
    bytes[i] = binaryString.charCodeAt(i);
  }
  return bytes;
};

export const playPcmAudio = async (
  base64Audio: string,
  audioContext: AudioContext
): Promise<void> => {
  try {
    if (audioContext.state === 'suspended') {
      await audioContext.resume();
    }

    const pcmBytes = decodeBase64ToBytes(base64Audio);

    // Alignment check: Int16Array requires a byte length multiple of 2.
    // We trim the last byte if the length is odd (rare but possible with raw streams).
    const safeLength = pcmBytes.length - (pcmBytes.length % 2);
    const dataInt16 = new Int16Array(pcmBytes.buffer, 0, safeLength / 2);

    // Gemini 2.5 TTS is typically 24kHz
    const sampleRate = 24000;
    const numChannels = 1;
    
    const buffer = audioContext.createBuffer(numChannels, dataInt16.length, sampleRate);
    const channelData = buffer.getChannelData(0);
    
    for (let i = 0; i < dataInt16.length; i++) {
      // Normalize Int16 to Float32 range [-1.0, 1.0]
      channelData[i] = dataInt16[i] / 32768.0;
    }

    const source = audioContext.createBufferSource();
    source.buffer = buffer;
    source.connect(audioContext.destination);
    source.start();
  } catch (e) {
    console.error("Audio playback error:", e);
  }
};