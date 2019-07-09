/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.riotx.features.home.room.detail.timeline.helper

import android.text.format.Formatter
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import im.vector.matrix.android.api.session.content.ContentUploadStateTracker
import im.vector.riotx.R
import im.vector.riotx.core.di.ActiveSessionHolder
import im.vector.riotx.features.media.ImageContentRenderer
import javax.inject.Inject

class ContentUploadStateTrackerBinder @Inject constructor(private val activeSessionHolder: ActiveSessionHolder) {

    private val updateListeners = mutableMapOf<String, ContentUploadStateTracker.UpdateListener>()

    fun bind(eventId: String,
             mediaData: ImageContentRenderer.Data,
             progressLayout: ViewGroup) {

        activeSessionHolder.getActiveSession().also { session ->
            val uploadStateTracker = session.contentUploadProgressTracker()
            val updateListener = ContentMediaProgressUpdater(progressLayout, mediaData)
            updateListeners[eventId] = updateListener
            uploadStateTracker.track(eventId, updateListener)
        }
    }

    fun unbind(eventId: String) {
        activeSessionHolder.getActiveSession().also { session ->
            val uploadStateTracker = session.contentUploadProgressTracker()
            updateListeners[eventId]?.also {
                uploadStateTracker.untrack(eventId, it)
            }
        }
    }

}

private class ContentMediaProgressUpdater(private val progressLayout: ViewGroup,
                                          private val mediaData: ImageContentRenderer.Data) : ContentUploadStateTracker.UpdateListener {

    override fun onUpdate(state: ContentUploadStateTracker.State) {
        when (state) {
            is ContentUploadStateTracker.State.Idle                -> handleIdle(state)
            is ContentUploadStateTracker.State.EncryptingThumbnail -> handleEncryptingThumbnail(state)
            is ContentUploadStateTracker.State.UploadingThumbnail  -> handleProgressThumbnail(state)
            is ContentUploadStateTracker.State.Encrypting          -> handleEncrypting(state)
            is ContentUploadStateTracker.State.Uploading           -> handleProgress(state)
            is ContentUploadStateTracker.State.Failure             -> handleFailure(state)
            is ContentUploadStateTracker.State.Success             -> handleSuccess(state)
        }
    }

    private fun handleIdle(state: ContentUploadStateTracker.State.Idle) {
        if (mediaData.isLocalFile()) {
            progressLayout.isVisible = true
            val progressBar = progressLayout.findViewById<ProgressBar>(R.id.mediaProgressBar)
            val progressTextView = progressLayout.findViewById<TextView>(R.id.mediaProgressTextView)
            progressBar?.isVisible = true
            progressBar?.isIndeterminate = true
            progressBar?.progress = 0
            progressTextView?.text = progressLayout.context.getString(R.string.send_file_step_idle)
        } else {
            progressLayout.isVisible = false
        }
    }

    private fun handleEncryptingThumbnail(state: ContentUploadStateTracker.State.EncryptingThumbnail) {
        doHandleEncrypting(R.string.send_file_step_encrypting_thumbnail)
    }

    private fun handleProgressThumbnail(state: ContentUploadStateTracker.State.UploadingThumbnail) {
        doHandleProgress(R.string.send_file_step_sending_thumbnail, state.current, state.total)
    }

    private fun handleEncrypting(state: ContentUploadStateTracker.State.Encrypting) {
        doHandleEncrypting(R.string.send_file_step_encrypting_file)
    }

    private fun handleProgress(state: ContentUploadStateTracker.State.Uploading) {
        doHandleProgress(R.string.send_file_step_sending_file, state.current, state.total)
    }

    private fun doHandleEncrypting(resId: Int) {
        progressLayout.visibility = View.VISIBLE
        val progressBar = progressLayout.findViewById<ProgressBar>(R.id.mediaProgressBar)
        val progressTextView = progressLayout.findViewById<TextView>(R.id.mediaProgressTextView)
        progressBar?.isIndeterminate = true
        progressTextView?.text = progressLayout.context.getString(resId)
    }

    private fun doHandleProgress(resId: Int, current: Long, total: Long) {
        progressLayout.visibility = View.VISIBLE
        val percent = 100L * (current.toFloat() / total.toFloat())
        val progressBar = progressLayout.findViewById<ProgressBar>(R.id.mediaProgressBar)
        val progressTextView = progressLayout.findViewById<TextView>(R.id.mediaProgressTextView)
        progressBar?.isVisible = true
        progressBar?.isIndeterminate = false
        progressBar?.progress = percent.toInt()
        progressTextView?.text = progressLayout.context.getString(resId,
                Formatter.formatShortFileSize(progressLayout.context, current),
                Formatter.formatShortFileSize(progressLayout.context, total))
    }

    private fun handleFailure(state: ContentUploadStateTracker.State.Failure) {
        progressLayout.visibility = View.VISIBLE
        val progressBar = progressLayout.findViewById<ProgressBar>(R.id.mediaProgressBar)
        val progressTextView = progressLayout.findViewById<TextView>(R.id.mediaProgressTextView)
        progressBar?.isVisible = false
        // TODO Red text
        progressTextView?.text = state.throwable.localizedMessage
    }

    private fun handleSuccess(state: ContentUploadStateTracker.State.Success) {
        progressLayout.visibility = View.GONE
    }
}
