package com.csc306.coursework.scheduler

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context

object NewArticlesScheduler {

    private const val JOB_ID = 0
    private const val JOB_PERIOD: Long = 1000 * 60 * 60 // 1 hour

    fun start(context: Context) {
        val service = ComponentName(context, NewArticlesService::class.java)
        val job: JobInfo = JobInfo.Builder(JOB_ID, service)
            .setPeriodic(JOB_PERIOD)
            .setPersisted(true)
            .build()
        val jobScheduler: JobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        jobScheduler.schedule(job)
    }

    fun stop(context: Context) {
        val jobScheduler: JobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        jobScheduler.cancel(JOB_ID)
    }

}