package macoli.musicocoaudioclip;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import java.nio.ByteBuffer;

/**
 * Created by macoli on 17/12/5.
 */

public class AudioDecoder {
    private final static String TAG = "VideoDecoder";
    private MediaCodec mediaDecoder;
    private MediaExtractor mediaExtractor;
    private MediaFormat mediaFormat;
    private MediaMuxer mediaMuxer;
    private String mime = null;

    public boolean decodeVideo(String url, long clipPoint, long clipDuration) {
        clipDuration = clipDuration * 1000 ;
        clipPoint = clipPoint * 1000 ;
        int audioTrackIndex = -1;
        int audioMaxInputSize = 0;
        int sourceATrack = 0;
        long audioDuration;
        //创建分离器
        mediaExtractor = new MediaExtractor();
        try {
            //设置文件路径
            mediaExtractor.setDataSource(url);
            //创建合成器
            mediaMuxer = new MediaMuxer(url.substring(0, url.lastIndexOf(".")) + "_output.mp3"
                    , MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (Exception e) {
            Log.e(TAG, "error path" + e.getMessage());
        }
        //获取每个轨道的信息
        for (int i = 0; i < mediaExtractor.getTrackCount(); i++) {
            try {
                mediaFormat = mediaExtractor.getTrackFormat(i);
                mime = mediaFormat.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("audio/")) {
                    sourceATrack = i;
                    int sampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                    int channelCount = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                    audioMaxInputSize = mediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
                    audioDuration = mediaFormat.getLong(MediaFormat.KEY_DURATION);
                    Log.d(TAG, "sampleRate is " + sampleRate
                            + ";channelCount is " + channelCount
                            + ";audioMaxInputSize is " + audioMaxInputSize
                            + ";audioDuration is " + audioDuration
                    );
                    //添加音轨
                    audioTrackIndex = mediaMuxer.addTrack(mediaFormat);
                    break ;
                }
                Log.d(TAG, "file mime is " + mime);
            } catch (Exception e) {
                Log.e(TAG, " read error " + e.getMessage());
            }
        }
        //分配缓冲
        ByteBuffer inputBuffer = ByteBuffer.allocate(500 * 1024);
        //根据官方文档的解释MediaMuxer的start一定要在addTrack之后
        mediaMuxer.start();
        //音频部分
        mediaExtractor.selectTrack(sourceATrack);
        MediaCodec.BufferInfo audioInfo = new MediaCodec.BufferInfo();
        audioInfo.presentationTimeUs = 0;
        long audioSampleTime;
        //获取音频帧时长
        {
            mediaExtractor.readSampleData(inputBuffer, 0);
            //skip first sample
            if (mediaExtractor.getSampleTime() == MediaExtractor.SAMPLE_FLAG_SYNC)
                mediaExtractor.advance();
            mediaExtractor.readSampleData(inputBuffer, 0);
            long firstAudioPTS = mediaExtractor.getSampleTime();
            mediaExtractor.advance();
            mediaExtractor.readSampleData(inputBuffer, 0);
            long SecondAudioPTS = mediaExtractor.getSampleTime();
            audioSampleTime = Math.abs(SecondAudioPTS - firstAudioPTS);
            Log.d(TAG, "AudioSampleTime is " + audioSampleTime);
        }
        mediaExtractor.seekTo(clipPoint, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
        while (true) {
            int sampleSize = mediaExtractor.readSampleData(inputBuffer, 0);
            if (sampleSize < 0) {
                mediaExtractor.unselectTrack(sourceATrack);
                break;
            }
            int trackIndex = mediaExtractor.getSampleTrackIndex();
            long presentationTimeUs = mediaExtractor.getSampleTime();
            Log.d(TAG, "trackIndex is " + trackIndex
                    + ";presentationTimeUs is " + presentationTimeUs);
            if ((clipDuration != 0) && (presentationTimeUs > (clipPoint + clipDuration))) {
                mediaExtractor.unselectTrack(sourceATrack);
                break;
            }
            mediaExtractor.advance();
            audioInfo.offset = 0;
            audioInfo.flags = mediaExtractor.getSampleFlags();
            audioInfo.size = sampleSize;
            mediaMuxer.writeSampleData(audioTrackIndex, inputBuffer, audioInfo);
            audioInfo.presentationTimeUs += audioSampleTime;//presentationTimeUs;
        }
        //全部写完后释放MediaMuxer和MediaExtractor
        mediaMuxer.stop();
        mediaMuxer.release();
        mediaExtractor.release();
        mediaExtractor = null;
        return true;
    }
}
