package com.example.iptv_sample.ui.main

import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.iptv_sample.R
import com.example.iptv_sample.dto.VideoStream
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util

class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()
    }

    private lateinit var viewModel: MainViewModel

    var playerView: PlayerView? = null
    var progressBar: ProgressBar? = null
    var linearLayout: LinearLayout? = null
    var channelNumberTextView: TextView? = null
    var channelNameTextView:TextView? = null
    var audioManager: AudioManager? = null
    var index = 0
    var mStreams = mutableListOf<VideoStream>()
    var player: ExoPlayer? = null
    var dataSourceFactory: DataSource.Factory? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val mainView = inflater.inflate(R.layout.main_fragment, container, false)

        playerView = mainView.findViewById<PlayerView>(R.id.playerView)
        progressBar = mainView.findViewById<ProgressBar>(R.id.progressBar)
        linearLayout = mainView.findViewById<LinearLayout>(R.id.linearLayout)
        channelNumberTextView = mainView.findViewById<TextView>(R.id.channelNumberTextView)
        channelNameTextView = mainView.findViewById<TextView>(R.id.channelNameTextView)
        return mainView
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        // TODO: Use the ViewModel
        mStreams.add(VideoStream("2x2", "http://192.168.98.16:8000/421"))

        setUpVideo()
        audioManager = requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager?

        // mStreams = MainActivity.streams
        //index = getIntent().getIntExtra("index", -1)
        play(index)
    }

    override fun onPause() {
        super.onPause()
        stop()
    }

    override fun onResume() {
        super.onResume()
        play(index)
    }

    private fun setUpVideo() {
        val context = requireContext()
        player = ExoPlayer.Builder(context).build()
        player?.playWhenReady = true

        playerView?.player = player
        playerView?.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        player?.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT

        // Produces DataSource instances through which media data is loaded.
        dataSourceFactory = DefaultDataSourceFactory(
            context, Util.getUserAgent( context, "iptv")
        )
        player!!.addListener(object : Player.EventListener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                if (playWhenReady && playbackState == Player.STATE_READY) {
                    // media actually playing
                    hideLoading()
                } else if (playWhenReady) {
                    // might be idle (plays after prepare()),
                    // buffering (plays when data available)
                    // or ended (plays when seek away from end)
                    if (playbackState == Player.STATE_ENDED) {
                        stop()
                        play(index)
                    }
                } else {
                    // player paused in any state
                }
            }

            fun onPlayerError(error: ExoPlaybackException?) {
                stop()
                play(index)
            }
        })
    }

    fun showLoading() {
        // progressBar.setVisibility(View.VISIBLE);
        channelNameTextView?.setText(mStreams[index].name)
        channelNumberTextView!!.text = player?.mediaMetadata?.title
        linearLayout!!.visibility = View.VISIBLE
    }

    fun hideLoading() {
        //   progressBar.setVisibility(View.INVISIBLE);
        linearLayout!!.visibility = View.INVISIBLE
    }


    fun play(index: Int) {
        showLoading()
        val uri = Uri.parse(mStreams[index].url)
        val videoSource: MediaSource = ProgressiveMediaSource.Factory(dataSourceFactory!!)
            .createMediaSource(uri)
        // Prepare the player with the source.
        player?.prepare(videoSource)
        // Auto Play video as soon as it buffers
        resumeLivePreview()
    }

    private fun resumeLivePreview() {
        player?.playWhenReady = true
    }

    private fun pauseLivePreview() {
        playerView?.player?.release()
    }

    fun stop() {
        player?.let {
            it.playWhenReady = false
            it.stop()
            it.seekTo(0)
        }
    }


}