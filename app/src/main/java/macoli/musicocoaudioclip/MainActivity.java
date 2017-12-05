package macoli.musicocoaudioclip;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {


    PlayDialg pd ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        pd = new PlayDialg(this) ;
        pd.show();
    }

    private class PlayDialg extends Dialog implements MediaPlayer.OnPreparedListener , View.OnClickListener{
        private String audioPath = "/sdcard/remusic/111.mp3" ;
        private MediaPlayer mediaPlayer ;
        private int currentPosition ;
        private int totalTime ;
        private SeekBar seekBar ;
        private TextView startTv ;
        private TextView endTv ;
        private TextView currentTv ;
        private TextView totalTv ;
        private Button setStartBtn , setEndBtn ;
        private long startTime , endTime ;
        private final int UPDATE_POSITION = 1;
        private final int PREPARE_COMPLETE = 2;
        private final int END_DECODE = 3 ;
        private ProgressDialog waitPb ;

        Handler handler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what){
                    case UPDATE_POSITION:
                        currentTv.setText(makeTimeString(mediaPlayer.getCurrentPosition())) ;
                        seekBar.setProgress(currentPosition);
                        this.sendEmptyMessageDelayed(UPDATE_POSITION , 1000) ;
                        break ;
                    case PREPARE_COMPLETE:
                        totalTv.setText(makeTimeString(totalTime)) ;
                        this.sendEmptyMessageDelayed(UPDATE_POSITION , 1000) ;
                        break ;
                    case END_DECODE:
                        dissmissWait();
                        break ;
                }
            }
        } ;

        public PlayDialg(@NonNull Context context) {
            super(context);
            mediaPlayer = new MediaPlayer() ;
            try {
                mediaPlayer.setDataSource(audioPath);
                mediaPlayer.setOnPreparedListener(this);
                mediaPlayer.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }

            initView();
        }

        private void initView() {
            View view = LayoutInflater.from(this.getContext()).inflate(R.layout.dialog_audio_clip, null);
            this.setContentView(view);
            seekBar = (SeekBar) view.findViewById(R.id.seekbar) ;
            startTv = (TextView) view.findViewById(R.id.start_time_tv) ;
            endTv = (TextView) view.findViewById(R.id.end_time_tv) ;
            currentTv = (TextView) view.findViewById(R.id.current_play_tv) ;
            totalTv = (TextView) view.findViewById(R.id.total_tv) ;
            setStartBtn = (Button) view.findViewById(R.id.set_start_btn) ;
            setEndBtn = (Button) view.findViewById(R.id.set_end_btn) ;
            setEndBtn.setOnClickListener(this);
            setStartBtn.setOnClickListener(this);
        }

        @Override
        public void onPrepared(MediaPlayer mediaPlayer) {
            mediaPlayer.start();
            currentPosition = mediaPlayer.getCurrentPosition() ;
            totalTime = mediaPlayer.getDuration() ;
            handler.sendEmptyMessage(PREPARE_COMPLETE) ;
        }

        public String makeTimeString(long milliSecs) {
            StringBuffer sb = new StringBuffer();
            long m = milliSecs / (60 * 1000);
            sb.append(m < 10 ? "0" + m : m);
            sb.append(":");
            long s = (milliSecs % (60 * 1000)) / 1000;
            sb.append(s < 10 ? "0" + s : s);
            return sb.toString();
        }

        @Override
        public void onClick(View view) {
            switch (view.getId()){
                case R.id.set_start_btn:
                    startTv.setText(makeTimeString(mediaPlayer.getCurrentPosition()));
                    startTime = mediaPlayer.getCurrentPosition() ;
                    break ;
                case R.id.set_end_btn:
                    endTv.setText(makeTimeString(mediaPlayer.getCurrentPosition()));
                    endTime = mediaPlayer.getCurrentPosition() ;
                    clipAudio() ;
                    break ;
            }
        }

        private void showWait(){
            if (waitPb == null){
                waitPb = new ProgressDialog(getContext()) ;
            }
            waitPb.show();
        }

        private void dissmissWait(){
            if (waitPb != null){
                waitPb.dismiss() ;
            }
        }

        private void clipAudio(){
            showWait();
            new Thread(){
                public void run(){
                    AudioDecoder decoder = new AudioDecoder() ;
                    decoder.decodeVideo(audioPath , startTime , endTime - startTime) ;
                    handler.sendEmptyMessage(END_DECODE) ;
                }
            }.start();

        }
    }
}
