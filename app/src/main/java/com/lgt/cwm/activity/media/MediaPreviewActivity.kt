package com.lgt.cwm.activity.media

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnAttach
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_IDLE
import com.lgt.cwm.R
import com.lgt.cwm.activity.media.adapter.MediaRailAdapter
import com.lgt.cwm.activity.media.fragments.MediaPreviewFragment
import com.lgt.cwm.activity.media.fragments.MediaPreviewFragment.Companion.DATA_MEDIA
import com.lgt.cwm.activity.media.fragments.MediaPreviewFragment.Companion.DATA_POSITION
import com.lgt.cwm.activity.media.fragments.MediaPreviewFragment.Companion.isImageType
import com.lgt.cwm.activity.media.fragments.MediaPreviewFragment.Companion.isVideo
import com.lgt.cwm.activity.media.models.MediaFileInfo
import com.lgt.cwm.databinding.ActivityMediaPreviewBinding
import com.lgt.cwm.ui.animation.DepthPageTransformer
import com.lgt.cwm.ui.components.ExtendedOnPageChangedListener
import com.lgt.cwm.ui.glide.GlideApp
import com.lgt.cwm.util.DebugConfig
import com.lgt.cwm.util.FullscreenHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MediaPreviewActivity : AppCompatActivity(), MediaRailAdapter.RailItemListener {
    private val TAG = MediaPreviewActivity::class.simpleName.toString()

    @Inject lateinit var debugConfig: DebugConfig

    private lateinit var fullscreenHelper: FullscreenHelper

    private lateinit var mediaPager: ViewPager2
    private lateinit var detailsContainer: View
    private lateinit var caption: TextView
    private lateinit var captionContainer: View
    private lateinit var albumRail: RecyclerView
    private lateinit var playbackControlsContainer: ViewGroup

    private lateinit var albumRailAdapter: MediaRailAdapter
    private lateinit var viewPagerListener: ViewPagerListener

    private var initialMediaType = ""
    private var initialMediaUri: Uri? = null
    private var initialActivePostion = 0

    private var mediaUris: MutableList<MediaFileInfo> = mutableListOf()

    private var setPlayControlsFirstTime = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fullscreenHelper = FullscreenHelper(this)

        val binding: ActivityMediaPreviewBinding = DataBindingUtil.setContentView(this, R.layout.activity_media_preview)
        binding.lifecycleOwner = this

        initializeViews(binding)

        val toolbar = binding.toolbar
        toolbar.setTitleTextColor(Color.WHITE)
        toolbar.setSubtitleTextColor(Color.WHITE)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initializeResources()
        initializeActionBar()
        initializeMedia()
    }

    private fun initializeResources() {
        intent.type?.let { initialMediaType = it }
        initialMediaUri = intent.data

        initialActivePostion = intent.getIntExtra(DATA_POSITION, 0)

        val list: ArrayList<MediaFileInfo> = intent.getParcelableArrayListExtra(DATA_MEDIA)!!

        mediaUris = list

        albumRail.visibility = if (mediaUris.size > 1) View.VISIBLE else View.GONE
        albumRailAdapter.setMedia(mediaUris, 0)
        albumRail.smoothScrollToPosition(0)
    }

    override fun onDestroy() {
        super.onDestroy()

        mediaPager.unregisterOnPageChangeCallback(viewPagerListener)
    }

    private fun initializeActionBar() {
//        val mediaItem: MediaItem =
//            getCurrentMediaItem()
//        if (mediaItem != null) {
//            supportActionBar.setTitle(getTitleText(mediaItem))
//            supportActionBar.setSubtitle(getSubTitleText(mediaItem))
//        }
        supportActionBar?.let {
            it.title = "Media Preview"
            it.subtitle = "14:02, 27 July"
        }
    }

    private fun initializeViews(binding: ActivityMediaPreviewBinding) {
        mediaPager = binding.mediaPager
        detailsContainer = binding.mediaPreviewDetailsContainer
        caption = binding.mediaPreviewCaption
        captionContainer = binding.mediaPreviewCaptionContainer
        albumRail = binding.mediaPreviewAlbumRail
        playbackControlsContainer = binding.mediaPreviewPlaybackControlsContainer

        mediaPager.offscreenPageLimit = 1
        mediaPager.setPageTransformer(DepthPageTransformer())
        viewPagerListener = ViewPagerListener()
        mediaPager.registerOnPageChangeCallback(viewPagerListener)

        albumRailAdapter = MediaRailAdapter(GlideApp.with(this), this, false)
        albumRail.itemAnimator = null // Or can crash when set to INVISIBLE while animating by FullscreenHelper https://issuetracker.google.com/issues/148720682
        albumRail.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        albumRail.adapter = albumRailAdapter
        detailsContainer.visibility = View.VISIBLE

        val toolbarLayout: View = binding.toolbarLayout
        anchorMarginsToBottomInsets(detailsContainer)
        fullscreenHelper.configureToolbarLayout(binding.toolbarCutoutSpacer, binding.toolbar)
        fullscreenHelper.showAndHideWithSystemUI(window, detailsContainer, toolbarLayout)
    }

    private fun initializeMedia() {
       if (mediaUris.size > 1) {
           mediaPager.adapter = CursorPagerAdapter(
               this,
               mediaUris,
               0,
               true
           )
       } else {
           mediaPager.adapter = SingleItemPagerAdapter(
               this,
               mediaUris.first(),
               0,
               false
           )
       }

        debugConfig.log(TAG, "set init position ${initialActivePostion}")

        mediaPager.doOnAttach {
            mediaPager.currentItem = initialActivePostion

        }
//        mediaPager.currentItem = initialActivePostion

//        changeActiveAlbumRailItem()
    }

    companion object {
        private fun anchorMarginsToBottomInsets(viewToAnchor: View) {
            ViewCompat.setOnApplyWindowInsetsListener(viewToAnchor) { view: View, insets: WindowInsetsCompat ->
                val layoutParams = view.layoutParams as MarginLayoutParams
                layoutParams.setMargins(
                    insets.systemWindowInsetLeft,
                    layoutParams.topMargin,
                    insets.systemWindowInsetRight,
                    insets.systemWindowInsetBottom
                )
                view.layoutParams = layoutParams
                insets
            }
        }

        fun isContentTypeSupported(contentType: String): Boolean {
            return isImageType(contentType) || isVideo(contentType)
        }
    }

    private fun cleanupMedia(): Int {
        val restartItem = mediaPager.currentItem
        mediaPager.removeAllViews()
        mediaPager.adapter = null
        return restartItem
    }

    override fun onResume() {
        super.onResume()
//        initializeMedia()
    }

    override fun onPause() {
        super.onPause()
//        restartItem = cleanupMedia()
    }

    fun changeActiveAlbumRailItem() {
        debugConfig.log(TAG, "changeActiveAlbumRailItem ${mediaPager.currentItem}")
        val playbackControls = (mediaPager.adapter as MediaItemAdapter).getPlaybackControls(mediaPager.currentItem)
        if (playbackControls != null) {
            debugConfig.log(TAG, "playbackControls add view")
            val params = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            playbackControls.layoutParams = params
            playbackControlsContainer.removeAllViews()
//            playbackControlsContainer.addView(playbackControls)
            debugConfig.log(TAG, "playbackControls add view item ${mediaPager.currentItem}")
            playbackControlsContainer.post{  playbackControlsContainer.addView(playbackControls) }
        } else {
            playbackControlsContainer.removeAllViews()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.clear()
        val inflater = this.menuInflater
        inflater.inflate(R.menu.media_preview, menu)
        super.onCreateOptionsMenu(menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {

        // Restricted to API26 because of MemoryFileUtil not supporting lower API levels well
        menu.findItem(R.id.media_preview__share).isVisible = Build.VERSION.SDK_INT >= 26

        super.onPrepareOptionsMenu(menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        when (item.itemId) {
            R.id.media_preview__overview -> {
                debugConfig.log(TAG, "showOverview")
                return true
            }
            R.id.media_preview__forward -> {
                debugConfig.log(TAG, "forward")
                return true
            }
            R.id.media_preview__share -> {
                debugConfig.log(TAG, "share")
                return true
            }
            R.id.save -> {
                debugConfig.log(TAG, "saveToDisk")
                return true
            }
            R.id.delete -> {
                debugConfig.log(TAG, "delete")
                return true
            }
            android.R.id.home -> {
                debugConfig.log(TAG, "saveToDisk")
                finish()
                return true
            }

            else -> {
                return false
            }
        }
    }

    private inner class ViewPagerListener : ExtendedOnPageChangedListener() {
        override fun onPageSelected(position: Int) {
            debugConfig.log(TAG, "onPageSelected position ${position}")
            super.onPageSelected(position)
            val adapter = mediaPager.adapter as MediaItemAdapter
            val item = adapter.getMediaItemFor(position)
            albumRailAdapter.setActivePosition(position)

            debugConfig.log(TAG, "has fragment for position ${position} - ${adapter.hasFragmentFor(position)}")

            initializeActionBar()

            changeActiveAlbumRailItem()
        }

        override fun onPageUnselected(position: Int) {
            debugConfig.log(TAG, "onPageUnselected position ${position}")
            val adapter = mediaPager.adapter as MediaItemAdapter
            val item = adapter.getMediaItemFor(position)
            adapter.pause(position)
        }

        override fun onPageScrollStateChanged(state: Int) {
            super.onPageScrollStateChanged(state)
            if (state == SCROLL_STATE_IDLE && setPlayControlsFirstTime && initialActivePostion > 0) {
                changeActiveAlbumRailItem()
                setPlayControlsFirstTime = false
            }
        }
    }

    private inner class CursorPagerAdapter constructor(activity: AppCompatActivity, mediaList: MutableList<MediaFileInfo>, autoPlayPosition: Int, leftIsRecent: Boolean) : FragmentStateAdapter(activity), MediaItemAdapter {

        @SuppressLint("UseSparseArrays")
        private val mediaFragments: MutableMap<Int, MediaPreviewFragment> = HashMap()
        private val context: Context
        private val leftIsRecent: Boolean
        private var active = false
        private var mediaList: MutableList<MediaFileInfo>
        private var autoPlayPosition: Int

        init {
            this.context = activity.applicationContext
            this.mediaList = mediaList
            this.autoPlayPosition = autoPlayPosition
            this.leftIsRecent = leftIsRecent
        }

        fun setActive(active: Boolean) {
            this.active = active
            notifyDataSetChanged()
        }

        fun setCursor(cursor: Cursor, autoPlayPosition: Int) {
//            this.cursor = cursor
            this.autoPlayPosition = autoPlayPosition
        }

        override fun getItemCount(): Int {
            return mediaList.count()
        }


        override fun createFragment(position: Int): Fragment {
            debugConfig.log(TAG, "createFragment pos ${position} - size ${mediaFragments.size}")
            val autoPlay = autoPlayPosition == position
            val media = mediaList[position]
            autoPlayPosition = -1

            val fragment = MediaPreviewFragment.newInstance(media.fileUri, media.mediaType, media.mediaSize, true, false)
            mediaFragments[position] = fragment

            return fragment
        }

        override fun getMediaItemFor(position: Int): MediaFileInfo {
            return mediaList[position]
        }

        override fun pause(position: Int) {
            mediaFragments[position]?.pause()
        }

        override fun getPlaybackControls(position: Int): View? {
            val mediaView = mediaFragments[position]
            debugConfig.log(TAG, "mediaView ${position} - size ${mediaFragments.size} view ${mediaView} - controls ${mediaView?.getPlaybackControls()} ")
            return mediaView?.getPlaybackControls()
        }

        override fun hasFragmentFor(position: Int): Boolean {
            return mediaFragments.containsKey(position)
        }
    }

    private class SingleItemPagerAdapter constructor(
        activity: AppCompatActivity,
        private val mediaFile: MediaFileInfo,
        private val size: Long,
        private val isVideoGif: Boolean) : FragmentStateAdapter(activity), MediaItemAdapter {

        private var mediaPreviewFragment: MediaPreviewFragment? = null

        override fun getItemCount(): Int {
            return 1
        }


        override fun createFragment(position: Int): Fragment {
            mediaPreviewFragment = MediaPreviewFragment.newInstance(
                mediaFile.fileUri,
                mediaFile.mediaType,
                size, true,
                isVideoGif
            )
            return mediaPreviewFragment as MediaPreviewFragment
        }

        override fun getMediaItemFor(position: Int): MediaFileInfo {
            return mediaFile
        }

        override fun pause(position: Int) {
            mediaPreviewFragment?.pause()
        }

        override fun getPlaybackControls(position: Int): View? {
            return mediaPreviewFragment?.getPlaybackControls()
        }

        override fun hasFragmentFor(position: Int): Boolean {
            return mediaPreviewFragment != null
        }
    }

    interface MediaItemAdapter {
        fun getMediaItemFor(position: Int): MediaFileInfo
        fun pause(position: Int)
        fun getPlaybackControls(position: Int): View?
        fun hasFragmentFor(position: Int): Boolean
    }

    override fun onRailItemClicked(distanceFromActive: Int) {
        mediaPager.currentItem = mediaPager.currentItem + distanceFromActive
    }

    override fun onRailItemDeleteClicked(distanceFromActive: Int) {
        throw UnsupportedOperationException("Callback unsupported.")
    }
}