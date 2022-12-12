package com.lgt.cwm.business.notification

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.content.LocusIdCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.lgt.cwm.R
import com.lgt.cwm.activity.conversation.ConversationActivity
import com.lgt.cwm.activity.home.MainActivity
import com.lgt.cwm.business.contact.ContactRepository
import com.lgt.cwm.business.cwmUser.CWMUserRepository
import com.lgt.cwm.business.message.MessageRepository
import com.lgt.cwm.db.entity.Contact
import com.lgt.cwm.db.entity.SignalThread
import com.lgt.cwm.di.IODispatcher
import com.lgt.cwm.models.SignalMsgExt
import com.lgt.cwm.models.ThreadParticipantInfo
import com.lgt.cwm.receiver.MarkMessageReadReceiver
import com.lgt.cwm.receiver.NotificationMessageDismissReceiver
import com.lgt.cwm.ui.avatar.AvatarConstants
import com.lgt.cwm.ui.avatar.AvatarGenerator
import com.lgt.cwm.ui.avatar.RandomColors
import com.lgt.cwm.util.Config
import com.lgt.cwm.util.Constants
import com.lgt.cwm.util.DebugConfig
import cwmSignalMsgPb.CwmSignalMsg
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton


/**
 * Created by giangtpu on 09/07/2022.
 */
@Singleton
class NotificationHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher,
    private val debugConfig: DebugConfig,
    private val contactRepository: ContactRepository,
    private val messageRepository: MessageRepository,
    private val cwmUserRepository: CWMUserRepository,
    ) {
    private val TAG = NotificationHandler::class.simpleName.toString()

    private var currentActiveThreadId : String? = null


    //use constant ID for notification used as group summary
    val CHAT_SUMMARY_ID = 0

//    val CHAT_SUMMARY_CHANNEL_ID = context.getString(R.string.notification_channel_chat_summary_id)
    val CHAT_CHANNEL_ID = context.getString(R.string.notification_channel_chat_id)
    val WORKER_CHANNEL_ID = context.getString(R.string.notification_channel_worker_id)
    val DEFAULT_CHANNEL_ID = context.getString(R.string.notification_default_channel_id)


    val CHAT_GROUP_KEY_WORK = "com.lgt.cwm.ui.WORK_CHAT"

    val NOTIFICATION_COLOR = 0xcd2626
    val NOTIFICATION_AVATAR_SIZE = 256  //256 pixel
    val NOTIFICATION_AVATAR_TEXTSIZE = 50  //24 pixel
    val NOTIFICATION_SHORCUTINFO_PREFIX = "ShortcutInfo_"

    val personMap = ConcurrentHashMap<String, Person>()

    fun registerNotificationChannels() {
        // Since android Oreo notification channel is needed.
        createNotificationChannel(
            channelId = DEFAULT_CHANNEL_ID,
            channelName = context.getString(R.string.notification_default_channel_name),
            channelDescripion = context.getString(R.string.notification_default_channel_description),
            importance = NotificationManager.IMPORTANCE_DEFAULT
        )

//        createNotificationChannel(
//            channelId = CHAT_SUMMARY_CHANNEL_ID,
//            channelName = context.getString(R.string.notification_channel_chat_summary_name),
//            channelDescripion = context.getString(R.string.notification_channel_chat_summary_description),
//            importance = NotificationManager.IMPORTANCE_DEFAULT
//        )

        createNotificationChannel(
            channelId = CHAT_CHANNEL_ID,
            channelName = context.getString(R.string.notification_channel_chat_name),
            channelDescripion = context.getString(R.string.notification_channel_chat_description),
            importance = NotificationManager.IMPORTANCE_HIGH        // The importance must be IMPORTANCE_HIGH to show Bubbles.
        )

        createNotificationChannel(
            channelId = WORKER_CHANNEL_ID,
            channelName = context.getString(R.string.notification_channel_worker_name),
            channelDescripion = context.getString(R.string.notification_channel_worker_description),
            importance = NotificationManager.IMPORTANCE_DEFAULT
        )
    }

    private fun createNotificationChannel(channelId: String, channelName: String, channelDescripion: String, importance : Int ) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDescripion
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    suspend fun updateCurrentActiveThread(threadId: String?){
        this.currentActiveThreadId = threadId
        threadId?.let {
            clearChatNotidication(threadId)
        }
    }

    suspend fun sendDefaultNotification(title: String, messageBody: String) {
        /*
            Set up a special activity PendingIntent
            Build a PendingIntent without a back stack
        */
//        val intent = Intent(context, TestChatActivity::class.java).apply {
//            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//        }
//        val pendingIntent = PendingIntent.getActivity(
//            context, 0, intent,
//            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )


        /*
            Set up a regular activity PendingIntent
            Build a PendingIntent with a back stack
        */
        // Create an Intent for the activity you want to start
        val resultIntent = Intent(context, MainActivity::class.java)
        // Create the TaskStackBuilder
        val pendingIntent: PendingIntent? = TaskStackBuilder.create(context).run {
            // Add the intent, which inflates the back stack
            addNextIntentWithParentStack(resultIntent)
            // Get the PendingIntent containing the entire back stack
            getPendingIntent(0,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }


        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(context, DEFAULT_CHANNEL_ID)
            .setContentTitle(title)
            .setSmallIcon(R.drawable.ic_app_name)   //https://stackoverflow.com/questions/45318614/why-is-my-smallicon-for-notifications-always-greyed-out
            .setColor(NOTIFICATION_COLOR)
            .setContentText(messageBody)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(defaultSoundUri)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) //Set lock screen visibility
            .setContentIntent(pendingIntent) // Set the intent that will fire when the user taps the notification
            .setAutoCancel(true)    //automatically removes the notification when the user taps it.

        with(NotificationManagerCompat.from(context)) {
            val notification = notificationBuilder.build()
            // notificationId is a unique int for each notification that you must define
            val notificationId = notification.hashCode()
            notify(notificationId, notification)
        }
    }

    suspend fun getDefaultWorkerNotification(): Notification{
        /*
            Set up a regular activity PendingIntent
            Build a PendingIntent with a back stack
        */
        // Create an Intent for the activity you want to start
        val resultIntent = Intent(context, MainActivity::class.java)
        // Create the TaskStackBuilder
        val pendingIntent: PendingIntent? = TaskStackBuilder.create(context).run {
            // Add the intent, which inflates the back stack
            addNextIntentWithParentStack(resultIntent)
            // Get the PendingIntent containing the entire back stack
            getPendingIntent(0,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }


        val notificationBuilder = NotificationCompat.Builder(context, WORKER_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.app_name))
            .setSmallIcon(R.drawable.ic_app_name)   //https://stackoverflow.com/questions/45318614/why-is-my-smallicon-for-notifications-always-greyed-out
            .setOngoing(true)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setLocalOnly(true)
            .setColor(NOTIFICATION_COLOR)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setContentIntent(pendingIntent) // Set the intent that will fire when the user taps the notification

        return notificationBuilder.build()
    }

    suspend fun checkAndSendChatNotification(signalThread: SignalThread, signalMsgExtList: List<SignalMsgExt>) {
        withContext(ioDispatcher) {
//            debugConfig.log(TAG, "sendChatNotification ${signalThread.threadId}")
            if (currentActiveThreadId != null && currentActiveThreadId.equals(signalThread.threadId)){
//                debugConfig.log(TAG, "sendChatNotification - In Chat -> not send")
                if (findActiveNotification(signalThread) != null){
                    clearChatNotidication(signalThread.threadId)
                }
                return@withContext
            }

            if (signalMsgExtList.isEmpty()){
                if (signalThread.unreadMsgs == 0L){
                    clearChatNotidication(signalThread.threadId)
                }
            }else{
                // SDK_INT < 24 -> MessagingStyle will not be shown
                if (Build.VERSION.SDK_INT < 24){
                    createCommonMessageNotification(signalThread, signalMsgExtList)
                }else{
                    val totalUnreadMsg = messageRepository.countAllUnreadMsg()
                    val totalUnreadThread = messageRepository.countAllUnreadThread()
                    createMessageNotification(signalThread, signalMsgExtList)
                    createSummaryNotification(totalUnreadThread, totalUnreadMsg)
                }
            }
        }
    }


    // SDK_INT < 24 -> MessagingStyle will not be shown
    fun restoreMessagingStyle(notificationId: Int): NotificationCompat.MessagingStyle? {
        if (Build.VERSION.SDK_INT >= 24){
            return (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .activeNotifications    //activeNotifications from API ver 23
                .find { it.id == notificationId }
                ?.notification
                ?.let { NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(it) }
        }
        return null
    }

    @SuppressLint("RestrictedApi")
    @RequiresApi(23)
    fun restoreInboxStyle(notificationId: Int): NotificationCompat.InboxStyle? {
        if (Build.VERSION.SDK_INT >= 23) {
            val style = (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .activeNotifications    //activeNotifications from API ver 23
                .find { it.id == notificationId }
                ?.notification
                ?.let { NotificationCompat.InboxStyle.extractStyleFromNotification(it) }
            if (style is NotificationCompat.InboxStyle){
                return style
            }
        }
        return null
    }


    fun findActiveNotification(signalThread: SignalThread): Notification? {
        val chatNotificationId = signalThread.threadId.hashCode()

        if (Build.VERSION.SDK_INT >= 23) {
            return (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .activeNotifications    //activeNotifications from API ver 23
                .find { it.id == chatNotificationId }?.notification
        }

        return null
    }

    fun findAllActiveChatNotification(): Map<Int, Notification> {
        val allActiveChatNotificationMap = hashMapOf<Int, Notification>()

        if (Build.VERSION.SDK_INT >= 26){
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .activeNotifications         //activeNotifications from API ver 23
                .toList()
                .filter { x -> x.notification.channelId == CHAT_CHANNEL_ID }    //ChannelId from API ver 26
                .forEach { x ->
                    allActiveChatNotificationMap.put(x.id, x.notification)
                }
        }else if (Build.VERSION.SDK_INT >= 23){
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .activeNotifications    //activeNotifications from API ver 23
                .toList()
                .forEach { x ->
                    allActiveChatNotificationMap.put(x.id, x.notification)
                }
        }

        return allActiveChatNotificationMap
    }

    suspend fun createPerson(phoneFull: String, svName: String?) : Person{
        var person = personMap.get(phoneFull)
        if (person != null) {
            return person
        }

        val contact = contactRepository.findOneContactByPhoneFull(phoneFull)

        val personName = contact?.name ?: (svName ?: phoneFull)


        //TODO - GET AVATAR FROM CONTACT
        val personAvatar = AvatarGenerator.AvatarBuilder(context)
            .setLabel(personName)
            .setAvatarSize(NOTIFICATION_AVATAR_SIZE)
            .setTextSize(NOTIFICATION_AVATAR_TEXTSIZE)
            .setBackgroundColor(RandomColors(AvatarConstants.COLOR700).getColor(personName))
            .toCircle()
            .build()

        person = Person.Builder()
//            .setImportant(true)
            .setName(personName)
            .setIcon(IconCompat.createWithBitmap(personAvatar.bitmap))
            .setKey(phoneFull)
            .build()

        if (contact != null){
            personMap.put(phoneFull, person)
        }

        return person
    }

    suspend fun clearChatNotidication(threadId: String){
//        debugConfig.log(TAG,"clearChatNotidication ${threadId}")
        val chatNotificationId = threadId.hashCode()
        if (Build.VERSION.SDK_INT < 24){   //MessagingStyle from api level 24
            with(NotificationManagerCompat.from(context)) {
                cancel(chatNotificationId)
            }
            return
        }

        val allActiveChatNotificationMap = findAllActiveChatNotification()

        with(NotificationManagerCompat.from(context)) {
            cancel(chatNotificationId)
        }

        var totalUnreadThreadNotification = 0
        var lastValidNotificationId : Int? = null
        var lastValidNotification : Notification? = null
        allActiveChatNotificationMap.forEach { entry ->
            val notificationId = entry.key
            val notification = entry.value
            if (notificationId != CHAT_SUMMARY_ID && notificationId != chatNotificationId){
                val oldMessagingStyle = restoreMessagingStyle(notificationId)
                oldMessagingStyle?.let {
                    totalUnreadThreadNotification += 1
                    lastValidNotificationId = notificationId
                    lastValidNotification = notification
                }
            }
        }


//        debugConfig.log(TAG, "totalUnreadThread ${totalUnreadThreadNotification}")
//        debugConfig.log(TAG, "lastValidNotificationId ${lastValidNotificationId ?: 0}")

        if(totalUnreadThreadNotification <= 1){
            with(NotificationManagerCompat.from(context)) {
                cancel(CHAT_SUMMARY_ID)
            }


            if (lastValidNotificationId != null && lastValidNotification != null){
                with(NotificationManagerCompat.from(context)) {
                    // notificationId is a unique int for each notification that you must define
                    notify(lastValidNotificationId!!, lastValidNotification!!)
                }
            }
        }else {
//            debugConfig.log(TAG, "totalUnreadThread > 1")
            val totalUnreadMsg = messageRepository.countAllUnreadMsg(threadId)
            val totalUnreadThread = messageRepository.countAllUnreadThread(threadId)
            createSummaryNotification(totalUnreadThread, totalUnreadMsg, true)
        }

    }

    suspend fun getThreadParticipantInfos(signalThread: SignalThread): List<ThreadParticipantInfo>{
        val cwmUsers = cwmUserRepository.getAllByListPhoneFull(signalThread.participants)
        val threadParticipantInfos = cwmUsers.map { cwmUser ->
            val contact: Contact? = contactRepository.findOneContactByPhoneFull(cwmUser.phoneFull)
            ThreadParticipantInfo(
                phoneFull = cwmUser.phoneFull,
                contactName = contact?.name ?: "",
                userId = cwmUser.userId ?: "",
                username = cwmUser.username ?: "",
                avatar = cwmUser.avatar ?: "",
                firstName = cwmUser.firstName ?: "",
                lastName = cwmUser.lastName ?: "",
                isMyAcc = cwmUser.isMyAcc
            )
        }

        return threadParticipantInfos
    }

    // TEST OK WITH SDK 19
    // SDK_INT < 24 -> MessagingStyle will not be shown
    suspend fun createCommonMessageNotification(signalThread: SignalThread, signalMsgExtList: List<SignalMsgExt>){
        /*
           Set up a regular activity PendingIntent
           Build a PendingIntent with a back stack
       */
        // Create an Intent for the activity you want to start
        val resultIntent = Intent(context, MainActivity::class.java).apply {
            setAction("${Config.IntentAction.OPEN_CHAT}_${signalThread.threadId}")
            putExtra(ConversationActivity.EXTRA_THREAD_ID, signalThread.threadId)
        }
        // Create the TaskStackBuilder
        val pendingIntent: PendingIntent? = TaskStackBuilder.create(context).run {
            // Add the intent, which inflates the back stack
            addNextIntentWithParentStack(resultIntent)
            // Get the PendingIntent containing the entire back stack
            getPendingIntent(0,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }




        var threadName = signalThread.threadName
        if (signalThread.threadType == CwmSignalMsg.SIGNAL_THREAD_TYPE.SOLO.number){
            var svName : String? = null
            if (!signalThread.threadName.isNullOrEmpty()){
                svName = signalThread.threadName
            }
            val person = createPerson(signalThread.phoneFull, svName)
            threadName = person.name.toString()
        }

        val messageNotificationBuilder = NotificationCompat.Builder(context, CHAT_CHANNEL_ID)
            .setContentTitle(threadName)
            .setSmallIcon(R.drawable.ic_app_name)       //https://stackoverflow.com/questions/45318614/why-is-my-smallicon-for-notifications-always-greyed-out
            .setColor(NOTIFICATION_COLOR)
            .setNumber(signalThread.unreadMsgs.toInt())
            .setGroup(CHAT_GROUP_KEY_WORK)
            .setGroupSummary(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent) // Set the intent that will fire when the user taps the notification
            .setAutoCancel(true)    //automatically removes the notification when the user taps it.
            .setOnlyAlertOnce(true)

        val chatNotificationId = signalThread.threadId.hashCode()
        var inboxStyle : NotificationCompat.InboxStyle? = null
        if (Build.VERSION.SDK_INT >= 23){
            inboxStyle = restoreInboxStyle(chatNotificationId)
        }
        if (inboxStyle == null){
            inboxStyle = NotificationCompat.InboxStyle()
            inboxStyle.setBigContentTitle(threadName)
        }

        val threadParticipantInfos = getThreadParticipantInfos(signalThread)
        for (signalMsgExt in signalMsgExtList){
            val content = when(signalMsgExt.imType) {
                CwmSignalMsg.SIGNAL_IM_TYPE.IM.number -> signalMsgExt.contentIMMessage ?: ""
                CwmSignalMsg.SIGNAL_IM_TYPE.MULTIMEDIA.number -> signalMsgExt.getLastSignalMultimediaMessageContent(context)
                CwmSignalMsg.SIGNAL_IM_TYPE.GROUP_THREAD_NOTIFICATION.number -> {
                    val signalGroupThreadNotificationMessageProto = signalMsgExt.contentSignalGroupThreadNotificationMessage
                    signalGroupThreadNotificationMessageProto?.let {
                        val executor = signalGroupThreadNotificationMessageProto.executor
                        val executorInfo = threadParticipantInfos.firstOrNull { participantInfo -> participantInfo.phoneFull.equals(executor) }

                        val creator = signalGroupThreadNotificationMessageProto.creator
                        val creatorInfo = threadParticipantInfos.firstOrNull { participantInfo -> participantInfo.phoneFull.equals(creator) }

                        val targetMembers = signalGroupThreadNotificationMessageProto.targetMembersList
                        val targetMemberNames = targetMembers.map { targetMember ->
                            val targetMemberInfo = threadParticipantInfos.firstOrNull { participantInfo -> participantInfo.phoneFull.equals(targetMember) }
                            return@map targetMemberInfo?.getName(context) ?: targetMember
                        }
                        val targetMemberInfoStr = targetMemberNames.joinToString(separator = ", ")

                        signalMsgExt.groupExecutorInfo = executorInfo
                        signalMsgExt.groupCreatorInfo = creatorInfo
                        signalMsgExt.groupTargetMemberInfoStr = targetMemberInfoStr
                    }
                    signalMsgExt.getSignalGroupThreadNotificationMessageContent(context)
                }
                else  -> ""
            }
            if (signalThread.threadType == CwmSignalMsg.SIGNAL_THREAD_TYPE.GROUP.number){
                if (!signalMsgExt.from.equals(Constants.ServerName.ServerEventName)){
                    var svName = signalMsgExt.fromFirstName
                    if (!signalMsgExt.fromLastName.isNullOrEmpty()){
                        svName += " ${signalMsgExt.fromLastName}"
                    }

                    val sender = createPerson(signalMsgExt.from, svName)
                    inboxStyle.addLine("${sender.name}: ${content}")
                }else{
                    inboxStyle.addLine(content)
                }
            }else{
                inboxStyle.addLine(content)
            }
        }

        inboxStyle.setSummaryText(context.getString(R.string.notification_detail_text, signalThread.unreadMsgs))
        messageNotificationBuilder.setStyle(inboxStyle)

        with(NotificationManagerCompat.from(context)) {
            // notificationId is a unique int for each notification that you must define
            notify(chatNotificationId, messageNotificationBuilder.build())
        }
    }

    // SDK_INT >= 24 -> MessagingStyle will be shown
    suspend fun createSummaryNotification(totalUnreadThread: Long, totalUnreadMsg: Long, silent: Boolean = false) {
        val sumtext = context.getString(R.string.notification_detail_text_multi_threads, totalUnreadMsg, totalUnreadThread)

        val inboxStyle = NotificationCompat.InboxStyle()
        inboxStyle.setBigContentTitle(context.getString(R.string.app_name))
        inboxStyle.setSummaryText(sumtext)

        val summaryNotificationBuilder = NotificationCompat.Builder(context, CHAT_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.app_name))
            .setSmallIcon(R.drawable.ic_app_name)
            .setNumber(totalUnreadMsg.toInt())
            .setStyle(inboxStyle)
            .setColor(NOTIFICATION_COLOR)
            .setGroup(CHAT_GROUP_KEY_WORK)
            .setGroupSummary(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
//            .setContentIntent(pendingIntent) // Set the intent that will fire when the user taps the notification
            .setAutoCancel(true)    //automatically removes the notification when the user taps it.
//            .setOnlyAlertOnce(true)

        if (silent) {
            summaryNotificationBuilder.setPriority(NotificationCompat.PRIORITY_LOW);
            summaryNotificationBuilder.setVibrate(longArrayOf(0, 0))
            summaryNotificationBuilder.setSilent(true)
        }else{
            summaryNotificationBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);
        }

        with(NotificationManagerCompat.from(context)) {
            notify(CHAT_SUMMARY_ID, summaryNotificationBuilder.build())
        }
    }

    // SDK_INT >= 24 -> MessagingStyle will be shown
    suspend fun createMessageNotification(signalThread: SignalThread, signalMsgExtList: List<SignalMsgExt>){
        /*
           Set up a regular activity PendingIntent
           Build a PendingIntent with a back stack
       */
        // Create an Intent for the activity you want to start
        val resultIntent = Intent(context, MainActivity::class.java).apply {
            setAction("${Config.IntentAction.OPEN_CHAT}_${signalThread.threadId}")
            putExtra(ConversationActivity.EXTRA_THREAD_ID, signalThread.threadId)
        }
        // Create the TaskStackBuilder
        val pendingIntent: PendingIntent = TaskStackBuilder.create(context).run {
            // Add the intent, which inflates the back stack
            addNextIntentWithParentStack(resultIntent)
            // Get the PendingIntent containing the entire back stack
            getPendingIntent(0,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

//        debugConfig.log(TAG, "createMessageNotification: threadName - ${signalThread.threadName}")

        //TODO - UPDATE notification if contact update

        var threadName = signalThread.threadName
        if (signalThread.threadType == CwmSignalMsg.SIGNAL_THREAD_TYPE.SOLO.number){
            var svName : String? = null
            if (!signalThread.threadName.isNullOrEmpty()){
                svName = signalThread.threadName
            }
            val person = createPerson(signalThread.phoneFull,svName)
            threadName = person.name.toString()
        }

        val chatNotificationId = signalThread.threadId.hashCode()

        val now = System.currentTimeMillis()

        //TODO - GET threadAvatar from signal thread
        val threadAvatar = AvatarGenerator.AvatarBuilder(context)
            .setLabel(threadName)
            .setAvatarSize(NOTIFICATION_AVATAR_SIZE)
            .setTextSize(NOTIFICATION_AVATAR_TEXTSIZE)
            .setBackgroundColor(RandomColors(AvatarConstants.COLOR700).getColor(threadName))
            .toCircle()
            .build()



        val messageNotificationBuilder = NotificationCompat.Builder(context, CHAT_CHANNEL_ID)
            .setContentTitle(threadName)
            .setSmallIcon(R.drawable.ic_app_name)       //https://stackoverflow.com/questions/45318614/why-is-my-smallicon-for-notifications-always-greyed-out
            .setAutoCancel(true)    //automatically removes the notification when the user taps it.
            .setNumber(signalThread.unreadMsgs.toInt())
            .setColor(NOTIFICATION_COLOR)
            .setGroupSummary(false)
            .setWhen(now)
            .setShowWhen(true)
            .setContentIntent(pendingIntent) // Set the intent that will fire when the user taps the notification
//            .setSortKey("${Long.MAX_VALUE - now.time}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setGroup(CHAT_GROUP_KEY_WORK)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)  //meaning that all children notification in a group should be silenced (no sound or vibration) even if they would otherwise make sound or vibrate. Use this constant to mute this notification if this notification is a group child. This must be applied to all children notifications you want to mute.
            .setSubText(context.getString(R.string.notification_detail_text, signalThread.unreadMsgs))
            .setLargeIcon(threadAvatar.bitmap)

        val messagingStyle = NotificationCompat.MessagingStyle("")
        if (signalThread.threadType == CwmSignalMsg.SIGNAL_THREAD_TYPE.GROUP.number){
            messagingStyle.conversationTitle = threadName
            messagingStyle.isGroupConversation = true
        }

        val msgs = arrayListOf<NotificationCompat.MessagingStyle.Message>()
        val oldMessagingStyle = restoreMessagingStyle(chatNotificationId)
        oldMessagingStyle?.messages?.forEach {
            var mSender = it.person
            if (mSender != null && mSender.key != null){
                if (mSender.name.toString().equals(mSender.key)){
                    mSender = createPerson(mSender.key!!, null)
                }
            }

            val msg = NotificationCompat.MessagingStyle.Message(
                it.text,
                it.timestamp,
                mSender
            )
            msgs.add(msg)
        }

        val threadParticipantInfos = getThreadParticipantInfos(signalThread)
        for (signalMsgExt in signalMsgExtList){
            val content = when(signalMsgExt.imType) {
                CwmSignalMsg.SIGNAL_IM_TYPE.IM.number -> signalMsgExt.contentIMMessage ?: ""
                CwmSignalMsg.SIGNAL_IM_TYPE.MULTIMEDIA.number -> signalMsgExt.getLastSignalMultimediaMessageContent(context)
                CwmSignalMsg.SIGNAL_IM_TYPE.GROUP_THREAD_NOTIFICATION.number -> {
                    val signalGroupThreadNotificationMessageProto = signalMsgExt.contentSignalGroupThreadNotificationMessage
                    signalGroupThreadNotificationMessageProto?.let {
                        val executor = signalGroupThreadNotificationMessageProto.executor
                        val executorInfo = threadParticipantInfos.firstOrNull { participantInfo -> participantInfo.phoneFull.equals(executor) }

                        val creator = signalGroupThreadNotificationMessageProto.creator
                        val creatorInfo = threadParticipantInfos.firstOrNull { participantInfo -> participantInfo.phoneFull.equals(creator) }

                        val targetMembers = signalGroupThreadNotificationMessageProto.targetMembersList
                        val targetMemberNames = targetMembers.map { targetMember ->
                            val targetMemberInfo = threadParticipantInfos.firstOrNull { participantInfo -> participantInfo.phoneFull.equals(targetMember) }
                            return@map targetMemberInfo?.getName(context) ?: targetMember
                        }
                        val targetMemberInfoStr = targetMemberNames.joinToString(separator = ", ")

                        signalMsgExt.groupExecutorInfo = executorInfo
                        signalMsgExt.groupCreatorInfo = creatorInfo
                        signalMsgExt.groupTargetMemberInfoStr = targetMemberInfoStr
                    }
                    signalMsgExt.getSignalGroupThreadNotificationMessageContent(context)
                }
                else  -> ""
            }
            var svName = signalMsgExt.fromFirstName
            if (!signalMsgExt.fromLastName.isNullOrEmpty()){
                svName += " ${signalMsgExt.fromLastName}"
            }
            val sender = createPerson(signalMsgExt.from, svName)
            val msg = NotificationCompat.MessagingStyle.Message(
                content,
                signalMsgExt.serverDate,
                sender
            )
            msgs.add(msg)
        }

        msgs.sortedBy { it.timestamp }
        msgs.forEach{ msg -> messagingStyle.addMessage(msg) }

        messageNotificationBuilder.setStyle(messagingStyle)


        if (Build.VERSION.SDK_INT >= 29){   // SDK_INT >= 29 -> Support ShortcutInfo
            val id = "${NOTIFICATION_SHORCUTINFO_PREFIX}_${signalThread.threadId}"
            val shortcutBuilder =
                ShortcutInfoCompat.Builder(context, id)
                    .setIntent(Intent(Intent.ACTION_DEFAULT))
                    .setIntent(resultIntent)
                    .setLongLived(true)
                    .setLocusId(LocusIdCompat(id))

            if (signalThread.threadType == CwmSignalMsg.SIGNAL_THREAD_TYPE.SOLO.number){
                var svName : String? = null
                if (!signalThread.threadName.isNullOrEmpty()){
                    svName = signalThread.threadName
                }
                val person = createPerson(signalThread.phoneFull,svName)

                //TODO - REMOVE OLD SHORTCUT BUILDER IF CONTACT UPDATE

                shortcutBuilder
                        .setShortLabel(person.name.toString())
                        .setLongLabel(person.name.toString())
                        .setPerson(person)
                        .setIcon(person.icon)

            }else if (signalThread.threadType == CwmSignalMsg.SIGNAL_THREAD_TYPE.GROUP.number){
                val person = Person.Builder()
                    .setName(signalThread.threadName)
                    .setIcon(IconCompat.createWithBitmap(threadAvatar.bitmap))
                    .setKey(signalThread.threadId)
                    .build()

                shortcutBuilder
                    .setShortLabel(signalThread.threadName)
                    .setLongLabel(signalThread.threadName)
                    .setPerson(person)
                    .setIcon(person.icon)
            }


            val shortcut = shortcutBuilder.build()
            ShortcutManagerCompat.pushDynamicShortcut(
                context,
                shortcut
            )
            messageNotificationBuilder.setShortcutInfo(shortcut)
        }



        val dismissIntent = Intent(context, NotificationMessageDismissReceiver::class.java).apply {
            putExtra(ConversationActivity.EXTRA_THREAD_ID, signalThread.threadId)
        }
        messageNotificationBuilder.setDeleteIntent(
            PendingIntent.getBroadcast(context, 0, dismissIntent, PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        )


        val markReadIntent = Intent(context, MarkMessageReadReceiver::class.java).apply {
            putExtra(ConversationActivity.EXTRA_THREAD_ID, signalThread.threadId)
        }
        val markReadAction = NotificationCompat.Action.Builder(
            R.drawable.ic_select_24,
            context.getString(R.string.notification_mark_as_read),
            PendingIntent.getBroadcast(context, 0, markReadIntent, PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        )
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ)
            .setShowsUserInterface(false)
            .build()

        messageNotificationBuilder.addAction(markReadAction)


        val replyAction = NotificationCompat.Action.Builder(
            R.drawable.ic_reply_24,
            context.getString(R.string.notification_reply),
            PendingIntent.getActivity(context, 0, resultIntent, PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT )
        )
            .setAllowGeneratedReplies(true)
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
            .setShowsUserInterface(false)
            .build()
        messageNotificationBuilder.addAction(replyAction)

//        val canbuble = canBubble()
//        val canbuble = true
//        debugConfig.log(TAG,"canbuble ${canbuble}")
//
//        if (canbuble){
//            val chatIntent = Intent(context, ConversationActivity::class.java).apply {
//                setAction("${Config.IntentAction.OPEN_CHAT}_${signalThread.threadId}")
//                putExtra(ConversationActivity.EXTRA_THREAD_ID, signalThread.threadId)
//            }
//
//            val bubbleBuilder = NotificationCompat.BubbleMetadata.Builder(
//                PendingIntent.getActivity(context, 0, chatIntent,PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT ),
//                IconCompat.createWithBitmap(threadAvatar.bitmap)
//            )
//            bubbleBuilder.setSuppressNotification(false)
//            bubbleBuilder.setAutoExpandBubble(false)
//            bubbleBuilder.setDesiredHeight(context.resources.getDimensionPixelSize(R.dimen.bubble_height))
//            messageNotificationBuilder.setBubbleMetadata(bubbleBuilder.build())
//        }

        with(NotificationManagerCompat.from(context)) {
            // notificationId is a unique int for each notification that you must define
            notify(chatNotificationId, messageNotificationBuilder.build())
        }
    }


    fun canBubble(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            with(NotificationManagerCompat.from(context)) {
                val channel = getNotificationChannel(CHAT_CHANNEL_ID)
                channel?.let {
                    return it.canBubble()
                }
            }
        }

       return false
    }


}