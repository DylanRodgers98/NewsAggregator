package com.csc306.coursework.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class StartSchedulerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            NewArticlesScheduler.start(context)
        }
    }

}