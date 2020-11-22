import org.lwjgl.openal.*;
import org.lwjgl.system.MemoryUtil;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.HashMap;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.stb.STBVorbis.stb_vorbis_decode_filename;
import static org.lwjgl.stb.STBVorbis.stb_vorbis_decode_memory;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.libc.LibCStdlib.free;

public class SoundPlayer {

    private long device, context;
    private HashMap<Sound, Integer> soundBuffer = new HashMap<>();

    private static final int NUM_SOURCES = 30;
    private int[] sources = new int[NUM_SOURCES];
    private float[] sourceTime = new float[NUM_SOURCES];
    private Sound[] sourceSound = new Sound[NUM_SOURCES];
    private float time;

    public SoundPlayer() {
        String deviceN = alcGetString(0, ALC_DEFAULT_DEVICE_SPECIFIER);
        device = alcOpenDevice(deviceN);
        int[] attributes = {0};
        context = alcCreateContext(device, attributes);
        alcMakeContextCurrent(context);
        ALCCapabilities alcCapabilities = ALC.createCapabilities(device);
        ALCapabilities alCapabilities = AL.createCapabilities(alcCapabilities);
        if(!alCapabilities.OpenAL10) {
            throw new RuntimeException("OpenAL error: no capabilities");
        }
        checkError("Init AL");

        addSound(Sound.HIT, "hit.ogg");
        addSound(Sound.MAGNET, "magnet_old.ogg");
        addSound(Sound.ROCKET, "rocket.ogg");

        alGenSources(sources);

        for(int i = 0; i < NUM_SOURCES; ++i) {
            sourceSound[i] = Sound.NONE;
        }

        checkError("Init hit");
    }

    public void update(float delta) {
        time += delta;
    }

    public void play(Sound sound) {
        int min = 0;
        for(int i = 1; i < sources.length; ++i) {
            if(sourceTime[i] < sourceTime[min]) {
                min = i;
            }
        }
        if(sourceSound[min] != Sound.NONE) {
            alSourceStop(sources[min]);
        }
        sourceTime[min] = time;
        sourceSound[min] = sound;
        alSourcei(sources[min], AL_BUFFER, soundBuffer.get(sound));
        alSourcePlay(sources[min]);
    }

    public void stopAll(Sound type) {
        for(int i = 0; i < sources.length; ++i) {
            if(sourceSound[i] == type) {
                alSourceStop(sources[i]);
                sourceSound[i] = Sound.NONE;
            }
        }
    }

    public void cleanUp() {
        alcDestroyContext(context);
        alcCloseDevice(device);
        soundBuffer.forEach((sound, buffer) -> {
            alDeleteBuffers(buffer);
        });
        alDeleteSources(sources);
    }

    private void checkError(String msg) {
        int err = AL10.alGetError();
        if(err != AL10.AL_NO_ERROR) {
            throw new RuntimeException("OPENAL ERROR: " + err + " " + msg);
        }
    }

    private void addSound(Sound sound, String fileName) {
        //Allocate space to store return information from the function
        stackPush();
        IntBuffer channelsBuffer = stackMallocInt(1);
        stackPush();
        IntBuffer sampleRateBuffer = stackMallocInt(1);

        ByteBuffer data;
        try {
            InputStream stream = Texture.class.getResourceAsStream(fileName);
            ReadableByteChannel channel = Channels.newChannel(stream);
            data = MemoryUtil.memAlloc(stream.available());
            channel.read(data);
            channel.close();
            data.flip();
        } catch (Exception e) {
            System.err.println("Error loading sound " + fileName);
            e.printStackTrace();
            return;
        }

        //        ShortBuffer rawAudioBuffer = stb_vorbis_decode_filename(fileName, channelsBuffer, sampleRateBuffer);
        ShortBuffer rawAudioBuffer = stb_vorbis_decode_memory(data, channelsBuffer, sampleRateBuffer);
        checkError("Init hit");

        //Retreive the extra information that was stored in the buffers by the function
        int channels = channelsBuffer.get();
        int sampleRate = sampleRateBuffer.get();

        //Find the correct OpenAL format
        int format = -1;
        if(channels == 1) {
            format = AL_FORMAT_MONO16;
        } else if(channels == 2) {
            format = AL_FORMAT_STEREO16;
        }

        //Request space for the buffer
        soundBuffer.put(sound, alGenBuffers());
        checkError("Init hit");

        //Send the data to OpenAL
        alBufferData(soundBuffer.get(sound), format, rawAudioBuffer, sampleRate);
        checkError("Init hit");

        //Free the memory allocated by STB
        free(rawAudioBuffer);
        //Free the space we allocated earlier
        stackPop();
        stackPop();
    }
}
